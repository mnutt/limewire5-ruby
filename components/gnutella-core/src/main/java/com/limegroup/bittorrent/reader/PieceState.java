package com.limegroup.bittorrent.reader;

import org.limewire.collection.NECallable;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTPiece;
import com.limegroup.bittorrent.messages.BadBTMessageException;

/**
 * State that parses the Piece message. The torrent is broken up into equal 
 * sized pieces (a range of data) which are later transferred.
 */
class PieceState extends BTReadMessageState implements NECallable<BTPiece> {

	private final int length;
	private final BTDataSource buf;
	
	private int chunkId = -1;
	private long offset = -1;
	private long currentOffset;
	private BTInterval complete;
	
	PieceState(ReaderData readerState) {
		super(readerState);
		length = readerState.getLength();
		buf = readerState.getDataSource();
	}
	
	/** Whether we actually requested this piece */
	private boolean welcome;
	/** Whether we are expecting someone to write our data to disk */
	private boolean writeExpected;
	
	@Override
    public BTReadMessageState addData() throws BadBTMessageException {
		// if we're expecting disk write, do nothing.
		if (writeExpected)
			return null;
		
		if (length < 9)
			throw new BadBTMessageException("piece too short");
		
		if (buf.size() < 4 && (chunkId < 0 || offset < 0))
			return null;
		
		// read chunk id
		if (chunkId < 0) {
            long newId = buf.getInt();
            if (newId > Integer.MAX_VALUE)
                throw new BadBTMessageException("unsupported bit chunk id");
            chunkId = (int)newId;
			return this; // shortcut :)
		}
		
		// read offset
		if (offset < 0) {
			offset = buf.getInt();
			currentOffset = offset;
			// check if the piece was requested
			complete = new BTInterval(offset, offset + length - 9, chunkId); 
			welcome = readerState.getHandler().startReceivingPiece(complete);
		}
		
		int available = getAmountLeft();
		if (available == 0)
			return null;
		
		// if the piece was requested, we process it.
		// otherwise we skip it.
		if (welcome) {
			// if the buffer is full, turn off read interest
			if (!writeExpected) { 
				writeExpected = true;
				readerState.getHandler().handlePiece(this);
			}
		} else {
			buf.discard(available);
			currentOffset += available;
			available = 0;
		}
		
		if (currentOffset + available == complete.getHigh() + 1) {
			// we're done receiving this piece, request more.
			readerState.getHandler().finishReceivingPiece();
			if (!writeExpected)
				return readerState.getEntryState();
		}
		return null;
	}
	
	@Override
    public String toString() {
		return "Piece "+complete + 
		" offset "+currentOffset+
		" welcome "+welcome+
		" write expected "+writeExpected;
	}
	
	private int getAmountLeft() {
		return (int)Math.min(buf.size(), complete.getHigh() - currentOffset + 1);
	}
	
	public BTPiece call() {
		synchronized(readerState) {
			assert(writeExpected);
			writeExpected = false;
			int toRead = getAmountLeft();
			
			BTInterval in = new BTInterval(currentOffset, 
					currentOffset + toRead - 1,
					chunkId);
			currentOffset += toRead;
			readerState.getHandler().readBytes(toRead);
			byte []data = new byte[toRead];
			buf.get(data);
			readerState.getPieceListener().dataConsumed(currentOffset > complete.getHigh());
			return new ReceivedPiece(in, data);
		}
	}
	
	private static class ReceivedPiece implements BTPiece {
		private final BTInterval interval;
		private final byte [] data;
		
		ReceivedPiece(BTInterval interval, byte [] data) {
			this.interval = interval;
			this.data = data;
		}
		
		public BTInterval getInterval() {
			return interval;
		}
		
		public byte [] getData() {
			return data;
		}
	}
}
