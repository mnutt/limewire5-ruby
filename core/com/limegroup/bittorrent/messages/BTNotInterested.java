package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * Indicates that the sender is not interested in downloading from the 
 * remote host.
 */
public class BTNotInterested extends BTMessage {
	private static final BTNotInterested INSTANCE = new BTNotInterested();

	/**
	 * factory method
	 * 
	 * @return new instance of message
	 */
	public static BTNotInterested createMessage() {
		return INSTANCE;
	}

	/**
	 * Constructs new BTInterested message
	 */
	BTNotInterested() {
		super(NOT_INTERESTED);
	}

	@Override
    public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}

	@Override
    public String toString() {
		return "BTNotInterested" ;
	}
	
}
