package org.limewire.util;

import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

public class BufferUtilsTest extends BaseTestCase {
    
    public BufferUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BufferUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testDelete()throws Exception {
        byte[] data = data(100);
        byte[] copy = new byte[100];
        System.arraycopy(data, 0, copy, 0, 100);
        ByteBuffer buf = ByteBuffer.wrap(copy).compact();
        
        assertEquals(100, buf.position());
        for(int i = 0; i < 100; i++)
            assertEquals("wrong data at: " + i, buf.get(i), data[i]);
        assertEquals(0, BufferUtils.delete(buf, 1));
        assertEquals(99, buf.position());
        for(int i = 0; i < 99; i++)
            assertEquals("wrong data at: " + i, buf.get(i), data[i+1]);
        
        assertEquals(51, BufferUtils.delete(buf, 150));
        assertEquals(0, buf.position());
        
        data = data(100);
        buf = ByteBuffer.wrap(data).compact();
        assertEquals(0, BufferUtils.delete(buf, 100));
        assertEquals(0, buf.position());
    }
    
    public void testTransfer() throws Exception {
        byte[] srcData = data(100);
        ByteBuffer src = buffer(srcData).compact();
        ByteBuffer dst = ByteBuffer.allocate(100);
        assertEquals(100, src.position());
        assertEquals(100, BufferUtils.transfer(src, dst));
        assertEquals(0, src.position());
        assertEquals(srcData, dst.array());
    }
    
    public void testTransferNoFlip() throws Exception {
        byte[] srcData = data(100);
        ByteBuffer src = buffer(srcData);
        ByteBuffer dst = ByteBuffer.allocate(100);
        assertTrue(src.hasRemaining());
        assertEquals(100, BufferUtils.transfer(src, dst, false));
        assertFalse(src.hasRemaining());
        assertEquals(srcData, dst.array());
    }
    
    public void testReadAll() throws Exception {
        byte[] bufferData = data(100);
        ByteBuffer bufferSrc = buffer(bufferData).compact();
        
        byte[] channelData = data(100);
        ByteBuffer channelBuf = buffer(channelData);
        ReadBufferChannel channelSrc = new ReadBufferChannel(channelBuf);
        
        ByteBuffer dst = ByteBuffer.allocate(500);
        
        assertEquals(100, bufferSrc.position());
        assertEquals(0, BufferUtils.readAll(bufferSrc, channelSrc, dst));
        assertEquals(0, bufferSrc.position());
        assertFalse(channelBuf.hasRemaining());
        assertEquals(bufferData, dst.array(), 0, 100);
        assertEquals(channelData, dst.array(), 100, 100);
        assertEquals(0, BufferUtils.readAll(bufferSrc, channelSrc, dst));
        assertEquals(0, bufferSrc.position());
        assertFalse(channelBuf.hasRemaining());
        assertEquals(bufferData, dst.array(), 0, 100);
        assertEquals(channelData, dst.array(), 100, 100);
        
        channelData = data(100);
        channelSrc = new ReadBufferChannel(buffer(channelData), true);
        assertEquals(-1, BufferUtils.readAll(bufferSrc, channelSrc, dst));
        assertEquals(channelData, dst.array(), 200, 100);
    }
    
    private ByteBuffer buffer(byte[] b) {
        byte[] copy = new byte[b.length];
        System.arraycopy(b, 0, copy, 0, copy.length);
        ByteBuffer buf = ByteBuffer.wrap(copy);
        return buf;
    }
    
    private byte[] data(int len) {
        Random rnd = new Random();
        byte[] b = new byte[len];
        rnd.nextBytes(b);
        return b;
    }
}
