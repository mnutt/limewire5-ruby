package org.limewire.core.impl.library;

import java.util.Collection;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.impl.search.FriendSearchListener;
import org.limewire.core.settings.SearchSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendSearcher {
    private final FriendLibraries libraries;

    @Inject
    FriendSearcher(FriendLibraries libraries) {
        this.libraries = libraries;
    }
    
    public void doSearch(SearchDetails searchDetails, FriendSearchListener listener) {
        if(SearchSettings.SEARCH_FRIENDS_LIBRARIES.getValue()) {
            Collection<RemoteFileItem>results = libraries.getMatchingItems(searchDetails.getSearchQuery(), searchDetails.getSearchCategory());
            listener.handleFriendResults(results);
        }
    }
}
