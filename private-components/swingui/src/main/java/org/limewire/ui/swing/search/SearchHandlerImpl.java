package org.limewire.ui.swing.search;

import java.util.Locale;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class SearchHandlerImpl implements SearchHandler {
    
    private final SearchHandler p2pLinkSearch;
    private final SearchHandler textSearch;
    
    @Inject
    public SearchHandlerImpl(@Named("p2p://") SearchHandler p2pLinkSearch,
                        @Named("text") SearchHandler textSearch) {
        this.p2pLinkSearch = p2pLinkSearch;
        this.textSearch = textSearch;
    }
    
    @Override
    public void doSearch(SearchInfo info) {
        String q = info.getQuery();
        if(q != null && q.toLowerCase(Locale.US).startsWith("p2p://")) {
            p2pLinkSearch.doSearch(info);
        } else {
            textSearch.doSearch(info);
        }
    }

}
