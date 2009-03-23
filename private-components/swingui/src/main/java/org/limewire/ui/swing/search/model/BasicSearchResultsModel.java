package org.limewire.ui.swing.search.model;

import java.awt.EventQueue;
import java.io.File;
import java.util.Comparator;
import java.util.List;
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
 * a search.  This assembles search results into a list, and provides 
 * processing to sort and filter the list.
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

    /** Sorted list of grouped search results. */
    private final SortedList<VisualSearchResult> sortedResultList;

    /** Filtered and sorted list of grouped search results. */
    private final FilterList<VisualSearchResult> filteredResultList;

    /** Listener to handle search request events. */
    private SearchListener searchListener;

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and property values.
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
        
        // Create sorted and filtered lists.  We may want to reverse this - a
        // filtered, unsorted list may be more useful for some tables. 
        sortedResultList = GlazedListsFactory.sortedList(observableList, null);
        filteredResultList = GlazedListsFactory.filterList(sortedResultList);
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

    @Override
    public EventList<VisualSearchResult> getCategorySearchResults(SearchCategory searchCategory) {
        if (searchCategory == SearchCategory.ALL) {
            return filteredResultList;
        } else {
            return GlazedListsFactory.filterList(filteredResultList, 
                    new CategoryMatcher(searchCategory.getCategory()));
        }
    }
    
    @Override
    public void setSortOption(SortOption sortOption) {
        sortedResultList.setComparator(SortFactory.getSortComparator(sortOption));
    }
    
    @Override
    public void setFilterEditor(MatcherEditor<VisualSearchResult> editor) {
        filteredResultList.setMatcherEditor(editor);
    }

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
