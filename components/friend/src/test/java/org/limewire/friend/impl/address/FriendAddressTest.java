package org.limewire.friend.impl.address;

import junit.framework.Test;

import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.util.BaseTestCase;

public class FriendAddressTest extends BaseTestCase {

    public FriendAddressTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FriendAddressTest.class);
    }

    public void testEqualsIsTrueForDifferentResources() {
        FriendAddress address1 = new FriendAddress("theflower@planet.x/ABCDEabcdef");
        FriendAddress address2 = new FriendAddress("theflower@planet.x/ABCDEfedcba");
        assertTrue(address1.equals(address2));
        assertTrue(address2.equals(address1));
        assertTrue(address1.equals(address1));
    }
    
    public void testHashCodeIsSameForDifferentResources() {
        FriendAddress address1 = new FriendAddress("theflower@planet.x/ABCDEabcdef");
        FriendAddress address2 = new FriendAddress("theflower@planet.x/ABCDEfedcba");
        assertEquals(address1.hashCode(), address2.hashCode());
    }
    
    public void testNotEqualsWhenResourcesAreDifferent() {
        FriendAddress address1 = new FriendAddress("theflower@planet.x/ABCDabcdef");
        FriendAddress address2 = new FriendAddress("theflower@planet.x/ABCDfedcba");
        assertFalse(address1.equals(address2));
        
        address1 = new FriendAddress("thesheep@planet.x/ABCDE");
        address2 = new FriendAddress("thesheep@planet.x/ABCD");
        assertFalse(address1.equals(address2));
    }
}
