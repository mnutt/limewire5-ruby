package org.limewire.net.address;

import java.util.Set;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

public class FirewalledAddressTest extends BaseTestCase {

    public FirewalledAddressTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FirewalledAddressTest.class);
    }
    
    public void testThrowsIllegalArgumentIfFWTAndInvalidPublicAddress() throws Exception {
        try {
            new FirewalledAddress(ConnectableImpl.INVALID_CONNECTABLE, new ConnectableImpl("192.168.0.1", 1000, true), new GUID(), Connectable.EMPTY_SET, 1);
            fail("should have thrown illegal argument");
        } catch (IllegalArgumentException iae) {
        }
        // make sure it doesn't throw if fwt is disabled
        new FirewalledAddress(ConnectableImpl.INVALID_CONNECTABLE, new ConnectableImpl("192.168.0.1", 1000, true), new GUID(), Connectable.EMPTY_SET, 0);
    }
    
    public void testEquals() throws Exception {
        // identity
        Connectable publicAddress = new ConnectableImpl("129.0.0.1", 2000, true);
        Connectable privateAddress = new ConnectableImpl("192.168.0.1", 1000, true);
        GUID clientGuid = new GUID();
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>(new ConnectableImpl("200.0.0.0", 1000, true),
                new ConnectableImpl("100.0.0.0", 1000, false));
        FirewalledAddress address1 = new FirewalledAddress(publicAddress, privateAddress, clientGuid, proxies, 1);
        assertEquals(address1, address1);
        // copy
        assertEquals(address1, new FirewalledAddress(new ConnectableImpl("129.0.0.1", 2000, true), 
                new ConnectableImpl("192.168.0.1", 1000, true), new GUID(clientGuid.bytes()),
                new StrictIpPortSet<Connectable>(new ConnectableImpl("200.0.0.0", 1000, true), 
                        new ConnectableImpl("100.0.0.0", 1000, false)), 1));
        
        // different public address
        FirewalledAddress address2 = new FirewalledAddress(new ConnectableImpl("100.0.0.1", 20, false), privateAddress, clientGuid, proxies, 1);
        assertNotEquals(address1, address2);
        // different private address
        address2 = new FirewalledAddress(publicAddress, new ConnectableImpl("10.0.0.1", 20, false), clientGuid, proxies, 1);
        assertNotEquals(address1, address2);
        // different client guid
        address2 = new FirewalledAddress(publicAddress, privateAddress, new GUID(), proxies, 1);
        assertNotEquals(address1, address2);
        // different proxy
        address2 = new FirewalledAddress(publicAddress, privateAddress, clientGuid, new StrictIpPortSet<Connectable>(new ConnectableImpl("200.0.0.0", 1000, true), new ConnectableImpl("100.0.0.0", 5555, false)), 1);
        assertNotEquals(address1, address2);
        // different fwt
        address2 = new FirewalledAddress(publicAddress, privateAddress, clientGuid, proxies, 2);
        assertNotEquals(address1, address2);
        address2 = new FirewalledAddress(publicAddress, privateAddress, clientGuid, proxies, 0);
        assertNotEquals(address1, address2);
    }

    public void testHashCode() throws Exception {
        // identity
        Connectable publicAddress = new ConnectableImpl("129.0.0.1", 2000, true);
        Connectable privateAddress = new ConnectableImpl("192.168.0.1", 1000, true);
        GUID clientGuid = new GUID();
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>(new ConnectableImpl("200.0.0.0", 1000, true), new ConnectableImpl("100.0.0.0", 1000, false));
        FirewalledAddress address1 = new FirewalledAddress(publicAddress, privateAddress, clientGuid, proxies, 1);
        assertEquals(address1.hashCode(), address1.hashCode());
        // copy
        assertEquals(address1.hashCode(), new FirewalledAddress(new ConnectableImpl("129.0.0.1", 2000, true), 
                new ConnectableImpl("192.168.0.1", 1000, true), new GUID(clientGuid.bytes()),
                new StrictIpPortSet<Connectable>(new ConnectableImpl("200.0.0.0", 1000, true), 
                        new ConnectableImpl("100.0.0.0", 1000, false)), 1).hashCode());
        
        // different public address
        FirewalledAddress address2 = new FirewalledAddress(new ConnectableImpl("100.0.0.1", 20, false), privateAddress, clientGuid, proxies, 1);
        assertNotEquals(address1.hashCode(), address2.hashCode());
        // different private address
        address2 = new FirewalledAddress(publicAddress, new ConnectableImpl("10.0.0.1", 20, false), clientGuid, proxies, 1);
        assertNotEquals(address1.hashCode(), address2.hashCode());
        // different client guid
        address2 = new FirewalledAddress(publicAddress, privateAddress, new GUID(), proxies, 1);
        assertNotEquals(address1.hashCode(), address2.hashCode());
        // different proxy
        address2 = new FirewalledAddress(publicAddress, privateAddress, clientGuid, new StrictIpPortSet<Connectable>(new ConnectableImpl("200.0.0.0", 1000, true), new ConnectableImpl("100.0.0.0", 5555, false)), 1);
        assertNotEquals(address1.hashCode(), address2.hashCode());
        // different fwt
        address2 = new FirewalledAddress(publicAddress, privateAddress, clientGuid, proxies, 2);
        assertNotEquals(address1.hashCode(), address2.hashCode());
        address2 = new FirewalledAddress(publicAddress, privateAddress, clientGuid, proxies, 0);
        assertNotEquals(address1.hashCode(), address2.hashCode());
    }

}
