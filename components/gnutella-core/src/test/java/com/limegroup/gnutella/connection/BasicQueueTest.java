package com.limegroup.gnutella.connection;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests that the basic queue is just that -- basic.
 */
public class BasicQueueTest extends LimeTestCase {
    
    private BasicQueue QUEUE;
    private Mockery context;
	
    private static final byte[] IP = new byte[] { 1, 1, 1, 1 };
    
	public BasicQueueTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(BasicQueueTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        QUEUE = new BasicQueue();
        context = new Mockery();
    }
        
    // test buffer doesn't get re-ordered.
    public void testBuffer() throws Exception {
        Message m = null;
        
        QUEUE.add(q("first"));
        QUEUE.add(q("second"));
        QUEUE.add(g(7000));
        QUEUE.add(q("third"));
        QUEUE.add(p(4));
        QUEUE.add(p(3));
        QUEUE.add(q("fourth"));
        QUEUE.add(s(6340));
        QUEUE.add(s(6341));
        QUEUE.add(r(6342));
        QUEUE.add(t());
        QUEUE.add(c(1));
        QUEUE.add(q("fifth"));
        QUEUE.add(c(2));
        
        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("first", ((QueryRequest)m).getQuery());

        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("second", ((QueryRequest)m).getQuery());
        
        m = QUEUE.removeNext();
        assertInstanceof(PingReply.class, m);
        assertEquals(7000, ((PingReply)m).getPort());
        
        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("third", ((QueryRequest)m).getQuery());
        
        m = QUEUE.removeNext();
        assertInstanceof(PingRequest.class, m);
        assertEquals(4, m.getTTL());

        m = QUEUE.removeNext();
        assertInstanceof(PingRequest.class, m);
        assertEquals(3, m.getTTL());
        
        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("fourth", ((QueryRequest)m).getQuery());
        
        m = QUEUE.removeNext();
        assertInstanceof(PushRequest.class, m);
        assertEquals(6340, ((PushRequest)m).getPort());
        
        m = QUEUE.removeNext();
        assertInstanceof(PushRequest.class, m);
        assertEquals(6341, ((PushRequest)m).getPort());
        
        m = QUEUE.removeNext();
        assertInstanceof(QueryReply.class, m);
        assertEquals(6342, ((QueryReply)m).getPort());
        
        m = QUEUE.removeNext();
        assertInstanceof(ResetTableMessage.class, m);
        
        m = QUEUE.removeNext();
        assertInstanceof(PatchTableMessage.class, m);
        assertEquals(1, ((PatchTableMessage)m).getSequenceNumber());

        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("fifth", ((QueryRequest)m).getQuery());
        
        m = QUEUE.removeNext();
        assertInstanceof(PatchTableMessage.class, m);
        assertEquals(2, ((PatchTableMessage)m).getSequenceNumber());
        
        assertNull(QUEUE.removeNext());
        
        context.assertIsSatisfied();
    }
    
    private QueryRequest q(final String query) {
        final QueryRequest qr = context.mock(QueryRequest.class);
       
        context.checking(new Expectations() {{
            allowing(qr).getTTL(); will(returnValue((byte)5));
            atLeast(1).of(qr).getQuery(); will(returnValue(query));
        }});
        
        return qr;
    }
    
    private PingReply g(final int port) {
        final PingReply pr = context.mock(PingReply.class);
        
        context.checking(new Expectations() {{
            allowing(pr).getTTL(); will(returnValue((byte)5));
            atLeast(1).of(pr).getPort(); will(returnValue(port));
            allowing(pr).getIPBytes(); will(returnValue(IP));
        }});
        
        return pr;
    }
    
    private PingRequest p(final int ttl) {
        final PingRequest pr = context.mock(PingRequest.class);
        
        context.checking(new Expectations() {{
            atLeast(1).of(pr).getTTL(); will(returnValue((byte)ttl));
        }});
        
        return pr;
    }
    
    private PushRequest s(int port) {
        return new PushRequestImpl(new byte[16], (byte)5, new byte[16], 0, IP, port);
    }
    
    private QueryReply r(final int port) {
        final QueryReply qr = context.mock(QueryReply.class);
        
        context.checking(new Expectations() {{
            allowing(qr).getTTL(); will(returnValue((byte)5));
            atLeast(1).of(qr).getPort(); will(returnValue(port));
            allowing(qr).getIPBytes(); will(returnValue(IP));
            allowing(qr).getSpeed(); will(returnValue(0));
        }});
        
        return qr;

    }
    
    private ResetTableMessage t() {
        return new ResetTableMessage(1024, (byte)2);
    }
    
    private PatchTableMessage c(int sequence) {
        return new PatchTableMessage((short)sequence, (short)sequence, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 5, 9);
    }
}
