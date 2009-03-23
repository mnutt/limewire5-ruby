package com.limegroup.gnutella.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.stubs.WriteBufferChannel;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests that MessageWriter deflates data written to it correctly,
 * passing it on to the source channel.
 */
public final class MessageWriterTest extends LimeTestCase {

    private static final byte[] IP = new byte[] { 1, 1, 1, 1 };
    
    private ConnectionStats STATS = new ConnectionStats();
    private StubQueue QUEUE = new StubQueue();
    private StubSentHandler SENT = new StubSentHandler();
    private WriteBufferChannel SINK = new WriteBufferChannel(1024 * 1024);
    private MessageWriter WRITER = new MessageWriter(STATS, QUEUE, SENT, SINK);

    private MessageFactory messageFactory;

    private QueryRequestFactory queryRequestFactory;

    private PingReplyFactory pingReplyFactory;

	public MessageWriterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(MessageWriterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	protected void setUp() throws Exception {
	    Injector injector = LimeTestUtils.createInjector();
		messageFactory = injector.getInstance(MessageFactory.class);
		queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
		pingReplyFactory = injector.getInstance(PingReplyFactory.class);
	}
	
	public void testSimpleWrite() throws Exception {
	    Message one, two, three;
	    one = q("query one");
	    two = g(7123);
	    three = s(8134);
	    
	    assertEquals(0, STATS.getSent());
	    assertFalse(SINK.interested());
	    
	    WRITER.send(one);
	    WRITER.send(two);
	    WRITER.send(three);
	    assertEquals(3, STATS.getSent());
	    
	    assertTrue(SINK.interested());
	    
	    assertEquals(0, SENT.size());
	    
	    assertFalse(WRITER.handleWrite()); // nothing left to write.
	    assertEquals(3, SENT.size());
	    
	    ByteBuffer buffer = SINK.getBuffer();
	    assertEquals(one.getTotalLength() + two.getTotalLength() + three.getTotalLength(), buffer.limit());
	    ByteArrayInputStream in = new ByteArrayInputStream(buffer.array(), 0, buffer.limit());
	    Message in1, in2, in3;
	    in1 = read(in);
	    in2 = read(in);
	    in3 = read(in);
	    assertEquals(-1, in.read());
	    
	    assertEquals(buffer(one), buffer(in1));
	    assertEquals(buffer(two), buffer(in2));
	    assertEquals(buffer(three), buffer(in3));
	    assertEquals(buffer(one), buffer(SENT.next()));
	    assertEquals(buffer(two), buffer(SENT.next()));
	    assertEquals(buffer(three), buffer(SENT.next()));
	    
	    assertFalse(SINK.interested());
	    assertEquals(3, STATS.getSent());
	}
	
	public void testWritePartialMsg() throws Exception {
	    assertEquals(0, SENT.size());
	    assertEquals(0, STATS.getSent());

	    Message m = q("reaalllllllllly long query");
	    SINK.resize(m.getTotalLength() - 20);
	    
	    WRITER.send(m);
	    assertEquals(1, STATS.getSent());
	    assertTrue(WRITER.handleWrite()); // still stuff left to write.
	    assertTrue(SINK.interested());
	    assertEquals(1, SENT.size()); // it's sent, even though the other side didn't receive it fully yet.
	    assertEquals(buffer(m), buffer(SENT.next()));	    
    	    
	    ByteBuffer buffer = ByteBuffer.allocate(m.getTotalLength());
	    buffer.put(SINK.getBuffer());
	    SINK.resize(100000);
	    
	    assertFalse(WRITER.handleWrite());
	    assertFalse(SINK.interested());
	    buffer.put(SINK.getBuffer());
	    Message in = read((ByteBuffer)buffer.flip());
	    assertEquals(buffer(m), buffer(in));
	}
	
	public void testWritePartialAndMore() throws Exception {
	    Message out1 = q("first long query");
	    Message out2 = q("second long query");
	    Message out3 = q("third long query");
	    assertEquals(0, STATS.getSent());
	    
	    SINK.resize(out1.getTotalLength() + 20);
	    WRITER.send(out1);
	    WRITER.send(out2);
	    assertEquals(2, STATS.getSent());
	    
	    assertEquals(0, SENT.size());
	    assertTrue(WRITER.handleWrite());
	    assertTrue(SINK.interested());
	    assertEquals(2, SENT.size()); // two were sent, one was received.
	    assertEquals(buffer(out1), buffer(SENT.next()));
	    assertEquals(buffer(out2), buffer(SENT.next()));
	    
	    ByteBuffer buffer = ByteBuffer.allocate(1000);
	    buffer.put(SINK.getBuffer()).flip();
	    SINK.resize(10000);
	    
	    read(buffer);
	    assertTrue(buffer.hasRemaining());
	    assertEquals(20, buffer.remaining());
	    buffer.compact();
	    
	    WRITER.send(out3);
	    assertEquals(3, STATS.getSent());
	    assertFalse(WRITER.handleWrite());
	    assertEquals(1, SENT.size());
	    assertEquals(buffer(out3), buffer(SENT.next()));
	    assertFalse(SINK.interested());
	    buffer.put(SINK.getBuffer()).flip();
	    
	    Message in2 = read(buffer);
	    Message in3 = read(buffer);
	    assertTrue(!buffer.hasRemaining());
	    assertEquals(buffer(out2), buffer(in2));
	    assertEquals(buffer(out3), buffer(in3));
    }
    
    public void testDroppingMessagesWhileAdded() throws Exception {
        assertEquals(0, STATS.getSent());
        assertEquals(0, STATS.getSentDropped());
        
        Message m[] = new Message[10];
        for(int i = 0; i < m.length; i++)
            m[i] = g(i+1);

        // Set queue to drop msgs (5 of'm) after the 3rd is added.
        QUEUE.setNumToDrop(4);
        QUEUE.setStartDropIn(3);
        for(int i = 0; i < m.length; i++)
            WRITER.send(m[i]);
        assertEquals(4, STATS.getSentDropped());
        assertEquals(10, STATS.getSent());
        
        assertFalse(WRITER.handleWrite());
        ByteBuffer buffer = SINK.getBuffer();
        Message in[] = read(buffer, 6);
        assertFalse(buffer.hasRemaining());
        assertEquals(6, SENT.size());
        for(int i = 0; i < in.length; i++)
            assertEquals(buffer(m[i+4]), buffer(in[i]));
    }
    
    public void testDroppingMessagesWhileSending() throws Exception {
        assertEquals(0, STATS.getSent());
        assertEquals(0, STATS.getSentDropped());
        
        Message m[] = new Message[10];
        for(int i = 0; i < m.length; i++)
            m[i] = g(i+1);

        // Set queue to drop msgs (5 of'm) after the 3rd is added.
        for(int i = 0; i < m.length; i++)
            WRITER.send(m[i]);
        assertEquals(0, STATS.getSentDropped());
        assertEquals(10, STATS.getSent());
        
        QUEUE.setNumToDrop(4);
        QUEUE.setStartDropIn(3);
        assertFalse(WRITER.handleWrite());
        assertEquals(4, STATS.getSentDropped());
        
        ByteBuffer buffer = SINK.getBuffer();
        Message in[] = read(buffer, 6);
        assertFalse(buffer.hasRemaining());
        assertEquals(6, SENT.size());
        assertEquals(buffer(m[0]), buffer(in[0]));
        assertEquals(buffer(m[1]), buffer(in[1]));
        assertEquals(buffer(m[2]), buffer(in[2])); // started dropping now.
        assertEquals(buffer(m[7]), buffer(in[3])); // finished dropping here.
        assertEquals(buffer(m[8]), buffer(in[4]));
        assertEquals(buffer(m[9]), buffer(in[5]));
    }
	
	private Message read(InputStream in) throws Exception {
	    return messageFactory.read(in, Network.TCP, (byte)100);
    }
    
    private Message read(ByteBuffer buffer) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
        Message m = read(in);
        buffer.position(buffer.position() + m.getTotalLength());
        return m;
    }
    
    private Message[] read(ByteBuffer buffer, int lim) throws Exception {
        Message m[] = new Message[lim];
        int length = 0;
        ByteArrayInputStream in = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
        for(int i = 0; i < lim; i++) {
            m[i] = read(in);
            length += m[i].getTotalLength();
        }
        buffer.position(buffer.position() + length);
        return m;
    }
    
    private ByteBuffer buffer(Message m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        out.flush();
        return ByteBuffer.wrap(out.toByteArray());
    }
    
    private QueryRequest q(String query) {
        return queryRequestFactory.createQuery(query, (byte)5);
    }
    
    private PingReply g(int port) {
        return pingReplyFactory.create(new byte[16], (byte)5, port, IP);
    }
    
    private PushRequest s(int port) {
        return new PushRequestImpl(new byte[16], (byte)5, new byte[16], 0, IP, port);
    }
}
