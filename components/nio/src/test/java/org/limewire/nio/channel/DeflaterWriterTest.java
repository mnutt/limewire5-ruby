package org.limewire.nio.channel;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Tests that DeflaterWriter deflates data written to it correctly,
 * passing it on to the source channel.
 */
public final class DeflaterWriterTest extends BaseTestCase {
    
    private Deflater DEFLATER = new Deflater();
    private WriteBufferChannel SINK = new WriteBufferChannel(1024 * 1024);
    private DeflaterWriter WRITER = new DeflaterWriter(DEFLATER, SINK);
    private static Random RND = new Random();
    private WriteBufferChannel SOURCE = new WriteBufferChannel(WRITER);
    private Inflater INFLATER = new Inflater();

	public DeflaterWriterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DeflaterWriterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
    public void tearDown() {
	    DEFLATER.end();
	    INFLATER.end();
	}
	
	public void testSimpleDeflation() throws Exception {
	    byte[] data = data(10 * 1024);
	    SOURCE.setBuffer(buffer(data));
	    assertFalse(WRITER.handleWrite()); // should have been able to write everything.
	    
	    ByteBuffer deflated = SINK.getBuffer();
	    assertGreaterThan(0, deflated.limit());
	    byte[] inflated = inflate(deflated);
	    assertEquals(data, inflated);
	}
	
	public void testSourceEmptiesAndFillsRepeatedly() throws Exception {
	    byte[] data = data(3 * 1024);
	    SOURCE.setBuffer(buffer(data));
	    assertFalse(WRITER.handleWrite()); // should have been able to write everything.
	    
	    ByteBuffer deflated = SINK.getBuffer();
	    assertGreaterThan(0, deflated.limit());
	    byte[] inflated = inflate(deflated);
	    assertEquals(data, inflated);
	    
	    SINK.clear();
	    data = data(8 * 1024 + 127);
	    SOURCE.setBuffer(buffer(data));
	    assertFalse(WRITER.handleWrite());
	    deflated = SINK.getBuffer();
	    assertGreaterThan(0, deflated.limit());
	    inflated = inflate(deflated);
	    assertEquals(data, inflated);
	    
	    SINK.clear();
	    data = data(123615);
	    SOURCE.setBuffer(buffer(data));
	    assertFalse(WRITER.handleWrite());
	    deflated = SINK.getBuffer();
	    assertGreaterThan(0, deflated.limit());
	    inflated = inflate(deflated);
	    assertTrue(Arrays.equals(data, inflated));
	    assertEquals(data, inflated);
    }
    
    public void testSinkFillsAndEmptiesRepeatedly() throws Exception {
        byte[] data = data(52 * 1024);
        ByteBuffer compare = ByteBuffer.wrap(data);
        ByteBuffer out = ByteBuffer.allocate(data.length);
        SOURCE.setBuffer(buffer(data));
        
        SINK.resize(100);
        assertTrue(WRITER.handleWrite()); // still data to write.
        ByteBuffer deflated = SINK.getBuffer();
        assertGreaterThan(0, deflated.limit());
        byte[] inflated = inflate(deflated);
        assertGreaterThan(0, inflated.length);
        int position = 0;
        int limit = inflated.length + position;
        assertEquals(compare.limit(limit).position(position), buffer(inflated));
        out.put(inflated);
        assertEquals(out.position(), compare.limit());        
        
        SINK.resize(5000);
        assertTrue(WRITER.handleWrite()); // still data to write.
        deflated = SINK.getBuffer();
        assertGreaterThan(0, deflated.limit());
        inflated = inflate(deflated);
        assertGreaterThan(0, inflated.length);
        position = compare.limit();
        limit = inflated.length + position;
        assertEquals(compare.limit(limit).position(position), buffer(inflated));
        out.put(inflated);
        assertEquals(out.position(), compare.limit());
        
        SINK.resize(13535);
        assertTrue(WRITER.handleWrite()); // still data to write.
        deflated = SINK.getBuffer();
        assertGreaterThan(0, deflated.limit());
        inflated = inflate(deflated);
        assertGreaterThan(0, inflated.length);
        position = compare.limit();
        limit = inflated.length + position;
        assertEquals(compare.limit(limit).position(position), buffer(inflated));
        out.put(inflated);
        assertEquals(out.position(), compare.limit());
        
        SINK.resize(data.length * 2); // top it off.
        assertFalse(WRITER.handleWrite()); // nothing left to write.
        deflated = SINK.getBuffer();
        assertGreaterThan(0, deflated.limit());
        inflated = inflate(deflated);
        assertGreaterThan(0, inflated.length);
        position = compare.limit();
        limit = inflated.length + position;
        assertEquals(compare.limit(limit).position(position), buffer(inflated));
        out.put(inflated);
        assertEquals(out.position(), compare.limit());
        
        assertEquals(buffer(data), out.flip());
    }
	
    public void testHasBufferedOutput() throws Exception {
        assertFalse(WRITER.hasBufferedOutput());
        WRITER.write(ByteBuffer.wrap("Hello".getBytes()));
        assertTrue(WRITER.hasBufferedOutput());
        WRITER.handleWrite();
        assertTrue(SINK.hasBufferedOutput());
        assertTrue("expected data in SINK", WRITER.hasBufferedOutput());
        SINK.getBuffer().clear();
        assertFalse(WRITER.hasBufferedOutput());

        SINK.getBuffer().limit(2);
        WRITER.write(ByteBuffer.wrap("Hello".getBytes()));
        assertTrue(WRITER.hasBufferedOutput());
        WRITER.handleWrite();
        SINK.getBuffer().clear();
        assertFalse(SINK.hasBufferedOutput());
        assertTrue("expected data in WRITER.outgoing", WRITER.hasBufferedOutput());
    }

	private byte[] data(int size) {
	    byte[] data = new byte[size];
	   // for(int i = 0; i < size; i++)
	     //   data[i] = (byte)(i % 10);
	    RND.nextBytes(data);
	    return data;
	}
	
	private ByteBuffer buffer(byte[] data) {
	    return ByteBuffer.wrap(data);
	}
	
	private byte[] inflate(ByteBuffer data) throws Exception {
	    INFLATER.setInput(data.array(), 0, data.limit());
	    byte[] buf = new byte[512];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
	    int inflated = 0;
	    while( (inflated = INFLATER.inflate(buf)) > 0)
	        out.write(buf, 0, inflated);
	    return out.toByteArray();
    }
}