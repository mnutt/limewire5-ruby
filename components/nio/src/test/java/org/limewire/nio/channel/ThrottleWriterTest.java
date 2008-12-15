package org.limewire.nio.channel;

import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Tests that ThrottleWriter throttles data correctly.
 */
public final class ThrottleWriterTest extends BaseTestCase {
    
    private FakeThrottle THROTTLE = new FakeThrottle();
    private WriteBufferChannel SINK = new WriteBufferChannel(1024 * 1024);
    private ThrottleWriter WRITER = new ThrottleWriter(THROTTLE, SINK);
    private static Random RND = new Random();
    private WriteBufferChannel SOURCE = new WriteBufferChannel(WRITER);

	public ThrottleWriterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ThrottleWriterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
    public void setUp() {
	    THROTTLE.clear();
	}
	
	public void testInterestAndBandwidthAvailable() throws Exception {
	    assertFalse(SINK.interested());
	    assertEquals(0, THROTTLE.interests());
	    WRITER.interestWrite(SOURCE, false);
	    assertFalse(SINK.interested());
	    assertEquals(0, THROTTLE.interests());
	    WRITER.interestWrite(SOURCE, true);
        assertEquals(1, THROTTLE.interests());
	    assertFalse(SINK.interested());
	    
        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).bandwidthAvailable();	        
	    assertTrue(SINK.interested());
    }
    
    public void testeHandleWriteDataLeft() throws Exception {
        // Test when data is still left after available.

        // set up writer & SINK.
        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).bandwidthAvailable();           
        assertTrue(SINK.interested());
        assertEquals(0, SINK.written());
        
        // set up SOURCE & THROTTLE.
        SOURCE.setBuffer(buffer(data(750)));
        assertEquals(1, THROTTLE.interests());
        THROTTLE.setAvailable(250);
        
        assertFalse(THROTTLE.didRequest());
        assertFalse(THROTTLE.didRelease());
        assertTrue(doWrite()); // still data left to write.
        assertTrue(THROTTLE.didRequest());
        assertTrue(THROTTLE.didRelease());
        
        assertEquals(250, SINK.written()); // only wrote 250
        assertEquals(0, THROTTLE.getAvailable()); // all throttle used up.
        assertEquals(500, SOURCE.remaining()); // data still in source
        assertEquals(2, THROTTLE.interests()); // is interested in more events.
        assertFalse(SINK.interested()); // sink should not give events.
    }
    
    public void testHandleWriteSourceEmptiesWithLeftover() throws Exception {
        SOURCE.setBuffer(buffer(data(500)));
        assertEquals(1, THROTTLE.interests());
        THROTTLE.setAvailable(550);        

        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).bandwidthAvailable();           
        assertTrue(SINK.interested());
        
        assertFalse(doWrite()); // all data written
        assertEquals(500, SINK.written()); // only wrote that we had.
        assertEquals(50, THROTTLE.getAvailable()); // throttle still has data
        assertEquals(1, THROTTLE.interests()); // didn't request interest again.
        assertFalse(SINK.interested()); // sink off.
    }
        
    public void testHandleWriteSourceEmptiesExactly() throws Exception {
        SOURCE.setBuffer(buffer(data(200)));
        assertEquals(1, THROTTLE.interests());
        THROTTLE.setAvailable(200);

        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).bandwidthAvailable();           

        assertEquals(0, SINK.written());
        assertTrue(SINK.interested());
        assertFalse(doWrite());
        assertEquals(1, THROTTLE.interests());
        assertEquals(200, SINK.written());
        assertEquals(0, THROTTLE.getAvailable());
        assertEquals(0, SOURCE.remaining());
        assertFalse(SINK.interested());
    }
    
    public void testHandleWriteSinkFills() throws Exception {
        // test to make sure if sink can't take data that we release &
        // say we wrote it all, even if we still had stuff to write.
        SINK.resize(100);
        SOURCE.setBuffer(buffer(data(250)));
        THROTTLE.setAvailable(300);

        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).bandwidthAvailable();           
        assertEquals(1, THROTTLE.interests());
        
        assertTrue(doWrite()); // data still leftover.
        assertEquals(100, SINK.written());
        assertEquals(150, SOURCE.remaining());
        assertEquals(200, THROTTLE.getAvailable());
        assertEquals(2, THROTTLE.interests());
        assertFalse(SINK.interested());
        
        SINK.resize(150);
        THROTTLE.clear();
        THROTTLE.setAvailable(200);

        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).bandwidthAvailable();           
        assertEquals(0, THROTTLE.interests());
        assertFalse(doWrite());
        assertEquals(150, SINK.written());
        assertEquals(50, THROTTLE.getAvailable());
        assertEquals(0, THROTTLE.interests());
        assertFalse(SINK.interested());
    }
    
    public void testBandwidthAvailableWhenClosed() throws Exception {
        assertFalse(SINK.interested());
        
        for(int i = 0; i < THROTTLE.listeners(); i++)
            assertTrue(THROTTLE.getListener(i).bandwidthAvailable());           
        
        assertTrue(SINK.interested());
        
        SINK.interestWrite(null, false);
        assertFalse(SINK.interested());
        SINK.close();

        for(int i = 0; i < THROTTLE.listeners(); i++)
            assertFalse(THROTTLE.getListener(i).bandwidthAvailable());           
        assertFalse(SINK.interested());
    }   
    
    // tests that if a handleRead is received if there is no bandwidth
    // interest is turned off.
    public void testInterestOffNoBW() throws Exception {
    	// pretend there is bandwidth available
        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).bandwidthAvailable();           
    	assertTrue(SINK.interested());
    	
    	// handleWrite calls come directly from the selector
    	// (no request/releaseBandwidth)
    	WRITER.handleWrite();
    	assertFalse(SINK.interested());
    }

    public void testeHasBufferedOutput() throws Exception {
        SINK.resize(10);
        THROTTLE.setAvailable(2);

        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).requestBandwidth();
        assertFalse(WRITER.hasBufferedOutput());
        WRITER.handleWrite();
        assertFalse(WRITER.hasBufferedOutput());
        assertEquals(2, WRITER.write(buffer(data(2))));
        assertTrue(SINK.hasBufferedOutput());
        assertTrue(WRITER.hasBufferedOutput());
        SINK.getBuffer().clear();
        assertFalse(WRITER.hasBufferedOutput());
        assertEquals(0, WRITER.write(buffer(data(2))));
        assertFalse(WRITER.hasBufferedOutput());
    }

	private byte[] data(int size) {
	    byte[] data = new byte[size];
	    RND.nextBytes(data);
	    return data;
	}
	
	private ByteBuffer buffer(byte[] data) {
	    return ByteBuffer.wrap(data);
	}
	
    
    private boolean doWrite() throws Exception {
        
        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).requestBandwidth();
        boolean ret = WRITER.handleWrite();
        for(int i = 0; i < THROTTLE.listeners(); i++)
            THROTTLE.getListener(i).releaseBandwidth();
        return ret;
    }
}