package com.limegroup.gnutella.gui.connection;




/**
 * Wrapper class that acts as a comparable for the messages i/o info.
 * @author sam berlin
 */
public final class MessagesHolder implements Comparable<MessagesHolder> {
	
	/**
	 * Variable for the string representation
	 */
	private String _string;

	/**
	 * Variable for the info.
	 */
	private int _recieved, _sent;

	/**
	 * Constructor
	 */
	public MessagesHolder(int r, int s) {
		_string = Integer.toString(r) + " / " + Integer.toString(s);
		_recieved = r;
		_sent = s;
	}
	
	/**
	 * Add up the two things and see which is larger.
	 */
	public int compareTo(MessagesHolder other) {
	    return ( 
	      ( _recieved + _sent) -
	      (other._recieved + other._sent)
	    );
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
