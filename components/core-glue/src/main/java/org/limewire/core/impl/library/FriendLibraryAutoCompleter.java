package org.limewire.core.impl.library;

import java.util.Collection;
import java.util.Iterator;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.SearchCategory;

public class FriendLibraryAutoCompleter implements AutoCompleteDictionary {
    private final FriendLibraries friendLibraries;
    private final SearchCategory category;

    public FriendLibraryAutoCompleter(FriendLibraries friendLibraries, SearchCategory category) {
        this.friendLibraries = friendLibraries;
        this.category = category;
    }
    
    public void addEntry(String entry) {
        throw new UnsupportedOperationException();
    }

    public boolean removeEntry(String entry) {
        throw new UnsupportedOperationException();
    }

    public String lookup(String prefix) {
        Iterator<String> it = getPrefixedBy(prefix).iterator();
        if (!it.hasNext())
            return null;
        return it.next();
    }

    public Iterator<String> iterator() {
        return getPrefixedBy("").iterator();
    }

    public Collection<String> getPrefixedBy(String prefix) {
        return friendLibraries.getSuggestions(prefix, category);
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}

