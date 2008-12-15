package org.limewire.mojito.io;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.messages.MessageHelper;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;


public class MessageDispatcherTest extends MojitoTestCase {
    
    public MessageDispatcherTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(MessageDispatcherTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSendMethod() throws Exception {
        MojitoDHT dht = MojitoFactory.createDHT("Test");
        try {
            dht.bind(5000);
            dht.start();
            
            boolean sent = false;
            
            Context context = (Context)dht;
            MessageHelper helper = context.getMessageHelper();
            MessageFactory factory = context.getMessageFactory();
            MessageDispatcher dispatcher = context.getMessageDispatcher();
            
            // Send to an address
            sent = dispatcher.send(new InetSocketAddress("www.google.com", 5000), 
                    helper.createPingRequest(new InetSocketAddress("www.google.com", 5000)), null);
            assertTrue(sent);
            
            // Send to local Node's contact address
            sent = dispatcher.send(context.getContactAddress(), 
                    helper.createPingRequest(context.getContactAddress()), null);
            assertFalse(sent);
            
            // Send to a non local Node
            Contact node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(), new InetSocketAddress("www.google.com", 5000));
            sent = dispatcher.send(node, helper.createPingRequest(node.getContactAddress()), null);
            assertTrue(sent);
            
            // Sent to local Node
            sent = dispatcher.send(context.getLocalNode(), 
                    helper.createPingRequest(context.getContactAddress()), null);
            assertFalse(sent);
            
            // Send to a Node that has the local Node's ID
            node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    context.getLocalNodeID(), new InetSocketAddress("www.google.com", 5000));
            sent = dispatcher.send(node, helper.createPingRequest(node.getContactAddress()), null);
            assertFalse(sent);
            
            // Sender is not firewalled
            Contact sender = ContactFactory.createLiveContact(
                    new InetSocketAddress("www.google.com", 5000), Vendor.UNKNOWN, Version.ZERO, 
                    context.getLocalNodeID().invert(), 
                    new InetSocketAddress("www.google.com", 5000), 0, Contact.DEFAULT_FLAG);
            
            RequestMessage request = factory.createPingRequest(
                    sender, new InetSocketAddress("www.google.com", 5000));
            sent = dispatcher.send(node, request, null);
            assertFalse(sent);
            
            // Same as above but sender is firewalled
            sender = ContactFactory.createLiveContact(
                    new InetSocketAddress("www.google.com", 5000), Vendor.UNKNOWN, Version.ZERO, 
                    context.getLocalNodeID().invert(), 
                    new InetSocketAddress("www.google.com", 5000), 0, Contact.FIREWALLED_FLAG);
            
            request = factory.createPingRequest(
                    sender, new InetSocketAddress("www.google.com", 5000));
            sent = dispatcher.send(node, request, null);
            assertTrue(sent);
            
        } finally {
            dht.close();
        }
    }
}
