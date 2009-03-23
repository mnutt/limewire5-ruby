package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * Indicates that the sender is interested in downloading from the remote host.
 */
public class BTInterested extends BTMessage {
	private static final BTInterested INSTANCE = new BTInterested();

	/**
	 * factory method
	 * 
	 * @return new instance of message
	 */
	public static BTInterested createMessage() {
		return INSTANCE;
	}

	/**
	 * Constructs new BTInterested message
	 */
	private BTInterested() {
		super(INTERESTED);
	}

	@Override
    public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}

	@Override
    public boolean isUrgent() {
		return true;
	}
	
	@Override
    public String toString() {
		return "BTInterested" ;
	}

}
