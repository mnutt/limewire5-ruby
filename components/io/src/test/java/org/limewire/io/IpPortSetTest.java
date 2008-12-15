package org.limewire.io;

import java.util.Arrays;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class IpPortSetTest extends BaseTestCase {

    public IpPortSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IpPortSetTest.class);
    }
    
    public void testRemoveAll() throws Exception {
        IpPort ipPort1 = new IpPortImpl("191.1.1.1", 4545);
        IpPort ipPort2 = new IpPortImpl("111.1.1.1", 5555);
        
        IpPortSet set = new IpPortSet(ipPort1, ipPort2);
        
        set.removeAll(Arrays.asList(new IpPortImpl(ipPort1.getAddress(), ipPort1.getPort()),
                new IpPortImpl(ipPort2.getAddress(), ipPort2.getPort())));
        
        assertTrue(set.isEmpty());
    }

}
