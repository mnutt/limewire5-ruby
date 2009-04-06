package com.limegroup.gnutella;

import java.util.Arrays;

import org.limewire.io.GUID;
import org.limewire.util.Base32;

import junit.framework.Test;


/**
 * Unit tests for Base32 class.
 */
public class Base32Test extends org.limewire.gnutella.tests.LimeTestCase {

    public Base32Test(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(Base32Test.class);
    }

    public void testBasic() {
        byte[] testBytes = new byte[5];
        testBytes[0] = (byte) 0xF0;
        testBytes[1] = (byte) 0x10;
        testBytes[2] = (byte) 0x24;
        testBytes[3] = (byte) 0xA5;
        testBytes[4] = (byte) 0x18;
        String encoded = Base32.encode(testBytes);
        assertEquals(8, encoded.length());
        assertEquals("6AICJJIY", encoded);
    }

    public void testGUID() {
        for (int i = 0; i < 1000; i++) {
            byte[] guid = GUID.makeGuid();
            assertTrue(Arrays.equals(guid, Base32.decode(Base32.encode(guid))));
            assertEquals(26, (Base32.encode(guid)).length());
        }
    }


}
