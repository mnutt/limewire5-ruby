package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.limewire.core.settings.ConnectionSettings;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.GnutellaFileListStub;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerStub;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * tests the server side handling of head pings through UDP.
 */
public class ServerSideHeadTest extends LimeTestCase {
	
	public ServerSideHeadTest(String name) {
		super(name);
	}
	
    public static Test suite() {
        return buildTestSuite(ServerSideHeadTest.class);
    }
    
    private DatagramSocket socket1, socket2;
    private int port1, port2;
    private InetSocketAddress addr1, addr2;
    private HeadPing ping1, ping2;
    private LifecycleManager lifecycleManager;
    private MessageRouter messageRouter;
    private MessageFactory messageFactory;

    
    @Override
    protected void setUp() throws Exception {

    	ConnectionSettings.SOLICITED_GRACE_PERIOD.setValue(1000l);
    	ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    	
    	port1 = 10000;
    	port2 = 20000;
    	
    	socket1 = new DatagramSocket(port1);
    	socket2 = new DatagramSocket(port2);

    	socket1.setSoTimeout(300);
    	socket2.setSoTimeout(300);
    	

    	ping1 = new HeadPing(GnutellaFileListStub.DEFAULT_URN);
    	ping2 = new HeadPing(URN.createSHA1Urn(FileDescStub.DEFAULT_URN));

    	ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    	ping1.write(baos1);
    	ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    	ping2.write(baos2);
    	
    	addr1 = new InetSocketAddress(InetAddress.getLocalHost(), port1);
    	addr2 = new InetSocketAddress(InetAddress.getLocalHost(), port2);

    	
    	Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
    	    @Override
    	    protected void configure() {
    	        bind(FileManager.class).to(FileManagerStub.class);
    	    } 
    	});
    	
    	lifecycleManager = injector.getInstance(LifecycleManager.class);
        messageRouter = injector.getInstance(MessageRouter.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        
    	lifecycleManager.start();
    }
    
    @Override
    public void tearDown() throws Exception {
        socket1.close();
        socket2.close();
        lifecycleManager.shutdown();
    }
  
    public void testGeneralBehavior() throws Exception{
    	
    	MessageRouter router = messageRouter;
    	router.handleUDPMessage(ping1,addr1);
    	Thread.sleep(100);
    	
    	DatagramPacket received = new DatagramPacket(new byte[1024],1024);
    	socket1.receive(received);
    	
    	HeadPong pong = (HeadPong) 
			messageFactory.read(new ByteArrayInputStream(received.getData()), Network.TCP);
    	
    	assertTrue(Arrays.equals(ping1.getGUID(),pong.getGUID()));
    	
		router.handleUDPMessage(ping2,addr2);
		Thread.sleep(100);

    	received = new DatagramPacket(new byte[1024],1024);
    	socket2.receive(received);
    	pong = (HeadPong) 
			messageFactory.read(new ByteArrayInputStream(received.getData()), Network.TCP);
    	
    	assertTrue(Arrays.equals(ping2.getGUID(),pong.getGUID()));
    	
    	//and if we sleep some time, we can send from the first host again
    	Thread.sleep(ConnectionSettings.SOLICITED_GRACE_PERIOD.getValue());
    	
    	router.handleUDPMessage(ping1,addr1);
    	Thread.sleep(100);
    	received = new DatagramPacket(new byte[1024],1024);
    	socket1.receive(received);
    	pong = (HeadPong) 
			messageFactory.read(new ByteArrayInputStream(received.getData()), Network.TCP);
    	
    	assertTrue(Arrays.equals(ping1.getGUID(),pong.getGUID()));
    }
    
}
