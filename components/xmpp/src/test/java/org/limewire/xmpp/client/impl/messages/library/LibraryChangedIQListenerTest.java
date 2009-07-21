package org.limewire.xmpp.client.impl.messages.library;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.util.BaseTestCase;

public class LibraryChangedIQListenerTest extends BaseTestCase {

    private Mockery context;
    private FeatureTransport.Handler<LibraryChangedNotifier> handler;
    private LibraryChangedIQListener libraryChangedIQListener;

    public LibraryChangedIQListenerTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        handler = context.mock(FeatureTransport.Handler.class);
        libraryChangedIQListener = new LibraryChangedIQListener(handler, null); 
    }
    
    public void testProcessPacket() {
        LibraryChangedIQ libraryChangedIQ = new LibraryChangedIQ();
        libraryChangedIQ.setType(Type.SET);
        libraryChangedIQ.setFrom("me@you.com/ldkfjd");
        
        context.checking(new Expectations() {{
            one(handler).featureReceived(with(equal("me@you.com/ldkfjd")), with(any(LibraryChangedNotifier.class)));
        }});
        
        libraryChangedIQListener.processPacket(libraryChangedIQ);
        
        context.assertIsSatisfied();
    }
}
