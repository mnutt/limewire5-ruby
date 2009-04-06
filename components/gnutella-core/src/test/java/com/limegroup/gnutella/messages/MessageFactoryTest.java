package com.limegroup.gnutella.messages;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;


public class MessageFactoryTest extends BaseTestCase {

    private MessageFactory messageFactory;

    public MessageFactoryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MessageFactoryTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        messageFactory = injector.getInstance(MessageFactory.class);
    }

    public void testGetParser() {
        assertNull(messageFactory.getParser((byte) 0xFF));
        assertNotNull(messageFactory.getParser((byte) 0x0));
        assertNotNull(messageFactory.getParser((byte) 0x100));
    }

}
