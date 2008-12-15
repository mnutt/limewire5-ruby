package org.limewire.collection;

import java.util.Collection;
import java.util.RandomAccess;

/** A collection that can retrieve items at specific indexes. */
public interface RandomAccessCollection<E> extends RandomAccess, Collection<E> {

    /** Retrieves the element at index i. */
    public E get(int i);
}
