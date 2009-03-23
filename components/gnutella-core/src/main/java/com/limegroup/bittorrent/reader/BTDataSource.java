package com.limegroup.bittorrent.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Interface describing the functionality needed by the parsing machine
 * from a data container
 */
interface BTDataSource {
    /** returns a 32-bit unsigned int value */
	public long getInt();
    
	public byte get();
	public void discard(int howMuch);
	public int size();
	public void get(byte [] dest);
	public void get(ByteBuffer dest);
	public void write(WritableByteChannel to, int number) throws IOException;
}
