package com.limegroup.gnutella.downloader;

import org.limewire.io.GUID;

import com.limegroup.gnutella.util.LimeTestCase;

import junit.framework.Test;

public class PushDetailsTest extends LimeTestCase {

    public PushDetailsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushDetailsTest.class);
    }
    
    public void testDetails(){
        
        GUID guid = new GUID();
        String address = "address";
        
        PushDetails pushDetails = new PushDetails(guid.bytes(), address);
        
        assertEquals(guid.bytes(), pushDetails.getClientGUID());
        assertEquals(address, pushDetails.getAddress());
        assertNotEquals(guid, pushDetails.getUniqueID());
        
        PushDetails newDetails = new PushDetails(guid.bytes(), address);
        
        assertFalse(pushDetails.equals(newDetails));
        assertFalse(pushDetails.equals(null));
        assertTrue(pushDetails.equals(pushDetails));
        
        
    }
    
    
}
