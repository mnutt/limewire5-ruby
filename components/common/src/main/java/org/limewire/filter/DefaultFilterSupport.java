package org.limewire.filter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of {@link FilterSupport}.
 * 
 * This class is threadsafe.
 */
public class DefaultFilterSupport<T> implements FilterSupport<T> {

    private final List<Filter<T>> blackListFilters = new CopyOnWriteArrayList<Filter<T>>();
    
    private final List<Filter<T>> whiteListFilters = new CopyOnWriteArrayList<Filter<T>>();
    
    @Override
    public void addBlackListFilter(Filter<T> filter) {
        blackListFilters.add(filter);
    }

    @Override
    public void addWhiteListFilter(Filter<T> filter) {
        whiteListFilters.add(filter);
    }

    @Override
    public void removeBlackListFilter(Filter<T> filter) {
        blackListFilters.remove(filter);
    }

    @Override
    public void removeWhiteListFilter(Filter<T> filter) {
        whiteListFilters.remove(filter);
    }
    
    @Override
    public boolean allow(T t) {
        for(Filter<T> blackFilter : blackListFilters) {
            if(!blackFilter.allow(t)) {
                for (Filter<T> whiteFilter : whiteListFilters) {
                    if (whiteFilter.allow(t)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }

}
