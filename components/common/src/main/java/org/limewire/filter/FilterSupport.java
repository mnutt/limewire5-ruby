package org.limewire.filter;

/**
 * Interface for classes that support addition and removal of black list and
 * white list filters.
 * <p>
 * Extends {@link Filter} whose implementation delegates to the added black list
 * and white list filters in the following fashion:
 * <p>
 * {@link #allow(T)} iterates over the black list to see if one disallows 
 * the instance of <code>T</code>, if not the instance is allowed. Otherwise,
 * the list of white list filters will be queried to veto the black filter's
 * decision. If there is one white list filter that allows the instance of 
 * <code>T</code>, it will be allowed, otherwise it will be rejected.
 */
public interface FilterSupport<T> extends Filter<T> {

    /**
     * Adds a black list filter.
     */
    void addBlackListFilter(Filter<T> filter);
    /**
     * Removes a black list filter.
     */
    void removeBlackListFilter(Filter<T> filter);
    /**
     * Adds a white list filter.
     */
    void addWhiteListFilter(Filter<T> filter);
    /**
     * Removes a white list filter. 
     */
    void removeWhiteListFilter(Filter<T> filter);
    
}
