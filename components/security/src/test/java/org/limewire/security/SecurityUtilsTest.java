package org.limewire.security;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class SecurityUtilsTest extends BaseTestCase {
    
    public SecurityUtilsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SecurityUtilsTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }          

    /** 
     * Tries to exhaust the system's entropy pool, and see
     * if SecureRandom generation still takes less than
     * 50 milliseconds. 
     */
    public static void testCreateSecureRandomNoBlock() {
        long startTime = System.currentTimeMillis();
       
        int testCount = 100000;
        for(int i=testCount; i > 0; --i) {
            // Generate the first int from each SecureRandom instance in 
            // order to catch cases of lazy seeding in which the generator
            // seeds itself in a blocking manner when the first data is
            // requested.
            SecurityUtils.createSecureRandomNoBlock().nextInt();   
        }
        assertLessThan("Non-blocking object creation took too long",
                50 * testCount, System.currentTimeMillis() - startTime);
    }
    
}
