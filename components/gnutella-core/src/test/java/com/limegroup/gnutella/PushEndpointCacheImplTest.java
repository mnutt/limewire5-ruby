package com.limegroup.gnutella;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;

public class PushEndpointCacheImplTest extends BaseTestCase {

    private PushEndpointCacheImpl pushEndpointCacheImpl;
    private Mockery context;
    private ScheduledExecutorService executorService;
    private Runnable weakCleaner;
    private HTTPHeaderUtils httpHeaderUtils;
    private NetworkInstanceUtils networkInstanceUtils;
    private ServiceScheduler serviceScheduler;
    
    public PushEndpointCacheImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushEndpointCacheImplTest.class);
    }    
   
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        executorService = context.mock(ScheduledExecutorService.class);
        serviceScheduler = context.mock(ServiceScheduler.class);
        
        context.checking(new Expectations() {{
            allowing(serviceScheduler).scheduleWithFixedDelay(with(any(String.class)), with(any(Runnable.class)), with(equal(30L)), with(equal(30L)), with(equal(TimeUnit.SECONDS)), with(equal(executorService)));
            will(new CustomAction("Schedule") {
               @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    weakCleaner = (Runnable) invocation.getParameter(1);
                    return null;
                } 
            });
        }});
        
        Injector injector = LimeTestUtils.createInjector();
        httpHeaderUtils = injector.getInstance(HTTPHeaderUtils.class);
        networkInstanceUtils = injector.getInstance(NetworkInstanceUtils.class);
        
        pushEndpointCacheImpl = new PushEndpointCacheImpl(httpHeaderUtils, networkInstanceUtils);
        pushEndpointCacheImpl.register(executorService, serviceScheduler);
    }

    public void testCachedEntriesAreExpunged() throws Exception {
        assertNotNull(weakCleaner);
        
        Set<IpPort> proxies = Collections.emptySet();
        GUID guid = new GUID();
        PushEndpoint pushEndpoint = new PushEndpointImpl(guid.bytes(), proxies, (byte)0, 1, new IpPortImpl("127.0.0.1:6666"), pushEndpointCacheImpl, networkInstanceUtils);
        
        assertNull(pushEndpointCacheImpl.getCached(guid));
        assertNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
        pushEndpoint.updateProxies(true);
        
        assertNotNull(pushEndpointCacheImpl.getCached(guid));
        assertNotNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
        System.gc();
        
        weakCleaner.run();
        
        assertNotNull(pushEndpointCacheImpl.getCached(guid));
        assertNotNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
        // now lose instance
        pushEndpoint = null;
        
        System.gc();
        
        weakCleaner.run();
        
        assertNull(pushEndpointCacheImpl.getCached(guid));
        assertNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
    }

    public void testRemoveProxy() throws Exception {
        Set<IpPort> proxies = new IpPortSet(new IpPortImpl("199.19.49.4", 55),
                new IpPortImpl("119.1.49.4", 4545));
        GUID guid = new GUID();
        PushEndpoint pushEndpoint = new PushEndpointImpl(guid.bytes(), proxies, (byte)0, 1, new IpPortImpl("127.0.0.1:6666"), pushEndpointCacheImpl, networkInstanceUtils);
        
        assertNull(pushEndpointCacheImpl.getCached(guid));
        assertNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
        pushEndpoint.updateProxies(true);
        
        assertNotNull(pushEndpointCacheImpl.getCached(guid));
        assertNotNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        assertEquals(2, pushEndpointCacheImpl.getPushEndpoint(guid).getProxies().size());
        
        pushEndpointCacheImpl.removePushProxy(guid.bytes(), new IpPortImpl("199.19.49.4", 55));
        assertEquals(1, pushEndpointCacheImpl.getPushEndpoint(guid).getProxies().size());
        assertEquals(1, pushEndpoint.getProxies().size());
        
        pushEndpointCacheImpl.removePushProxy(guid.bytes(), new IpPortImpl("119.1.49.4", 4545));
        assertEquals(0, pushEndpointCacheImpl.getPushEndpoint(guid).getProxies().size());
        assertEquals(0, pushEndpoint.getProxies().size());
    }

}
