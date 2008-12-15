package org.limewire.core.impl.search.friend;

import java.util.HashMap;
import java.util.Map;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.impl.library.FriendLibraries;
import org.limewire.core.impl.library.FriendLibraryAutoCompleter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendAutoCompletersImpl implements FriendAutoCompleters {
    
    private Map<SearchCategory, AutoCompleteDictionary> dictionaries = new HashMap<SearchCategory, AutoCompleteDictionary>();
    
    @Inject
    FriendAutoCompletersImpl(FriendLibraries friendLibraries) {
        for(SearchCategory category : SearchCategory.values()) {
            dictionaries.put(category, new FriendLibraryAutoCompleter(friendLibraries, category));
        }
    }
    
    public AutoCompleteDictionary getDictionary(SearchCategory category) {
        return dictionaries.get(category);    
    }
}
