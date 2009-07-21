package org.limewire.friend.impl.address;

import junit.framework.Test;

import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendFirewalledAddress;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.util.BaseTestCase;

public class FriendFirewalledAddressTest extends BaseTestCase {

    public FriendFirewalledAddressTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FriendFirewalledAddressTest.class);
    }
    
    public void testFieldsAreSetCorrectlyInConstructor() throws Exception {
        FriendAddress friendAddress = new FriendAddress("mimi@me.com/BHDKFBH");
        FirewalledAddress firewalledAddress = new FirewalledAddress(ConnectableImpl.INVALID_CONNECTABLE, 
                new ConnectableImpl("192.168.0.1", 1000, true), new GUID(),
                Connectable.EMPTY_SET, 0);
        FriendFirewalledAddress friendFirewalledAddress = new FriendFirewalledAddress(friendAddress, firewalledAddress);
        assertSame(friendAddress, friendFirewalledAddress.getFriendAddress());
        assertSame(firewalledAddress, friendFirewalledAddress.getFirewalledAddress());
    }

}
