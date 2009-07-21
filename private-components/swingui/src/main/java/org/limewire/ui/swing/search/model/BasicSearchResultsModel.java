package org.limewire.ui.swing.search.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.DisposalListener;
import org.limewire.ui.swing.filter.FilterDebugger;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.DownloadExceptionHandler;
import org.limewire.ui.swing.util.PropertiableHeadings;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransactionList;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor.Event;
import ca.odell.glazedlists.util.concurrent.Lock;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Provider;

/**
 * The default implementation of SearchResultsModel containing the results of
 * a search.  This assembles search results into grouped, filtered, and sorted
 * lists, provides access to details about the search request, and handles 
 * requests to download a search result.
 */
class BasicSearchResultsModel implements SearchResultsModel, VisualSearchResultStatusListener {
    private static final Log LOG = LogFactory.getLog(BasicSearchResultsModel.class);
    
    /** Filter debugger associated with this model. */
    private final FilterDebugger<VisualSearchResult> filterDebugger;

    /** Descriptor containing search details. */
    private final SearchInfo searchInfo;
    
    /** Search request object. */
    private final Search search;

    /** Core download manager. */
    private final DownloadListManager downloadListManager;

    /** Download exception handler. */
    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;

    /** Total number of search results. */
    private int resultCount;

    /** List of search results grouped by URN. */
    private final EventList<VisualSearchResult> groupedUrnResults;
    
    /** A TransactionList for upstream changes. */
    private final TransactionList<VisualSearchResult> transactionList;

    /** Filtered list of grouped search results. */
    private final FilterList<VisualSearchResult> filteredResultList;

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

    /** Current matcher editor for filtered search results. */
    private MatcherEditor<VisualSearchResult> filterEditor;

    /** Listener for filter editor. */
    private MatcherEditor.Listener<VisualSearchResult> filterEditorListener;

    /** Matcher editor for visible search results. */
    private final VisibleMatcherEditor visibleEditor = new VisibleMatcherEditor();
    
    private List<DisposalListener> disposalListeners = new ArrayList<DisposalListener>();
    
    /** Headings to create search results with. */
    private final Provider<PropertiableHeadings> propertiableHeadings;
    
    /** A list of listeners for changes. */
    private final List<VisualSearchResultStatusListener> changeListeners;
    
    /** The object that queues up list changes & applies them in bulk. */
    private final ListQueuer listQueuer = new ListQueuer();
    
    /** Comparator that searches through the list of results & finds them based on URN. */
    private final UrnResultFinder resultFinder = new UrnResultFinder();
    
    //TODO Using this to fix a case where events are coming in for items no longer in the list after a clear.
    //We should remove this after the release and fix the root cause, that DownloadListeners for visual search results 
    //are not being removed from the downloaders after search tabs are removed or refreshed. 
    private boolean cleared = false;

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and services.
     */
    public BasicSearchResultsModel(SearchInfo searchInfo, Search search, 
            Provider<PropertiableHeadings> propertiableHeadings,
            DownloadListManager downloadListManager,
            Provider<DownloadExceptionHandler> downloadExceptionHandler) {
        
        this.searchInfo = searchInfo;
        this.search = search;
        this.downloadListManager = downloadListManager;
        this.downloadExceptionHandler = downloadExceptionHandler;
        this.propertiableHeadings = propertiableHeadings;
        this.changeListeners = new ArrayList<VisualSearchResultStatusListener>(3);
        
        // Create filter debugger.
        filterDebugger = new FilterDebugger<VisualSearchResult>();
        
        // Underlying list, with no locks -- always accessed on EDT.
        groupedUrnResults = new BasicEventList<VisualSearchResult>(new ReadWriteLock() {
            private Lock noopLock = new Lock() {
                @Override public void lock() {}
                @Override public boolean tryLock() { return true; }
                @Override public void unlock() {}
            };
            
            @Override
            public Lock readLock() {
                return noopLock;
            }
            
            @Override
            public Lock writeLock() {
                return noopLock;
            }
        });
        
        transactionList = GlazedListsFactory.transactionList(groupedUrnResults);
        
        // Create filtered list. 
        filteredResultList = GlazedListsFactory.filterList(transactionList);
        
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
        
        groupedUrnResults.dispose();
        
        notifyDisposalListeners();
    }
    
    @Override
    public SearchCategory getFilterCategory() {
        return searchInfo.getSearchCategory();
    }
    
    @Override
    public FilterDebugger<VisualSearchResult> getFilterDebugger() {
        return filterDebugger;
    }
    
    @Override
    public EventList<VisualSearchResult> getUnfilteredList() {
        return transactionList;
    }
    
    @Override
    public EventList<VisualSearchResult> getFilteredList() {
        return filteredResultList;
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
    public String getSearchTitle() {
        return searchInfo.getTitle();
    }
    
    @Override
    public int getResultCount() {
        return resultCount;
    }
    
    @Override
    public SearchType getSearchType() {
        return searchInfo.getSearchType();
    }

    /**
     * Returns a list of filtered results.
     */
    @Override
    public EventList<VisualSearchResult> getFilteredSearchResults() {
        return filteredResultList;
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
            updateSortedList();
        }
    }
    
    /**
     * Updates sorted list of visible results.  This method is called when the
     * selected category is changed.
     */
    private void updateSortedList() {
        // Create visible and sorted lists if necessary.
        if (visibleResultList == null) {
            sortedResultList = GlazedListsFactory.sortedList(filteredResultList, sortOption != null ? SortFactory.getSortComparator(sortOption) : null);
            visibleResultList = GlazedListsFactory.filterList(sortedResultList, visibleEditor);
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
    
    @Override
    public void setFilterEditor(MatcherEditor<VisualSearchResult> editor) {
        // Remove listener from existing filter editor.
        if ((filterEditor != null) && (filterEditorListener != null)) {
            filterEditor.removeMatcherEditorListener(filterEditorListener);
        }
        
        // Create listener to handle changes to the filter.
        if (filterEditorListener == null) {
            filterEditorListener = new MatcherEditor.Listener<VisualSearchResult>() {
                @Override
                public void changedMatcher(Event<VisualSearchResult> matcherEvent) {
                    // Post Runnable on event queue to update filtered list for
                    // visible items.  This allows us to display child results
                    // whose parents become hidden due to filtering.
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            visibleEditor.update();
                        }
                    });
                }
            };
        }
        
        // Set filter editor and add listener.
        filterEditor = editor;
        filterEditor.addMatcherEditorListener(filterEditorListener);
        
        // Apply filter editor.
        filteredResultList.setMatcherEditor(filterEditor);
    }
    
    public void addResultListener(VisualSearchResultStatusListener vsrChangeListener) {
        changeListeners.add(vsrChangeListener);
    }
    
    @Override
    public void resultCreated(VisualSearchResult vsr) {
    }
    
    @Override
    public void resultsCleared() {
    }
    
    @Override
    public void resultChanged(VisualSearchResult vsr, String propertyName, Object oldValue, Object newValue) {
        // Scan through the list & find the item.
        URN urn = vsr.getUrn();
        int idx = Collections.binarySearch(groupedUrnResults, urn, resultFinder);
        //TODO clean up, see comment about cleared variable, and why it should be removed.
        assert cleared || idx >= 0;

        if(idx >=0) {
        
            VisualSearchResult existing = groupedUrnResults.get(idx);
            VisualSearchResult replaced = groupedUrnResults.set(idx, existing);
            assert cleared || replaced == vsr;
            if(replaced == vsr) {
                for(VisualSearchResultStatusListener listener : changeListeners) {
                    listener.resultChanged(vsr, propertyName, oldValue, newValue);
                }
            }
        }
    }
    
    @Override
    public void addSearchResult(SearchResult result) {
        if(result.getUrn() == null) {
            // Some results can be missing a URN, specifically
            // secure results.  For now, we drop these.
            // We should figure out a way to show them later on.
            return;
        }
        
//        LOG.debugf("Adding result urn: {0} EDT: {1}", result.getUrn(), SwingUtilities.isEventDispatchThread());
        try {
            listQueuer.add(result);
        } catch (Throwable th) {
            // Throw wrapper exception with detailed message.
            throw new RuntimeException(createMessageDetail("Problem adding result", result), th);
        }
    }
    
    @Override
    public void addSearchResults(Collection<? extends SearchResult> results) {
        boolean ok = true;
        // make certain there's nothing w/o a URN in here
        for(SearchResult result : results) {
            if(result.getUrn() == null) {
                // crap, we need to really work on this list & remove bad items.
                ok = false;
                break;
            }
        }
        
        // filter out any items w/o a URN
        if(!ok) {
            ArrayList<SearchResult> cleanup = new ArrayList<SearchResult>(results);
            for(int i = cleanup.size() - 1; i >= 0; i--) {
                if(cleanup.get(i).getUrn() == null) {
                    cleanup.remove(i);
                }
            }
            results = cleanup;
        }        
        
        listQueuer.addAll(results);
    }
    
    /**
     * Removes all results from the model
     */
    public void clear(){
        listQueuer.clear();
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
            BasicDownloadState state = BasicDownloadState.fromState(di.getState());
            if(state != null) {
                vsr.setDownloadState(state);
            }
        } catch (final DownloadException e) {
            if (e.getErrorCode() == DownloadException.ErrorCode.FILE_ALREADY_DOWNLOADING) {
                DownloadItem downloadItem = downloadListManager.getDownloadItem(vsr.getUrn());
                if (downloadItem != null) {
                    downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                    BasicDownloadState state = BasicDownloadState.fromState(downloadItem.getState());
                    if(state != null) {
                        vsr.setDownloadState(state);
                    }
                    if (saveFile != null) {
                        try {
                            // Update save file in DownloadItem.
                            downloadItem.setSaveFile(saveFile, true);
                        } catch (DownloadException ex) {
                            LOG.infof(ex, "Unable to relocate downloading file {0}", ex.getMessage());
                        }
                    }
                }
            } else {
                downloadExceptionHandler.get().handleDownloadException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws DownloadException {
                        DownloadItem di = downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, overwrite);
                        di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                        BasicDownloadState state = BasicDownloadState.fromState(di.getState());
                        if(state != null) {
                            vsr.setDownloadState(state);
                        }
                    }

                    @Override
                    public void downloadCanceled(DownloadException ignored) {
                        //nothing to do                        
                    }

                }, e, true);
            }
        }
    }
    
    /**
     * Returns a detailed message including the specified prefix and search 
     * result.
     */
    private String createMessageDetail(String prefix, SearchResult result) {
        StringBuilder buf = new StringBuilder(prefix);
        
        buf.append(", searchCategory=").append(searchInfo.getSearchCategory());
        buf.append(", resultCategory=").append(result.getCategory());
        buf.append(", spam=").append(result.isSpam());
        buf.append(", unfilteredSize=").append(getUnfilteredList().size());
        buf.append(", filteredSize=").append(getFilteredList().size());
        buf.append(", EDT=").append(SwingUtilities.isEventDispatchThread());
        buf.append(", filters=").append(filterDebugger.getFilterString());
        
        return buf.toString();
    }
    
    /**
     * Returns true if the specified search result is allowed by the current
     * filter editor.  This means the result is a member of the filtered list.
     */
    private boolean isFilterMatch(VisualSearchResult vsr) {
        if (filterEditor != null) {
            return filterEditor.getMatcher().matches(vsr);
        }
        return true;
    }

    /**
     * A matcher editor used to filter visible search results. 
     */
    private class VisibleMatcherEditor extends AbstractMatcherEditor<VisualSearchResult> {
        
        public VisibleMatcherEditor() {
            currentMatcher = new Matcher<VisualSearchResult>() {
                @Override
                public boolean matches(VisualSearchResult item) {
                    // Determine whether item has parent, and parent is hidden
                    // due to filtering.  If so, we treat the item as visible.
                    VisualSearchResult parent = item.getSimilarityParent();
                    boolean parentHidden = (parent != null) && !isFilterMatch(parent);
                    return item.isVisible() || parentHidden;
                }
            };
        }
        
        /**
         * Updates the list by firing a matcher change event to registered
         * listeners.
         */
        public void update() {
            fireChangedMatcher(new MatcherEditor.Event<VisualSearchResult>(
                    this, Event.CHANGED, currentMatcher));
        }
    }

    private void addResultsInternal(Collection<? extends SearchResult> results) {
        transactionList.beginEvent(true);
        try {
            for(SearchResult result : results) {
                URN urn = result.getUrn();
                if(urn != null) {
                    int idx = Collections.binarySearch(groupedUrnResults, urn, resultFinder);
                    if(idx >= 0) {
                        SearchResultAdapter vsr = (SearchResultAdapter)groupedUrnResults.get(idx);
                        vsr.addNewSource(result);
                        groupedUrnResults.set(idx, vsr);
                        for(VisualSearchResultStatusListener listener : changeListeners) {
                            listener.resultChanged(vsr, "new-sources", null, null);
                        }
                    } else {
                        idx = -(idx + 1);
                        SearchResultAdapter vsr = new SearchResultAdapter(result, propertiableHeadings, this);
                        groupedUrnResults.add(idx, vsr);
                        for(VisualSearchResultStatusListener listener : changeListeners) {
                            listener.resultCreated(vsr);
                        }
                    }
                    resultCount += result.getSources().size();
                }
            }
        } finally {
            transactionList.commitEvent();
        }
    }

    @Override
    public void addDisposalListener(DisposalListener listener) {
        disposalListeners.add(listener);
    }

    @Override
    public void removeDisposalListener(DisposalListener listener) {
        disposalListeners.remove(listener);
    }
    
    private void notifyDisposalListeners(){
        for (DisposalListener listener : disposalListeners){
            listener.objectDisposed(this);
        }
    }
    
    private class ListQueuer implements Runnable {
        private final Object LOCK = new Object();
        private final List<SearchResult> queue = new ArrayList<SearchResult>();
        private final ArrayList<SearchResult> transferQ = new ArrayList<SearchResult>();
        private boolean scheduled = false;
        
        void add(SearchResult result) {
            synchronized(LOCK) {
                queue.add(result);
                schedule();
            }
        }
        
        void addAll(Collection<? extends SearchResult> results) {
            synchronized(LOCK) {
                queue.addAll(results);
                schedule();
            }
        }
        
        void clear() {
            synchronized(LOCK) {
                queue.clear();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        cleared = true;
                        groupedUrnResults.clear();
                        for(VisualSearchResultStatusListener listener : changeListeners) {
                            listener.resultsCleared();
                        }
                    }
                });
            }
        }
        
        private void schedule() {
            if(!scheduled) {
                scheduled = true;
                // purposely SwingUtilities & not SwingUtils so
                // that we force it to the back of the stack
                SwingUtilities.invokeLater(this);
            }
        }
        
        @Override
        public void run() {
            // move to transferQ inside lock
            transferQ.clear();
            synchronized(LOCK) {
                transferQ.addAll(queue);
                queue.clear();
            }
            
            // move to searchResults outside lock,
            // so we don't hold a lock while allSearchResults
            // triggers events.
            addResultsInternal(transferQ);
            transferQ.clear();
            
            synchronized(LOCK) {
                scheduled = false;
                // If the queue wasn't empty, we need to reschedule
                // ourselves, because something got added to the queue
                // without scheduling itself, since we had scheduled=true
                if(!queue.isEmpty()) {
                    schedule();
                }
            }
        }
    }
    
    private static class UrnResultFinder implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            return ((VisualSearchResult)o1).getUrn().compareTo(((URN)o2));
        }
    }
}
