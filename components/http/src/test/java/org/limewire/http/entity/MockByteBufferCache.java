package org.limewire.http.entity;

import java.nio.ByteBuffer;

import org.limewire.nio.ByteBufferCache;

public class MockByteBufferCache extends ByteBufferCache {

    public int bytes;

    public int buffers;

    @Override
    public ByteBuffer getHeap(int size) {
        bytes += size;
        buffers++;
        return super.getHeap(size);
    }

    @Override
    public void release(ByteBuffer buffer) {
        bytes -= buffer.capacity();
        buffers--;
        super.release(buffer);
    }

}
