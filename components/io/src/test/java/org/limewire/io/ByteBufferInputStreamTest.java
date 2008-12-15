package org.limewire.io;

import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class ByteBufferInputStreamTest extends BaseTestCase {

    private static Random RANDOM = new Random();
    
    public ByteBufferInputStreamTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ByteBufferInputStreamTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testAvailable() {
        ByteBufferInputStream in = new ByteBufferInputStream();
        assertEquals(0, in.available());
        in.close();
        assertEquals(0, in.available());
        
        in = new ByteBufferInputStream(buffer(3));
        assertEquals(3, in.available());
        in.read();
        assertEquals(2, in.available());
        in.skip(2);
        assertEquals(0, in.available());
        
        in = new ByteBufferInputStream(buffer(50), buffer(23), buffer(3));
        assertEquals(76, in.available());
        in.skip(72);
        assertEquals(4, in.available());
        in.close();
        assertEquals(0, in.available());
    }
    
    public void testRead() {
        ByteBuffer b1, b2, b3;
        b1 = buffer(1); b2 = buffer(2); b3 = buffer(3);
        ByteBufferInputStream in = new ByteBufferInputStream(b1, b2, b3);
        assertEquals(b1.array()[0], (byte)in.read());
        assertEquals(b2.array()[0], (byte)in.read());
        assertEquals(b2.array()[1], (byte)in.read());
        assertEquals(b3.array()[0], (byte)in.read());
        assertEquals(b3.array()[1], (byte)in.read());
        assertEquals(b3.array()[2], (byte)in.read());
        assertEquals(-1, in.read());
        
        assertFalse(b1.hasRemaining());
        assertFalse(b2.hasRemaining());
        assertFalse(b3.hasRemaining());
    }
    
    public void testReadBulk() {
        ByteBuffer b1, b2, b3;
        b1 = buffer(23); b2 = buffer(34); b3 = buffer(512);
        ByteBufferInputStream in = new ByteBufferInputStream(b1, b2, b3);
        byte[] b = new byte[21];
        assertEquals(21, in.read(b));        // test simple bulk read
        assertEquals(b1.array(), 0, 21, b);
        b = new byte[100];
        assertEquals(30, in.read(b, 0, 30)); // test reading over multiple buffers 
        assertEquals(b1.array(), 21, 2, b, 0, 2);
        assertEquals(b2.array(), 0, 28, b, 2, 28);
        assertEquals(6, in.read(b, 50, 6)); // test reading into middle of array
        assertEquals(b2.array(), 28, 6, b, 50, 6);
        b = new byte[1000];
        assertEquals(512, in.read(b));      // test read finishes
        assertEquals(b3.array(), b, 0, 512);
        assertEquals(-1, in.read());        // test EOF
    }
    
    public void testSkip() {
        ByteBuffer b1, b2, b3;
        b1 = buffer(23); b2 = buffer(34); b3 = buffer(512);
        ByteBufferInputStream in = new ByteBufferInputStream(b1, b2, b3);
        assertEquals(569, in.available());
        assertEquals(20, in.skip(20)); // test skipping within a buffer
        assertEquals(549, in.available());
        
        // make sure what we read is after the skipped portion
        byte[] b = new byte[21];
        assertEquals(21, in.read(b));
        assertEquals(b1.array(), 20, 3, b, 0, 3);
        assertEquals(b2.array(), 0, 18, b, 3, 18);
        
        assertEquals(500, in.skip(500)); // test skipping to another buffer
        assertEquals(28, in.available());
        assertEquals(21, in.read(b));
        assertEquals(b3.array(), 484, 21, b);
        
        assertEquals(7, in.available());
        assertEquals(7, in.skip(100)); // test skipping past EOF
        assertEquals(0, in.skip(1));
        assertEquals(-1, in.read());
    }
    
    public void testReadBuffer() {
        ByteBuffer b1, b2, b3;
        b1 = buffer(23); b2 = buffer(34); b3 = buffer(512);
        ByteBufferInputStream in = new ByteBufferInputStream(b1, b2, b3);
        ByteBuffer b = ByteBuffer.allocate(21);
        assertEquals(21, in.read(b));        // test simple bulk read
        assertEquals(b1.array(), 0, 21, b.array());
        b = ByteBuffer.allocate(100);
        b.limit(30);
        assertEquals(30, in.read(b)); // test reading to limited buffer 
        assertEquals(b1.array(), 21, 2, b.array(), 0, 2);
        assertEquals(b2.array(), 0, 28, b.array(), 2, 28);
        b.limit(56);
        b.position(50);
        assertEquals(6, in.read(b)); // test reading into middle of buffer
        assertEquals(b2.array(), 28, 6, b.array(), 50, 6);
        b = ByteBuffer.allocate(1000);
        assertEquals(512, in.read(b));      // test read finishes
        assertEquals(b3.array(), b.array(), 0, 512);
        assertEquals(-1, in.read());        // test EOF        
    }
    
    public void testBufferFor() {
        ByteBuffer b1, b2, b3;
        b1 = buffer(23); b2 = buffer(34); b3 = buffer(512);
        ByteBufferInputStream in = new ByteBufferInputStream(b1, b2, b3);
        ByteBuffer b = in.bufferFor(21); 
        assertSame(b1.array(), b.array()); // test reference
        assertEquals(21, b.remaining());
        assertEquals(0, b.arrayOffset());
        b = in.bufferFor(30);
        assertNotSame(b1.array(), b.array()); // can't reference if overlap
        assertNotSame(b2.array(), b.array());
        assertEquals(30, b.remaining()); 
        assertEquals(b1.array(), 21, 2, b.array(), 0, 2);
        assertEquals(b2.array(), 0, 28, b.array(), 2, 28);
        assertEquals(0, b.arrayOffset());
        b = in.bufferFor(6);
        assertSame(b2.array(), b.array()); // reference to end of buffer
        assertEquals(6, b.remaining());
        assertEquals(28, b.arrayOffset());
        b = in.bufferFor(1000);
        assertSame(b3.array(), b.array());
        assertEquals(0, b.arrayOffset());
        assertEquals(512, b.limit());
        assertEquals(-1, in.read());        // test EOF        
    }
    
    private ByteBuffer buffer(int len) {
        byte[] b = new byte[len];
        RANDOM.nextBytes(b);
        return ByteBuffer.wrap(b);
    }
}
