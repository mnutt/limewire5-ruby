package com.limegroup.gnutella;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


import junit.framework.Test;

import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;

public class ClientSideInitialMessagesTest extends ClientSideTestCase {
    protected static int TIMEOUT = 2000;

    public ClientSideInitialMessagesTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideInitialMessagesTest.class);
    }    
   
    @Override
    public int getNumberOfPeers() {
        return 2;
    }
    
    @Override
    public boolean shouldRespondToPing() {
        return false;
    }
    
    public static void setUpQRPTables() {}
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    // BEGIN TESTS
    // ======================================================
    public void testInitialUpMessages() throws Exception {
        BlockingConnection up1 = testUP[0];
        BlockingConnection up2 = testUP[1];
        
        List<Message> queue1 = new ArrayList<Message>();
        List<Message> queue2 = new ArrayList<Message>();

        Thread.sleep(10 * 1000);
        parseWaitingMessages(up1, queue1);
        parseWaitingMessages(up2, queue2);

        // Check that initial messages are sent & received correctly
        assertQueue(queue1);
        assertQueue(queue2);
        
        // One of q1 or q2 is empty -- because the handshake on the first
        // connection will update our capabilities to let us know
        // whether or not we're firewalled.
        if(queue1.isEmpty()) {
            Message extraCapVM = getFirstMessageOfTypeFromQueue(queue2, CapabilitiesVM.class);
            assertNotNull(extraCapVM);
        } else {
            Message extraCapVM = getFirstMessageOfTypeFromQueue(queue1, CapabilitiesVM.class);
            assertNotNull(extraCapVM);
        }
        
        assertEquals(queue1.toString(), 0, queue1.size());
        assertEquals(queue2.toString(), 0, queue2.size());
    }
    
    private void assertQueue(List<Message> q) throws Exception {
        Message mCapVM = getFirstMessageOfTypeFromQueue(q, CapabilitiesVM.class);
        Message mReset = getFirstMessageOfTypeFromQueue(q, ResetTableMessage.class);
        Message mPatch = getFirstMessageOfTypeFromQueue(q, PatchTableMessage.class);
        Message mMsgVM = getFirstMessageOfTypeFromQueue(q, MessagesSupportedVendorMessage.class );
        // Leaf supports PONG CACHING so we don't send it an initial ping.
//      Message mqePingR=getFirstMessageOfTypeFromQueue( PingRequest.class );
        
        assertTrue(mCapVM != null);
        assertTrue(mReset != null);
        assertTrue(mPatch != null);
        assertTrue(mMsgVM != null );
        // See above - NOTE that ClientSideTestCase was responding to a non-existing ping
        //      Request with a PingReply, so the other side USED to get a PingREPLY anyway,
        //      despite supporting PONG CACHING.
//      assertTrue( mqePingR!=null );
    }
    // ======================================================
    
    private void parseWaitingMessages( BlockingConnection con, List<Message> q ) throws Exception {
        try {
            Message m=con.receive( 100 );
            
            while( m!=null ) {
                q.add( m );
                m=con.receive(100);
            }
        } catch (InterruptedIOException ie) {
        }
    }

    private Message getFirstMessageOfTypeFromQueue(List<Message> q,  Class type ) {
        ListIterator li=q.listIterator();
        Message m=null;
        
        while( li.hasNext() ) {
            Message n=(Message)li.next();
            
            if( type.isInstance(n) ) {
                m = n;
                li.remove();
                break;
            }
        }
            
        return m;
    }
    


}
