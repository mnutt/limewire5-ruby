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

@SuppressWarnings("unchecked")
public class ClientSideInitialMessagesTest extends ClientSideTestCase {
    protected static int TIMEOUT = 2000;

    private List /*of Messsage*/ _queue;

    public ClientSideInitialMessagesTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideInitialMessagesTest.class);
    }    
   
    @Override
    public int getNumberOfPeers() {
        return 1;
    }
    
    @Override
    public boolean shouldRespondToPing() {
        return false;
    }
    
    public static void setUpQRPTables() {}
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        _queue=new ArrayList(10);
    }

    // BEGIN TESTS
    // ======================================================
    public void testInitialUpMessages() throws Exception {
        BlockingConnection up=testUP[0];
        
        Thread.sleep( 10*1000 );
        parseWaitingMessages( up );
        
        // Check that initial messages are sent & received correctly
        Message mCapVM=getFirstMessageOfTypeFromQueue( CapabilitiesVM.class );
        Message mReset=getFirstMessageOfTypeFromQueue( ResetTableMessage.class );
        Message mPatch=getFirstMessageOfTypeFromQueue( PatchTableMessage.class );
        Message mMsgVM=getFirstMessageOfTypeFromQueue( MessagesSupportedVendorMessage.class );
        // Leaf supports PONG CACHING so we don't send it an initial ping.
//      Message mqePingR=getFirstMessageOfTypeFromQueue( PingRequest.class );
        
        assertTrue( mCapVM!=null );
        assertTrue( mReset!=null );
        assertTrue( mPatch!=null );
        assertTrue( mMsgVM!=null );
        // See above - NOTE that ClientSideTestCase was responding to a non-existing ping
        //      Request with a PingReply, so the other side USED to get a PingREPLY anyway,
        //      despite supporting PONG CACHING.
//      assertTrue( mqePingR!=null );
        
        assertEquals( "UP messages queue not empty" + _queue, 0, _queue.size() );
    }
    // ======================================================
    
    private void parseWaitingMessages( BlockingConnection con ) throws Exception {
        try {
            Message m=con.receive( 100 );
            
            while( m!=null ) {
                _queue.add( m );
                m=con.receive(100);
            }
        } catch (InterruptedIOException ie) {
        }
    }

    private Message getFirstMessageOfTypeFromQueue( Class type ) {
        ListIterator li=_queue.listIterator();
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
