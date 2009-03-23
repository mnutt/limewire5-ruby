package org.limewire.core.impl.library;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.search.SearchCategory;

class MockFriendAutoCompleterFactory implements FriendAutoCompleterFactory{

    @Override
    public AutoCompleteDictionary getDictionary(SearchCategory categoryToSearch) {
        return new MockFriendLibraryAutoCompleter();
    }

    @Override
    public AutoCompleteDictionary getDictionary(SearchCategory categoryToSearch,
            FilePropertyKey filePropertyKey) {
        return new MockFriendLibraryAutoCompleter();
    }

}
