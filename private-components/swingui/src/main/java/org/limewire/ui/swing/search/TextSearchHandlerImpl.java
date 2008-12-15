package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class TextSearchHandlerImpl implements SearchHandler {
    
    private final SearchFactory searchFactory;
    private final SearchResultsPanelFactory panelFactory;
    private final SearchNavigator searchNavigator;
    private final SearchResultsModelFactory searchResultsModelFactory;
    private final LifeCycleManager lifeCycleManager;
    private final GnutellaConnectionManager connectionManager;
    
    @Inject
    TextSearchHandlerImpl(SearchFactory searchFactory,
            SearchResultsPanelFactory panelFactory,
            SearchNavigator searchNavigator,
            SearchResultsModelFactory searchResultsModelFactory,
            LifeCycleManager lifeCycleManager, 
            GnutellaConnectionManager connectionManager) {
        this.searchNavigator = searchNavigator;
        this.searchFactory = searchFactory;
        this.panelFactory = panelFactory;
        this.searchResultsModelFactory = searchResultsModelFactory;
        this.lifeCycleManager = lifeCycleManager;
        this.connectionManager = connectionManager;
    }

    @Override
    public void doSearch(final SearchInfo info) {
        final SearchCategory searchCategory = info.getSearchCategory();

        final Search search = searchFactory.createSearch(new SearchDetails() {
            @Override
            public SearchCategory getSearchCategory() {
                return searchCategory;
            }
            
            @Override
            public String getSearchQuery() {
                return info.getQuery();
            }
            
            @Override
            public SearchType getSearchType() {
                return info.getSearchType();
            }
        });
        
        String panelTitle = info.getTitle();
        final SearchResultsModel model = searchResultsModelFactory.createSearchResultsModel();
        final SearchResultsPanel searchPanel =
            panelFactory.createSearchResultsPanel(
                info, model.getObservableSearchResults(), search);
        final SearchNavItem item =
            searchNavigator.addSearch(panelTitle, searchPanel, search);
        item.select();
        
        search.addSearchListener(new SearchListener() {
            @Override
            public void handleSearchResult(final SearchResult searchResult) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // Expect that the core properly filtered the result.
                        // That is, we won't see it if we didn't want to.
                        model.addSearchResult(searchResult);
                        
                        // We can update the source count here because
                        // we never expect things to be removed.
                        // Changes only happen on insertion.
                        // If removes ever happen, we'll need to switch
                        // to adding a ListEventListener to
                        // model.getVisualSearchResults.
                        item.sourceCountUpdated(model.getResultCount());
                    }
                });
            }

            @Override
            public void searchStarted() {
            }

            @Override
            public void searchStopped() {
            }

            @Override
            public void handleSponsoredResults(final List<SponsoredResult> sponsoredResults) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        searchPanel.addSponsoredResults(sponsoredResults);
                    }
                });
            }
        });
        
        addConnectionWarnings(search, searchPanel, item);        
        startSearch(search, searchPanel, item);
    }

    private void startSearch(final Search search, final SearchResultsPanel searchPanel, NavItem navItem) {
        //prevent search from starting until lifecycle manager completes loading
        if(lifeCycleManager.isStarted()) {
            search.start();
        } else {
             searchPanel.setLifeCycleComplete(false);
             final EventListener<LifeCycleEvent> listener = new EventListener<LifeCycleEvent>() {
                 @SwingEDTEvent
                 public void handleEvent(LifeCycleEvent event) {
                     if(event == LifeCycleEvent.STARTED) {
                         searchPanel.setLifeCycleComplete(true);
                         search.start();
                         lifeCycleManager.removeListener(this);
                     }
                 }
             };
             lifeCycleManager.addListener(listener);
             navItem.addNavItemListener(new NavItemListener() {                 
                 public void itemRemoved() {
                     lifeCycleManager.removeListener(listener);
                 }
                 
                 public void itemSelected(boolean selected) {}
             });
        }
    }
    
    private boolean setConnectionStrength(ConnectionStrength type, SearchResultsPanel searchPanel) {
        switch(type) {
        case TURBO:
        case FULL:
            searchPanel.setFullyConnected(true);
            return true;
        }
        
        return false;
    }
    
    private void removeListeners(Search search, AtomicReference<SearchListener> searchListenerRef, AtomicReference<PropertyChangeListener> connectionListenerRef) {
        SearchListener searchListener = searchListenerRef.get();
        if(searchListener != null) {
            search.removeSearchListener(searchListener);
            searchListenerRef.set(null);
        }
        
        PropertyChangeListener connectionListener = connectionListenerRef.get();
        if(connectionListener != null) {
            connectionManager.removePropertyChangeListener(connectionListener);
            connectionListenerRef.set(null);
        }
    }

    private void addConnectionWarnings(final Search search, final SearchResultsPanel searchPanel, NavItem navItem) {
        final AtomicReference<SearchListener> searchListenerRef = new AtomicReference<SearchListener>();
        final AtomicReference<PropertyChangeListener> connectionListenerRef = new AtomicReference<PropertyChangeListener>();
        
        connectionListenerRef.set(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("strength")) {
                    if(setConnectionStrength((ConnectionStrength)evt.getNewValue(), searchPanel)) {
                        removeListeners(search, searchListenerRef, connectionListenerRef);
                    }
                }
            }
        });  

        //override warning message if a certain number of search results comes in, for now 10, 
        //we assume that while not fully connected, we have a good enough search going
        searchListenerRef.set(new SearchListener() {
            private final AtomicInteger numberOfResults = new AtomicInteger(0);
            
            @Override
            public void handleSearchResult(SearchResult searchResult) {
                if(numberOfResults.addAndGet(1) > 10) {
                    SwingUtils.invokeLater(new Runnable() {
                        public void run() {
                            // while not fully connected, assume the
                            // connections we have are enough
                            // based on the number of results coming in.
                            searchPanel.setFullyConnected(true);
                        }
                    });
                    removeListeners(search, searchListenerRef, connectionListenerRef);
                }
            }

            @Override public void handleSponsoredResults(List<SponsoredResult> sponsoredResults) {}
            @Override public void searchStarted() {}
            @Override public void searchStopped() {}                
        });
        
        searchPanel.setFullyConnected(false);
        connectionManager.addPropertyChangeListener(connectionListenerRef.get());        
        if(setConnectionStrength(connectionManager.getConnectionStrength(), searchPanel)) {
            removeListeners(search, searchListenerRef, connectionListenerRef);
        } else {
            search.addSearchListener(searchListenerRef.get());
            navItem.addNavItemListener(new NavItemListener() {
                @Override
                public void itemRemoved() {
                    removeListeners(search, searchListenerRef, connectionListenerRef);
                }
                @Override public void itemSelected(boolean selected) {}
            });
        }
        
    }
}