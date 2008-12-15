package com.limegroup.gnutella.gui.tables;

import com.limegroup.gnutella.gui.GUIUtils;


/**
 * Wrapper class that holds on to the size integer for a file so that 
 * we don't have to read it from disk every time while sorting.
 */
public final class SizeHolder implements Comparable<SizeHolder> {
	
	/**
	 * Variable for the string representation of the file size.
	 */
	private final String _string;

	/**
	 * Variable for the size of the file in kilobytes.
	 */
	private final long _size;

	/**
	 * The constructor sets the size and string variables, creating a
	 * formatted string in kilobytes from the size value.
	 *
	 * @param size the size of the file in kilobytes
	 */
	public SizeHolder(long size) {
	    if(size >= 0) {
    		_string = GUIUtils.toUnitbytes(size);
    		_size = size;
	    } else {
	        _string = "--";
	        _size = -1;
	    }
	}
	
	public int compareTo(SizeHolder o) {
	    long otherSize = o.getSize();
	    if (_size > otherSize)
	    	return 1;
	    else if (_size < otherSize)
	    	return -1;
	    else
	    	return 0;
	}

	/**
	 * Returns the string value of this size, formatted with commas and
	 * "KB" appended to the end.
	 *
	 * @return the formatted string representing the size
	 */
	@Override
    public String toString() {
		return _string;
	}

	/**
	 * Returns the size of the file in kilobytes.
	 *
	 * @return the size of the file in kilobytes
	 */
	public long getSize() {
		return _size;
	}
}
