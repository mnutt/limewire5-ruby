package com.limegroup.gnutella.version;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.google.inject.Injector;

public class UpdateMessageVerifierImplTest extends BaseTestCase {
    
    public UpdateMessageVerifierImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UpdateMessageVerifierImplTest.class);
    }   

    public void testDefaultBindings() {
        Injector injector = LimeTestUtils.createInjector();
        assertEquals(UpdateMessageVerifierImpl.class, injector.getInstance(UpdateMessageVerifier.class).getClass());
    }
    
    private final String SIGNED_ASDF = "GAWAEFCXLZGA6CDODRDP35ZRU3XQS7LGUK3OH4ICCR5FMKRSATRE4RSHBGJ6CB34E5RAYQATJY||asdf\n";
    
    public void testMessageVerifies() {
        Injector injector = LimeTestUtils.createInjector();
        UpdateMessageVerifier updateMessageVerifier = injector.getInstance(UpdateMessageVerifier.class);
        String data = updateMessageVerifier.getVerifiedData(StringUtils.toUTF8Bytes(SIGNED_ASDF));
        assertEquals("asdf\n", data);
    }

}
