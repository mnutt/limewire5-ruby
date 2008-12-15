package org.limewire.net;

import junit.framework.Test;

import org.limewire.common.LimeWireCommonModule;
import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class WhoIsRequestTest extends BaseTestCase {

    public WhoIsRequestFactory factory;
    
    public WhoIsRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(WhoIsRequestTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
        
    @Override
    protected void setUp() throws Exception {
        Injector injector = Guice.createInjector(new LimeWireCommonModule(), new LimeWireNetTestModule());        
        
        factory = injector.getInstance(WhoIsRequestFactory.class); 
    }
    
    public void testName() throws Exception {
        factory.createWhoIsRequest("google.com").doRequest();
    }
    
    public void testAddr() throws Exception {
        WhoIsRequest request = factory.createWhoIsRequest("64.233.187.99");
        
        request.doRequest();
        
        assertNotNull(request.getNetRange());
    }
    
}
