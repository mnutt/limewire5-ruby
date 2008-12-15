package com.limegroup.gnutella.connection;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for PriorityMessageQueue
 */
public class PriorityMessageQueueTest extends LimeTestCase {
    
    private Mockery context;
        
    /**
     * A non-blank IP.
     */
    private static final byte[] IP = new byte[] { 1, 1, 1, 1 };
    
	public PriorityMessageQueueTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(PriorityMessageQueueTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

   @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
            

    private QueryReply createQueryReply(final int port, final int priority) {
       final QueryReply m1 = context.mock(QueryReply.class);
       context.checking(new Expectations() {{
           allowing(m1).getTTL(); will(returnValue((byte)5));
           atLeast(1).of(m1).getPort(); will(returnValue(port));
           allowing(m1).getIPBytes(); will(returnValue(IP));
           atLeast(1).of(m1).getPriority(); will(returnValue(priority));
       }});
       
       return m1;
    }
    
    private QueryRequest createQueryRequest(final String query, final byte ttl, final byte hops) {
        final QueryRequest m1 = context.mock(QueryRequest.class);
        context.checking(new Expectations() {{
            allowing(m1).getTTL(); will(returnValue(ttl));
            atLeast(1).of(m1).getHops(); will(returnValue(hops));
            allowing(m1).getQuery(); will(returnValue(query));
        }});
        
        return m1;
    }
    
    private PingReply createPingReply(final int port, final byte ttl, final byte hops) {
        final PingReply m1 = context.mock(PingReply.class);
        context.checking(new Expectations() {{
            allowing(m1).getTTL(); will(returnValue(ttl));
            atLeast(1).of(m1).getHops(); will(returnValue(hops));
            allowing(m1).getPort(); will(returnValue(port));
        }});
        
        return m1;
    }
    
   
    public void testLegacy() {

        //By guid volume
        PriorityMessageQueue q=new PriorityMessageQueue(
            1000, Integer.MAX_VALUE, 100);
        
        
        q.add(createQueryReply(6341,1000));
        
        
        q.add(createQueryReply(6331,1000));

        q.add(createQueryReply(6349,9000));
        
        q.add(createQueryReply(6340,0));
        
        Message m=null;
        
        m=q.removeNextInternal();
        assertEquals(6340, ((QueryReply)m).getPort());
        m=q.removeNextInternal();
        assertEquals(6331, ((QueryReply)m).getPort());
        m=q.removeNextInternal();
        assertEquals(6341, ((QueryReply)m).getPort());
        m=q.removeNextInternal();
        assertEquals(6349, ((QueryReply)m).getPort());
        assertNull(q.removeNext());
        m=null;

        //By hops
        q=new PriorityMessageQueue(1000, Integer.MAX_VALUE, 100);
        
        q.add(createQueryRequest("low hops", (byte)5, (byte)0));
        
        q.add(createQueryRequest("high hops", (byte)0, (byte)8));
        
        q.add(createQueryRequest("medium hops", (byte)4, (byte)1));
        
        q.add(createQueryRequest("medium hops2", (byte)4, (byte)1));
        

        QueryRequest query=(QueryRequest)q.removeNextInternal();
        assertEquals("low hops", query.getQuery());
        query=(QueryRequest)q.removeNextInternal();
        assertEquals("medium hops2", query.getQuery());
        query=(QueryRequest)q.removeNextInternal();
        assertEquals("medium hops", query.getQuery());
        query=(QueryRequest)q.removeNextInternal();
        assertEquals("high hops", query.getQuery());
        assertNull(q.removeNextInternal());
        query=null;

        //By negative hops
        q=new PriorityMessageQueue(1000, Integer.MAX_VALUE, 100);
        PingReply pong = createPingReply(6340, (byte)5, (byte)0);
        q.add(pong);
        pong = createPingReply(6330, (byte)5, (byte)0);
        q.add(pong);
        pong = createPingReply(6342, (byte)3, (byte)2);
        q.add(pong);
        pong = createPingReply(6341, (byte)4, (byte)1);
        q.add(pong);
        pong=(PingReply)q.removeNextInternal();
        assertEquals(6342, pong.getPort());
        pong=(PingReply)q.removeNextInternal();
        assertEquals(6341, pong.getPort());
        pong=(PingReply)q.removeNextInternal();
        assertEquals(6330, pong.getPort());
        pong=(PingReply)q.removeNextInternal();
        assertEquals(6340, pong.getPort());
        assertNull(q.removeNextInternal());
        
        context.assertIsSatisfied();
    }
    
}
