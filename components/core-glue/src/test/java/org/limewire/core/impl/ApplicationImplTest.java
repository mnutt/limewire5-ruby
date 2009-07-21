package org.limewire.core.impl;

import java.lang.reflect.InvocationTargetException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivateAccessor;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.util.LimeWireUtils;

public class ApplicationImplTest extends BaseTestCase {

    public ApplicationImplTest(String name) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        super(name);
    }
    
    /** 
     * Tests the getUniqueUrl() method and ensure it returns a consistent 
     *  URL based on the GUID and original URL.
     */
    public void testAddClientInfoToUrl() {
        Mockery context = new Mockery();
        
        final ApplicationServices applicationServices = context.mock(ApplicationServices.class); 
            
        final ApplicationImpl applicationImpl = new ApplicationImpl(applicationServices, null);
        
        context.checking(new Expectations() {{
            allowing(applicationServices).getMyGUID();
            will(returnValue(new byte[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}));
        }});
        
        String urlOne = applicationImpl.addClientInfoToUrl("hello");
        String urlTwo = applicationImpl.addClientInfoToUrl("%Q#$@#$%testan");
        
        // Ensure the return is consistent
        assertEquals(urlOne, applicationImpl.addClientInfoToUrl("hello"));
        
        // Should at least have the original url
        assertTrue(urlOne.startsWith("hello"));
        assertTrue(urlTwo.startsWith("%Q#$@#$%testan"));
        
        // Should contain the guid
        assertTrue(urlOne.contains("guid=0102030405060708090A0B0C0D0E0F10"));
        assertTrue(urlTwo.contains("guid=0102030405060708090A0B0C0D0E0F10"));
        
        context.assertIsSatisfied();
    }

    /**
     * Test the core startup and shutdown hooks, also setting a flag.
     */
    public void testStartStopCore() {
        Mockery context = new Mockery();
        
        final LifecycleManager lifecycleManager = context.mock(LifecycleManager.class); 
            
        final ApplicationImpl applicationImpl = new ApplicationImpl(null, lifecycleManager);
        
        context.checking(new Expectations() {{
            exactly(1).of(lifecycleManager).start();
            exactly(1).of(lifecycleManager).shutdown();
            exactly(1).of(lifecycleManager).shutdown("flag");
        }});
        
        applicationImpl.startCore();
        applicationImpl.stopCore();
        applicationImpl.setShutdownFlag("flag");
        applicationImpl.stopCore();        
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests delegate getters for version information that link to LimeWireUtils using
     *  reflection.
     */
    public void testVersionDelegates() throws SecurityException, IllegalArgumentException,
        NoSuchFieldException, IllegalAccessException, ClassNotFoundException, 
        NoSuchMethodException, InvocationTargetException {
        
        final ApplicationImpl applicationImpl = new ApplicationImpl(null, null);
        
        PrivateAccessor testVersionAccessor = new PrivateAccessor(LimeWireUtils.class, null, "testVersion");
        PrivateAccessor isProAccessor = new PrivateAccessor(LimeWireUtils.class, null, "_isPro");
       
        testVersionAccessor.setValue("hello");
        String version1 = applicationImpl.getVersion();
        testVersionAccessor.setValue(null);
        String version2 = applicationImpl.getVersion();
        testVersionAccessor.reset();
        assertEquals("hello", version1);
        assertNotNull(version2);
        
        // Not fully testable since based on final String LimeWireUtils.LIMEWIRE_VERSION
        applicationImpl.isTestingVersion();
        
        isProAccessor.setValue(true);
        boolean isPro1 = applicationImpl.isProVersion();
        isProAccessor.setValue(false);
        boolean isPro2 = applicationImpl.isProVersion();
        isProAccessor.reset();
        assertTrue(isPro1);
        assertFalse(isPro2);
    }
}
