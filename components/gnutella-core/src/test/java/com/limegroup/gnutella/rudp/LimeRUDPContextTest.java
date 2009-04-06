package com.limegroup.gnutella.rudp;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.nio.NIODispatcher;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.messages.RUDPMessage;

import com.google.inject.Injector;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageParser;

public class LimeRUDPContextTest extends LimeTestCase {
    
    private MessageFactory messageFactory;
    private Injector injector;

    public LimeRUDPContextTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimeRUDPContextTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector();
		messageFactory = injector.getInstance(MessageFactory.class);
    }
    
    public void testReferences() {
        // Make sure there's nothing linked up to RUDP beforehand
        assertNull(messageFactory.getParser(RUDPMessage.F_RUDP_MESSAGE));
        RUDPContext context = injector.getInstance(RUDPContext.class);
        assertEquals("LimeRUDPMessageFactory", context.getMessageFactory().getClass().getSimpleName());
        assertEquals(LimeUDPService.class, context.getUDPService().getClass());
        assertEquals(LimeRUDPSettings.class, context.getRUDPSettings().getClass());
        assertSame(NIODispatcher.instance().getTransportListener(), context.getTransportListener());
        // make sure the parser was installed in the MessageFactory
        assertNotNull(messageFactory.getParser(RUDPMessage.F_RUDP_MESSAGE));
        assertEquals(LimeRUDPMessageParser.class, messageFactory.getParser(RUDPMessage.F_RUDP_MESSAGE).getClass());
    }
}
