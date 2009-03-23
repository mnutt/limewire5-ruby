package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import org.limewire.io.GUID;

import junit.framework.Test;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.util.EmptyResponder;

/**
 *  Tests that an Ultrapeer correctly handles connect back redirect messages.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  This test only covers Ultrapeer behavior - leaves don't participate in
 *  server side connect back stuff.
 */
@SuppressWarnings( { "cast" } )
public final class ServerSideIntermediateRedirectTest 
    extends ServerSideTestCase {

    private final int TIMEOUT = 2000;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private DatagramSocket UDP_ACCESS;

    /**
     * Just a TCP connection to use for testing.
     */
    private ServerSocket TCP_ACCESS;

    /**
     * The port for TCP_ACCESS
     */ 
    private final int TCP_ACCESS_PORT = 10776;

    private MessageFactory messageFactory;

    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    public ServerSideIntermediateRedirectTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideIntermediateRedirectTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	@Override
	public int getNumberOfUltrapeers() {
	    return 2;
    }

	@Override
	public int getNumberOfLeafpeers() {
	    return 1;
    }
	
	@Override
	protected void setUp() throws Exception {
	    Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
	    super.setUp(injector);
	    messageFactory = injector.getInstance(MessageFactory.class);
	    messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
	    
	    UDP_ACCESS = new DatagramSocket();
        TCP_ACCESS = new ServerSocket(TCP_ACCESS_PORT);
	}
	
	@Override
	public void tearDown() throws Exception {
	    super.tearDown();
	    if (TCP_ACCESS != null) {
	        TCP_ACCESS.close();
	    }
	}
	
    public void testNoRedirectCandidates() throws Exception {
    
        // ok, currently there are no redirect ultrapeers, so we've changed
        // the behavior - the Ultrapeer should just drop them on the floor
        TCPConnectBackVendorMessage tcp = 
            new TCPConnectBackVendorMessage(TCP_ACCESS_PORT);
        LEAF[0].send(tcp);
        LEAF[0].flush();

        try {
            TCP_ACCESS.setSoTimeout(TIMEOUT);
            TCP_ACCESS.accept();
            fail("got a socket");
        } catch (InterruptedIOException expected) {}

        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackVendorMessage udp =
            new UDPConnectBackVendorMessage(UDP_ACCESS.getLocalPort(), cbGuid);
        LEAF[0].send(udp);
        LEAF[0].flush();

        try {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            UDP_ACCESS.setSoTimeout(TIMEOUT);
            UDP_ACCESS.receive(pack);
            ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
            messageFactory.read(bais, Network.TCP);
            fail("Got a message");
        } catch (InterruptedIOException expected) {}
    }


    public void testSendsRedirect() throws Exception {

        BlockingConnection redirUP = blockingConnectionFactory.createConnection("localhost", PORT);
        
        redirUP.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        assertTrue(redirUP.isOpen());
        BlockingConnectionUtils.drain(redirUP);

        Message msvm = messagesSupportedVendorMessage;
        redirUP.send(msvm);
        redirUP.flush();

        Thread.sleep(1000);

        // now actually test....
        { // make sure tcp vm's are redirected
            TCPConnectBackVendorMessage tcp = 
                new TCPConnectBackVendorMessage(TCP_ACCESS_PORT);
            LEAF[0].send(tcp);
            LEAF[0].flush();

            TCPConnectBackRedirect tcpR = (TCPConnectBackRedirect)
                BlockingConnectionUtils.getFirstInstanceOfMessageType(redirUP, 
                                              TCPConnectBackRedirect.class,
                                              TIMEOUT);
            assertNotNull(tcpR);
            assertEquals(TCP_ACCESS_PORT, tcpR.getConnectBackPort());
            assertEquals("127.0.0.1", tcpR.getConnectBackAddress().getHostAddress());
        }

        { // make sure udp vm's are redirected
            GUID cbGuid = new GUID(GUID.makeGuid());
            UDPConnectBackVendorMessage udp =
                new UDPConnectBackVendorMessage(UDP_ACCESS.getLocalPort(), 
                                                cbGuid);
            LEAF[0].send(udp);
            LEAF[0].flush();

            UDPConnectBackRedirect udpR = (UDPConnectBackRedirect)
                BlockingConnectionUtils.getFirstInstanceOfMessageType(redirUP, 
                                              UDPConnectBackRedirect.class, 
                                              TIMEOUT);
            assertNotNull(udpR);
            assertEquals(UDP_ACCESS.getLocalPort(), udpR.getConnectBackPort());
            assertEquals("127.0.0.1", udpR.getConnectBackAddress().getHostAddress());
            assertEquals(cbGuid, udpR.getConnectBackGUID());
        }

        redirUP.close();
    }


    public void testSendsRedirectMultiple() throws Exception {

        BlockingConnection redirUP1 = blockingConnectionFactory.createConnection("localhost", PORT);
        redirUP1.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        assertTrue(redirUP1.isOpen());
        BlockingConnectionUtils.drain(redirUP1);

        Message msvm = messagesSupportedVendorMessage;
        redirUP1.send(msvm);
        redirUP1.flush();

        BlockingConnection redirUP2 = blockingConnectionFactory.createConnection("localhost", PORT);
        redirUP2.initialize( headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        assertTrue(redirUP2.isOpen());
        BlockingConnectionUtils.drain(redirUP2);

        msvm = messagesSupportedVendorMessage;
        redirUP2.send(msvm);
        redirUP2.flush();


        Thread.sleep(1000);

        // now actually test....
        { // make sure tcp vm's are redirected
            TCPConnectBackVendorMessage tcp = 
                new TCPConnectBackVendorMessage(TCP_ACCESS_PORT);
            LEAF[0].send(tcp);
            LEAF[0].flush();

            TCPConnectBackRedirect tcpR = (TCPConnectBackRedirect)
                BlockingConnectionUtils.getFirstInstanceOfMessageType(redirUP1, 
                                              TCPConnectBackRedirect.class,
                                              TIMEOUT);
            if (tcpR == null)
                tcpR = (TCPConnectBackRedirect)
                    BlockingConnectionUtils.getFirstInstanceOfMessageType(redirUP2, 
                                                  TCPConnectBackRedirect.class,
                                                  TIMEOUT);
            assertNotNull(tcpR);
            assertEquals(TCP_ACCESS_PORT, tcpR.getConnectBackPort());
            assertEquals("127.0.0.1", tcpR.getConnectBackAddress().getHostAddress());
        }

        { // make sure udp vm's are redirected
            GUID cbGuid = new GUID(GUID.makeGuid());
            UDPConnectBackVendorMessage udp =
                new UDPConnectBackVendorMessage(UDP_ACCESS.getLocalPort(), 
                                                cbGuid);
            LEAF[0].send(udp);
            LEAF[0].flush();

            UDPConnectBackRedirect udpR = (UDPConnectBackRedirect)
                BlockingConnectionUtils.getFirstInstanceOfMessageType(redirUP1, 
                                              UDPConnectBackRedirect.class, 
                                              TIMEOUT);
            if (udpR == null)
                udpR = (UDPConnectBackRedirect)
                    BlockingConnectionUtils.getFirstInstanceOfMessageType(redirUP2, 
                                                  UDPConnectBackRedirect.class,
                                                  TIMEOUT);
            assertNotNull(udpR);
            assertEquals(UDP_ACCESS.getLocalPort(), udpR.getConnectBackPort());
            assertEquals("127.0.0.1", udpR.getConnectBackAddress().getHostAddress());
            assertEquals(cbGuid, udpR.getConnectBackGUID());
        }

        redirUP1.close();
        redirUP2.close();
    }
    

    // ------------------------------------------------------

}
