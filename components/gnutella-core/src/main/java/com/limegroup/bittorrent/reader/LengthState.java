package com.limegroup.bittorrent.reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.messages.BadBTMessageException;

/**
 * State to set the length of the BitTorrent data source.
 */
class LengthState extends BTReadMessageState {
	
	private static final Log LOG = LogFactory.getLog(LengthState.class);
	
	/** Max size for a piece message */
	private static final int MAX_PIECE_SIZE = 32 * 1024 + 9;

	private final TypeState TYPE_STATE;
	
	LengthState(ReaderData readerState) {
		super(readerState);
		TYPE_STATE = new TypeState(readerState);
	}
	
	@Override
    public BTReadMessageState addData() throws BadBTMessageException {
		BTDataSource buf = readerState.getDataSource();
		if (buf.size() < 4)
			return null;
		
		long length = buf.getInt();
		
		if (LOG.isDebugEnabled())
			LOG.debug(this +" parsed length " + length);
		
		if (length < 0 || length > MAX_PIECE_SIZE)
			throw new BadBTMessageException("bad message size " + length);
		
		if (length == 0) {
			readerState.dataRead(); 
			return this;
		}
		
		length--;
		readerState.setLength((int)length);
		return TYPE_STATE;
	}
	
	@Override
    public String toString() {
		return "length state of "+readerState;
	}
}
