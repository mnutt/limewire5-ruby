package org.limewire.core.impl.xmpp;

import org.limewire.util.BaseTestCase;

/**
 * Test to make sure the class can be created and interfaces properly with SystemUtils and native.
 */
public class IdleTimeImplTest extends BaseTestCase {

    public IdleTimeImplTest(String name) {
        super(name);
    }
    
    /**
     * Just make sure the function does not return any exceptions.  The 
     *  result doesn't much matter.
     */
    public void testSupportsIdleTime() {
        IdleTimeImpl idleTime = new IdleTimeImpl();
        
        // Return result is irrelevant because it is system dependent 
        idleTime.supportsIdleTime();
    }
    
    /**
     * Make sure getIdleTime() returns a sane result and one consistent with 
     *  supportsIdleTime().
     */
    public void testGetIdleTime() {
        IdleTimeImpl idleTime = new IdleTimeImpl();
        
        boolean isSupported = idleTime.supportsIdleTime();
        long time = idleTime.getIdleTime();
        
        if (!isSupported) {
            assertEquals(0, time);
        } 
        else {
            assertGreaterThanOrEquals(0, time);
        }
    }

}
