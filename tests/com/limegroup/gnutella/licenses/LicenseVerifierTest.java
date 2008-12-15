package com.limegroup.gnutella.licenses;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.inject.Providers;
import org.limewire.util.BaseTestCase;

public class LicenseVerifierTest extends BaseTestCase {

    public LicenseVerifierTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LicenseVerifierTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testVerify() throws Exception {
        Mockery context = new Mockery();
        
        final LicenseCache licenseCache = new LicenseCache(); 
        final LicenseVerifier licenseVerifier = new LicenseVerifier(Providers.of(licenseCache), Providers.nullProvider(LimeHttpClient.class));
                
        final License license = context.mock(License.class);
        
        context.checking(new Expectations() {{ 
            one(license).verify(licenseCache, null);
        }});
        
        Listener listener = new Listener();
        
        licenseVerifier.verify(license, listener);
        assertTrue(listener.latch.await(500, TimeUnit.MILLISECONDS));
        
        context.assertIsSatisfied();
    }

    public void testVerifyAndWait() throws Exception {
        Mockery context = new Mockery();
        
        final LicenseCache licenseCache = new LicenseCache(); 
        final LicenseVerifier licenseVerifier = new LicenseVerifier(Providers.of(licenseCache), Providers.nullProvider(LimeHttpClient.class));
                
        final License license = context.mock(License.class);
        
        context.checking(new Expectations() {{ 
            one(license).verify(licenseCache, null);
        }});
        
        Listener listener = new Listener();
        
        licenseVerifier.verifyAndWait(license, listener);
        assertEquals(0, listener.latch.getCount());
        
        context.assertIsSatisfied();
    }

    private class Listener implements VerificationListener {

        CountDownLatch latch = new CountDownLatch(1);
        
        public void licenseVerified(License license) {
            latch.countDown();
        }
    }
    
}
