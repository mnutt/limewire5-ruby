package com.limegroup.gnutella.rudp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.IOErrorObserver;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.UDPConnectionProcessor;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.rudp.UDPSocketChannelConnectionEvent;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.SynMessage.Role;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.listener.EventListenerList;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageHelper;
import com.limegroup.gnutella.rudp.messages.RUDPMessageHandlerHelper;
import com.limegroup.gnutella.util.LimeTestCase;

public class RUDPIntegrationTest extends LimeTestCase {
    
    private static final int PORT = 9313;
    
    private LifecycleManager lifecycleManager;

    private UDPSelectorProvider selectorProvider;

    private NetworkManager networkManager;

    private UDPService udpService;

    private MessageRouter messageRouter;

    public RUDPIntegrationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RUDPIntegrationTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void setSettings() throws Exception {
        String localIP = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] {"127.*.*.*", localIP});
        NetworkSettings.PORT.setValue(PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
    }
    
    @Override
    public void setUp() throws Exception {
        setSettings();
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        selectorProvider = injector.getInstance(UDPSelectorProvider.class);
        networkManager = injector.getInstance(NetworkManager.class);
        udpService = injector.getInstance(UDPService.class);
        messageRouter = injector.getInstance(MessageRouter.class);
        
        
        lifecycleManager.start();
    }
    
    @Override
    protected void tearDown() throws Exception {
        lifecycleManager.shutdown();
    }
    
    @SuppressWarnings("unchecked")
    public void testLimeRUDPMessageHandlerIsInstalled() throws Exception {
        Class[] handledMessageClasses = RUDPMessageHandlerHelper.getMessageClasses();
        for (Class clazz : handledMessageClasses) {
            assertEquals("LimeRUDPMessageHandler", messageRouter.getUDPMessageHandler(clazz).getClass().getSimpleName());
        }
    }
    
    public void testIncomingUDPPacketsSentToHandler() throws Exception {
        StubRUDPMessageHandler handler = new StubRUDPMessageHandler();
        RUDPMessageHandlerHelper.addHandler(messageRouter, handler);
        checkMessage(handler, LimeRUDPMessageHelper.getAck(1));
        checkMessage(handler, LimeRUDPMessageHelper.getData(1));
        checkMessage(handler, LimeRUDPMessageHelper.getFin(1));
        checkMessage(handler, LimeRUDPMessageHelper.getKeepAlive(1));
        checkMessage(handler, LimeRUDPMessageHelper.getSyn(1));
    }
    
    public void testPacketsSentToMultiplexorAndGoesToProcessor() throws Exception {
        StubUDPConnectionProcessor stub = new StubUDPConnectionProcessor(selectorProvider.getContext());
        SelectableChannel channel = createUDPSocketChannel(stub);
        assertEquals(0, stub.id);
        // This is done only to allow the multiplexor to learn about the processor.
        NIODispatcher.instance().register(channel,  new StubIOErrorObserver());
        Thread.sleep(100);
        assertNotEquals(0, stub.id);
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", networkManager.getNonForcedPort());
        stub.addr = addr;
        stub.isConnecting = false;
        checkMessage(stub, LimeRUDPMessageHelper.getAck(stub.id));
        checkMessage(stub, LimeRUDPMessageHelper.getData(stub.id));
        checkMessage(stub, LimeRUDPMessageHelper.getFin(stub.id));
        checkMessage(stub, LimeRUDPMessageHelper.getKeepAlive(stub.id));
        checkMessage(stub, LimeRUDPMessageHelper.getSyn(stub.id));
        channel.close();
    }
    
    private SelectableChannel createUDPSocketChannel(UDPConnectionProcessor stub) throws Exception {
        Class clazz = Class.forName("org.limewire.rudp.UDPSocketChannel");
        return (SelectableChannel)PrivilegedAccessor.invokeConstructor(clazz, new Object[] { stub, Role.UNDEFINED }, new Class[] { UDPConnectionProcessor.class, Role.class });
    }
    
    private void checkMessage(MessageObserver handler, Message m) throws Exception {
        assertNull(handler.getCurrentMessage());
        sendToUDP(m);
        Message read = handler.getLastMessage(100);
        assertNotNull(read);
        assertEquals(m.getClass(), read.getClass());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        byte[] b1 = out.toByteArray();
        out.reset();
        read.write(out);
        byte[] b2 = out.toByteArray();
        assertEquals(b1, b2);
    }
    
    private void sendToUDP(Message m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        udpService.send(m, new InetSocketAddress("127.0.0.1", PORT));
    }
    
    private interface MessageObserver {
        public Message getCurrentMessage();
        public Message getLastMessage(int waitTime) throws Exception;
    }
    
    private class StubRUDPMessageHandler implements MessageHandler, MessageObserver {
        private Message lastMessage;
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            synchronized(this) {
                lastMessage = msg;
                notify();
            }
        }
        
        public synchronized Message getCurrentMessage() {
            return lastMessage;
        }
        
        public Message getLastMessage(int waitTime) throws Exception {
            synchronized(this) {
                if(lastMessage == null)
                    wait(waitTime);
                Message m = lastMessage;
                lastMessage = null;
                return m;
            }
        }
    }
    
    private class StubUDPConnectionProcessor extends UDPConnectionProcessor implements MessageObserver {
        private volatile byte id;
        private volatile InetSocketAddress addr;
        private volatile RUDPMessage lastMessage;
        private volatile boolean isConnecting;
        
        StubUDPConnectionProcessor(RUDPContext context) {
            super(null, context, Role.UNDEFINED, new EventListenerList<UDPSocketChannelConnectionEvent>());
        }
        
        @Override
        protected void setConnectionId(byte id) {
            this.id = id;
            super.setConnectionId(id);
        }

        @Override
        protected InetSocketAddress getSocketAddress() {
            return addr;
        }

        @Override
        protected synchronized void handleMessage(RUDPMessage msg) {
            this.lastMessage = msg;
            notify();
        }

        @Override
        protected synchronized boolean isConnecting() {
            return isConnecting;
        }
        
        public synchronized Message getLastMessage(int waitTime) throws Exception {
            if(lastMessage == null)
                wait(waitTime);
            Message m = (Message)lastMessage;
            lastMessage = null;
            return m;
        }

        public synchronized Message getCurrentMessage() {
            return (Message)lastMessage;
        }
    }
    
    private class StubIOErrorObserver implements IOErrorObserver {
        public void handleIOException(IOException iox) {
        }

        public void shutdown() {
        }
        
    }
}
