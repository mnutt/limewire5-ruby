package com.limegroup.gnutella.gui.connection;


import java.text.NumberFormat;

import com.limegroup.gnutella.gui.GUIUtils;


/**
 * Wrapper class that acts as a comparable for the bandwidth i/o info.
 * @author sam berlin
 */
public final class BandwidthHolder implements Comparable<BandwidthHolder> {
	
	/**
	 * Static number formatter
	 */
    static NumberFormat formatter;
    static {
        formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(3);
        formatter.setMaximumFractionDigits(3);
    }
	
	/**
	 * Variable for the string representation
	 */
	private String _string;

	/**
	 * Variable for the info.
	 */
	private float _down, _up;

	/**
	 * Only care to show a single value? Use this.
	 * 
	 */
	public BandwidthHolder (float x) {
	    _string = formatter.format(x) + " " + GUIUtils.GENERAL_UNIT_KBPSEC;
	    _up = x;
	    _down = 0.0f;
	}
	
	/**
	 * The constructor sets d / u
	 *
	 */
	public BandwidthHolder(float d, float u) {
		_string = formatter.format(d) + " / " + formatter.format(u) + 
		    GUIUtils.GENERAL_UNIT_KBPSEC;
		_down = d;
		_up = u;
	}
	
	/**
	 * Add up the two things and see which is larger.
	 */
	public int compareTo(BandwidthHolder other) {
	    float me = _down + _up;
	    float you = other._down + other._up;
	    if ( me > you ) return 1;
	    if ( me < you ) return -1;
	    return 0;
	}

	/**
	 *
	 * @return the formatted string
	 */
	@Override
    public String toString() {
		return _string;
	}
}
