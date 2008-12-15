package com.limegroup.bittorrent.reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.bittorrent.messages.BadBTMessageException;

/**
 * State that parses the type of a BitTorrent message (either a <code>BITFIELD</code>,
 * <code>PIECE</code> or <code>MessageState</code>).
 */
class TypeState extends BTReadMessageState {

	private static final Log LOG = LogFactory.getLog(TypeState.class);
	
	private byte type = -1;
	
	TypeState(ReaderData readerState) {
		super(readerState);
	}
	
	@Override
    public BTReadMessageState addData() throws BadBTMessageException {
		BTDataSource buf = readerState.getDataSource();
		if (buf.size() < 1)
			return null;
		
		type = buf.get();
		
		if (LOG.isDebugEnabled())
			LOG.debug(this+" parsed type "+type);
		
		boolean wasFirst = !readerState.anyDataRead();
		readerState.dataRead();
		if (type == BTMessage.BITFIELD) {
			if (!wasFirst)
				throw new BadBTMessageException("Bitfield can be only first message");
			return new BitFieldState(readerState); 
		} else if (type == BTMessage.PIECE)
			return new PieceState(readerState);
		else 
			return new MessageState(readerState,type);
	}
	
	@Override
    public String toString() {
		return "type state of "+readerState;
	}

}
