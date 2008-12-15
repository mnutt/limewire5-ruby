package org.limewire.mojito.messages;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.messages.impl.DefaultMessageID;
import org.limewire.mojito.messages.impl.DefaultMessageID.MessageSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.util.PrivilegedAccessor;

public class MessageIDTest extends MojitoTestCase {
    
    private MACCalculatorRepositoryManager macManager = new MACCalculatorRepositoryManager();
    public MessageIDTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(MessageIDTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testEquals() {
        DefaultMessageID messageId1 = DefaultMessageID.createWithSocketAddress(new InetSocketAddress("localhost", 1024), macManager);
        DefaultMessageID messageId2 = DefaultMessageID.createWithSocketAddress(new InetSocketAddress("localhost", 1024), macManager);
        
        // Except for the first four bytes (AddressSecurityToken) they shouldn't be equal
        assertNotEquals(messageId1, messageId2);
        for (int i = 0; i < 4; i++) {
            assertEquals(messageId1.getBytes()[i], messageId2.getBytes()[i]);
        }
        
        // Same if created from the bytes
        MessageID messageId3 = DefaultMessageID.createWithBytes(messageId2.getBytes());
        assertEquals(messageId2, messageId3);
        
        // Same if created from the hex string
        MessageID messageId4 = DefaultMessageID.createWithHexString(messageId2.toHexString());
        assertEquals(messageId4, messageId3);
    }
    
    public void testEmbeddedSecurityToken() throws Exception {
        InetSocketAddress addr1 = new InetSocketAddress("localhost", 1234);
        SecurityToken key1 = new MessageSecurityToken(new DefaultMessageID.DHTTokenData(addr1),macManager);
        
        MessageID messageId1 = DefaultMessageID.createWithSocketAddress(addr1, macManager);
        SecurityToken key2 = (SecurityToken)PrivilegedAccessor.invokeMethod(messageId1, "getSecurityToken", new Object[0]);
        
        assertTrue(key1.equals(key2));
        assertTrue(messageId1.isFor(addr1));
        
        InetSocketAddress addr2 = new InetSocketAddress("www.google.com", 1234);
        assertFalse(messageId1.isFor(addr2));
    }
}
