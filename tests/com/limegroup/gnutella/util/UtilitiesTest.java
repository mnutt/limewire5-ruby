package com.limegroup.gnutella.util;

import java.util.Arrays;

import junit.framework.Test;

/**
 * Unit tests for Utilities
 */
public class UtilitiesTest extends com.limegroup.gnutella.util.LimeTestCase {
    public UtilitiesTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UtilitiesTest.class);
    }

    public void testFill() {
        int[] in=null;
        int[] out=null;
        in= new int[] {0, 0, 0, 0, 0, 0, 0};
        out=new int[] {1, 1, 1, 1, 0, 0, 0};
        Utilities.fill(in, 0, 4, 1);
        assertTrue(Arrays.equals(in, out));

        in= new int[] {0, 0, 0, 0, 0, 0, 0};
        out=new int[] {1, 1, 1, 1, 1, 0, 0};
        Utilities.fill(in, 0, 5, 1);
        assertTrue(Arrays.equals(in, out));

        in= new int[] {0, 0, 0, 0, 0, 0, 0};
        out=new int[] {0, 0, 1, 1, 1, 1, 0};
        Utilities.fill(in, 2, 6, 1);
        assertTrue(Arrays.equals(in, out));

        in= new int[] {0, 0, 0, 0, 0, 0, 0};
        out=new int[] {0, 0, 1, 1, 1, 0, 0};
        Utilities.fill(in, 2, 5, 1);
        assertTrue(Arrays.equals(in, out));
    }

    public void testLog2() {
        for (int i=0; i<31; i++) {
            assertEquals(i, Utilities.log2(1<<i));
        }
    }     
}
