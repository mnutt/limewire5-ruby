package com.limegroup.gnutella.connection;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;

/**
 * Tests that the composite queue correctly expires messages & prioritizes
 * things correctly.
 */
public class CompositeQueueTest extends LimeTestCase {
    
    private CompositeQueue QUEUE = new CompositeQueue(3000, 100, 1000, 1);
	
    private final byte[] IP = new byte[] { 1, 1, 1, 1 };

    private QueryRequestFactory queryRequestFactory;

    private PingRequestFactory pingRequestFactory;

    private QueryReplyFactory queryReplyFactory;

    private PingReplyFactory pingReplyFactory;

	public CompositeQueueTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(CompositeQueueTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
    }
        
    public void testControlMessages() throws Exception {
        Mockery mockery = new Mockery();
        final Message m = mockery.mock(VendorMessage.ControlMessage.class);
        final Message m2 = mockery.mock(VendorMessage.ControlMessage.class);
        mockery.checking(new Expectations() {{
            allowing(m).getCreationTime();
            will(returnValue(System.currentTimeMillis() - 100000)); // long ago doesn't matter
            allowing(m2).getCreationTime();
            will(returnValue(System.currentTimeMillis() - 900000));
        }});
        
        
        // add lots of messages before and after the control messages
        for (int i = 0; i < 1000; i++) {
            QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                    0, IP, 6340));
        }
        QUEUE.add(m);
        for (int i = 0; i < 1000; i++) {
            QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                    0, IP, 6340));
        }
        QUEUE.add(m2);
        for (int i = 0; i < 1000; i++) {
            QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                    0, IP, 6340));
        }
        
        // 6 push requests per cycle, after that our control messages.
        for (int i = 0; i < 6; i++)
            assertInstanceof(PushRequest.class, QUEUE.removeNext());
        
        // no reordering either
        assertSame(m, QUEUE.removeNext());
        assertSame(m2, QUEUE.removeNext());
        
        mockery.assertIsSatisfied();
    }
    
    /**
     * This checks to make sure that messages in the outgoing queue are ordered
     * and prioritized correctly.
     * NOTE: It is important to remember that the queues the messages are stored
     * in are BucketQueues -- that means that even if the capacity of the queue is
     * 1, then EACH BUCKET will have 1 item.  So two PingReply's with different
     * hops will BOTH be stored in the queue, because each hop is a different
     * priority, and each priority is a different bucket.
     */
    public void testReorderBuffer() throws Exception {
        Message m=null;

        // send QueryRequest		
		QUEUE.add(queryRequestFactory.createQuery("test", (byte)5));

        // send PingRequest with one hop
        m=pingRequestFactory.createPingRequest((byte)5);
        m.hop();
        QUEUE.add(m);

        // send QueryReply with priority 1
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6340,
                IP, 0, new Response[0], new byte[16], false);
        m.setPriority(30000);
        QUEUE.add(m);
        
        // send 3 push requests
        QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6340));
        QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6341));
        QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6342));
                                 
        // send QueryReply with priority 1
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6343,
                IP, 0, new Response[0], new byte[16], false);
        m.setPriority(30000);
        QUEUE.add(m);
                                 
        // send 4 push requests                                 
        QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6343));
        QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6344));
        QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6345));
        QUEUE.add(new PushRequestImpl(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6346));                                                                                                                                    
                                 
        // send QueryReply with priority 7                                 
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6341,
                IP, 0, new Response[0], new byte[16], false);
        QUEUE.add(m);
        
        // send PingReply with 3 hops
        m=pingReplyFactory.create(new byte[16], (byte)5, 6343, IP);
        m.hop();  m.hop();  m.hop();        
        QUEUE.add(m);
        
        // send PingReply with 3 hops
        m=pingReplyFactory.create(new byte[16], (byte)5, 6344, IP);
        m.hop();  m.hop();  m.hop();
        QUEUE.add(m);        
        
        // send QueryReply with priority 2
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6344,
                IP, 0, new Response[0], new byte[16], false);
        m.setPriority(20000);
        QUEUE.add(m);
        
        // send QueryReply with priority 0
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6345,
                IP, 0, new Response[0], new byte[16], false);
        m.setPriority(50000);
        QUEUE.add(m);              
        
        // send Reset message
        QUEUE.add(new ResetTableMessage(1024, (byte)2));
        
        // send a watchdog pong
        QUEUE.add(pingReplyFactory.create(new byte[16], (byte)1, 6342, IP));
        
        // send PingReply with 2 hops
        m = pingReplyFactory.create(new byte[16], (byte)3, 6340, IP);
        m.hop();
        m.hop();
        QUEUE.add(m);
        
        // send QueryReply with priority 4
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6346,
                IP, 0, new Response[0], new byte[16], false);
        m.setPriority(5000);
        QUEUE.add(m);            
        
        // send QueryReply with priority 5
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6342,
                IP, 0, new Response[0], new byte[16], false);
        m.setPriority(1000);
        QUEUE.add(m);
        
        // send Patch message
        QUEUE.add(new PatchTableMessage((short)1, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 0, 5));
                                       
        // send a watchdog ping                                       
        QUEUE.add(pingRequestFactory.createPingRequest((byte)1));
        
        // send Patch message
        QUEUE.add(new PatchTableMessage((short)2, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 5, 9));

        // send QueryRequest
		QUEUE.add(queryRequestFactory.createQuery("test2", (byte)5));
				 
        // send QueryRequest with 1 hop
		m = queryRequestFactory.createQuery("test far", (byte)5);
        m.hop();
        QUEUE.add(m);
        
        // send QueryRequest with 2 hop
		m = queryRequestFactory.createQuery("test farther", (byte)5);
        m.hop(); m.hop();
        QUEUE.add(m);        
               
        //2. Buffers should look this before emptying:
        // Except for QRP messages, all queues are LIFO when priorities match
        //  WATCHDOG: pong/6342 ping
        //  PUSH: x/6340 x/6341 x/6342 x/6343 x/6344 x/6345 x/6346
        //  QUERY_REPLY: 6345/50k 6340/30k 6343/30k 
        //               6344/20k 6346/5k 6342/1k 6341/0
        //  QUERY: "test farther"/2 "test far"/1, "test"/0, "test2"/0
        //  PING_REPLY: x/6340/2 x/6344/3
        //  PING: x
        //  OTHER: reset patch1 patch2
        
        // cheat 'cause it's easy.
        PrivilegedAccessor.setValue(QUEUE, "_cycled", Boolean.FALSE);

        //3. Read them...now in different order!
        m=QUEUE.removeNext();
        assertInstanceof("Unexpected message: "+m, PingRequest.class, m);
        assertEquals("Unexpected # of hops"+m, 0, m.getHops());

        m=QUEUE.removeNext(); //push        
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6346,
            ((PushRequest)m).getPort());

        m=QUEUE.removeNext(); //push        
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6345,
            ((PushRequest)m).getPort());

        m=QUEUE.removeNext(); //push
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6344,
            ((PushRequest)m).getPort());

        m=QUEUE.removeNext(); //push
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6343,
            ((PushRequest)m).getPort());
            
        m=QUEUE.removeNext(); //push        
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6342,
            ((PushRequest)m).getPort());

        m=QUEUE.removeNext(); //push        
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6341,
            ((PushRequest)m).getPort());            

        m=QUEUE.removeNext(); //reply/6341 (high priority)
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port.  priority: "
             + m.getPriority(), 6341, ((QueryReply)m).getPort());

        m=QUEUE.removeNext(); //reply/6342 (medium priority)
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected query reply port", 6342,
            ((QueryReply)m).getPort());
            
        m=QUEUE.removeNext(); //reply/6346
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6346, ((QueryReply)m).getPort());

        m=QUEUE.removeNext(); //reply/6344
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6344, ((QueryReply)m).getPort());            
        
        m=QUEUE.removeNext(); //reply/6343
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6343, ((QueryReply)m).getPort());

        m=QUEUE.removeNext(); //reply/6340
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6340, ((QueryReply)m).getPort());

        m=QUEUE.removeNext(); //query "test2"/0
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "test2",
            ((QueryRequest)m).getQuery());
            
        m=QUEUE.removeNext(); //query "test"/0
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "test",
            ((QueryRequest)m).getQuery());            

        m=QUEUE.removeNext(); //query "test far"/1
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query",
            "test far", ((QueryRequest)m).getQuery());

        m=QUEUE.removeNext(); //reply 6343
        assertInstanceof("m not a pingreply", PingReply.class, m);
        assertEquals("unexpected pingreply port",
            6344, ((PingReply)m).getPort());

        m=QUEUE.removeNext(); //ping 6340
        assertInstanceof("m not a pingrequest", PingRequest.class, m);
        assertGreaterThan("unexpected number of hops (>0)",
            0, m.getHops());

        m=QUEUE.removeNext(); //QRP reset
        assertInstanceof("m not a resettablemessage",
            ResetTableMessage.class, m);

        m=QUEUE.removeNext(); //watchdog pong/6342
        assertInstanceof("m not a pingreply", PingReply.class, m);
        assertEquals("unexpected pingreply port",
            6342, ((PingReply)m).getPort());
        assertEquals("unexpected number of hops",
            0, m.getHops());  //watchdog response pong

        m=QUEUE.removeNext(); //push
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6340,
            ((PushRequest)m).getPort());            

        m=QUEUE.removeNext(); //reply/6341 (high priority)
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port.  priority: "
             + m.getPriority(),  6345, ((QueryReply)m).getPort());
             
        m=QUEUE.removeNext(); //query "test farther"/2
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query",
            "test farther", ((QueryRequest)m).getQuery());             

        m=QUEUE.removeNext(); //reply 6340
        assertInstanceof("m not a pingreply", PingReply.class, m);
        assertEquals("unexpected pingreply port",
            6340, ((PingReply)m).getPort());

        m=QUEUE.removeNext(); //QRP patch1
        assertInstanceof("m not a patchtablemessage",
            PatchTableMessage.class, m);
        assertEquals("unexpected patchtablemessage sequencenumber",
            1, ((PatchTableMessage)m).getSequenceNumber());

        m=QUEUE.removeNext(); //QRP patch2
        assertInstanceof("m not a patchtable message",
            PatchTableMessage.class, m);
        assertEquals("unexpected patchtablemessage sequencenumber",
            2, ((PatchTableMessage)m).getSequenceNumber());
    }

    /**
     * Test to make sure that messages properly timeout in the message
     * queues and are dropped.
     */
    public void testBufferTimeout() throws Exception {
        //Drop one message
        QUEUE.add(queryRequestFactory.createQuery("0", (byte)3));   
        sleep(1200);
        QUEUE.add(queryRequestFactory.createQuery("1200", (byte)3)); 
        
        Message m = QUEUE.removeNext();
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "1200", ((QueryRequest)m).getQuery());
        assertNull(QUEUE.removeNext());
        assertEquals(1, QUEUE.resetDropped());

        //Drop many messages
        QUEUE.add(queryRequestFactory.createQuery("0", (byte)3));   
        sleep(300);
        QUEUE.add(queryRequestFactory.createQuery("300", (byte)3));        
        sleep(300);
        QUEUE.add(queryRequestFactory.createQuery("600", (byte)3));        
        sleep(500);
        QUEUE.add(queryRequestFactory.createQuery("1100", (byte)3));
        sleep(900);
        QUEUE.add(queryRequestFactory.createQuery("2000", (byte)3));
        m=QUEUE.removeNext();
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "2000", ((QueryRequest)m).getQuery());
        m=QUEUE.removeNext();
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", ((QueryRequest)m).getQuery(), "1100");
        assertNull(QUEUE.removeNext());
        assertEquals("unexpected # of dropped sent messages", 3, QUEUE.resetDropped());
    }
    
    public void testPriorityHint() throws Exception {
        //Tests wrap-around loop of sendQueuedMessages
        Message m=null;

        // head...tail
        QUEUE.add(hopped(pingRequestFactory.createPingRequest((byte)4)));
        QUEUE.add(queryRequestFactory.createQuery("a", (byte)3));
        assertInstanceof("didn't recieve queryrequest", QueryRequest.class, QUEUE.removeNext());
        assertInstanceof("didn't recieve pingrequest", PingRequest.class, QUEUE.removeNext());
        assertNull(QUEUE.removeNext()); // force it to reset the current cycle.

        //tail...<wrap>...head
        QUEUE.add(queryRequestFactory.createQuery("a", (byte)3));
        QUEUE.add(hopped(pingRequestFactory.createPingRequest((byte)5)));
        assertInstanceof("didn't recieve pingrequest", PingRequest.class, QUEUE.removeNext());
        assertInstanceof("didn't recieve queryrequest", QueryRequest.class, QUEUE.removeNext());
        assertNull(QUEUE.removeNext()); // force it to reset the current cycle.

        //tail...<wrap>...head
        //  WATCHDOG: ping
        //  PUSH:
        //  QUERY_REPLY: reply
        //  QUERY: query
        //  PING_REPLY: 
        //  PING: 
        //  OTHER: reset
        QUEUE.add(pingRequestFactory.createPingRequest((byte)1));
        QUEUE.add(queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6341,
                IP, 0, new Response[0], new byte[16], false));
        QUEUE.add(new ResetTableMessage(1024, (byte)2));
        QUEUE.add(queryRequestFactory.createQuery("a", (byte)3));
        m=QUEUE.removeNext();
        assertInstanceof("Got: "+m, QueryRequest.class, m);
        m=QUEUE.removeNext();
        assertInstanceof("GOt: " +m, ResetTableMessage.class, m);
        m=QUEUE.removeNext();
        assertInstanceof("Got: " + m, PingRequest.class, m);
        m=QUEUE.removeNext();
        assertInstanceof("Got: " + m, QueryReply.class, m);
    }

    private static Message hopped(Message m) {
        m.hop();
        return m;
    }

    private static void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
    }  
}
