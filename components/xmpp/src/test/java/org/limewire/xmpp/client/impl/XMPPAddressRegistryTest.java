package org.limewire.xmpp.client.impl;

import junit.framework.Test;

import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressRegistry;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.BaseTestCase;

public class XMPPAddressRegistryTest extends BaseTestCase {

    private FriendAddressRegistry addressRegistry;

    public XMPPAddressRegistryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMPPAddressRegistryTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        addressRegistry = new FriendAddressRegistry();
    }

    public void testPutAndGetWorkWithDifferentResourceIds() throws Exception {
        FriendAddress address1 = new FriendAddress("baobab@planet.x/ABCDEabcdefg");
        FriendAddress address2 = new FriendAddress("baobab@planet.x/ABCDEgfedcba");
        assertTrue(address1.equals(address2));
        
        Connectable address = new ConnectableImpl("129.0.0.1", 5000, true);
        addressRegistry.put(address1, address);
        assertSame(address, addressRegistry.get(address2));
    }
    
}
