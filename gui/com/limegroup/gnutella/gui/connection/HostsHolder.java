package com.limegroup.gnutella.gui.connection;




/**
 * Wrapper class that acts as a comparable for the hosts info.
 * @author sam berlin
 */
public final class HostsHolder implements Comparable<HostsHolder> {
	
	/**
	 * Variable for the string representation
	 */
	private String _string;

	/**
	 * Variable for the info.
	 */
	private long _hosts;

	/**
	 * The constructor sets # hosts
	 *
	 */
	public HostsHolder(long hosts) {
	    _string = hosts == -1 ? "?" : Long.toString(hosts);
	    _hosts = hosts;
	}
	
	/**
	 * Add up the two things and see which is larger.
	 */
	public int compareTo(HostsHolder other) {
	    return (int)( _hosts - other._hosts );
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
