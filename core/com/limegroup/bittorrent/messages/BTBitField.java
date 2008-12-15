package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.TorrentContext;

/**
 * Represents the pieces that the sender has successfully downloaded.A peer must 
 * send this message immediately after the handshake operation. This message 
 * must not be sent at any other time during the communication.
 */
public class BTBitField extends BTMessage {
	
	/** the bitfield itself */
	private ByteBuffer _bitfield = null;

	
	public BTBitField(ByteBuffer payload) {
		super(BITFIELD);
		_bitfield = payload;
	}

	/**
	 * factory method, creates a new BitField message
	 * 
	 * @return new instance of BTBitField
	 */
	public static BTBitField createMessage(TorrentContext context) {
		byte[] bitfield = context.getDiskManager().createBitField();
		return new BTBitField(ByteBuffer.wrap(bitfield));
	}

	@Override
    public ByteBuffer getPayload() {
		_bitfield.clear();
		return _bitfield;
	}
	
	@Override
    public String toString() {
		return "BTBitfield";
	}
}
