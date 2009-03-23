package org.limewire.xmpp.client.impl.messages.library;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.listener.EventBroadcaster;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.LibraryChanged;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;

public class LibraryChangedIQListenerTest extends BaseTestCase {

    private Mockery context;
    private EventBroadcaster eventBroadcaster;
    private XMPPConnection connection;
    private LibraryChangedIQListener libraryChangedIQListener;

    public LibraryChangedIQListenerTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        eventBroadcaster = context.mock(EventBroadcaster.class);
        connection = context.mock(XMPPConnection.class);
        libraryChangedIQListener = new LibraryChangedIQListener(eventBroadcaster, connection); 
    }
    
    @SuppressWarnings("unchecked")
    public void testProcessFiresLibraryChangedEvent() {
        LibraryChangedIQ libraryChangedIQ = new LibraryChangedIQ();
        libraryChangedIQ.setType(Type.SET);
        libraryChangedIQ.setFrom("me@you.com/ldkfjd");
        
        final User user = context.mock(User.class);
        final FriendPresence friendPresence = context.mock(FriendPresence.class);

        final AtomicReference<LibraryChangedEvent> event = new AtomicReference<LibraryChangedEvent>();
        context.checking(new Expectations() {{
            one(connection).getUser("me@you.com");
            will(returnValue(user));
            one(user).getFriendPresences();
            will(returnValue(Collections.singletonMap("me@you.com/ldkfjd", friendPresence)));
            one(eventBroadcaster).broadcast(with(any(LibraryChangedEvent.class)));
            will(new AssignParameterAction<LibraryChangedEvent>(event, 0));
        }});
        
        libraryChangedIQListener.processPacket(libraryChangedIQ);
        
        assertSame(friendPresence, event.get().getData());
        assertEquals(LibraryChanged.LIBRARY_CHANGED, event.get().getType());
        
        context.assertIsSatisfied();
    }
}
