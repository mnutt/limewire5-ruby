package com.limegroup.gnutella.util;

import java.util.Arrays;
import java.util.List;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import com.google.inject.Injector;

import junit.framework.Test;

public class FECUtilsImplTest extends LimeTestCase {

    public FECUtilsImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FECUtilsImplTest.class);
    }
    
    private FECUtils fecUtils;
    
    @Override
    public void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        fecUtils = injector.getInstance(FECUtils.class);
    }
    
    /**
     * tests that we're running in pure java mode
     */
    public void testPureCode() throws Exception {
        assertEquals("pure8,pure16", System.getProperty("com.onionnetworks.fec.keys"));
    }
    
    public void testFEC() throws Exception {
        
        byte [] data = new byte[10000];
        Arrays.fill(data, (byte)1);

        List<byte []> encoded = fecUtils.encode(data, 1001, 1.3f);
        assertEquals(13,encoded.size());

        // lose some 3 random packets
        encoded.set(0, null);
        encoded.set(5, null);
        encoded.set(12, null);
        byte [] decoded = fecUtils.decode(encoded, 10000);
        assertTrue(Arrays.equals(data, decoded));
        
        // lose one more packet and decoding fails
        encoded.set(2, null);
        assertNull(fecUtils.decode(encoded, 10000));
    }
}
