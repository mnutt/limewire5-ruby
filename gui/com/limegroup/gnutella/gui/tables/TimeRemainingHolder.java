package com.limegroup.gnutella.gui.tables;

import org.limewire.util.CommonUtils;


/**
 * simple class to store the numeric value of time remaining (or ETA)
 * used so we can sort by a value, but display a human-readable time.
 * @author sberlin
 */
public final class TimeRemainingHolder implements Comparable<TimeRemainingHolder> {
	
	private int _timeRemaining;
	
	public TimeRemainingHolder(int intValue) 
	{
		_timeRemaining = intValue;
	}
	
	public int compareTo(TimeRemainingHolder o) {
	    return o._timeRemaining - _timeRemaining;
	}
	
    @Override
    public String toString() {
        return _timeRemaining == 0 ? "" : CommonUtils.seconds2time(_timeRemaining);
    }
}
