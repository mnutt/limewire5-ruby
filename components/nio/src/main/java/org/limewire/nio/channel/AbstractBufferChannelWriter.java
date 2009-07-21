package org.limewire.nio.channel;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/** 
 * A simple channel that buffers data in a <code>ByteBuffer</code>. 
 */
public abstract class AbstractBufferChannelWriter implements ChannelWriter {
    
    /**
     * The sink channel we write to & interest ourselves on.
     */
    protected InterestWritableByteChannel channel;
    
    protected ByteBuffer buffer;
    
    protected volatile boolean shutdown;
    
    public AbstractBufferChannelWriter(int bufferSize) {
        buffer = ByteBuffer.allocate(bufferSize);
    }
    
    /** The channel we're writing to. */
    public synchronized InterestWritableByteChannel getWriteChannel() {
        return channel;
    }
    
    /** The channel we're writing to. */
    public synchronized void setWriteChannel(InterestWritableByteChannel channel) {
        this.channel = channel;
        channel.interestWrite(this, true);
    }
    
    /**
     * Adds <code>data</code> to the buffer and signals interest in writing to
     * the channel.
     * 
     * @throws IOException if the channel is already shutdown
     * @throws BufferOverflowException if there is insufficient space in the buffer 
     */
    public synchronized void put(byte[] data) throws IOException {
        if (shutdown) {
            throw new EOFException();
        }
        
        buffer.put(data);
        
        if(channel != null)
            channel.interestWrite(this, true);
    }
    
    
    /**
     * Writes as many messages as possible to the sink.
     */
    public synchronized boolean handleWrite() throws IOException {
        if(channel == null)
            throw new IllegalStateException("writing with no source.");
            
        buffer.flip();
        while (buffer.hasRemaining() && channel.write(buffer) > 0)
            ;
        
        boolean remaining = buffer.hasRemaining();
        if (remaining) {
            buffer.compact();
            return true;
        } else {
            buffer.clear();
            channel.interestWrite(this, false);
            return false;
        }
    }

    /**
     * Does nothing.
     */
    public void handleIOException(IOException iox) {
        // ignore
    }
    
    public void shutdown() {
        shutdown = true;
    }

}