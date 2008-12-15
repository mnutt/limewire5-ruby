package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.WriteObserver;

public class StubWriteableChannel implements InterestWritableByteChannel {

    private ByteBuffer buffer;
    private boolean closed;

    public StubWriteableChannel(int size) {
        resize(size);
    }

    public StubWriteableChannel() {
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
    
    public void resize(int size) {
        buffer = ByteBuffer.allocate(size);
    }
    
    public boolean hasBufferedOutput() {
        return buffer.position() > 0;
    }

    public void interestWrite(WriteObserver observer, boolean status) {        
    }

    public int write(ByteBuffer src) throws IOException {
        int bytes = src.remaining();
        buffer.put(src);
        return bytes;
    }

    public void close() throws IOException {
        this.closed = true;
    }

    public boolean isOpen() {
        return closed;
    }

    public boolean handleWrite() throws IOException {
        return false;
    }

    public void handleIOException(IOException e) {
        throw new RuntimeException(e);
    }

    public void shutdown() {
        this.closed = true;
    }

}
