package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import org.limewire.nio.channel.ReadBufferChannel;
import org.limewire.util.BaseTestCase;

public class ReadSkipStateTest extends BaseTestCase {
    
    private ByteBuffer BUFFER = ByteBuffer.allocate(1024);
    
    public ReadSkipStateTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ReadSkipStateTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private byte[] data(int len) {
        Random rnd = new Random();
        byte b[] = new byte[len];
        rnd.nextBytes(b);
        return b;
    }
    
    public void testSimpleProcess() throws Exception {
        byte[] data = data(100);
        ByteBuffer dbuf = ByteBuffer.wrap(data); 
        ReadBufferChannel channel = new ReadBufferChannel(dbuf);
        ReadSkipState state = new ReadSkipState(53);
        
        assertFalse(state.process(channel, BUFFER));
        assertFalse(dbuf.hasRemaining());
        assertEquals(47, BUFFER.flip().remaining());
        assertEquals(data, 53, 47, BUFFER.array(), 0, 47);
    }
    
    public void testComplexProcess() throws Exception {
        byte[] data = data(100);
        ByteBuffer dbuf = ByteBuffer.wrap(data);
        ReadBufferChannel channel = new ReadBufferChannel(dbuf);
        ReadSkipState state = new ReadSkipState(53);

        dbuf.limit(49);
        assertTrue(state.process(channel, BUFFER));
        assertTrue(state.process(channel, BUFFER));
        dbuf.limit(55);
        assertFalse(state.process(channel, BUFFER));
        assertFalse(dbuf.hasRemaining());
        assertEquals(2, BUFFER.flip().remaining());
        assertEquals(data, 53, 2, BUFFER.array(), 0, 2);
    }
    
    public void testEOF() throws Exception {
        byte[] data = data(100);
        ByteBuffer dbuf = ByteBuffer.wrap(data);
        ReadBufferChannel channel = new ReadBufferChannel(dbuf, true);
        ReadSkipState state = new ReadSkipState(101);

        try {
            state.process(channel, BUFFER);
            fail("expected exception");
        } catch(IOException iox) {
            assertEquals("EOF", iox.getMessage());
        }
    }
}
