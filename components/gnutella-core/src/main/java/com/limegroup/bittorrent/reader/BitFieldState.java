package com.limegroup.bittorrent.reader;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.ByteBufferOutputStream;
import org.limewire.service.ErrorService;

import com.limegroup.bittorrent.messages.BTBitField;
import com.limegroup.bittorrent.messages.BadBTMessageException;

/**
 * State that parses the <code>BTBitField</code> message. 
 */
class BitFieldState extends BTReadMessageState {
	
	private static final Log LOG = LogFactory.getLog(BitFieldState.class);
	
	/* 
	 * We use a BufferByteArrayOutputStream 
	 * that grows as data arrives on the wire. It is deliberately 
	 * not pre-allocated even though we know how large it will 
	 * eventually get.
	 */
	private ByteBufferOutputStream bbaos;
	private WritableByteChannel bbaosChan;

	/**
	 * the expected length of this bitfield
	 */
	private final int length;
	
	BitFieldState(ReaderData readerState) {
		super(readerState);
		length = readerState.getLength();
	}
	
	@Override
    public BTReadMessageState addData() throws BadBTMessageException {

		BTDataSource buf = readerState.getDataSource();

		if (bbaos == null) {
			bbaos = new ByteBufferOutputStream();
			bbaosChan = Channels.newChannel(bbaos);
		}
		
		int toWrite = length - bbaos.size();
		try {
			buf.write(bbaosChan, toWrite);
		} catch (IOException impossible) {
			ErrorService.error(impossible);
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug(this + " parsing bitfield incrementally, so far " + bbaos.size());
		
		if (bbaos.size() == length) { 
			countAndProcess(ByteBuffer.wrap(bbaos.toByteArray()));
			return readerState.getEntryState();
		}
		
		return null;
	}
		
	private void countAndProcess(ByteBuffer b) {
		BTBitField field = new BTBitField(b);
		readerState.getHandler().processMessage(field);
	}

	@Override
    public String toString() {
		return "BitFieldReader for "+readerState;
	}

}
