package org.limewire.io;


import java.net.UnknownHostException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class IpPortImplTest extends BaseTestCase {
    
    public IpPortImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IpPortImplTest.class);
    }
    
    public void testHostPortConstructor() throws Exception {
        IpPort basic = new IpPortImpl("www.google.com:92");
        assertEquals("www.google.com", basic.getAddress());
        assertEquals(92, basic.getPort());
        
        IpPort noport = new IpPortImpl("www.yahoo.com");
        assertEquals("www.yahoo.com", noport.getAddress());
        assertEquals(80, noport.getPort());
        
        try {
            new IpPortImpl("failsfailafsilasdflihasdlihasdfliashdfi.com");
            fail("shouldn't have succeeded");
        } catch(UnknownHostException expected) {}
    }
}
