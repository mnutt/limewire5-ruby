package com.limegroup.gnutella.dht.db;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;

public class PushEndpointManagerImplTest extends BaseTestCase {

    private Mockery context;
    private PushEndpointCache pushEndpointCache;
    private PushEndpointService pushEndpointFinder;
    private SearchListener<PushEndpoint> listener;
    private PushEndpoint result;
    private PushEndpointManagerImpl pushEndpointManager;

    public PushEndpointManagerImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushEndpointManagerImplTest.class);
    }
    
    @Override
    @SuppressWarnings({ "cast", "unchecked" })
    protected void setUp() throws Exception {
        DHTSettings.ENABLE_PUSH_PROXY_QUERIES.setValue(true);
        
        context = new Mockery();
        
        // the players
        pushEndpointCache = context.mock(PushEndpointCache.class);
        pushEndpointFinder = context.mock(PushEndpointService.class);
        listener = (SearchListener<PushEndpoint>)context.mock(SearchListener.class);
        result = context.mock(PushEndpoint.class);
        pushEndpointManager = new PushEndpointManagerImpl(pushEndpointCache, pushEndpointFinder);
    }
    
    public void testNoSearchIfPushEndpointInCache() throws Exception {
        final GUID guid = new GUID();
        final IpPortSet proxies = new IpPortSet(new IpPortImpl("192.168.1.1:4545"));
        
        context.checking(new Expectations() {{
            one(pushEndpointCache).getPushEndpoint(guid);
            will(returnValue(result));
            one(result).getProxies();
            will(returnValue(proxies));
            one(listener).handleResult(result);
            never(pushEndpointFinder).findPushEndpoint(guid, listener);
        }});
        
        pushEndpointManager.findPushEndpoint(guid, listener);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Cache is empty and two searches are started right away, second one
     * should fail immediately and not ask trigger a search.
     */
    @SuppressWarnings("unchecked")
    public void testTimeoutBetweenSearchesIsHonored() throws Exception {
        final GUID guid = new GUID();
        
        final Sequence sequence = context.sequence("sequence");
        
        context.checking(new Expectations() {{
            one(pushEndpointCache).getPushEndpoint(guid);
            will(returnValue(null));
            inSequence(sequence);
            one(pushEndpointFinder).findPushEndpoint(with(same(guid)), with(any(SearchListener.class)));
            inSequence(sequence);
            one(pushEndpointCache).getPushEndpoint(guid);
            will(returnValue(null));
            inSequence(sequence);
            one(listener).searchFailed();
            inSequence(sequence);
            // zero calls to the finder here
            
            // then after the timeout:
            one(pushEndpointCache).getPushEndpoint(guid);
            will(returnValue(null));
            inSequence(sequence);
            one(pushEndpointFinder).findPushEndpoint(with(same(guid)), with(any(SearchListener.class)));
            inSequence(sequence);
        }});
        
        pushEndpointManager.setTimeBetweenSearches(1000L);
        
        // does search
        pushEndpointManager.findPushEndpoint(guid, listener);
        // does not search
        pushEndpointManager.findPushEndpoint(guid, listener);
        
        Thread.sleep(1000L);
        
        // does search again
        pushEndpointManager.findPushEndpoint(guid, listener);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Asserts that {@link PushEndpointManagerImpl#startSearch(org.limewire.io.GUID, SearchListener)}
     * starts a search and notifies the cache of results.
     */
    @SuppressWarnings("unchecked")
    public void testSearchIsStartedAndCacheIsNotified() throws Exception {
        final GUID guid = new GUID();
        final IpPort validExternalAddress = new IpPortImpl("129.1.1.1", 4545);
        
        context.checking(new Expectations() {{
            one(pushEndpointFinder).findPushEndpoint(with(same(guid)), with(any(SearchListener.class)));
            will(new CustomAction("call listener") {
                public Object invoke(Invocation invocation) throws Throwable {
                    ((SearchListener<PushEndpoint>)invocation.getParameter(1)).handleResult(result);
                    return null;
                }
            });
            one(listener).handleResult(result);
            one(result).updateProxies(true);
            one(result).getValidExternalAddress();
            will(returnValue(validExternalAddress));
            one(result).getClientGUID();
            will(returnValue(guid.bytes()));
            one(pushEndpointCache).setAddr(guid.bytes(), validExternalAddress);
        }});
        
        pushEndpointManager.startSearch(guid, listener);
        
        context.assertIsSatisfied();
    }

    public void testPurge() throws Exception {
        PushEndpointManagerImpl pushEndpointManager = new PushEndpointManagerImpl(null, null);
        pushEndpointManager.setTimeBetweenSearches(100);
        ConcurrentMap<GUID, AtomicLong> lastSearchTimeByGUID = pushEndpointManager.getLastSearchTimeByGUID();
        
        // add an entry that should be purged, and one that shouldn't
        GUID guid1 = new GUID();
        GUID guid2 = new GUID();
        long currentTime = System.currentTimeMillis();

        lastSearchTimeByGUID.put(guid1, new AtomicLong(currentTime));
        lastSearchTimeByGUID.put(guid2, new AtomicLong(currentTime + 200));
        
        Thread.sleep(250);
        
        pushEndpointManager.purge();
        
        assertEquals(1, lastSearchTimeByGUID.size());
        
        assertTrue(lastSearchTimeByGUID.containsKey(guid2));
        assertFalse(lastSearchTimeByGUID.containsKey(guid1));
        
        Thread.sleep(250);
        pushEndpointManager.purge();
        assertTrue(lastSearchTimeByGUID.isEmpty());
    }
}
