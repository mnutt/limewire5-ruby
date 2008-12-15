package org.limewire.util;

import junit.framework.Test;

public class DebugRunnableTest  extends BaseTestCase {
    
    public DebugRunnableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DebugRunnableTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testTraceNoCause() {
        Runnable r = new Runnable() {
            public void run() {
                throw new RuntimeException("parent");
            }
        };
        
        DebugRunnable debugR = new DebugRunnable(r);
        try {
            debugR.run();
            fail("should have thrown!");
        } catch(RuntimeException re) {
            assertEquals("parent", re.getMessage());
            assertEquals("Debug Exception Creation", re.getCause().getMessage());
        }
        
    }
    
    public void testTraceWithCause() {
        Runnable r = new Runnable() {
            public void run() {
                throw new RuntimeException("parent", new RuntimeException("cause"));
            }
        };
        
        DebugRunnable debugR = new DebugRunnable(r);
        try {
            debugR.run();
            fail("should have thrown!");
        } catch(RuntimeException re) {
            assertEquals("parent", re.getMessage());
            assertEquals("cause", re.getCause().getMessage());
        }
    }
    
    /**
     * Creates debug runnable with null cause and thereby marks its creation
     * time stack with its method frame so the test can search for it.
     */
    private DebugRunnable createDebugRunnableAndMarkStackTrace() {
        Runnable r = new Runnable() {
            public void run() {
                throw new RuntimeException("parent", null);
            }
        };
        return new DebugRunnable(r);
    }
    
    public void testTraceWithNullCause() {
        DebugRunnable debugRunnable = createDebugRunnableAndMarkStackTrace();
        try {
            debugRunnable.run();
            fail("should have thrown");
        } catch (IllegalStateException ise) {
            fail("Should not have thrown illegal state: " + ise);
        } catch( RuntimeException re) {
            assertEquals("parent", re.getMessage());
            StackTraceElement[] stackTrace = re.getStackTrace();
            boolean containsDebugCreationException = false;
            for (StackTraceElement element : stackTrace) {
                containsDebugCreationException |= element.getMethodName().contains("createDebugRunnableAndMarkStackTrace");
            }
            assertTrue(containsDebugCreationException);
        }
        
    }
}
