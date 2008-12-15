package org.limewire.core.api.search.friend;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.SearchCategory;

public interface FriendAutoCompleters {
    AutoCompleteDictionary getDictionary(SearchCategory category);
}
