package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.limewire.collection.AutoCompleteDictionary;

public class MockFriendLibraryAutoCompleter implements AutoCompleteDictionary {
    public void addEntry(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean removeEntry(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String lookup(String s) {
        return "";
    }

    public Iterator<String> iterator() {
        return new Iterator<String>() {
            public boolean hasNext() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String next() {
                return "";
            }

            public void remove() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    public Collection<String> getPrefixedBy(String s) {
        return new ArrayList<String>();
    }

    public void clear() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
