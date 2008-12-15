package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.inject.Providers;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.guess.GUESSEndpoint;

public class BypassedResultsCacheTest extends BaseTestCase {

    private ActivityCallback  activityCallback;
    private Mockery mockery;
    private DownloadManager downloadManager;
    
    private GUESSEndpoint point1;
    
    private GUESSEndpoint point2;
    
    private GUESSEndpoint point3;
    
    private BypassedResultsCache bypassedResultsCache;
    
    public BypassedResultsCacheTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        mockery = new Mockery();
        downloadManager = mockery.mock(DownloadManager.class);
        activityCallback = mockery.mock(ActivityCallback.class);
        bypassedResultsCache = new BypassedResultsCache(Providers.of(activityCallback), downloadManager); 
        
        point1 = new GUESSEndpoint(InetAddress.getLocalHost(), 5555);
        point2 = new GUESSEndpoint(InetAddress.getLocalHost(), 6666);
        point3 = new GUESSEndpoint(InetAddress.getLocalHost(), 7777);
    }
    
    @Override
    protected void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }
    
    public static Test suite() {
        return buildTestSuite(BypassedResultsCacheTest.class);
    }
    
    public void testAddBypassedSource() {
        final GUID guid = new GUID();
        mockery.checking(new Expectations() {{
            exactly(2).of(activityCallback).isQueryAlive(with(same(guid)));
            will(returnValue(true));
        }});
        assertTrue(bypassedResultsCache.addBypassedSource(guid, point1));
        assertFalse(bypassedResultsCache.addBypassedSource(guid, point1));

        mockery.checking(new Expectations() {{
            exactly(2).of(activityCallback).isQueryAlive(with(same(guid)));
            will(returnValue(false));
            
            exactly(2).of(downloadManager).isGuidForQueryDownloading(with(same(guid)));
            will(returnValue(true));
        }});
        assertTrue(bypassedResultsCache.addBypassedSource(guid, point2));
        assertFalse(bypassedResultsCache.addBypassedSource(guid, point2));
        
        mockery.checking(new Expectations() {{
            exactly(1).of(activityCallback).isQueryAlive(with(same(guid)));
            will(returnValue(false));
            
            exactly(1).of(downloadManager).isGuidForQueryDownloading(with(same(guid)));
            will(returnValue(false));
        }});
        assertFalse(bypassedResultsCache.addBypassedSource(guid, point3));
        
        assertContains(bypassedResultsCache.getQueryLocs(guid), point1);
        assertContains(bypassedResultsCache.getQueryLocs(guid), point2);
    }
    
    public void testExpiration() {
        final GUID guid = new GUID();       
 
        mockery.checking(new Expectations() {{
            exactly(1).of(activityCallback).isQueryAlive(with(same(guid)));
            will(returnValue(true));
        }});
        assertTrue(bypassedResultsCache.addBypassedSource(guid, point1));
        
        mockery.checking(new Expectations() {{
            exactly(1).of(downloadManager).isGuidForQueryDownloading(with(same(guid)));
            will(returnValue(false));
        }}); 
        bypassedResultsCache.queryKilled(guid);
        
        assertTrue(bypassedResultsCache.getQueryLocs(guid).isEmpty());
        
        mockery.checking(new Expectations() {{
            exactly(1).of(activityCallback).isQueryAlive(with(same(guid)));
            will(returnValue(true));
        }});
        assertTrue(bypassedResultsCache.addBypassedSource(guid, point1));
        
        
        mockery.checking(new Expectations() {{
            exactly(1).of(activityCallback).isQueryAlive(with(same(guid)));
            will(returnValue(false));
            
            exactly(1).of(downloadManager).isGuidForQueryDownloading(with(same(guid)));
            will(returnValue(false));
        }});
        bypassedResultsCache.downloadFinished(guid);        
        assertTrue(bypassedResultsCache.getQueryLocs(guid).isEmpty());
    }
    
    public void testUpperThreshholdIsHonored() throws UnknownHostException {
        final int max = 150;
        final GUID guid = new GUID();
        mockery.checking(new Expectations() {{
            exactly(max+1).of(activityCallback).isQueryAlive(with(same(guid)));
            will(returnValue(true));
        }});
        for (int i = 0; i < max; i++) {
            assertTrue(bypassedResultsCache.addBypassedSource(guid, new GUESSEndpoint(InetAddress.getLocalHost(), 500 + i)));
        }        
        assertFalse(bypassedResultsCache.addBypassedSource(guid, point1));
    }
}
