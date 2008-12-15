package com.limegroup.bittorrent.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

import org.limewire.nio.CircularByteBuffer;

/**
 * An implementation of <tt>BTDataSource</tt> that delegates to  
 * a provided CircularByteBuffer
 */
class CBCDataSource implements BTDataSource {
    
    private final CircularByteBuffer in;
    
    CBCDataSource(CircularByteBuffer in) {
        this.in = in;
    }
            
    public void discard(int howMuch) {
        in.discard(howMuch);
    }

    public byte get() {
        return in.get();
    }

    public void get(byte[] dest) {
        in.get(dest);
    }

    public long getInt() {
        in.order(ByteOrder.BIG_ENDIAN);
        return in.getInt() & 0xFFFFFFFFL;
    }

    public int size() {
        return in.size();
    }
    
    public void get(ByteBuffer dest) {
        in.get(dest);
    }
    
    public void write(WritableByteChannel to, int howMuch) throws IOException {
        in.write(to, howMuch);
    }

}
