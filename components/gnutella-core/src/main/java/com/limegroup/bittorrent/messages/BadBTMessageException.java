package com.limegroup.bittorrent.messages;

import java.io.IOException;

/**
 * Exception used to distinguish common IO problems from BitTorrent 
 * non parse-able messages.
 */
public class BadBTMessageException extends IOException {
	private static final long serialVersionUID = -9138724347393610325L;

	public BadBTMessageException() {
		super();
	}

	public BadBTMessageException(String s) {
		super(s);
	}
}
