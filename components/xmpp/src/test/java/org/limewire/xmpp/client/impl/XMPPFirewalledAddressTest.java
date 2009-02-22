package org.limewire.xmpp.client.impl;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPAddress;

public class XMPPFirewalledAddressTest extends BaseTestCase {

    public XMPPFirewalledAddressTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMPPFirewalledAddressTest.class);
    }
    
    public void testFieldsAreSetCorrectlyInConstructor() throws Exception {
        XMPPAddress xmppAddress = new XMPPAddress("mimi@me.com/BHDKFBH");
        FirewalledAddress firewalledAddress = new FirewalledAddress(ConnectableImpl.INVALID_CONNECTABLE, 
                new ConnectableImpl("192.168.0.1", 1000, true), new GUID(),
                Connectable.EMPTY_SET, 0);
        XMPPFirewalledAddress xmppFirewalledAddress = new XMPPFirewalledAddress(xmppAddress, firewalledAddress);
        assertSame(xmppAddress, xmppFirewalledAddress.getXmppAddress());
        assertSame(firewalledAddress, xmppFirewalledAddress.getFirewalledAddress());
    }

}
