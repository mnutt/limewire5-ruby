package org.limewire.filter;

/**
 * Generic filter interface. Filters object of type T. 
 */
public interface Filter<T> {

    /**
     * Returns true if <code>t</code> is allowed by this filter, false if
     * if it should be discarded. 
     */
    boolean allow(T t);
    
}
