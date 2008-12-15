package org.limewire.core.impl.search;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.impl.library.MockFriendLibraryAutoCompleter;

public class MockFriendAutoCompleters implements FriendAutoCompleters {
    public AutoCompleteDictionary getDictionary(SearchCategory category) {
        return new MockFriendLibraryAutoCompleter();
    }
}
