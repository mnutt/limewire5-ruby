package com.limegroup.gnutella.guess;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Test;

import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;

import com.google.inject.Injector;
import com.limegroup.gnutella.Backend;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.util.LimeTestCase;

/** Tests the Server Side of GUESS....
 */ 
public class GUESSServerSideTest extends LimeTestCase {

	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

	private final int SOCKET_TIMEOUT = 5*1000; // 5 second wait for a message

    private DatagramSocket _socket = null;

    private PingRequestFactory pingRequestFactory;

    private QueryRequestFactory queryRequestFactory;

    private MessageFactory messageFactory;
    
    private MACCalculatorRepositoryManager macManager;

    
    public GUESSServerSideTest(String name) {
        super(name);
        //System.out.println("YOU MUST RUN THIS TEST WITH A ULTRAPEER WITH A NON-FORCED ADDRESS!!!  KEEP IN MIND THAT THIS TEST MAY NOT ALWAYS WORK - IT DOES USE UDP AFTER ALL!!");
		//_backend = Backend.createBackend(0);
        //_backend.start();
        //try {
            // wait for backend to get up to speed
        //  Thread.sleep(3000);
        //}
        //catch (Exception whatever) {}
    }

    public static Test suite() {
        return buildTestSuite(GUESSServerSideTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
        launchBackend();
		Injector injector = LimeTestUtils.createInjector();
		pingRequestFactory = injector.getInstance(PingRequestFactory.class);
		queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
		messageFactory = injector.getInstance(MessageFactory.class);
		macManager = injector.getInstance(MACCalculatorRepositoryManager.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testThisOnly() throws Exception  {
        //RouterService rs = _backend.getRouterService();
        InetAddress address = InetAddress.getLocalHost();
        //}
        //catch (UnknownHostException damn) {
            //cleanUp();
        //  fail("Couldn't get the local address!!!!");
        //}            
        //int port = rs.getPort();

		//try {
			_socket = new DatagramSocket();
			_socket.setSoTimeout(SOCKET_TIMEOUT);
            //} 
            //catch (SocketException e) {
            //fail("unexpected exception: "+e);
            //cleanUp();
			//return;
            //}
            //catch (RuntimeException e) {
            //cleanUp();
			//return;
            //}

        // first try to get a AddressSecurityToken....
        PingRequest pr = pingRequestFactory.createQueryKeyRequest();
        AddressSecurityToken qkToUse = null;
        send(pr, address, Backend.BACKEND_PORT);
        //try {
            PingReply pRep = (PingReply) receive();
            assertNotNull("query key should not be null", pRep.getQueryKey());
            qkToUse = pRep.getQueryKey();
            //}
            //catch (Exception damn) {
            //damn.printStackTrace();
            // cleanUp();
            //assertTrue("Couldn't get a AddressSecurityToken!!", false);
            //}

        // send a normal ping, should get a pong....
        pr = pingRequestFactory.createPingRequest((byte)1);
        send(pr, address, Backend.BACKEND_PORT);
        //try {
            pRep = (PingReply) receive();
            assertNull("query key should be null", pRep.getQueryKey());
            //}
            //catch (Exception damn) {
            //damn.printStackTrace();
            //cleanUp();
            //assertTrue("Didn't get a Pong!!", false);
            //}

        // first try a bad AddressSecurityToken....
        byte[] fakeQueryKey = new byte[8];
        (new Random()).nextBytes(fakeQueryKey);

		QueryRequest crapQuery = 
			queryRequestFactory.createQueryKeyQuery("susheel", 
											 new AddressSecurityToken(fakeQueryKey, macManager));
        //QueryRequest crapQuery = 
		//  new QueryRequest(GUID.makeGuid(), (byte) 1, 0, "susheel", null, 
		//                   false, null, null, 
		//                   AddressSecurityToken.getQueryKey(fakeQueryKey, true), false);
        send(crapQuery, address, Backend.BACKEND_PORT);
        try {
            receive();
            //cleanUp();
            assertTrue("Fake Query Key worked!!", false);
        }
        catch (Exception shouldHappen) {
        }

        // now try a legit one....
        //byte[] guid = GUID.makeGuid();
        //QueryRequest goodQuery = 
		//  new QueryRequest(guid, (byte) 1, 0, "susheel", null, 
		//                   false, null, null, qkToUse, false);

        // now try a legit one....
		QueryRequest goodQuery =
			queryRequestFactory.createQueryKeyQuery("susheel", qkToUse);

		byte[] guid = goodQuery.getGUID();
        send(goodQuery, address, Backend.BACKEND_PORT);
        //try {
            pRep = (PingReply) receive();
            assertTrue("guids should be equal",
                       Arrays.equals(pRep.getGUID(), guid));
            //}
            //catch (Exception damn) {
            // damn.printStackTrace();
            //cleanUp();
            //assertTrue("Good Query Key didn't work!!", false);
            //}
                             
        //cleanUp();
    }

//      private void cleanUp() {
//          Thread newThread = new Thread() {
//                  public void run() {
//                      try {
//                          sleep(1*1000);
//                      }
//                      catch (Exception whatever) {}
//                      // the LAST thing i need to do....
//                      _socket.close();
//                      _backend.shutdown("GUESSServerSideTest is OUTTIE....");
//                  }
//              };
//          newThread.start();
//      }

    private Message receive() throws Exception {
		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                     BUFFER_SIZE);
        _socket.receive(datagram);
        byte[] data = datagram.getData();
        // construct a message out of it...
        InputStream in = new ByteArrayInputStream(data);
        Message message = messageFactory.read(in, Network.TCP);		
        return message;
    }

    private void send(Message msg, InetAddress ip, int port) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			msg.write(baos);
		} catch(IOException e) {
			e.printStackTrace();
			// can't send the hit, so return
			return;
		}

		byte[] data = baos.toByteArray();
		DatagramPacket dg = new DatagramPacket(data, data.length, ip, port); 
		try {
            _socket.send(dg);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}


}
