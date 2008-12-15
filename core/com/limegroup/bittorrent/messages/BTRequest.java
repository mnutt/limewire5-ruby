package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.bittorrent.BTInterval;

/**
 * Indicates a block within a 
 * <a href="http://jonas.nitro.dk/bittorrent/bittorrent-rfc.html#anchor3">Piece</a> 
 * that the sender is interested in downloading 
 * from the recipient. The recipient must only send Piece messages to a sender 
 * that has already requested it. 
 */
public class BTRequest extends BTMessage {
	/*
	 * if anyone requests more than 2^16 bytes we will rip his head off
	 */
	private static final int MAX_REQUEST_SIZE = 65536;

	private BTInterval in;
	
	private ByteBuffer _payload;

	/**
	 * Constructs new BTRequest message
	 */
	public BTRequest(BTInterval in) {
		super(REQUEST);
		this.in = in;
	}

	/**
	 * read message from network
	 * 
	 * @param payload
	 *            the payload of the message wrapped in a ByteBuffer.
	 * @return new BTRequest
	 * @throws BadBTMessageException
	 */
	public static BTRequest readMessage(ByteBuffer payload) throws BadBTMessageException {
		if (payload.remaining() != 12)
			throw new BadBTMessageException(
					"unexpected payload in request message: "
							+ new String(payload.array()));

		payload.order(ByteOrder.BIG_ENDIAN);

		int pieceNum = payload.getInt();

		if (pieceNum < 0)
			throw new BadBTMessageException(
					"invalid piece number in request message: " + pieceNum);

		long offset = payload.getInt();
		if (offset < 0)
			throw new BadBTMessageException("negative offset in mesage");

		long length = payload.getInt(); 
		if (length <= 0 || length > MAX_REQUEST_SIZE)
			throw new BadBTMessageException(
					"invalid requested length in request message: " + length);
		
		return new BTRequest(new BTInterval(offset, offset + length - 1, pieceNum));
	}

	
	public BTInterval getInterval() {
		return in;
	}

	@Override
    public ByteBuffer getPayload() {
		if (_payload == null) {
			ByteBuffer buf = ByteBuffer.allocate(12);
			buf.order(ByteOrder.BIG_ENDIAN);
			buf.putInt(in.getId());
			buf.putInt(in.get32BitLow());
			buf.putInt(in.get32BitLength());
			_payload = buf.asReadOnlyBuffer();
		}
		_payload.clear();
		return _payload;
	}
	
	@Override
    public boolean isUrgent() {
		return true;
	}
	
	@Override
    public String toString() {
		return "BTRequest (" + in + ")" ;
	}
}
