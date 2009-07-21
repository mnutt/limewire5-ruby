package org.limewire.collection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Unit tests for <code>IntBuffer</code>.
 */
public class IntBufferTest extends BaseTestCase {

    public IntBufferTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IntBufferTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testLegacy() {
        IntBuffer buf = new IntBuffer(10);
        for (int i = 0; i < 10; i++) {
            buf.addLast(i);
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(i, buf.get(i));
        }

        assertEquals(0, buf.addLast(10));
        assertEquals(1, buf.get(0));

        try {
            buf.get(11);
            fail("expected exception");
        } catch (IndexOutOfBoundsException e) {
        }
    }
}