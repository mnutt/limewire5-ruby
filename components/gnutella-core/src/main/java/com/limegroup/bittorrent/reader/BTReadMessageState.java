package com.limegroup.bittorrent.reader;

import com.limegroup.bittorrent.messages.BadBTMessageException;

/**
 * A state of the BT message parser.
 */
abstract class BTReadMessageState {
	
	protected final ReaderData readerState;
	
	protected BTReadMessageState(ReaderData readerState) {
		this.readerState = readerState;
	}
	
	/**
	 * Add data to the current parsing state from the _in buffer.
	 * @return the next parsing state or null if we need to stay
	 * in this state
	 * @throws BadBTMessageException the message parsing fails.
	 */
	public abstract BTReadMessageState addData() throws BadBTMessageException;
}