package com.limegroup.gnutella.chat;

import java.net.InetAddress;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;
import org.limewire.core.settings.ChatSettings;

import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.stubs.SocketStub;

public class ChatManagerTest extends BaseTestCase {

    static {
        ChatSettings.CHAT_ENABLED.setValue(true);
    }

    private final Mockery context = new Mockery();
    
    private InetAddress address;

    public ChatManagerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        address = InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 });
    }

    public void testAcceptConnection() {
        final SocketStub socket = new SocketStub(address, 5678);
        final SpamServices spamServices = context.mock(SpamServices.class);
        final InstantMessengerFactory instantMessengerFactory = context.mock(InstantMessengerFactory.class);
        final InstantMessenger instantMessenger = context.mock(InstantMessenger.class);
        
        context.checking(new Expectations() {{
            one(spamServices).isAllowed(address);
            will(returnValue(true));
            
            one(instantMessengerFactory).createIncomingInstantMessenger(socket);
            will(returnValue(instantMessenger));
            
            one(instantMessenger).start();
        }});
        
        ChatManager chatManager = new ChatManager(spamServices, instantMessengerFactory);
        chatManager.acceptConnection("CHAT", socket);
        
        context.assertIsSatisfied();
    }

    public void testAcceptConnectionNotAllowed() {
        final SocketStub socket = new SocketStub(address, 5678);
        final SpamServices spamServices = context.mock(SpamServices.class);
        final InstantMessengerFactory instantMessengerFactory = context.mock(InstantMessengerFactory.class);
        
        context.checking(new Expectations() {{
            one(spamServices).isAllowed(address);
            will(returnValue(false));
        }});
        
        ChatManager chatManager = new ChatManager(spamServices, instantMessengerFactory);
        chatManager.acceptConnection("CHAT", socket);

        assertTrue(socket.closed);
        
        context.assertIsSatisfied();
    }

    public void testCreateConnection() {
        final SpamServices spamServices = context.mock(SpamServices.class);
        final InstantMessengerFactory instantMessengerFactory = context.mock(InstantMessengerFactory.class);
        final InstantMessenger instantMessenger = context.mock(InstantMessenger.class);
        
        context.checking(new Expectations() {{
            one(instantMessengerFactory).createOutgoingInstantMessenger("host", 5678);
            will(returnValue(instantMessenger));
        }});
        
        ChatManager chatManager = new ChatManager(spamServices, instantMessengerFactory);
        assertSame(instantMessenger, chatManager.createConnection("host", 5678));
        
        context.assertIsSatisfied();
    }

}
