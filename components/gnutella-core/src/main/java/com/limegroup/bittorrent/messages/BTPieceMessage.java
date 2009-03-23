package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTPiece;

/**
 * Pieces of a BitTorrent are equal sized chunks of a file.
 * <p>
 * The recipient must only send <code>BTPieceMessage</code> to a sender that 
 * has already requested it.
 */
public class BTPieceMessage extends BTMessage implements BTPiece {
	private BTInterval in;

	private final byte[] _data;

	/**
	 * Constructs new BTPiece message
	 */
	public BTPieceMessage(BTInterval in, byte[] data) {
		super(PIECE);
		this.in = in;
		_data = data;
	}

	public BTInterval getInterval() {
		return in;
	}

	/**
	 * @return requested data
	 */
	public byte[] getData() {
		return _data;
	}

	@Override
    public ByteBuffer getPayload() {
		ByteBuffer payload = ByteBuffer.allocate(_data.length + 8);
		payload.order(ByteOrder.BIG_ENDIAN);
		payload.putInt(in.getId());
		payload.putInt(in.get32BitLow());
		payload.put(_data);
		payload.clear();
		return payload;
	}
	
	@Override
    public boolean isUrgent() {
		return true;
	}
	
	@Override
    public String toString() {
		return "BTPiece (" + in.getId() + ";" + in.getLow() + ";" + _data.length + ")" ;
	}
}
