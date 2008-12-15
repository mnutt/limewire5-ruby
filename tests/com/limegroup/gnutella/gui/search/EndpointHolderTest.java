package com.limegroup.gnutella.gui.search;

import junit.framework.Test;

import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;

import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.GUIBaseTestCase;

public class EndpointHolderTest extends GUIBaseTestCase { 

    public EndpointHolderTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(EndpointHolderTest.class); 
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LimeTestUtils.createInjector(GUI_CORE_MEDIATOR_INJECTION);
    }

    /**
     * Ensures that the ip:port is returned in {@link EndpointHolder#toString()}
     * if only one endpoint is known other wise Multiple + the number of results
     */
    public void testToStringAfterAddingHosts() throws Exception {
        String ip = "127.0.0.1";
        int port = 6666;
        EndpointHolder endpointHolder = new EndpointHolder(ip, port, false);
        assertEquals("127.0.0.1", endpointHolder.toString());
        
        endpointHolder.addHost(ip, port);
        assertEquals("127.0.0.1", endpointHolder.toString());
        
        endpointHolder.addHost("129.0.0.1", port);
        assertTrue(endpointHolder.toString().contains("(2)"));
        
        endpointHolder.addHosts(new IpPortSet(new IpPortImpl("192.168.0.1", 5555)));
        assertTrue(endpointHolder.toString().contains("(3"));
        
        endpointHolder.addHosts(new IpPortSet(new IpPortImpl("192.168.0.1", 5555)));
        assertTrue(endpointHolder.toString().contains("(3"));
    }

}
