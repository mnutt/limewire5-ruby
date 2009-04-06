package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Iterator;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.GUID;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.UrnCache;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;

/**
 * Checks whether a leaf node handles GUESS queries correctly.  Tests the
 * Server side of GUESS.
 */
public class ServerSideLeafGuessTest extends ClientSideTestCase {
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    private NetworkManagerStub networkManagerStub;

    private PingRequestFactory pingRequestFactory;

    private QueryRequestFactory queryRequestFactory;

    private UrnCache urnCache;

    private MessageFactory messageFactory;
    
    private MACCalculatorRepositoryManager macManager;

    public ServerSideLeafGuessTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideLeafGuessTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new LimeTestUtils.NetworkManagerStubModule(networkManagerStub));
        super.setUp(injector);
        
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        urnCache = injector.getInstance(UrnCache.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        macManager = injector.getInstance(MACCalculatorRepositoryManager.class);

        // first we need to set up GUESS capability
        networkManagerStub.setCanReceiveSolicited(true);
        networkManagerStub.setCanReceiveUnsolicited(true);
        networkManagerStub.setGuessCapable(true);
        networkManagerStub.setAcceptedIncomingConnection(true);
        
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(1000 * 20);

        for (int i = 0; i < testUP.length; i++) {
            BlockingConnectionUtils.drain(testUP[i]);
            // OOB client side needs server side leaf guidance
            testUP[i].send(messagesSupportedVendorMessage);
            testUP[i].flush();
        }

    }
    
    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testBasicProtocol() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        // first send a AddressSecurityToken request....
        send(pingRequestFactory.createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a AddressSecurityToken....
        Message m = null;
        AddressSecurityToken qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // we should be able to send a query
        QueryRequest goodQuery = queryRequestFactory.createQueryKeyQuery("susheel", 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, SERVER_PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // followed by a query hit
        m = receive();
        assertTrue(m instanceof QueryReply);
        QueryReply qRep = (QueryReply) m;
        assertEquals(new GUID(guid), new GUID(qRep.getGUID()));

    }

    public void testGoodURNQuery() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        // first send a AddressSecurityToken request....
        send(pingRequestFactory.createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a AddressSecurityToken....
        Message m = null;
        AddressSecurityToken qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // now send a URN query, make sure that works....
        File berkeley = TestUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        Iterator iter = UrnHelper.calculateAndCacheURN(berkeley, urnCache).iterator();
        URN berkeleyURN = (URN) iter.next();
        while (!berkeleyURN.isSHA1())
            berkeleyURN = (URN) iter.next();
        
        // we should be able to send a URN query
        QueryRequest goodQuery = queryRequestFactory.createQueryKeyQuery(berkeleyURN, 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, SERVER_PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // followed by a query hit with a URN
        m = receive();
        assertTrue(m instanceof QueryReply);
        QueryReply qRep = (QueryReply) m;
        assertEquals(new GUID(guid), new GUID(qRep.getGUID()));
        iter = qRep.getResults();
        Response first = (Response) iter.next();
        assertTrue(UrnHelper.calculateAndCacheURN(berkeley, urnCache).containsAll(first.getUrns()));
    }


    public void testQueryWithNoHit() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        // first send a AddressSecurityToken request....
        send(pingRequestFactory.createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a AddressSecurityToken....
        Message m = null;
        AddressSecurityToken qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // send a query that shouldn't get results....
        QueryRequest goodQuery = queryRequestFactory.createQueryKeyQuery("anita", 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, SERVER_PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // but not a query hit
        try { 
            m = receive();
            assertTrue(false);
        }
        catch (InterruptedIOException expected) {};
    }


    public void testBadQueryKey() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();

        AddressSecurityToken qkToUse = new AddressSecurityToken(localHost, 0, macManager);
        assertNotNull(qkToUse);

        {
            // we shouldn't get any response to our query...
            QueryRequest badQuery = queryRequestFactory.createQueryKeyQuery("susheel", 
                                                                      qkToUse);
            send(badQuery, localHost, SERVER_PORT);
            
            try {
                // now we should NOT get an ack            
                receive();
                assertTrue(false);
            } catch (InterruptedIOException expected) {}
        }
    }
    


    //////////////////////////////////////////////////////////////////

    private Message receive() throws Exception {
		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                     BUFFER_SIZE);
        UDP_ACCESS.receive(datagram);
        byte[] data = datagram.getData();
        // construct a message out of it...
        InputStream in = new ByteArrayInputStream(data);
        Message message = messageFactory.read(in, Network.TCP);		
        return message;
    }

    private void send(Message msg, InetAddress ip, int port) 
        throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.write(baos);

		byte[] data = baos.toByteArray();
		DatagramPacket dg = new DatagramPacket(data, data.length, ip, port); 
        UDP_ACCESS.send(dg);
	}

    @Override
    public int getNumberOfPeers() {
        return 3;
    }

}

