package org.limewire.nio.timeout;


import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class TimeoutControllerTest extends BaseTestCase {
    
    public TimeoutControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(TimeoutControllerTest.class);
    }
    
    public void testNoTimeout() {
        TimeoutController controller = new TimeoutController();
        assertEquals(-1, controller.getNextExpireTime());
        controller.processTimeouts(100); // does nothing.
    }

    public void testBasicTimeout() {
        TimeoutController controller = new TimeoutController();
        StubTimeoutable t1 = new StubTimeoutable();
        controller.addTimeout(t1, 100, 101);
        assertEquals(201, controller.getNextExpireTime());
        for(int i = 0; i < 201; i++) {
            controller.processTimeouts(i);
            assertFalse(t1.isNotified());
        }
        controller.processTimeouts(201);
        assertTrue(t1.isNotified());
        assertEquals(201, t1.getNow());
        assertEquals(201, t1.getExpired());
        assertEquals(101, t1.getTimeoutLength());
        assertEquals(-1, controller.getNextExpireTime());
    }
    
    public void testDelayedTimeout() {
        TimeoutController controller = new TimeoutController();
        StubTimeoutable t1 = new StubTimeoutable();
        controller.addTimeout(t1, 100, 150);
        assertEquals(250, controller.getNextExpireTime());
        controller.processTimeouts(500);
        assertTrue(t1.isNotified());
        assertEquals(500, t1.getNow());
        assertEquals(250, t1.getExpired());
        assertEquals(150, t1.getTimeoutLength());
        assertEquals(-1, controller.getNextExpireTime());        
    }
    
    public void testMultipleTimeoutsAtSameTime() {
        TimeoutController controller = new TimeoutController();
        StubTimeoutable t1 = new StubTimeoutable();
        StubTimeoutable t2 = new StubTimeoutable();
        
        assertEquals(-1, controller.getNextExpireTime());
        
        controller.addTimeout(t1, 100, 50);
        assertEquals(150, controller.getNextExpireTime());
        
        controller.addTimeout(t2, 120, 30);
        assertEquals(150, controller.getNextExpireTime());
        
        controller.processTimeouts(149);
        assertFalse(t1.isNotified());
        assertFalse(t2.isNotified());
        assertEquals(150, controller.getNextExpireTime());
        
        controller.processTimeouts(151);
        assertTrue(t1.isNotified());
        assertEquals(151, t1.getNow());
        assertEquals(150, t1.getExpired());
        assertEquals(50, t1.getTimeoutLength());
        assertTrue(t2.isNotified());
        assertEquals(151, t2.getNow());
        assertEquals(150, t2.getExpired());
        assertEquals(30, t2.getTimeoutLength());
        
        assertEquals(-1, controller.getNextExpireTime());
    }
    
    public void testMultipleTimeoutsDifferentTimes() {
        TimeoutController controller = new TimeoutController();
        StubTimeoutable t1 = new StubTimeoutable();
        StubTimeoutable t2 = new StubTimeoutable();
        StubTimeoutable t3 = new StubTimeoutable();
        

        assertEquals(-1, controller.getNextExpireTime());
        
        controller.addTimeout(t1, 100, 50);
        assertEquals(150, controller.getNextExpireTime());
        
        controller.addTimeout(t2, 120, 70);
        assertEquals(150, controller.getNextExpireTime());
        
        controller.addTimeout(t3, 140, 5);
        assertEquals(145, controller.getNextExpireTime());
        
        controller.processTimeouts(147);
        assertFalse(t1.isNotified());
        assertFalse(t2.isNotified());
        assertTrue(t3.isNotified());
        assertEquals(147, t3.getNow());
        assertEquals(145, t3.getExpired());
        assertEquals(5, t3.getTimeoutLength());
        
        assertEquals(150, controller.getNextExpireTime());
        
        controller.processTimeouts(200);
        assertTrue(t1.isNotified());
        assertEquals(200, t1.getNow());
        assertEquals(150, t1.getExpired());
        assertEquals(50, t1.getTimeoutLength());
        assertTrue(t2.isNotified());
        assertEquals(200, t2.getNow());
        assertEquals(190, t2.getExpired());
        assertEquals(70, t2.getTimeoutLength());
        
        assertEquals(-1, controller.getNextExpireTime());
    }
    
    
}
