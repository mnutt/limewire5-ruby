package org.limewire.ui.swing.search.model;

import java.awt.EventQueue;
import java.io.File;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.FunctionList.AdvancedFunction;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * The default implementation of SearchResultsModel containing the results of
 * a search.  This assembles search results into grouped, filtered, and sorted
 * lists, provides access to details about the search request, and handles 
 * requests to download a search result.
 */
class BasicSearchResultsModel implements SearchResultsModel {
    private final Log LOG = LogFactory.getLog(getClass());

    /** Descriptor containing search details. */
    private final SearchInfo searchInfo;
    
    /** Search request object. */
    private final Search search;

    /** Core download manager. */
    private final DownloadListManager downloadListManager;

    /** Save exception handler. */
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    /** List of all search results. */
    private final EventList<SearchResult> allSearchResults;

    /** Total number of search results. */
    private final AtomicInteger resultCount = new AtomicInteger();

    /** List of search results grouped by URN. */
    private final FunctionList<List<SearchResult>, VisualSearchResult> groupedUrnResults;

    /** Observable list of grouped search results. */
    private final ObservableElementList<VisualSearchResult> observableList;

    /** Filtered list of grouped search results. */
    private final FilterList<VisualSearchResult> filteredResultList;

    /** Cache of filtered lists by media category. */
    private final Map<Category, FilterList<VisualSearchResult>> categoryListMap = 
        new EnumMap<Category, FilterList<VisualSearchResult>>(Category.class);

    /** Listener to handle search request events. */
    private SearchListener searchListener;

    /** Current list of sorted and filtered results. */
    private SortedList<VisualSearchResult> sortedResultList;

    /** Current list of visible, sorted and filtered results. */
    private FilterList<VisualSearchResult> visibleResultList;

    /** Current selected search category. */
    private SearchCategory selectedCategory;
    
    /** Current sort option. */
    private SortOption sortOption;

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and services.
     */
    public BasicSearchResultsModel(SearchInfo searchInfo, Search search, 
            PropertiableHeadings propertiableHeadings,
            DownloadListManager downloadListManager,
            SaveLocationExceptionHandler saveLocationExceptionHandler) {
        
        this.searchInfo = searchInfo;
        this.search = search;
        this.downloadListManager = downloadListManager;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        
        // Create list of all search results.
        allSearchResults = new BasicEventList<SearchResult>();
        
        // Create list of search results grouped by URN.
        GroupingList<SearchResult> groupingListUrns = GlazedListsFactory.groupingList(
                allSearchResults, new UrnComparator());
        
        // Create list of visual search results where each element represents
        // a single group.
        groupedUrnResults = GlazedListsFactory.functionList(
                groupingListUrns, new SearchResultGrouper(resultCount, propertiableHeadings));
        
        // Create observable list that fires an event when results are modified.
        observableList = GlazedListsFactory.observableElementList(groupedUrnResults,
                GlazedLists.beanConnector(VisualSearchResult.class));
        
        // Create filtered list. 
        filteredResultList = GlazedListsFactory.filterList(observableList);
        
        // Initialize display category and sorted list.
        setSelectedCategory(searchInfo.getSearchCategory());
    }

    /**
     * Installs the specified search listener and starts the search.  The
     * search listener should handle search results by calling the 
     * <code>addSearchResult(SearchResult)</code> method.
     */
    @Override
    public void start(SearchListener searchListener) {
        if (searchListener == null) {
            throw new IllegalArgumentException("Search listener cannot be null");
        }
        
        // Install search listener.
        this.searchListener = searchListener;
        search.addSearchListener(searchListener);
        
        // Start search.
        search.start();
    }
    
    /**
     * Stops the search and removes the current search listener. 
     */
    @Override
    public void dispose() {
        // Stop search.
        search.stop();
        
        // Remove search listener.
        if (searchListener != null) {
            search.removeSearchListener(searchListener);
            searchListener = null;
        }
    }
    
    @Override
    public SearchCategory getSearchCategory() {
        return searchInfo.getSearchCategory();
    }
    
    @Override
    public String getSearchQuery() {
        return searchInfo.getSearchQuery();
    }
    
    @Override
    public int getResultCount() {
        return resultCount.get();
    }

    @Override
    public EventList<VisualSearchResult> getGroupedSearchResults() {
        return groupedUrnResults;
    }

    @Override
    public EventList<VisualSearchResult> getObservableSearchResults() {
        return observableList;
    }

    /**
     * Returns a list of filtered results for the specified search category.
     */
    @Override
    public EventList<VisualSearchResult> getCategorySearchResults(SearchCategory searchCategory) {
        if (searchCategory == SearchCategory.ALL) {
            return filteredResultList;
            
        } else {
            // Get filtered list from cache.
            Category category = searchCategory.getCategory();
            FilterList<VisualSearchResult> filteredList = categoryListMap.get(category);
            // Create filtered list if necessary, and add to cache.
            if (filteredList == null) {
                filteredList = GlazedListsFactory.filterList(filteredResultList, 
                        new CategoryMatcher(category));
                categoryListMap.put(category, filteredList);
            }
            return filteredList;
        }
    }

    /**
     * Returns a list of sorted and filtered results for the selected search
     * category and sort option.  Only visible results are included in the list.
     */
    @Override
    public EventList<VisualSearchResult> getSortedSearchResults() {
        return visibleResultList;
    }

    /**
     * Returns the selected search category.
     */
    @Override
    public SearchCategory getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Selects the specified search category.  If the selected category is
     * changed, this method updates the sorted list.
     */
    @Override
    public void setSelectedCategory(SearchCategory selectedCategory) {
        if (this.selectedCategory != selectedCategory) {
            this.selectedCategory = selectedCategory;
            // Dispose of existing visible and sorted lists.
            if (visibleResultList != null) {
                visibleResultList.dispose();
                sortedResultList.dispose();
            }
            // Update visible and sorted lists.
            EventList<VisualSearchResult> filteredList = getCategorySearchResults(selectedCategory);
            sortedResultList = GlazedListsFactory.sortedList(filteredList, null);
            visibleResultList = GlazedListsFactory.filterList(sortedResultList, new VisibleMatcher());
            sortedResultList.setComparator((sortOption != null) ? SortFactory.getSortComparator(sortOption) : null);
        }
    }
    
    /**
     * Sets the sort option.  This method updates the sorted list by changing 
     * the sort comparator.
     */
    @Override
    public void setSortOption(SortOption sortOption) {
        this.sortOption = sortOption;
        sortedResultList.setComparator((sortOption != null) ? SortFactory.getSortComparator(sortOption) : null);
    }
    
    /**
     * Sets the MatcherEditor used to filter search results. 
     */
    @Override
    public void setFilterEditor(MatcherEditor<VisualSearchResult> editor) {
        filteredResultList.setMatcherEditor(editor);
    }

    /**
     * Adds the specified search result to the results list.
     */
    @Override
    public void addSearchResult(SearchResult result) {
        if(result.getUrn() == null) {
            // Some results can be missing a URN, specifically
            // secure results.  For now, we drop these.
            // We should figure out a way to show them later on.
            return;
        }
        
        LOG.debugf("Adding result urn: {0} EDT: {1}", result.getUrn(), EventQueue.isDispatchThread());
        allSearchResults.add(result);
    }

    /**
     * Removes the specified search result from the results list.
     */
    @Override
    public void removeSearchResult(SearchResult result) {
        allSearchResults.remove(result);
    }
    
    /**
     * Initiates a download of the specified visual search result.
     */
    @Override
    public void download(VisualSearchResult vsr) {
        download(vsr, null);
    }
    
    /**
     * Initiates a download of the specified visual search result to the
     * specified save file.
     */
    @Override
    public void download(final VisualSearchResult vsr, File saveFile) {
        try {
            // Add download to manager.  If save file is specified, then set
            // overwrite to true because the user has already confirmed it.
            DownloadItem di = (saveFile == null) ?
                    downloadListManager.addDownload(search, vsr.getCoreSearchResults()) :
                    downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, true);
            
            // Add listener, and initialize download state.
            di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
            vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
            
        } catch (final SaveLocationException sle) {
            if (sle.getErrorCode() == SaveLocationException.LocationCode.FILE_ALREADY_DOWNLOADING) {
                DownloadItem downloadItem = downloadListManager.getDownloadItem(vsr.getUrn());
                if (downloadItem != null) {
                    downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                    vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    if (saveFile != null) {
                        try {
                            // Update save file in DownloadItem.
                            downloadItem.setSaveFile(saveFile, true);
                        } catch (SaveLocationException ex) {
                            LOG.infof(ex, "Unable to relocate downloading file {0}", ex.getMessage());
                        }
                    }
                }
            } else {
                saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws SaveLocationException {
                        DownloadItem di = downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, overwrite);
                        di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                        vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    }
                }, sle, true);
            }
        }
    }
    
    /**
     * A matcher used to filter search results by category.
     */
    private static class CategoryMatcher implements Matcher<VisualSearchResult> {
        private final Category category;
        
        public CategoryMatcher(Category category) {
            this.category = category;
        }

        @Override
        public boolean matches(VisualSearchResult vsr) {
            return (vsr.getCategory() == category);
        }
    }

    /**
     * A matcher used to filter visible search results. 
     */
    private static class VisibleMatcher implements Matcher<VisualSearchResult> {
        @Override
        public boolean matches(VisualSearchResult item) {
            return item.isVisible();
        }
    }
    
    /**
     * A comparator used to group search results by URN.
     */
    private static class UrnComparator implements Comparator<SearchResult> {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
            return o1.getUrn().compareTo(o2.getUrn());
        }
    }

    /**
     * A GlazedList function used to transform a group of search results into
     * a single VisualSearchResult.
     */
    private static class SearchResultGrouper implements
            AdvancedFunction<List<SearchResult>, VisualSearchResult> {
        private final AtomicInteger resultCount;
        private final PropertiableHeadings propertiableHeadings;

        public SearchResultGrouper(AtomicInteger resultCount, PropertiableHeadings propertiableHeadings) {
            this.resultCount = resultCount;
            this.propertiableHeadings = propertiableHeadings;
        }

        @Override
        public void dispose(List<SearchResult> sourceValue, VisualSearchResult transformedValue) {
            resultCount.addAndGet(-transformedValue.getSources().size());
        }

        @Override
        public VisualSearchResult evaluate(List<SearchResult> sourceValue) {
            VisualSearchResult adapter = new SearchResultAdapter(sourceValue, propertiableHeadings);

            resultCount.addAndGet(adapter.getSources().size());
            return adapter;
        }

        @Override
        public VisualSearchResult reevaluate(List<SearchResult> sourceValue,
                VisualSearchResult transformedValue) {
            resultCount.addAndGet(-transformedValue.getSources().size());
            ((SearchResultAdapter) transformedValue).update();
            resultCount.addAndGet(transformedValue.getSources().size());
            return transformedValue;
        }
    }
}
