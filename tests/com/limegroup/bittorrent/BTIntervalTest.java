package com.limegroup.bittorrent;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class BTIntervalTest extends LimeTestCase {

    public BTIntervalTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BTIntervalTest.class);
    }
    
    public void testEquals() throws Exception {
        BTInterval a = new BTInterval(1,2,3);
        BTInterval b = new BTInterval(1,2,3);
        assertEquals(a,b);
        assertEquals(3,a.getId());
        assertEquals(3,b.getId());
        
        b = new BTInterval(1,1,3);
        assertNotEquals(a,b);
        b = new BTInterval(1,2,4);
        assertNotEquals(a,b);
    }
    
    public void testGet32Bit() throws Exception {
        BTInterval large = new BTInterval(Integer.MAX_VALUE+ 5L, Integer.MAX_VALUE+10L,1);
        assertEquals(Integer.MAX_VALUE+5, large.get32BitLow());
        assertEquals(Integer.MAX_VALUE+10, large.get32BitHigh());
        
        // test that binary representation is identical
        large = new BTInterval(0xFFFFFFF0L, 0xFFFFFFFFL, 1);
        assertEquals(0xFFFFFFF0, large.get32BitLow());
        assertEquals(0xFFFFFFFF, large.get32BitHigh());
        
        // test that length is Integer.MAX_VALUE + 6 (inclusive).
        large = new BTInterval(2L * Integer.MAX_VALUE, 3L * Integer.MAX_VALUE + 5,1 );
        assertEquals(Integer.MAX_VALUE + 6, large.get32BitLength()); 
    }
}
