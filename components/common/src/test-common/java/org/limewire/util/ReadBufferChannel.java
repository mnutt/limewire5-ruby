package org.limewire.util;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;


public class ReadBufferChannel implements ReadableByteChannel {
    private ByteBuffer buffer;
    private boolean useEOF;
    private boolean closed = false;
    
    public ReadBufferChannel() {
        this(new byte[0]);
    }
    
    public ReadBufferChannel(ByteBuffer source, boolean useEOF) {
        this.buffer = source;
        this.useEOF = useEOF;
    }
    
    public ReadBufferChannel(ByteBuffer source) {
        this(source, false);
    }
    
    public ReadBufferChannel(byte[] source) {
        this(ByteBuffer.wrap(source));
    }
    
    public ReadBufferChannel(byte[] source, boolean useEOF) {
        this(ByteBuffer.wrap(source), useEOF);
    }
    
    public ReadBufferChannel(byte[] source, int off, int len) {
        this(ByteBuffer.wrap(source, off, len));
    }
    
    public void setBuffer(ByteBuffer data) {
        this.buffer = data;
    }
    
    public void setUseEOF(boolean eof) {
        this.useEOF = eof;
    }
    
    public void clear() {
        this.closed = false;
    }
    
    public ByteBuffer getBuffer() {
        return buffer;
    }
    
    public int read(ByteBuffer toBuffer) {
        int read = 0;
        
        int remaining = buffer.remaining();
        int toRemaining = toBuffer.remaining();
        if(toRemaining >= remaining) {
            toBuffer.put(buffer);
            read += remaining;
        } else {
            int limit = buffer.limit();
            int position = buffer.position();
            buffer.limit(position + toRemaining);
            toBuffer.put(buffer);
            read += toRemaining;
            buffer.limit(limit);
        }
        
        if(read == 0 && useEOF)
            return -1;
        else
            return read;
    }
    
    public boolean isOpen() {
        return !closed;
    }
    
    public void close() throws IOException {
        closed = true;
    }
    
    public void setClosed(boolean closed) {
        this.closed = closed;
    }
    
}