package com.limegroup.gnutella.gui.search;

/**
 * A filter that takes multiple other filters.
 */
class CompositeFilter implements TableLineFilter {
    /**
     * The underlying filters.
     */
    private TableLineFilter[] delegates;
    
    /**
     * Creates a new CompositeFilter of the specified depth.
     * By default, all the filters are an AllowFilter.
     */
    CompositeFilter(int depth) {
        this.delegates = new TableLineFilter[depth];
        reset();
    }
    
    /**
     * Resets this filter to all AllowFilters.
     */
    public void reset() {
        for(int i = 0; i < delegates.length; i++) {
            delegates[i] = AllowFilter.instance();
        }
    }
    
    /**
     * Determines whether or not the specified TableLine
     * can be displayed.
     */
    public boolean allow(TableLine line) {
        for (int i=0; i<delegates.length; i++) {
            if (! delegates[i].allow(line))
                return false;
        }
        return true;
    }
    
    /**
     * Sets the filter at the specified depth.
     */
    boolean setFilter(int depth, TableLineFilter filter) {
        if (filter == this) {
            throw new IllegalArgumentException("Filter must not be composed of itself");
        }
        if(delegates[depth].equals(filter))
            return false;
        else {
            delegates[depth] = filter;
            return true;
        }
    }
}
