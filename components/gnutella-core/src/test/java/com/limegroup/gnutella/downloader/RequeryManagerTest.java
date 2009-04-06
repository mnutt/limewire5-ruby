package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.dht.NullDHTController;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.SearchListener;
import com.limegroup.gnutella.downloader.RequeryManager.QueryType;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.LimeWireUtils;

public class RequeryManagerTest extends LimeTestCase {

    public RequeryManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RequeryManagerTest.class);
    }
    
    private RequeryManagerFactory requeryManagerFactory;
    private MyDHTManager dhtManager;
    private RequeryListener requeryListener;
    private DownloadManager downloadManager;
    private MyAltLocFinder altLocFinder;
    private Mockery mockery;
    private Sequence sequence;
    
    private URN sha1Urn;
    
    @Override
    public void setUp() throws Exception {
        DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
        RequeryManager.NO_DELAY = true;
        
        mockery = new Mockery();
        requeryListener = mockery.mock(RequeryListener.class);
        downloadManager = mockery.mock(DownloadManager.class);
        sequence = mockery.sequence("Sequence");
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
           @Override
            protected void configure() {
                bind(DHTManager.class).to(MyDHTManager.class);
                bind(DownloadManager.class).toInstance(downloadManager);
                bind(AltLocFinder.class).to(MyAltLocFinder.class);
            } 
        });

        dhtManager = (MyDHTManager)injector.getInstance(DHTManager.class);    
        altLocFinder = (MyAltLocFinder)injector.getInstance(AltLocFinder.class);
        requeryManagerFactory = injector.getInstance(RequeryManagerFactory.class);
        
        sha1Urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        
        setPro(false);
    }
    
    @Override
    protected void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }
    
    private void setPro(boolean pro) throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "_isPro", pro);
        assertEquals(pro, LimeWireUtils.isPro());
    }
    
    public void testRegistersWithDHTManager() throws Exception {
        assertNull(dhtManager.listener);
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        assertSame(requeryManager, dhtManager.listener);
        requeryManager.cleanUp();
        assertNull(dhtManager.listener);
    }
    
    public void testCancelsQueries() throws Exception {
        dhtManager.on = true;
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength()); inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        requeryManager.activate();
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener);
        assertTrue(requeryManager.isWaitingForResults());
        assertFalse(altLocFinder.cancelled);
        
        // We're stubbing out the Shutdownable, so it doesn't actually notify us it finished..
        // but we do check to make sure it was cancelled.
//        mockery.checking(new Expectations() {{
//            one(requeryListener).lookupFinished(QueryType.DHT);
//        }});
        
        requeryManager.cleanUp();
        assertTrue(altLocFinder.cancelled);
    }
    
    public void testNotInitedDoesNothingBasic() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        // shouldn't trigger any queries
        dhtManager.on = true;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertNull(altLocFinder.listener);
    }
    
    public void testNotInitedAutoDHTPro() throws Exception {
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        setPro(true);
        
        // if dht is off do nothing
        dhtManager.on = false;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertNull(altLocFinder.listener);
        
        // if dht is on start querying
        dhtManager.on = true;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength()); inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener);
        
        // But immediately after, requires an activate (for gnet query)
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // but if some time passes, a dht query will work again.
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupFinished(QueryType.DHT); inSequence(sequence);
        }});
        requeryManager.handleAltLocSearchDone(false);
        PrivilegedAccessor.setValue(requeryManager, "lastQuerySent", 1);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength()); inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        // and we should start a lookup
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // make sure after that lookup finishes, can still do gnet 
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupFinished(QueryType.DHT); inSequence(sequence);
        }});
        requeryManager.handleAltLocSearchDone(false);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // some time passes, but we've hit our dht queries limit
        // can still send gnet though
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupFinished(QueryType.DHT); inSequence(sequence);
        }});
        requeryManager.handleAltLocSearchDone(false);
        PrivilegedAccessor.setValue(requeryManager, "lastQuerySent", 1);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    /**
     * tests that only a single gnet query is sent if dht is off. 
     */
    public void testOnlyGnetIfNoDHT() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        
        dhtManager.on = false;
        assertNull(altLocFinder.listener);
        
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.activate();
        assertTrue(requeryManager.canSendQueryNow());
        
        final QueryRequest queryRequest = mockery.mock(QueryRequest.class);
        // first try a requery that will not work
        mockery.checking(new Expectations() {{
            one(requeryListener).createQuery(); inSequence(sequence);
            will(returnValue(queryRequest));

            one(downloadManager).sendQuery(with(same(queryRequest))); inSequence(sequence);
            
            one(requeryListener).lookupStarted(QueryType.GNUTELLA, RequeryManager.TIME_BETWEEN_REQUERIES); inSequence(sequence);
           
        }});
        requeryManager.sendQuery();
        assertNull(altLocFinder.listener); // should not try dht
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // but if we try again, nothing happens.
        requeryManager.sendQuery();
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testWaitsForStableConns() throws Exception {
        // no DHT nor connections
        dhtManager.on = false;
        RequeryManager.NO_DELAY = false;
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        requeryManager.activate();
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupPending(QueryType.GNUTELLA, 750); inSequence(sequence);
        }});
        requeryManager.sendQuery();
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        
        // now we get connected
        RequeryManager.NO_DELAY = true;
        final QueryRequest queryRequest = mockery.mock(QueryRequest.class);
        mockery.checking(new Expectations() {{
            one(requeryListener).createQuery(); inSequence(sequence);
            will(returnValue(queryRequest));
            
            one(downloadManager).sendQuery(with(same(queryRequest))); inSequence(sequence);
            
            one(requeryListener).lookupStarted(QueryType.GNUTELLA, RequeryManager.TIME_BETWEEN_REQUERIES); inSequence(sequence);
        }});
        requeryManager.sendQuery();
        // should be sent.
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testDHTTurnsOnStartsAutoIfInited() throws Exception {
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        // with dht off, send a query
        dhtManager.on = false;
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        final QueryRequest queryRequest = mockery.mock(QueryRequest.class);
        // first try a requery that will not work
        mockery.checking(new Expectations() {{
            one(requeryListener).createQuery(); inSequence(sequence);
            will(returnValue(queryRequest));
            
            one(downloadManager).sendQuery(with(same(queryRequest)));  inSequence(sequence);
            
            one(requeryListener).lookupStarted(QueryType.GNUTELLA, RequeryManager.TIME_BETWEEN_REQUERIES);  inSequence(sequence);            
        }});
        requeryManager.activate();
        requeryManager.sendQuery();
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // query fails, dht still off
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertNull(altLocFinder.listener);
        
        // turn the dht on should immediately query
        // even though the gnet query happened recently
        dhtManager.on = true;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength());  inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        // and we should start a lookup
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener);
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // make sure after that lookup finishes, we still can't do gnet 
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupFinished(QueryType.DHT);  inSequence(sequence);
        }}); 
        requeryManager.handleAltLocSearchDone(false);
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // some time passes, can send one more dht query
        altLocFinder.listener = null;
        PrivilegedAccessor.setValue(requeryManager, "lastQuerySent", 1);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength());  inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener);
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // more time passes, but we hit our dht query limit so we can't do anything.
        altLocFinder.listener = null;
        PrivilegedAccessor.setValue(requeryManager, "lastQuerySent", 1);
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testGnetFollowsDHT() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        dhtManager.on = true;
        requeryManager.activate();
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        
        // with dht on, start a requery
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength());  inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener);
        assertTrue(requeryManager.isWaitingForResults());
        
        // pretend the dht lookup fails
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupFinished(QueryType.DHT);
        }});
        requeryManager.handleAltLocSearchDone(false);
        assertFalse(requeryManager.isWaitingForResults());
        
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        
        // the next requery should be gnet
        altLocFinder.listener = null;
        final QueryRequest queryRequest = mockery.mock(QueryRequest.class);
        // first try a requery that will not work
        mockery.checking(new Expectations() {{
            one(requeryListener).createQuery();  inSequence(sequence);
            will(returnValue(queryRequest));
            
            one(downloadManager).sendQuery(with(same(queryRequest)));  inSequence(sequence);
            
            one(requeryListener).lookupStarted(QueryType.GNUTELLA, RequeryManager.TIME_BETWEEN_REQUERIES);  inSequence(sequence);
        }});
        requeryManager.sendQuery();
        assertTrue(requeryManager.isWaitingForResults());
        assertNull(altLocFinder.listener);
        
        // from now on we should give up & no more requeries
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testGnetFollowsDHTPro() throws Exception {
        setPro(true);
        testGnetFollowsDHT();
    }
    
    public void testOnlyGnetPro() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        dhtManager.on = true;
        setPro(true);
        
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength());  inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener); // sent a DHT query
        assertTrue(requeryManager.isWaitingForResults());
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupFinished(QueryType.DHT);  inSequence(sequence);
        }});
        requeryManager.handleAltLocSearchDone(false); // finish it
        assertFalse(requeryManager.isWaitingForResults());
        
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.activate(); // now activate it.
        altLocFinder.listener = null;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        final QueryRequest queryRequest = mockery.mock(QueryRequest.class);
        // first try a requery that will not work
        mockery.checking(new Expectations() {{
            one(requeryListener).createQuery();  inSequence(sequence);
            will(returnValue(queryRequest));
            
            one(downloadManager).sendQuery(with(same(queryRequest)));  inSequence(sequence);

            one(requeryListener).lookupStarted(QueryType.GNUTELLA, RequeryManager.TIME_BETWEEN_REQUERIES);  inSequence(sequence);
        }});
        requeryManager.sendQuery();       
        assertTrue(requeryManager.isWaitingForResults());
        assertNull(altLocFinder.listener);
        
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testDHTTurnsOff() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(requeryListener);
        dhtManager.on = true;
        setPro(true); // so we immediately launch a query
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength());  inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener); // sent a DHT query
        assertTrue(requeryManager.isWaitingForResults());
        
        // now turn the dht off
        dhtManager.on = false;
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupFinished(QueryType.DHT);  inSequence(sequence);
        }});
        requeryManager.handleDHTEvent(new DHTEvent(new NullDHTController(), DHTEvent.Type.STOPPED));
        assertFalse(requeryManager.isWaitingForResults());
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // turn the dht on again, and even though no time has passed
        // since the last query we can still do one
        dhtManager.on = true;
        
        altLocFinder.listener = null;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        mockery.checking(new Expectations() {{
            one(requeryListener).lookupStarted(QueryType.DHT, dhtQueryLength());  inSequence(sequence);
            one(requeryListener).getSHA1Urn();
            will(returnValue(sha1Urn));
            inSequence(sequence);
        }});
        requeryManager.sendQuery();
        assertSame(requeryManager.searchHandler, altLocFinder.listener); // sent a DHT query
        assertTrue(requeryManager.isWaitingForResults());
    }
    
    private long dhtQueryLength() {
        return Math.max(RequeryManager.TIME_BETWEEN_REQUERIES, LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue());
    }
    
    @Singleton
    private static class MyDHTManager extends DHTManagerStub {

        private volatile DHTEventListener listener;
        private volatile boolean on;
        
        @Override
        public void addEventListener(DHTEventListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean isMemberOfDHT() {
            return on;
        }

        @Override
        public void removeEventListener(DHTEventListener listener) {
            if (this.listener == listener)
                this.listener = null;
        }
    }
    
    @Singleton    
    private static class MyAltLocFinder implements AltLocFinder {
        
        private volatile SearchListener<AlternateLocation> listener;
        
        volatile boolean cancelled;
        
        public Shutdownable findAltLocs(URN urn, SearchListener<AlternateLocation> listener) {
            this.listener = listener;
            return new Shutdownable() {
                public void shutdown() {
                    cancelled = true;
                }
            };
        }

        public boolean findPushAltLocs(GUID guid, URN urn, SearchListener<AlternateLocation> listener) {
            return false;
        }

        public AlternateLocation getAlternateLocation(GUID guid, URN urn) {
            return null;
        }
 
    }
}
