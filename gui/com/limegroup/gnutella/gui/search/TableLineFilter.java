package com.limegroup.gnutella.gui.search;

/**
 * Interface that all Filters should implement
 * if they wish to filter out TableLines.
 */
public interface TableLineFilter {
	/**
     * Determines whether or not the specified
     * TableLine should be displayed.
     */ 
    public boolean allow(TableLine node);

}
    
    
