package org.limewire.mojito.routing.impl;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;


public class ContactTest extends MojitoTestCase {
    
    public ContactTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(ContactTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testEquals() {
        LocalContact lc1 = (LocalContact)ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.ZERO, false);
        LocalContact lc2 = (LocalContact)ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.ZERO, false);
        
        RemoteContact rc1 = (RemoteContact)ContactFactory
            .createLiveContact(null, Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(), new InetSocketAddress(1000), 0, Contact.DEFAULT_FLAG);
        
        RemoteContact rc2 = (RemoteContact)ContactFactory
            .createLiveContact(null, Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(), new InetSocketAddress(1000), 0, Contact.DEFAULT_FLAG);
        
        // Initial state
        assertEquals(lc1, lc1);
        assertEquals(lc2, lc2);
        assertEquals(rc1, rc1);
        assertEquals(rc2, rc2);
        assertNotEquals(lc1, lc2);
        assertNotEquals(rc1, rc2);
        assertNotEquals(lc1, rc2);
        assertNotEquals(lc2, rc2);
        assertNotEquals(rc1, lc1);
        assertNotEquals(rc2, lc1);
        
        // Two LocalContacts with the same KUID are considered to be equal
        lc1.setNodeID(lc2.getNodeID());
        assertEquals(lc1, lc2);
        
        // The SocketAddress doesn't change anything as there can be only
        // one LocalContact per Node (RouteTable)
        lc1.setContactAddress(new InetSocketAddress(3000));
        lc1.setSourceAddress(new InetSocketAddress(3000));
        lc2.setContactAddress(new InetSocketAddress(4000));
        lc2.setSourceAddress(new InetSocketAddress(4000));
        assertEquals(lc1, lc2);
        
        // Two RemoteContacts with the same KUID and SocketAddress are
        // considered to be equal
        rc1 = (RemoteContact)ContactFactory
            .createLiveContact(null, Vendor.UNKNOWN, Version.ZERO, 
                    rc2.getNodeID(), new InetSocketAddress(1000), 0, Contact.DEFAULT_FLAG);
        assertEquals(rc1, rc2);
        
        // But if their SocketAddress is different they're not equal
        rc1 = (RemoteContact)ContactFactory
            .createLiveContact(null, Vendor.UNKNOWN, Version.ZERO, 
                    rc2.getNodeID(), new InetSocketAddress(2000), 0, Contact.DEFAULT_FLAG);
        assertNotEquals(rc1, rc2);
        
        // A Local- and RemoteContact cannot be equal!
        rc1 = (RemoteContact)ContactFactory
            .createLiveContact(null, Vendor.UNKNOWN, Version.ZERO, 
                    lc1.getNodeID(), new InetSocketAddress(1000), 0, Contact.DEFAULT_FLAG);
        assertNotEquals(lc1, rc1);
    }
}
