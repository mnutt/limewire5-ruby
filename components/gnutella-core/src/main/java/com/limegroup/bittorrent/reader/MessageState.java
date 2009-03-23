package com.limegroup.bittorrent.reader;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.bittorrent.messages.BadBTMessageException;

class MessageState extends BTReadMessageState {

	private final byte type;
	
	MessageState(ReaderData readerState, byte type) {
		super(readerState);
		this.type = type;
	}
	
	@Override
    public BTReadMessageState addData() throws BadBTMessageException {
		BTDataSource in = readerState.getDataSource();
		if (in.size() < readerState.getLength()) 
			return null;
		
		ByteBuffer buf;
		if (readerState.getLength() == 0)
			buf = BTMessage.EMPTY_PAYLOAD;
		else {
			buf = ByteBuffer.allocate(readerState.getLength());
			in.get(buf);
			buf.clear();
		}
		
		BTMessage message = BTMessage.parseMessage(buf, type);
		readerState.getHandler().processMessage(message);
		return readerState.getEntryState();
	}
}
