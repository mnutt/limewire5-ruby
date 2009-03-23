package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * Defines BitTorrent messages as defined in the specification,
 * http://www.bittorrent.org/beps/bep_0003.html#all-non-keepalive-messages-start-with-a-single-byte-which-gives-their-type.
 * Additionally, <code>BTMessage</code> parses data from the network.
 */
public abstract class BTMessage {
	// private static final Log LOG = LogFactory.getLog(BTMessage.class);

	/*
	 * final message identifiers
	 */
	public static final byte KEEP_ALIVE = (byte) 0xFF;

	public static final byte CHOKE = 0x00;

	public static final byte UNCHOKE = 0x01;

	public static final byte INTERESTED = 0x02;

	public static final byte NOT_INTERESTED = 0x03;

	public static final byte HAVE = 0x04;

	public static final byte BITFIELD = 0x05;

	public static final byte REQUEST = 0x06;

	public static final byte PIECE = 0x07;

	public static final byte CANCEL = 0x08;

	/**
	 * Buffer used for messages without payload
	 * TODO: use the one from BufferUtils once backported
	 */
	public static final ByteBuffer EMPTY_PAYLOAD;

	// initialize ByteBuffer for this message
	static {
		EMPTY_PAYLOAD = ByteBuffer.allocate(0);
	}

	/*
	 * the identifier of a message
	 */
	private final byte _type;

	/**
	 * A private constructor that will be called by parseMessage()
	 * 
	 * @param type
	 *            a byte setting the type of the message
	 */
	BTMessage(byte type) {
		_type = type;
	}

	/**
	 * Accessor for the message's type
	 * 
	 * @return byte identifying the message's type
	 */
	public byte getType() {
		return _type;
	}

	/**
	 * Accessor for the message's payload without message length
	 * 
	 * @return ByteBuffer, the payload
	 */
	public abstract ByteBuffer getPayload();
	
	/**
	 * @return true if this message should not be buffered for batch sending.
	 */
	public boolean isUrgent() {
		return false;
	}

	/**
	 * Reads a BTMessage from a given <tt>ByteBuffer</tt>. Removes the ranges
	 * read from the buffer and moves the rest to the front of the Buffer.
	 * 
	 * @param in
	 *            the <tt>ByteBuffer</tt> to read from
	 * @param type the BitTorrent message type
	 * @return new BTMessages of unknown type
	 * @throws BadBTMessageException
	 */
	public static BTMessage parseMessage(ByteBuffer in, int type)
			throws BadBTMessageException {
		// in case this is a keep alive message, length is not necessarily > 0
		// the reason we do not return here is that we reset the buffer below.
		
		switch (type) {
		case CHOKE:
			return BTChoke.createMessage();
		case UNCHOKE:
			return BTUnchoke.createMessage();
		case INTERESTED:
			return BTInterested.createMessage();
		case NOT_INTERESTED:
			return BTNotInterested.createMessage();
		case HAVE:
			return BTHave.readMessage(in);
		case BITFIELD:
			throw new BadBTMessageException("unexpected bitfield");
		case REQUEST:
			return BTRequest.readMessage(in);
		case PIECE:
			throw new IllegalArgumentException("do not parse pieces here");
		case CANCEL:
			return BTCancel.readMessage(in);
		default:
			throw new BadBTMessageException("unknown message, type " + type);
		}
	}
}
