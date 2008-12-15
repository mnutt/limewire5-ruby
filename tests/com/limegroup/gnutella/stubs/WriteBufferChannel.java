package com.limegroup.gnutella.stubs;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.WriteObserver;


public class WriteBufferChannel implements ChannelWriter, InterestWritableByteChannel {
    private ByteBuffer buffer;
    private boolean closed = false;
    public InterestWritableByteChannel channel;
    public WriteObserver observer;
    public boolean status;
    private boolean shutdown;
    
    public WriteBufferChannel(int size) {
        buffer = ByteBuffer.allocate(size);
    }
    
    public WriteBufferChannel(ByteBuffer buffer, InterestWritableByteChannel channel) {
        this.buffer = buffer;
        this.channel = channel;
        channel.interestWrite(this, true);
    }
    
    public WriteBufferChannel(byte[] data, InterestWritableByteChannel channel) {
        this(ByteBuffer.wrap(data), channel);
    }
    
    public WriteBufferChannel(byte[] data, int off, int len, InterestWritableByteChannel channel) {
        this(ByteBuffer.wrap(data, off, len), channel);
    }
    
    public WriteBufferChannel() {
        this(0);
    }
    
    public WriteBufferChannel(InterestWritableByteChannel channel) {
        this(ByteBuffer.allocate(0), channel);
    }
    
    public int write(ByteBuffer source) throws IOException {
        int wrote = 0;
        
        if(buffer.hasRemaining()) {
            int remaining = buffer.remaining();
            int adding = source.remaining();
            if(remaining >= adding) {
                buffer.put(source);
                wrote = adding;
            } else {
                int oldLimit = source.limit();
                int position = source.position();
                source.limit(position + remaining);
                buffer.put(source);
                source.limit(oldLimit);
                wrote = remaining;
            }
        }
        
        return wrote;
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
    
    public int written() {
        return buffer.position();
    }
    
    public int remaining() {
        return buffer.remaining();
    }
    
    public ByteBuffer getBuffer() {
        return (ByteBuffer)buffer.flip();
    }
    
    public String getDataAsString() {
        ByteBuffer buffer = getBuffer();
        return new String(buffer.array(), 0, buffer.limit());
    }
    
    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
        channel.interestWrite(this, true);
    }
    
    public void resize(int size) {
        buffer = ByteBuffer.allocate(size);
    }
    
    public void clear() {
        buffer.clear();
    }
    
    public boolean interested() {
        return status;
    }
    
    public void setWriteChannel(InterestWritableByteChannel chan) {
        channel = chan;
    }
    
    public InterestWritableByteChannel getWriteChannel() {
        return channel;
    }
    
    public void interestWrite(WriteObserver observer, boolean status) {
        this.observer = observer;
        this.status = status;
    }
    
    public boolean handleWrite() throws IOException {
        while(buffer.hasRemaining() && channel.write(buffer) > 0);
        if(!buffer.hasRemaining())
            channel.interestWrite(this, false);
        return buffer.hasRemaining();
    }
    
    public void shutdown() {
        shutdown = true;
    }
    
    public void handleIOException(IOException iox) {
        throw (RuntimeException)new UnsupportedOperationException("not implemented").initCause(iox);
    }
    
    public int position() {
        return buffer.position();
    }
    
    public int limit() {
        return buffer.limit();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public boolean hasBufferedOutput() {
        return buffer.hasRemaining() || (channel != null && channel.hasBufferedOutput());
    }   
    
}