package org.limewire.core.impl.library;

import java.util.Collection;
import java.util.Iterator;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;

/**
 * Provides suggestions for a query based on category and file property key for what the user's friends
 * have in their libraries for that category.
 */
class FriendLibraryPropertyAutoCompleter implements AutoCompleteDictionary {
    private final FriendLibraries friendLibraries;
    private final SearchCategory category;
    private final FilePropertyKey filePropertyKey;
    
    public FriendLibraryPropertyAutoCompleter(FriendLibraries friendLibraries, SearchCategory category,  FilePropertyKey filePropertyKey) {
        this.friendLibraries = friendLibraries;
        this.category = category;
        this.filePropertyKey = filePropertyKey;
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
        return friendLibraries.getSuggestions(prefix, category, filePropertyKey);
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}

