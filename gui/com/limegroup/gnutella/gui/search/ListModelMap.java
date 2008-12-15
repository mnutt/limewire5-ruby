package com.limegroup.gnutella.gui.search;

import java.util.Iterator;
import java.util.SortedMap;

import javax.swing.ListModel;

/**
 * An interface that combines a SortedMap and a ListModel.
 *
 * SortedMap is used so that the ListBox is sorted in a certain order.
 */
interface ListModelMap<K, V> extends SortedMap<K, V>, ListModel, Iterable<K> {
    /**
     * Returns true if the ListModel contains the specified object.
     */
    public boolean contains(Object o);
    
    /**
     * Returns the index of the specified object or -1 if it doesn't exist.
     */
    public int indexOf(Object o);
    
    /**
     * Returns the iterator of the possible choices in this map.
     *
     * The 'All' option is excluded from this.
     */
    public Iterator<K> iterator();
}