package org.limewire.nio;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import junit.framework.Test;

import org.limewire.nio.channel.ReadBufferChannel;
import org.limewire.nio.channel.WriteBufferChannel;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;



/**
 * Tests some of the functionality of the Circular buffer.  
 *
 */
public class CircularByteBufferTest extends BaseTestCase {

    public CircularByteBufferTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CircularByteBufferTest.class);
    }
    
    static StubCache cache;
    
    @Override
    public void setUp() {
    	cache = new StubCache();
    }
    
    public void testBasic() throws Exception {
        byte [] in = new byte[]{(byte)1,(byte)2};
        CircularByteBuffer buf = new CircularByteBuffer(2,cache);
        assertEquals(2,buf.remainingIn());
        assertEquals(0,buf.remainingOut());
        
        buf.put(ByteBuffer.wrap(in));
        
        assertEquals(0,buf.remainingIn());
        assertEquals(2,buf.remainingOut());
        
        byte [] out = new byte[2];
        buf.get(out);
        
        assertEquals(2,buf.remainingIn());
        assertEquals(0,buf.remainingOut());

        assertEquals(in[0],out[0]);
        assertEquals(in[1],out[1]);
    }
    
    public void testOverflow() throws Exception {
        byte [] in = new byte[]{(byte)1,(byte)2};
        CircularByteBuffer buf = new CircularByteBuffer(1,cache);
        
        assertEquals(1,buf.remainingIn());
        assertEquals(0,buf.remainingOut());
        
        try {
            buf.put(ByteBuffer.wrap(in));
            fail(" should have overflown");
        } catch (BufferOverflowException expected) {}
        
        assertEquals(1,buf.remainingIn());
        assertEquals(0,buf.remainingOut());
    }
    
    public void testUnderflow() throws Exception {
        byte [] in = new byte[]{(byte)1,(byte)2};
        CircularByteBuffer buf = new CircularByteBuffer(2,cache);
        
        buf.put(ByteBuffer.wrap(in));
        
        assertEquals(0,buf.remainingIn());
        assertEquals(2,buf.remainingOut());
        
        byte [] out = new byte[3];
        
        try {
            buf.get(out);
            fail("should have underflown");
        } catch (BufferUnderflowException expected) {}
        
        assertEquals(0,buf.remainingIn());
        assertEquals(2,buf.remainingOut());
    }
    
    public void testWrapAround() throws Exception {
        byte [] in = new byte[30];
        for(int i = 0;i < 20;i++) in[i]=(byte)i;
        
        CircularByteBuffer buf = new CircularByteBuffer(5,cache);
        ReadableByteChannel source = new ReadBufferChannel(in);
        
        // R1 R2 R3 R4 R5
        assertEquals(5,buf.read(source));
        
        assertEquals(0,buf.remainingIn());
        assertEquals(5,buf.remainingOut());
        assertEquals(5, buf.size());
        
        // R1 R2 - - -
        buf.get();buf.get();
        assertEquals(2,buf.remainingIn());
        assertEquals(3,buf.remainingOut());
        assertEquals(2,buf.read(source));
        
        assertEquals(0,buf.remainingIn());
        assertEquals(5,buf.remainingOut());
        
        // W1 W2 W3 W4 W5
        WritableByteChannel sink = new WriteBufferChannel(20);
        assertEquals(5,buf.write(sink));
        assertEquals(5,buf.read(source));
        
        // - - W1 W2 W3
        buf.get();buf.get();
        assertEquals(3,buf.write(sink));
        assertEquals(5,buf.read(source));
        
        // W5 W1 W2 W3 W4
        buf.get();
        assertEquals(1,buf.read(source));
        assertEquals(5,buf.write(sink));
        
        // R5 R1 R2 R3 R4
        assertEquals(5,buf.read(source));
    }
    
    /**
     * tests getting of int with varying endinanness.
     */
    public static void testGetInt() throws Exception {
    	CircularByteBuffer buf = new CircularByteBuffer(4, cache);
    	
    	byte [] data = new byte[4];
    	ByteUtils.int2leb(50, data, 0);
    	ReadableByteChannel source = new ReadBufferChannel(data);
    	buf.read(source);
    	
    	// default order should be big endian
    	assertNotEquals(50,buf.getInt());
    	buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    	source = new ReadBufferChannel(data);
    	buf.read(source);
    	assertEquals(50, buf.getInt());
    	
    	ByteUtils.int2beb(50, data, 0);
    	buf.order(java.nio.ByteOrder.BIG_ENDIAN);
    	source = new ReadBufferChannel(data);
    	buf.read(source);
    	assertEquals(50, buf.getInt());
    }
    
    /**
     * tests that the buffer uses the provided cache.
     */
    public static void testUsesCache() throws Exception {
    	// at first, the cache has not checked out any buffers.
    	assertNull(cache.checkedOut);
    	CircularByteBuffer buf = new CircularByteBuffer(4, cache);
    	byte [] data = new byte[4];
    	ByteUtils.int2leb(50, data, 0);
    	ReadableByteChannel source = new ReadBufferChannel(data);
    	buf.read(source);
    	
    	// after we read, something does get checked out.
    	assertNotNull(cache.checkedOut);
    	ByteBuffer used = cache.checkedOut;
    	
    	// read some more, but not all.  Buffer should be still checked out.
    	buf.get();buf.get();
    	assertEquals(2, buf.size());
    	assertSame(used, cache.checkedOut);
    	
    	// read everything
    	buf.get();buf.get();
    	assertEquals(0,buf.size());
    	assertNull(cache.checkedOut);
    }
    
    /**
     * tests the discarding of data.
     */
    public static void testDiscard() throws Exception {
    	CircularByteBuffer buf = new CircularByteBuffer(4, cache);
    	byte [] data = new byte[4];
    	ByteUtils.int2leb(50, data, 0);
    	ReadableByteChannel source = new ReadBufferChannel(data);
    	buf.read(source);
    	
    	assertEquals(4, buf.size());
    	buf.discard(3);
    	assertEquals(1, buf.size());
    	try {
    		buf.discard(2);
    		fail("bue expected");
    	} catch (BufferUnderflowException expected){}
    	
    	buf.discard(1);
    	assertEquals(0,buf.size());
    }
    
    private static class StubCache extends ByteBufferCache {

    	ByteBuffer checkedOut;
		public StubCache() {}

		@Override
		public void clearCache() {}

		@Override
		public ByteBuffer getDirect() {
			return null;
		}

		@Override
		public ByteBuffer getHeap() {
			fail(" illegal method invocation");
			return null;
		}

		@Override
		public ByteBuffer getHeap(int size) {
			assertNull(checkedOut);
			checkedOut = ByteBuffer.allocate(size);
			return checkedOut;
		}

		@Override
		public long getHeapCacheSize() {
			return 0;
		}

		@Override
		public void release(ByteBuffer buffer) {
			assertSame(checkedOut, buffer);
			checkedOut = null;
		}
    	
    }
}
