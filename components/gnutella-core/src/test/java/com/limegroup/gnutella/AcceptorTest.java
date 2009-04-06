package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.connection.FirewallStatus;
import org.limewire.core.api.connection.FirewallStatusEvent;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.nio.ssl.TLSNIOSocket;
import org.limewire.util.OSUtils;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class AcceptorTest extends LimeTestCase {
    
    private static final Log LOG = LogFactory.getLog(AcceptorTest.class);
    
    private Injector injector;
    private AcceptorImpl acceptor;
    private ConnectionDispatcher connectionDispatcher;
    private StubCM connectionManager;
    private StubAC activityCallback;

    private SocketsManager socketsManager;

    public AcceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AcceptorTest.class);
    }

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    @Override
    public void setUp() throws Exception {
        connectionManager = new StubCM();
        activityCallback = new StubAC();
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).toInstance(connectionManager);
                bind(ActivityCallback.class).toInstance(activityCallback);
                
            }
        });

       injector.getInstance(Key.get(new TypeLiteral<ListenerSupport<FirewallStatusEvent>>(){})).addListener(activityCallback);

        connectionDispatcher = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
        socketsManager = injector.getInstance(SocketsManager.class);
        
        acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setIncomingExpireTime(2000);
        acceptor.setTimeBetweenValidates(2000);
        acceptor.setWaitTimeAfterRequests(2000);
        acceptor.start();
        
        //shut off the various services,
        //if an exception is thrown, something bad happened.
        acceptor.setListeningPort(0);
    }
    
    @Override
    public void tearDown() throws Exception {
        //shut off the various services,
        //if an exception is thrown, something bad happened.
        acceptor.setListeningPort(0);
        ConnectionSettings.LOCAL_IS_PRIVATE.revertToDefault();
    }
    
    public void testValidateIncomingTimer() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        int port = bindAcceptor();
        
        assertEquals(0, activityCallback.getChanges());
        assertFalse(activityCallback.getLastStatus());
        connectionManager.setShouldSendRequests(true);
        assertTrue(connectionManager.awaitSend());
        
        // make sure we don't send it again which could screw the test!
        connectionManager.setShouldSendRequests(false);
        
        Thread.sleep(500); // Make sure the resetter gets scheduled.
        
        // Turn incoming on, make sure it triggers a change...
        activityCallback.resetLatch();
        acceptor.setIncoming(true);
        activityCallback.waitForSingleChange(true);
        assertEquals(1, activityCallback.getChanges());
        assertTrue(activityCallback.getLastStatus());
        
        // Make sure we revert back to false, since no incoming came...
        connectionManager.setShouldSendRequests(true);
        assertTrue(connectionManager.awaitSend());
        connectionManager.setShouldSendRequests(false);
        assertFalse(activityCallback.waitForNextChange());
        assertEquals(2, activityCallback.getChanges());
       
        // Turn incoming on, make sure we get the status...
        activityCallback.resetLatch();
        acceptor.setIncoming(true);
        activityCallback.waitForSingleChange(true);
        assertEquals(3, activityCallback.getChanges());
        assertTrue(activityCallback.getLastStatus());
        
        // Send another request, but this time we're gonna make sure
        // incoming stays on!
        connectionManager.setShouldSendRequests(true);
        assertTrue(connectionManager.awaitSend());
        connectionManager.setShouldSendRequests(false);
        
        // Now send the connectback..
        Socket socket = socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port), 1000);
        socket.getOutputStream().write(StringUtils.toAsciiBytes("CONNECT BACK\r\n"));
        socket.getOutputStream().flush();
        IOUtils.close(socket);
        
        // Sleep a bit, make sure incoming is still on!
        Thread.sleep(10000);
        
        // Note that the last status of CM.shouldSendRequests is false,
        // so that future checks won't schedule a resetter.
        // We're only concerned with the one that we waited for 
        // and send a connectback from.
        // This just validates that if we receive a connectback,
        // the resetter is cancelled.
                
        assertTrue(acceptor.acceptedIncoming());
        assertEquals(3, activityCallback.getChanges());
        assertTrue(activityCallback.getLastStatus());
    }
        
    /**
     * This test checks to ensure that Acceptor.setListeningPort
     * cannot use a port if the UDP part is already bound.
     */
    public void testCannotUseBoundUDPPort() {        
        int portToTry = 2000;
        DatagramSocket udp = null;
        while (true) {
            // get a free port for UDP traffic.
            try {
                udp = new DatagramSocket(portToTry);
                break;
            }
            catch (IOException e) {
                portToTry++;
                continue;
            }
        }

        try {
            acceptor.setListeningPort(portToTry);
            assertTrue("had no trouble binding UDP port!", false);
        } catch (IOException expected) {
            IOUtils.close(udp);
        }
    }
        
    /**
     * This test checks to ensure that Acceptor.setListeningPort
     * cannot use port if the TCP part is already bound.
     */
    public void testCannotUseBoundTCPPort() {
        int portToTry = 2000;
        ServerSocket tcp = null;
        while (true) {
            // get a free port for UDP traffic.
            try {
                tcp = new ServerSocket(portToTry);
                break;
            } catch (IOException e) {
                portToTry++;
                continue;
            }
        }

        try {
            acceptor.setListeningPort(portToTry);
            if (OSUtils.isWindows())
                fail("jvm oddity - disable socketResueAddress");
            else
                fail("had no trouble binding TCP port!");
        }
        catch (IOException expected) {
            IOUtils.close(tcp);
        }
    }

    /**
     * This test checks to make sure that Acceptor.setListeningPort
     * correctly binds the UDP & TCP port.
     */
     public void testAcceptorBindsUDPandTCP() {
        int portToTry = 2000;
        while (true) {
            try {
                acceptor.setListeningPort(portToTry);
                break;
            } catch (IOException occupied) {
                portToTry++;
                continue;
            }
        }
        
        try {
            DatagramSocket udp = new DatagramSocket(portToTry);
            udp.close();
            fail("had no trouble binding UDP to occupied port!");
        } catch (IOException good) {
        }

        try {
            ServerSocket tcp = new ServerSocket(portToTry);
            tcp.close();
            fail("had no trouble binding TCP to occupied port!");
        } catch (IOException good) {
        }
    }
     
     public void testAcceptedIncoming() throws Exception {
         ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
         int port = bindAcceptor();
         assertFalse(acceptor.acceptedIncoming());
         // open up incoming to the test node
         {
             Socket sock = null;
             OutputStream os = null;
             try {
                 sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                      port), 12);
                 os = sock.getOutputStream();
                 os.write(StringUtils.toAsciiBytes("\n\n"));
                 os.flush();
             } catch (IOException ignored) {
             } catch (SecurityException ignored) {
             } finally {
                 IOUtils.close(sock);
                 IOUtils.close(os);
             }
         }        

         Thread.sleep(250);
         // CONNECT-BACK is hardcoded on
         assertFalse(acceptor.acceptedIncoming());
     }
     
     public void testAcceptedConnectBack() throws Exception {
         ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
         int port = bindAcceptor();
         assertFalse(acceptor.acceptedIncoming());
         // open up incoming to the test node
         {
             Socket sock = null;
             OutputStream os = null;
             try {
                 sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                      port), 12);
                 os = sock.getOutputStream();
                 os.write(StringUtils.toAsciiBytes("CONNECT "));
                 os.flush();
             } catch (IOException ignored) {
             } catch (SecurityException ignored) {
             } finally {
                 IOUtils.close(sock);
                 IOUtils.close(os);
             }
         }        

         Thread.sleep(250);
         // test on acceptor since network manager is stubbed
         assertTrue(acceptor.acceptedIncoming());
     }

     public void testTLSAcceptedIncoming() throws Exception {
         ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
         int port = bindAcceptor();
         assertFalse(acceptor.acceptedIncoming());
         // open up incoming to the test node
         {
             Socket sock = null;
             OutputStream os = null;
             try {
                 sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                      port), 12, ConnectType.TLS);
                 os = sock.getOutputStream();
                 os.write(StringUtils.toAsciiBytes("\n\n"));
                 os.flush();
             } catch (IOException ignored) {
             } catch (SecurityException ignored) {
             } finally {
                 IOUtils.close(sock);
                 IOUtils.close(os);
             }
         }        

         Thread.sleep(250);
         // CONNECT-BACK is hardcoded on
         assertFalse(acceptor.acceptedIncoming());
     }
     
     public void testTLSAcceptedConnectBack() throws Exception {
         ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
         int port = bindAcceptor();
         assertFalse(acceptor.acceptedIncoming());
         // open up incoming to the test node
         {
             Socket sock = null;
             OutputStream os = null;
             try {
                 sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                      port), 12, ConnectType.TLS);
                 os = sock.getOutputStream();
                 os.write(StringUtils.toAsciiBytes("CONNECT "));
                 os.flush();
             } catch (IOException ignored) {
             } catch (SecurityException ignored) {
             } finally {
                 IOUtils.close(sock);
                 IOUtils.close(os);
             }
         }        

         Thread.sleep(250);
         // test on acceptor since network manager is stubbed
         assertTrue(acceptor.acceptedIncoming());
     }
     
     public void testIncomingTLSHomemadeTLS() throws Exception {
         int port = bindAcceptor();
         
         MyConnectionAcceptor acceptor = new MyConnectionAcceptor("BLOCKING");
         
         connectionDispatcher.addConnectionAcceptor(acceptor, true, "BLOCKING");
         
         Socket tls = new TLSNIOSocket("localhost", port);
         tls.getOutputStream().write(StringUtils.toAsciiBytes("BLOCKING MORE DATA"));
         tls.getOutputStream().flush();
         assertTrue(acceptor.waitForAccept());
         tls.close();
     }
     
     public void testIncomingTLSBuiltIn() throws Exception {
         int port = bindAcceptor();
         
         MyConnectionAcceptor acceptor = new MyConnectionAcceptor("BLOCKING");
         
         connectionDispatcher.addConnectionAcceptor(acceptor, true, "BLOCKING");
         
         SSLContext context = SSLUtils.getTLSContext();
         SSLSocket tls = (SSLSocket)context.getSocketFactory().createSocket();
         tls.setUseClientMode(true);
         tls.setEnabledCipherSuites(new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" } );
         tls.connect(new InetSocketAddress("localhost", port));
         tls.getOutputStream().write(StringUtils.toAsciiBytes("BLOCKING MORE DATA"));
         tls.getOutputStream().flush();
         assertTrue(acceptor.waitForAccept());
         tls.close();
     }
     
     public void testAcceptorPortForcing() throws Exception {
         int localPort = acceptor.getPort(false);
         ConnectionSettings.FORCED_PORT.setValue(1000);
         ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
         ConnectionSettings.FORCED_IP_ADDRESS_STRING.set(InetAddress.getLocalHost().getHostAddress());
         assertEquals(1000, acceptor.getPort(true));
         assertNotEquals(1000,localPort); 
     }
     
     /**
      * Ensures the external address is returned as forced address if the client
      * accepts incoming connections. This can happen if port forwarding is configured
      * in the firewall but not in the client.
      * 
      * This is necessary to ensure that the client advertises its external address
      * correctly to peers. 
      */
     public void testGetAddressReturnsForcedExternalAddressIfAcceptedIncoming() throws Exception {
         acceptor.setAcceptedIncoming(true);
         assertTrue(acceptor.acceptedIncoming());
         acceptor.setExternalAddress(InetAddress.getByName("129.0.0.2"));
         assertTrue(NetworkUtils.isValidAddress(acceptor.getExternalAddress()));
         assertFalse(ConnectionSettings.FORCE_IP_ADDRESS.getValue());
         assertNotEquals(new byte[] { (byte)129, 0, 0, 2}, acceptor.getAddress(false));
         
         assertEquals(new byte[] { (byte)129, 0, 0, 2}, acceptor.getAddress(true));
     }
     
     private int bindAcceptor() throws Exception {
        for (int p = 2000; p < Integer.MAX_VALUE; p++) {
            try {
                acceptor.setListeningPort(p);
                return p;
            } catch (IOException ignored) {
            }
        }
        throw new IOException("unable to bind acceptor");
    }

    private static class MyConnectionAcceptor implements ConnectionAcceptor {
        private final String word;

        private CountDownLatch latch = new CountDownLatch(1);

        MyConnectionAcceptor(String word) {
            this.word = word;
        }

        public void acceptConnection(String word, Socket s) {
            LOG.debug("Got connection for word: " + word + ", socket: " + s);
            assertEquals(this.word, word);
            if ("BLOCKING".equals(word)) {
                try {
                    LOG.debug("Getting IS");
                    InputStream in = s.getInputStream();
                    byte[] b = new byte[1000];
                    LOG.debug("Reading");
                    int read = in.read(b);
                    LOG.debug("read");
                    assertEquals("MORE DATA", StringUtils.getASCIIString(b, 0, read));
                    latch.countDown();
                    s.close();
                } catch (IOException iox) {
                    throw new RuntimeException(iox);
                }
            }
        }

        boolean waitForAccept() throws Exception {
            return latch.await(5, TimeUnit.SECONDS);
        }

        public boolean isBlocking() {
            return true;
        }

    }

    private static class StubCM extends ConnectionManagerAdapter {
        private volatile CountDownLatch latch;
        private volatile boolean shouldSendRequests;

        void setShouldSendRequests(boolean send) {
            this.latch = new CountDownLatch(1);
            this.shouldSendRequests = send;
        }

        boolean awaitSend() throws InterruptedException {
            return latch.await(8000, TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean sendTCPConnectBackRequests() {
            if(shouldSendRequests) {
                latch.countDown();
                return true;
            }            
            return false;
        }
    }
    
    private static class StubAC extends ActivityCallbackAdapter implements EventListener<FirewallStatusEvent> {
        private volatile int changes = 0;
        private volatile boolean lastStatus = false;
        private volatile CountDownLatch latch;
        private volatile CountDownLatch singleChangeLatch;
        
        void resetLatch() {
            singleChangeLatch = new CountDownLatch(1);
        }
        
        @Override
        public void handleEvent(FirewallStatusEvent event) {
            changes++;
            lastStatus = event.getData() == FirewallStatus.NOT_FIREWALLED;
            if(latch != null)
                latch.countDown();
            if(singleChangeLatch != null) {
                singleChangeLatch.countDown();
            }
        }
        
        int waitForSingleChange(boolean expect) throws Exception {
            assertEquals(expect, singleChangeLatch.await(500, TimeUnit.MILLISECONDS));
            return changes;
        }
        
        int getChanges() {
            return changes;
        }
        
        boolean getLastStatus() {
            return lastStatus;
        }
        
        boolean waitForNextChange() throws InterruptedException {
            latch = new CountDownLatch(1);
            if(!latch.await(5000, TimeUnit.MILLISECONDS))
                fail("Didn't get countdown");
            return lastStatus;
        }
    }
}
