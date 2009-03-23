package com.limegroup.bittorrent;


import org.limewire.collection.NECallable;

import com.limegroup.bittorrent.messages.BTMessage;

/**
 * A handler for received BTMessages. Since Piece messages
 * are large, they are handled in a four step process:
 * <ol>
 * <li> startReceivingPiece() is called when the Piece header is parsed
 * <li> handlePiece() is called whenever there is data received that belongs
 * to the Piece
 * <li> readBytes() is called whenever data that belongs to the Piece is about
 * to be written to disk
 * <li> finishReceivingPiece() is called when the entire message is received.
 */
public interface BTMessageHandler {
	
	/**
	 * @param message the incoming message to process.
	 */
	public void processMessage(BTMessage message);

	/**
	 * Handles a Piece message and sends its payload to disk.
	 * @param factory the<tt>BTPieceFactory</tt> that will
	 * create the Piece.
	 */
	public void handlePiece(NECallable<BTPiece> factory);
	
	/**
	 * Notification that some bytes belonging to a Piece message 
	 * are about to be written to disk.
	 */
	public void readBytes(int read);

	/**
	 * Notification to start receiving a Piece message 
	 * carrying the specified interval.
	 * @return true if the message was expected.
	 */
	public boolean startReceivingPiece(BTInterval piece);
	
	/**
	 * Notification to finish receiving a Piece message.
	 */
	public void finishReceivingPiece();
}
