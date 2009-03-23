package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * Indicates that we will are no longer being chocked.
 */
public class BTUnchoke extends BTMessage {
	private static final BTUnchoke INSTANCE = new BTUnchoke();

	/**
	 * factory method
	 * 
	 * @return new instance of message
	 */
	public static BTUnchoke createMessage() {
		return INSTANCE;
	}
	
	/**
	 * Constructs new BTUnchoke message
	 */
	BTUnchoke() {
		super(UNCHOKE);
	}
	
	@Override
    public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}
	
	@Override
    public String toString() {
		return "BTUnchoke";
	}

}
