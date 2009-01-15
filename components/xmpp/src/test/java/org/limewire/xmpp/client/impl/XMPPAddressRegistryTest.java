package org.limewire.xmpp.client.impl;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPAddress;

public class XMPPAddressRegistryTest extends BaseTestCase {

    private XMPPAddressRegistry addressRegistry;

    public XMPPAddressRegistryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMPPAddressRegistryTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        addressRegistry = new XMPPAddressRegistry();
    }

    public void testPutAndGetWorkWithDifferentResourceIds() throws Exception {
        XMPPAddress address1 = new XMPPAddress("baobab@planet.x/ABCDEabcdefg");
        XMPPAddress address2 = new XMPPAddress("baobab@planet.x/ABCDEgfedcba");
        assertTrue(address1.equals(address2));
        
        Connectable address = new ConnectableImpl("129.0.0.1", 5000, true);
        addressRegistry.put(address1, address);
        assertSame(address, addressRegistry.get(address2));
    }
    
}
