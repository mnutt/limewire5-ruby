package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.bittorrent.BTInterval;

/**
 * Cancels a request we may have sent to the remote host.
 */
public class BTCancel extends BTMessage {
	private ByteBuffer _payload = null;

	private BTInterval in;

	/**
	 * Constructs new BTCancel message
	 */
	public BTCancel(BTInterval in) {
		super(CANCEL);
		this.in = in;
	}

	/**
	 * creates BTCancel message from network
	 * 
	 * @param payload
	 *            the payload of the message wrapped in a ByteBuffer.
	 * @return new BTCancel message 
	 * 	
	 * @throws BadBTMessageException
	 */
	public static BTCancel readMessage(ByteBuffer payload) throws BadBTMessageException {
		if (payload.remaining() != 12)
			throw new BadBTMessageException(
					"unexpected payload in cancel message: "
							+ new String(payload.array()));

		payload.order(ByteOrder.BIG_ENDIAN);

		int pieceNum = payload.getInt();

		if (pieceNum < 0)
			throw new BadBTMessageException(
					"invalid piece number in cancel message: " + pieceNum);

		long offset = payload.getInt();

		long length = payload.getInt();
		
		if (length == 0)
			throw new BadBTMessageException("0 length in cancel message " + pieceNum);
		
		return new BTCancel(new BTInterval(offset, offset + length + -1, pieceNum));
	}

	/**
	 * @return piece number of the requested piece
	 */
	public BTInterval getInterval() {
		return in;
	}

	@Override
    public ByteBuffer getPayload() {
		if (_payload == null) {
			_payload = ByteBuffer.allocate(12);
			_payload.order(ByteOrder.BIG_ENDIAN);
			_payload.putInt(in.getId());
			_payload.putInt(in.get32BitLow());
			_payload.putInt(in.get32BitLength());
			_payload = _payload.asReadOnlyBuffer();
		}
		_payload.clear();
		return _payload;
	}
	

	@Override
    public String toString() {
		return "BTCancel (" + in + ")" ;
	}
}
