package com.limegroup.gnutella.gui.chat;

import java.awt.event.WindowEvent;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.ChatSettings;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.chat.InstantMessengerFactory;
import com.limegroup.gnutella.chat.InstantMessengerImpl;
import com.limegroup.gnutella.gui.GUITestUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LimeWireGUIModule;
import com.limegroup.gnutella.gui.ResourceManager;
import com.limegroup.gnutella.gui.VisualConnectionCallback;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;

public class ChatUIManagerTest extends BaseTestCase {

    static {
        ChatSettings.CHAT_ENABLED.setValue(true);        
    }

    private static final int CHAT_PORT = 9999;

    private ChatFrame outgoing;

    private ChatFrame incoming;

    private Acceptor acceptor;

    private InstantMessengerFactory instantMessengerFactory;

    private ChatManager chatManager;

    public ChatUIManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ChatUIManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // make sure local connections are accepted
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        Injector injector = LimeTestUtils.createInjector(VisualConnectionCallback.class,
                new LimeWireGUIModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
            }
        });

        // initialize chat classes
        ConnectionDispatcher connectionDispatcher = injector.getInstance(Key.get(
                ConnectionDispatcher.class, Names.named("global")));
        chatManager = injector.getInstance(ChatManager.class);
        connectionDispatcher.addConnectionAcceptor(chatManager, false, "CHAT");
        acceptor = injector.getInstance(Acceptor.class);
        acceptor.setListeningPort(CHAT_PORT);
        acceptor.start();
        
        instantMessengerFactory = injector.getInstance(InstantMessengerFactory.class);
        
        // initialize ui
        ResourceManager.instance();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        ChatUIManager.instance().clear();
        
        if (outgoing != null) {
            outgoing.getChat().stop();
            outgoing.dispose();
            outgoing = null;
        }

        if (acceptor != null) {
            acceptor.setListeningPort(0);
            acceptor.shutdown();
        }
        
        LimeTestUtils.waitForNIO();
    }

    // tests that chats to localhost are closed right away
    public void testChatLocalhost() throws Exception {
        InstantMessenger chatter = chatManager.createConnection("127.0.0.1", CHAT_PORT);
        outgoing = ChatUIManager.instance().acceptChat(chatter);
        chatter.start();
        ((InstantMessengerImpl) outgoing.getChat()).waitForConnect(4000);
        LimeTestUtils.waitForNIO();
        // wait for SwingUtilities.invokeLater() to create chat frame
        GUITestUtils.waitForSwing();
        // wait for socket disconnect
        LimeTestUtils.waitForNIO();
        assertTrue(!outgoing.getChat().isConnected());
    }

    public void testChatThroughAcceptor() throws Exception {
        setupChat();
        assertTrue(outgoing.getChat().isConnected());
        assertTrue(incoming.getChat().isConnected());
        incoming.setMessage("foo");
        assertEquals("foo", incoming.getMessage());
        incoming.send();
        Thread.sleep(1000);
        assertTrue(incoming.getText().indexOf("foo") > 0);
        assertEquals("", incoming.getMessage());
        outgoing.setMessage("bar");
        outgoing.send();
        LimeTestUtils.waitForNIO();
        GUITestUtils.waitForSwing();
        assertTrue(incoming.getText().indexOf("bar") > 0);
        assertTrue(outgoing.getText().indexOf("bar") > 0);
    }

    public void testCloseIncoming() throws Exception {
        setupChat();
        incoming.dispatchEvent(new WindowEvent(incoming,
                WindowEvent.WINDOW_CLOSING));
        GUITestUtils.waitForSwing();
        LimeTestUtils.waitForNIO();
        assertTrue(!outgoing.getChat().isConnected());
        assertTrue(!incoming.getChat().isConnected());
    }

    public void testCloseOutgoing() throws Exception {
        setupChat();
        outgoing.getChat().stop();
        LimeTestUtils.waitForNIO();
        assertTrue(!outgoing.getChat().isConnected());
        assertTrue(!incoming.getChat().isConnected());
        GUITestUtils.waitForSwing();
        assertTrue(incoming.getText().indexOf(
                I18n.tr("Host is unavailable")) != -1);
    }

    private ChatFrame getIncomingChat() {
        for (ChatFrame frame : ChatUIManager.instance().getChatFrames()) {
            if (!frame.getChat().isOutgoing()) {
                return frame;
            }
        }
        fail("Incoming chat not found");
        return null;
    }

    private void setupChat() throws Exception {
        // the outgoing chat frame can not be registered with ChatUIManager
        // otherwise the connection will be closed since there can only be
        // one chat frame per host
        InstantMessengerImpl chat = (InstantMessengerImpl) instantMessengerFactory.createOutgoingInstantMessenger("localhost", CHAT_PORT);
        outgoing = new ChatFrame(chat);
        chat.start();
        chat.waitForConnect(4000);
        assertTrue("Could not establish outgoing chat connection", chat.isConnected());
        LimeTestUtils.waitForNIO();
        GUITestUtils.waitForSwing();
        incoming = getIncomingChat();
    }
    
}
