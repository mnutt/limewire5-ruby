package org.limewire.mojito.util;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.messages.PingRequest;
import org.limewire.mojito.routing.Contact;

public class MessageUtilsTest extends MojitoTestCase {
    
    public MessageUtilsTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(MessageUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testIsCollisionPing() {
        MojitoDHT dht = MojitoFactory.createDHT();
        MessageFactory factory = ((Context)dht).getMessageFactory();
        PingRequest ping = null;
        
        assertFalse(ContactUtils.isCollisionPingSender(dht.getLocalNodeID(), dht.getLocalNode()));
        ping = factory.createPingRequest(dht.getLocalNode(), new InetSocketAddress("localhost", 2000));
        assertFalse(MessageUtils.isCollisionPingRequest(dht.getLocalNodeID(), ping));
        
        Contact sender = ContactUtils.createCollisionPingSender(dht.getLocalNode());
        assertTrue(ContactUtils.isCollisionPingSender(dht.getLocalNodeID(), sender));
        ping = factory.createPingRequest(sender, new InetSocketAddress("localhost", 2000));
        assertTrue(MessageUtils.isCollisionPingRequest(dht.getLocalNodeID(), ping));
    }
}
