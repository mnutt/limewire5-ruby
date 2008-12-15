package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.io.IOUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackRedirect;
import com.limegroup.gnutella.util.EmptyResponder;

/**
 *  Tests that an Ultrapeer correctly handles connect back redirect messages.
 *
 *  ULTRAPEER[0]  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER[1]
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  This test only covers Ultrapeer behavior - leaves don't participate in
 *  server side connect back stuff.
 */
@SuppressWarnings( { "cast" } )
public final class ServerSideConnectBackRedirectTest extends ServerSideTestCase {

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

    public ServerSideConnectBackRedirectTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideConnectBackRedirectTest.class);
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
	    
	    UDP_ACCESS = new DatagramSocket();
        TCP_ACCESS = new ServerSocket(TCP_ACCESS_PORT);

        LEAF[0] = blockingConnectionFactory.createConnection("localhost", PORT);
        LEAF[0].initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000);
        
	    exchangeCapabilities();
	}
	
	@Override
	public void tearDown() throws Exception {
	    super.tearDown();
	    if (TCP_ACCESS != null) {
	        TCP_ACCESS.close();
	    }
	}
	
    public void exchangeCapabilities() throws Exception {
		assertTrue("LEAF[0] should be connected", LEAF[0].isOpen());

        //  Give the connection a chance to send its initial messages
        Thread.sleep( 1000*2 );        
        
        MessagesSupportedVendorMessage msvm = 
            (MessagesSupportedVendorMessage)BlockingConnectionUtils.getFirstMessageOfType(LEAF[0],
                MessagesSupportedVendorMessage.class, 500);
        assertNotNull(msvm);
        assertGreaterThan(0, msvm.supportsTCPConnectBackRedirect());
        assertGreaterThan(0, msvm.supportsUDPConnectBackRedirect());
    }

    public void testUDPConnectBackRedirect() throws Exception {
        drainAll();
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
            new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                       UDP_ACCESS.getLocalPort());
        
        LEAF[0].send(udp);
        LEAF[0].flush();
        
        // we should NOT get a ping request over our UDP socket....
        UDP_ACCESS.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            UDP_ACCESS.receive(pack);
            fail("got UDP msg");
        } catch (IOException good) {}
 
        cbGuid = new GUID(GUID.makeGuid());
        udp = new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                         UDP_ACCESS.getLocalPort());
        
        ULTRAPEER[0].send(udp);
        ULTRAPEER[0].flush();

        // we should get a ping reply over our UDP socket....
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                Message m = messageFactory.read(in, Network.TCP);
                if (m instanceof PingRequest) {
                    PingRequest reply = (PingRequest) m; 
                    assertEquals(new GUID(reply.getGUID()), cbGuid);
                    break;
                }
            } catch (IOException bad) {
                fail("got IOX", bad);
           }
        }
    }


    public void testUDPConnectBackAlreadyConnected() throws Exception {
        drainAll();

        DatagramSocket tempSock = new DatagramSocket(LEAF[0].getSocket().getLocalPort());
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
        new UDPConnectBackRedirect(cbGuid, LEAF[0].getSocket().getInetAddress(),
                                   LEAF[0].getSocket().getLocalPort());
        
        ULTRAPEER[0].send(udp);
        ULTRAPEER[0].flush();

        // we should NOT get a ping request over our UDP socket....
        tempSock.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            tempSock.receive(pack);
            fail("got UDP msg");
        } catch (IOException good) {}

        tempSock.close();
    }

    
    public void testTCPTSLConnectBackRedirect() throws Exception {
        drainAll();

        // first attempt with be with TLS
        TCP_ACCESS.close();
        TCP_ACCESS = SSLServerSocketFactory.getDefault().createServerSocket(TCP_ACCESS_PORT);
        SSLServerSocket sslss = (SSLServerSocket)TCP_ACCESS;
        sslss.setEnabledCipherSuites(new String[]{"TLS_DH_anon_WITH_AES_128_CBC_SHA"});
        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                       TCP_ACCESS.getLocalPort());
        
        LEAF[0].send(tcp);
        LEAF[0].flush();
        
        // we should NOT get a incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            fail("got IOX");
        } catch (IOException good) {}

        tcp = new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                         TCP_ACCESS.getLocalPort());
        
        ULTRAPEER[0].send(tcp);
        ULTRAPEER[0].flush();

        // we should get a incoming connection
        try {
            Socket x = TCP_ACCESS.accept();
            // just like Acceptor reads words.
            String word = IOUtils.readLargestWord(x.getInputStream(), 8);
            assertEquals("CONNECT", word);
        } catch (IOException bad) {
            fail("got IOX", bad);
        }
    }

    public void testTCPConnectBackAlreadyConnected() throws Exception {
        drainAll();

        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(LEAF[0].getSocket().getInetAddress(),
                                       TCP_ACCESS.getLocalPort());
        
        ULTRAPEER[0].send(tcp);
        ULTRAPEER[0].flush();

        // we should NOT get an incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            fail("got TCP connection");
        } catch (IOException good) {}
    }


    public void testConnectBackExpirer() throws Exception {
        drainAll();
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
            new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                       UDP_ACCESS.getLocalPort());
        
        ULTRAPEER[1].send(udp);
        ULTRAPEER[1].flush();
        
        // we should NOT get a ping request over our UDP because we just did this
        UDP_ACCESS.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            UDP_ACCESS.receive(pack);
            fail("got UDP msg");
        } catch (IOException good) {}

        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                       TCP_ACCESS.getLocalPort());
        
        ULTRAPEER[1].send(tcp);
        ULTRAPEER[1].flush();
        
        // we should NOT get a incoming connection since we did this already
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            fail("got TCP connection");
        } catch (IOException good) {}

        // simulate the running of the thread - technically i'm not testing
        // the situation precisely, but i'm confident the schedule work so the
        // abstraction isn't terrible
        Thread cbThread = new Thread(new MessageRouterImpl.ConnectBackExpirer());
        cbThread.start();
        cbThread.join();

        // now these two things should work....
        cbGuid = new GUID(GUID.makeGuid());
        udp = new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                         UDP_ACCESS.getLocalPort());
        
        ULTRAPEER[1].send(udp);
        ULTRAPEER[1].flush();
        
        // we should get a ping request over our UDP
        UDP_ACCESS.setSoTimeout(1000);
        // we should get a ping reply over our UDP socket....
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                Message m = messageFactory.read(in, Network.TCP);
                if (m instanceof PingRequest) {
                    PingRequest reply = (PingRequest) m; 
                    assertEquals(new GUID(reply.getGUID()), cbGuid);
                    break;
                }
            } catch (IOException bad) {
                fail("got IOX", bad);
            }
        }

        tcp = new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                         TCP_ACCESS.getLocalPort());
        
        ULTRAPEER[1].send(tcp);
        ULTRAPEER[1].flush();
        
        // we should get a incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            Socket x = TCP_ACCESS.accept();
            byte[] read = new byte["CONNECT BACK\r\n\r\n".length() + 1];
            int n = x.getInputStream().read(read);
            assertGreaterThan(read.length - 1, n); // tls
            assertFalse((new String(read).contains("CONNECT BACK"))); // encrypted
        } catch (IOException bad) {
            fail("got IOX", bad);
        }
    }


    // ------------------------------------------------------

}
