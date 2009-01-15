package org.limewire.xmpp.api.client;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class XMPPAddressTest extends BaseTestCase {

    public XMPPAddressTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMPPAddressTest.class);
    }

    public void testEqualsIsTrueForDifferentResources() {
        XMPPAddress address1 = new XMPPAddress("theflower@planet.x/ABCDEabcdef");
        XMPPAddress address2 = new XMPPAddress("theflower@planet.x/ABCDEfedcba");
        assertTrue(address1.equals(address2));
        assertTrue(address2.equals(address1));
        assertTrue(address1.equals(address1));
    }
    
    public void testHashCodeIsSameForDifferentResources() {
        XMPPAddress address1 = new XMPPAddress("theflower@planet.x/ABCDEabcdef");
        XMPPAddress address2 = new XMPPAddress("theflower@planet.x/ABCDEfedcba");
        assertEquals(address1.hashCode(), address2.hashCode());
    }
    
    public void testNotEqualsWhenResourcesAreDifferent() {
        XMPPAddress address1 = new XMPPAddress("theflower@planet.x/ABCDabcdef");
        XMPPAddress address2 = new XMPPAddress("theflower@planet.x/ABCDfedcba");
        assertFalse(address1.equals(address2));
        
        address1 = new XMPPAddress("thesheep@planet.x/ABCDE");
        address2 = new XMPPAddress("thesheep@planet.x/ABCD");
        assertFalse(address1.equals(address2));
    }
}
