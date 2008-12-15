package com.limegroup.gnutella.gui.search;

/**
 * Filter denoting that anything is allowed.
 */
class AllowFilter implements TableLineFilter {
    /**
     * The sole instance that can be returned, for convenience.
     */
	private static AllowFilter INSTANCE = new AllowFilter();
    
    /**
     * Returns a reusable instance of AllowFilter.
     */
    public static AllowFilter instance() {
        return INSTANCE;
    }

    /**
     * Returns true.
     */
    public boolean allow(TableLine line) {
        return true;
    }
    
    @Override
    public boolean equals(Object o) {
        return (o instanceof AllowFilter);
    }    
}