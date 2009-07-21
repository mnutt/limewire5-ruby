package com.limegroup.gnutella.messages.vendor;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.ByteUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.downloader.PingRanker;
import com.limegroup.gnutella.downloader.RemoteFileDescContext;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.GnutellaFileCollectionStub;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.IncompleteFileCollection;
import com.limegroup.gnutella.library.IncompleteFileDescStub;
import com.limegroup.gnutella.library.LibraryStubModule;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.stubs.UploadManagerStub;


/**
 * this class tests the handling of udp head requests and responses.
 */
@SuppressWarnings({"unchecked", "null"})
public class HeadTest extends LimeTestCase {

	
	/**
	 * URNs for the 3 files that will be requested
	 */
    private  URN _haveFull,_notHave,_havePartial, _tlsURN, _largeURN;
	
	/**
	 * file descs for the partial and complete files
	 */
    private IncompleteFileDescStub _partial, _partialLarge;
    /**
	 * an interval that can fit in a packet, and one that can't
	 */
    private IntervalSet _ranges, _rangesMedium, _rangesJustFit, _rangesTooBig, _rangesLarge, _rangesOnlyLarge;
	
    private PushEndpoint pushCollectionPE, tlsCollectionPE;
    
    private RemoteFileDescContext blankRFD;
	
    @Inject private HeadPongFactory headPongFactory;
	
    @Inject private Injector injector;
    
    private Mockery mockery;
    @Inject private DownloadManager downloadManager;
    
    @Inject private RemoteFileDescFactory remoteFileDescFactory;
    @Inject @GnutellaFiles private FileCollection gnutellaFileCollection;
    @Inject private IncompleteFileCollection incompleteFileCollection;
    
	public HeadTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(HeadTest.class);
	}
	
	
	@Override
    public void setUp() throws Exception {
	    SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
	    mockery = new Mockery();
	    downloadManager = mockery.mock(DownloadManager.class);
	    
	    
	    injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MessageRouter.class).to(MessageRouterStub.class);
                bind(ConnectionManager.class).to(ConnectionManagerStub.class);
                bind(NetworkManager.class).to(NetworkManagerStub.class);
                bind(UploadManager.class).to(UploadManagerStub.class);
                bind(DownloadManager.class).toInstance(downloadManager);
            }
	    }, new LibraryStubModule(), LimeTestUtils.createModule(this));
	    
	    NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
	    networkManager.setAcceptedIncomingConnection(true);
        networkManager.setIncomingTLSEnabled(true);
	    
        ConnectionManagerStub connectionManager = (ConnectionManagerStub)injector.getInstance(ConnectionManager.class);
	    connectionManager.setPushProxies(new HashSet<Connectable>(Collections.singletonList(new ConnectableImpl("1.2.3.4", 6346, false))));
	    
	    int base=0;
		_ranges = new IntervalSet();
		for (int i=2;i<10;i++) {
			int low = base;
			_ranges.add(Range.createRange(low,low+i));
			base+=2*i;
		}
		
		base=0;
		_rangesMedium = new IntervalSet();
		for (int i=2;i<70;i++) {
			int low = base;
			_rangesMedium.add(Range.createRange(low,low+i));
			base+=2*i;
		}
		
		base=0;
		_rangesJustFit = new IntervalSet();
		for (int i=2;i<73;i++) {
			int low = base;
			_rangesJustFit.add(Range.createRange(low,low+i));
			base+=2*i;
		}
		
		base=0;
		_rangesTooBig = new IntervalSet();
		for (int i=2;i<220;i++) {
			int low = base;
			_rangesTooBig.add(Range.createRange(low,low+i));
			base+=2*i;
		}
        
        _rangesLarge = new IntervalSet();
        _rangesLarge.add(Range.createRange(10, 20));
        _rangesLarge.add(Range.createRange(0xFFFFFF00l, 0xFFFFFFFFFFl));
        _rangesOnlyLarge = new IntervalSet();
        _rangesOnlyLarge.add(Range.createRange(0xFFFFFF00l, 0xFFFFFFFFFFl));
		
        _notHave =      GnutellaFileCollectionStub.DEFAULT_URN;
		_haveFull =     URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
		_havePartial =  URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFD");
        _tlsURN =       URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYTLS");
        _largeURN =     URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYTLG");
		
		_partial = new IncompleteFileDescStub("incomplete",_havePartial,3);
		_partial.setRangesByte(_ranges.toBytes());
        _partialLarge = new IncompleteFileDescStub("incompleteLArge", _largeURN, 4) {
            @Override
            public long getFileSize() {
                return 0xFFFFFFFF00l;
            }
        };
        _partialLarge.setRangesByte(_rangesLarge.toBytes());

        FileDescStub complete = new FileDescStub("complete", _haveFull, 2);        
        gnutellaFileCollection.add(complete);
        gnutellaFileCollection.add(new FileDescStub("test", _tlsURN, 100));
        incompleteFileCollection.add(_partial);
        incompleteFileCollection.add(_partialLarge);
        
        assertEquals(_partial,incompleteFileCollection.getFileDesc(_havePartial));
        assertEquals(_partialLarge,incompleteFileCollection.getFileDesc(_largeURN));
        assertEquals(complete,gnutellaFileCollection.getFileDesc(_haveFull));
        
        
        blankRFD = new RemoteFileDescContext(remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("1.1.1.1", 1, false), 1, "file", 1, new byte[16], 1, -1,
                false, null, URN.NO_URN_SET, false, null, -1));
        assertFalse(blankRFD.isBusy());
        assertFalse(blankRFD.isPartialSource());
        assertFalse(blankRFD.isReplyToMulticast());
        assertEquals(Integer.MAX_VALUE, blankRFD.getQueueStatus());
        
        createCollections();
        
        PrivilegedAccessor.setValue(HeadPongFactoryImpl.class, "PACKET_SIZE", HeadPongFactoryImpl.DEFAULT_PACKET_SIZE);
    }
	
	@Override
	protected void tearDown() throws Exception {
	    mockery.assertIsSatisfied();
	}
    
    private void clearStoredProxies() throws Exception {
        PushEndpointCache pushEndpointCache = injector.getInstance(PushEndpointCache.class);
        pushEndpointCache.clear();
    }
    
	/**
	 * tests the scenario where the file cannot be found.
	 */
	public void testFileNotFound() throws Exception {
		HeadPing ping = new HeadPing(_notHave);
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
		pong = reparse(pong);
		
		assertEquals(pong.getGUID(),ping.getGUID());
		
		assertFalse(pong.hasFile());
		assertFalse(pong.hasCompleteFile());
		assertEmpty(pong.getAltLocs());
		assertNull(pong.getRanges());
	}
	
    /** Tests that a binary headping gets a 404 for large files */
    public void testBinaryLarge() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_largeURN)));
            will(returnValue(false));
        }});
        
        MockHeadPongRequestor ping = new MockHeadPongRequestor();
        ping.setGuid(GUID.makeGuid());
        ping.setUrn(_largeURN);
        ping.setPongGGEPCapable(false);
        HeadPong pong = headPongFactory.create(ping);
        pong = reparse(pong);
        assertFalse(pong.hasFile());
        
        ping.setPongGGEPCapable(true);
        pong = headPongFactory.create(ping);
        pong = reparse(pong);
        assertTrue(pong.hasFile());
    }
    
	/**
	 * tests the scenarios where an incomplete and complete files are
	 * behind firewall or open.
	 */
	public void testFirewalledNoAcceptedIncoming() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        	    
	    NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
	    networkManager.setAcceptedIncomingConnection(false);
		
		HeadPing ping = new HeadPing(_haveFull);
		HeadPing pingi = new HeadPing(_havePartial);
		HeadPong pong = headPongFactory.create(ping);
		HeadPong pongi = headPongFactory.create(pingi);
        clearStoredProxies();
        pong = reparse(pong);
        pongi = reparse(pongi);
		
		assertEquals(ping.getGUID(),pong.getGUID());
		assertEquals(pingi.getGUID(),pongi.getGUID());
		
		assertTrue(pong.hasFile());
		assertTrue(pong.isFirewalled());
		assertTrue(pong.hasCompleteFile());
		assertEmpty(pong.getAltLocs());
		assertNull(pong.getRanges());
		
		assertTrue(pongi.hasFile());
		assertTrue(pongi.isFirewalled());
		assertFalse(pongi.hasCompleteFile());
		assertEmpty(pongi.getAltLocs());
		assertNull(pongi.getRanges());
    }
    
    public void testFirewalledAcceptedIncoming() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
        NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        networkManager.setAcceptedIncomingConnection(true);
		
		HeadPing ping = new HeadPing(_haveFull);
        HeadPing pingi = new HeadPing(_havePartial);
		HeadPong pong = headPongFactory.create(ping);
        HeadPong pongi = headPongFactory.create(pingi);
        clearStoredProxies();
        pong = reparse(pong);
        pongi = reparse(pongi);
		
		assertEquals(pong.getGUID(),ping.getGUID());
		assertEquals(pingi.getGUID(),pongi.getGUID());
		
		assertFalse(pong.isFirewalled());
		assertTrue(pong.hasFile());
		assertTrue(pong.hasCompleteFile());
		assertEmpty(pong.getAltLocs());
		assertNull(pong.getRanges());
		
		assertTrue(pongi.hasFile());
		assertFalse(pongi.isFirewalled());
		assertFalse(pongi.hasCompleteFile());
		assertEmpty(pongi.getAltLocs());
		assertNull(pongi.getRanges());
	}
	
	/**
	 * tests whether the downloading flag is set properly.
	 */
	public void testActivelyDownloading() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_partial.getSHA1Urn())));
            will(returnValue(true));
        }});
		HeadPing ping = new HeadPing(_havePartial);
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
		pong = reparse(pong);
		assertTrue(pong.isDownloading());
    }
    
    public void testNotActivelyDownloading() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        HeadPing ping = new HeadPing(_havePartial);
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);
		assertFalse(pong.isDownloading());
	}
	/**
	 * tests requesting ranges from complete, incomplete files
	 * as well as requesting too big ranges to fit in packet.
	 */
	public void testRangesFit() throws Exception {		
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
	    
		HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),_haveFull,HeadPing.INTERVALS);
		HeadPing pingi = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,HeadPing.INTERVALS);		
		HeadPong pong = headPongFactory.create(ping);
		HeadPong pongi = headPongFactory.create(pingi);
        clearStoredProxies();
        pong = reparse(pong);
        pongi = reparse(pongi);
		
		assertTrue(pong.hasCompleteFile());
		assertFalse(pongi.hasCompleteFile());
		
		assertNull(pong.getRanges());
		assertNotNull(pongi.getRanges());
        
        PingRanker.updateContext(blankRFD, pongi);
        assertTrue(blankRFD.isPartialSource());
        assertEquals(pongi.getRanges(), blankRFD.getAvailableRanges());
        
        PingRanker.updateContext(blankRFD, pong);
        assertFalse(blankRFD.isPartialSource());        
		
		assertTrue(Arrays.equals(_ranges.toBytes().ints,pongi.getRanges().toBytes().ints));
    }
    
    public void testRangesDontFit() throws Exception {		
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
		//now make the incomplete file desc carry ranges which are too big to
		//fit in a packet
		_partial.setRangesByte(_rangesTooBig.toBytes());
        try {    		
    		HeadPing pingi = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,HeadPing.INTERVALS);
    		HeadPong pongi = headPongFactory.create(pingi);
            clearStoredProxies();
            pongi = reparse(pongi);
    		
    		assertNull(pongi.getRanges());
    		assertLessThan(HeadPongFactoryImpl.DEFAULT_PACKET_SIZE,pongi.getPayload().length);
        } finally {
            _partial.setRangesByte(_ranges.toBytes());
        }
	}
	
    public void testLargeRanges() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_largeURN)));
            will(returnValue(false));
        }});
        
       _partialLarge.setRangesByte(_rangesLarge.toBytes());
       HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),_largeURN,HeadPing.INTERVALS);
       HeadPong pong = headPongFactory.create(ping);
       clearStoredProxies();
       pong = reparse(pong);
       IntervalSet large = pong.getRanges();
       assertEquals(_rangesLarge, large);
    }
    
    public void testOnlyLargeRanges() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_largeURN)));
            will(returnValue(false));
        }});
        
        _partialLarge.setRangesByte(_rangesOnlyLarge.toBytes());
        HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),_largeURN,HeadPing.INTERVALS);
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);
        IntervalSet large = pong.getRanges();
        assertEquals(_rangesOnlyLarge, large); 
    }
    
	/**
	 * tests various values for the queue rank
	 */
	public void testQueueStatusEmpty() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
	    
		HeadPing ping = new HeadPing(_havePartial);		
		HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);
		int allFree =  pong.getQueueStatus();
		assertEquals(-UploadSettings.HARD_MAX_UPLOADS.getValue(), allFree);
        PingRanker.updateContext(blankRFD, pong);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertFalse(blankRFD.isBusy());
    }
    
    public void testQueueStatusSomeTaken() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
        UploadManagerStub uploadManager = (UploadManagerStub)injector.getInstance(UploadManager.class);
        HeadPing ping = new HeadPing(_havePartial);     
        uploadManager.setUploadsInProgress(10);
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);
		assertEquals(-UploadSettings.HARD_MAX_UPLOADS.getValue()+10,pong.getQueueStatus());
        PingRanker.updateContext(blankRFD, pong);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertFalse(blankRFD.isBusy());
    }
		
    public void testQueueStatusSomeQueued() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
        UploadManagerStub uploadManager = (UploadManagerStub)injector.getInstance(UploadManager.class);
        HeadPing ping = new HeadPing(_havePartial);
        uploadManager.setNumQueuedUploads(5);
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);
		assertEquals(5,pong.getQueueStatus());
		PingRanker.updateContext(blankRFD, pong);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertFalse(blankRFD.isBusy());
    }
    
    public void testQueueStatusAllTakenNoneQueued() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
        UploadManagerStub uploadManager = (UploadManagerStub)injector.getInstance(UploadManager.class);
        HeadPing ping = new HeadPing(_havePartial);
        uploadManager.setUploadsInProgress(UploadSettings.HARD_MAX_UPLOADS.getValue());
        uploadManager.setNumQueuedUploads(0);
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);    
		assertEquals(0,pong.getQueueStatus());
		PingRanker.updateContext(blankRFD, pong);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertFalse(blankRFD.isBusy());
    }
    
    public void testQueueStatusBusy() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
        UploadManagerStub uploadManager = (UploadManagerStub)injector.getInstance(UploadManager.class);
        HeadPing ping = new HeadPing(_havePartial);
        uploadManager.setUploadsInProgress(UploadSettings.HARD_MAX_UPLOADS.getValue());
        uploadManager.setNumQueuedUploads(0);
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);  
		
		uploadManager.setNumQueuedUploads(UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
		pong = reparse(headPongFactory.create(ping));
		assertGreaterThanOrEquals(0x7F,pong.getQueueStatus());
	    PingRanker.updateContext(blankRFD, pong);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertTrue(blankRFD.isBusy());
	}
	
	/**
	 * tests handling of alternate locations.
	 */
	public void testAltLocsFitWithRanges() throws Exception {	
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
	    
        PrivilegedAccessor.setValue(HeadPongFactoryImpl.class, "PACKET_SIZE", 600);
        
		//add some big interval that fill most of the packet but not all
		_partial.setRangesByte(_rangesMedium.toBytes());
		
		//ping 1 should contain alternate locations. 
		
		//the second ping should be too big to contain all altlocs.
		HeadPing ping1 = new HeadPing(new GUID(GUID.makeGuid()),_haveFull,HeadPing.ALT_LOCS);
		HeadPing ping2 = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,
				HeadPing.ALT_LOCS | HeadPing.INTERVALS);
		
		HeadPong pong1 = headPongFactory.create(ping1);
		HeadPong pong2 = headPongFactory.create(ping2);
        clearStoredProxies();
        pong1 = reparse(pong1);
        pong2 = reparse(pong2);
		
		assertNull(pong1.getRanges());
		assertNotNull(pong2.getRanges());
		assertTrue(Arrays.equals(_rangesMedium.toBytes().ints,pong2.getRanges().toBytes().ints));
		assertGreaterThan(pong1.getPayload().length,pong2.getPayload().length);
		
		assertLessThan(pong1.getAltLocs().size(),pong2.getAltLocs().size());
		assertLessThan(600,pong2.getPayload().length);
    }
    
    public void testAltLocsDontFitBecauseOfTooManyRanges() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
        PrivilegedAccessor.setValue(HeadPongFactoryImpl.class, "PACKET_SIZE", 600);
        
		//now test if no locs will fit because of too many ranges
		_partial.setRangesByte(_rangesJustFit.toBytes());
		HeadPing ping2 = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,
				HeadPing.ALT_LOCS | HeadPing.INTERVALS);
		HeadPong pong2 = headPongFactory.create(ping2);
        clearStoredProxies();
        pong2 = reparse(pong2);
        
		
		assertNotNull(pong2.getRanges());
		assertEmpty(pong2.getAltLocs());
		
		//restore medium ranges to partial file
		_partial.setRangesByte(_rangesMedium.toBytes());
	}
	
	public void testPushAltLocsRequestedButNoLocs() throws Exception {		
		//try with a file that doesn't have push locs
		HeadPing ping1 = new HeadPing(new GUID(GUID.makeGuid()),_haveFull,HeadPing.PUSH_ALTLOCS);
		assertTrue(ping1.requestsPushLocs());
		HeadPong pong1 = headPongFactory.create(ping1);
        clearStoredProxies();
        pong1 = reparse(pong1);
		assertEmpty(pong1.getPushLocs());
    }
    
    public void testPushAltLocsReturned() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
		HeadPing ping1 = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,HeadPing.PUSH_ALTLOCS);
		assertTrue(ping1.requestsPushLocs());
		HeadPong pong1 = headPongFactory.create(ping1);
        clearStoredProxies();
        pong1 = reparse(pong1);
        // this used to be done in the factory and is now handled by PingRanker,
        // so simulate ping ranker here
        for (PushEndpoint pushEndpoint : pong1.getPushLocs()) {
            pushEndpoint.updateProxies(true);
        }

		assertNull(pong1.getRanges());
		assertEmpty(pong1.getAltLocs());
		assertNotEmpty(pong1.getPushLocs());
		
		RemoteFileDesc dummy = 
		    remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("www.limewire.org", 6346, false), 10, "asdf", 10, GUID.makeGuid(), 10, 2, true,
                null, UrnHelper.URN_SETS[1], false, "", -1);
		
		Set received = pong1.getAllLocsRFD(dummy, remoteFileDescFactory);
		assertEquals(1,received.size());
		RemoteFileDesc rfd = (RemoteFileDesc)received.toArray()[0]; 
		PushEndpoint point = (PushEndpoint) rfd.getAddress();
		assertEquals(pushCollectionPE,point);
		assertEquals(pushCollectionPE.getProxies() + " expected to have the same size as " + point.getProxies(), pushCollectionPE.getProxies().size(),point.getProxies().size());
        Set parsedProxies = new IpPortSet(point.getProxies());
		parsedProxies.retainAll(pushCollectionPE.getProxies());
		assertEquals(pushCollectionPE.getProxies().size(),parsedProxies.size());
    }
    
    public void testPushAltLocsWantOnlyFWT() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
        
		//now ask only for fwt push locs - nothing returned
		HeadPing ping1 = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,HeadPing.PUSH_ALTLOCS | HeadPing.FWT_PUSH_ALTLOCS);
		assertTrue(ping1.requestsFWTOnlyPushLocs());
		HeadPong pong1 = headPongFactory.create(ping1);
        clearStoredProxies();
        pong1 = reparse(pong1);
		assertEmpty(pong1.getPushLocs());
	}
    
    public void testTLSPushLocs() throws Exception {
        HeadPing ping = new HeadPing(new GUID(), _tlsURN, HeadPing.PUSH_ALTLOCS);
        assertTrue(ping.requestsPushLocs());
        HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);
        assertNull(pong.getRanges());
        assertEmpty(pong.getAltLocs());
        assertEquals(1, pong.getPushLocs().size());
        Set<? extends IpPort> proxies = pong.getPushLocs().iterator().next().getProxies();
        assertEquals(3, proxies.size());
        // the proxies: 2.3.4.5:5;3.4.5.6:7;4.5.6.7:8
        Set expectedProxies = new IpPortSet(new IpPortImpl("2.3.4.5:5"), new IpPortImpl("3.4.5.6:7"), new IpPortImpl("4.5.6.7:8"));
        expectedProxies.retainAll(proxies);
        assertEquals(3, expectedProxies.size());
        // We expect the 3.4.5.6 & 4.5.6.7 to be the TLS ones.
        int tls = 0;
        IpPort nonTLS = null;        
        for(IpPort ipp : proxies) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                tls++;
            else
                nonTLS = ipp;
        }
        assertEquals(2, tls);
        assertEquals("2.3.4.5", nonTLS.getAddress());
        
        RemoteFileDesc dummy = 
            remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("www.limewire.org", 6346, false), 10, "asdf", 10, GUID.makeGuid(), 10, 2, true,
                null, UrnHelper.URN_SETS[1], false, "", -1);
        
        Set<RemoteFileDesc> rfds = pong.getAllLocsRFD(dummy, remoteFileDescFactory);
        assertEquals(1, rfds.size());
        RemoteFileDesc rfd = rfds.iterator().next();
        assertEquals(tlsCollectionPE.getClientGUID(), rfd.getClientGUID());
        PushEndpoint pe = (PushEndpoint) rfd.getAddress();
        assertEquals(3, pe.getProxies().size());
        Set parsedProxies = new IpPortSet(pe.getProxies());
        parsedProxies.retainAll(expectedProxies);
        assertEquals(3, parsedProxies.size());
        tls = 0;
        nonTLS = null;        
        for(IpPort ipp : pe.getProxies()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                tls++;
            else
                nonTLS = ipp;
        }
        assertEquals(2, tls);
        assertEquals("2.3.4.5", nonTLS.getAddress());        
    }
    
    public void testTLSPushLocsWithOldHeadPing() throws Exception {
        HeadPing ping = new HeadPing(new GUID(), _tlsURN, HeadPing.PUSH_ALTLOCS);
        assertTrue(ping.requestsPushLocs());
        HeadPong pong = headPongFactory.create(reversion(ping, (short)1));
        clearStoredProxies();
        pong = reparse(pong);
        assertNull(pong.getRanges());
        assertEmpty(pong.getAltLocs());
        assertEquals(1, pong.getPushLocs().size());
        Set<? extends IpPort> proxies = pong.getPushLocs().iterator().next().getProxies();
        assertEquals(3, proxies.size());
        // the proxies: 2.3.4.5:5;3.4.5.6:7;4.5.6.7:8
        Set expectedProxies = new IpPortSet(new IpPortImpl("2.3.4.5:5"), new IpPortImpl("3.4.5.6:7"), new IpPortImpl("4.5.6.7:8"));
        expectedProxies.retainAll(proxies);
        assertEquals(3, expectedProxies.size());
        for(IpPort ipp : proxies) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                fail("tls capable!: " + ipp);
        }
        
        RemoteFileDesc dummy = 
            remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("www.limewire.org", 6346, false), 10, "asdf", 10, GUID.makeGuid(), 10, 2, true,
                null, UrnHelper.URN_SETS[1], false, "", -1);
        
        Set rfds = pong.getAllLocsRFD(dummy, remoteFileDescFactory);
        assertEquals(1, rfds.size());
        RemoteFileDesc rfd = (RemoteFileDesc)rfds.toArray()[0]; 
        assertEquals(tlsCollectionPE.getClientGUID(), rfd.getClientGUID());
        PushEndpoint pe = (PushEndpoint) rfd.getAddress();
        assertEquals(tlsCollectionPE.getClientGUID(), pe.getClientGUID());
        assertEquals(3, pe.getProxies().size());
        Set parsedProxies = new IpPortSet(pe.getProxies());
        parsedProxies.retainAll(expectedProxies);
        assertEquals(3, parsedProxies.size());
        for(IpPort ipp : pe.getProxies()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                fail("tls capable!: " + ipp);
        }        
    }
	
	public void testMixedLocs() throws Exception {
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(_havePartial)));
            will(returnValue(false));
        }});
	    
		HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,
				HeadPing.PUSH_ALTLOCS | HeadPing.ALT_LOCS);		
		HeadPong pong = headPongFactory.create(ping);
        clearStoredProxies();
        pong = reparse(pong);
		
		assertNotEmpty(pong.getAltLocs());
		assertNotEmpty(pong.getPushLocs());
		
		RemoteFileDesc rfd = remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("1.2.3.4", 1, false), 1, "filename", 1, null, 1, 1, false,
                null, URN.NO_URN_SET, false, "", 1);
		
		Set rfds = pong.getAllLocsRFD(rfd, remoteFileDescFactory);
		
		assertEquals(pong.getAltLocs().size() + pong.getPushLocs().size(), rfds.size());		
	}
    
    public void testForwardHeadPingDoesntChangeVersion() throws Exception {
        HeadPing ping = new HeadPing(_tlsURN);
        ping = reversion(ping, (short)(HeadPing.VERSION+5));
        assertEquals(HeadPing.VERSION+5, ping.getVersion()); // control.
        
        HeadPing forwarded = new HeadPing(ping);
        assertEquals(HeadPing.VERSION+5, forwarded.getVersion());
    }
	
	private HeadPong reparse(HeadPong original) throws Exception{
	    MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		original.write(baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return (HeadPong) messageFactory.read(bais, Network.TCP);
	}
    
    /** Constructs a new HeadPing exactly the same, but with a different version. */
    private HeadPing reversion(HeadPing ping, short version) throws Exception {
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ping.write(baos);
        byte[] out = baos.toByteArray();
        ByteUtils.short2leb(version, out, 29); // location of the version of a VM
        HeadPing p2 = (HeadPing)messageFactory.read(new ByteArrayInputStream(out), Network.TCP);
        assertEquals(version, p2.getVersion());
        return p2;
    }
	
	private void  createCollections() throws Exception{
	    AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
	    AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
		for(int i=0;i<10;i++ ) {
            AlternateLocation al = alternateLocationFactory.create("1.2.3."+i+":1234",_haveFull);
            altLocManager.add(al, null);
		}
        AlternateLocationCollection col = altLocManager.getDirect(_haveFull);
        assertEquals("failed to set test up", 10, col.getAltLocsSize());
        

        for(int i=0;i<10;i++ ) {
            AlternateLocation al = alternateLocationFactory.create("1.2.3."+i+":1234",_havePartial);
            altLocManager.add(al, null);
		}
        col = altLocManager.getDirect(_havePartial);
        assertEquals("failed to set test up", 10, col.getAltLocsSize());
        
        
        //add some push altlocs to the incomplete collection        
        GUID guid = new GUID(GUID.makeGuid());		
        PushAltLoc firewalled = (PushAltLoc)alternateLocationFactory.create(guid.toHexString()+";1.2.3.4:5",_havePartial);
        firewalled.updateProxies(true);
		pushCollectionPE = firewalled.getPushAddress();
		altLocManager.add(firewalled, null);
        col = altLocManager.getPushNoFWT(_havePartial);
        assertEquals(1, col.getAltLocsSize());
        
        GUID g = new GUID();
        PushAltLoc tls = (PushAltLoc)alternateLocationFactory.create(g.toHexString() + ";pptls=6;2.3.4.5:5;3.4.5.6:7;4.5.6.7:8", _tlsURN);
        tls.updateProxies(true);
        tlsCollectionPE = tls.getPushAddress();
        altLocManager.add(tls, null);
        col = altLocManager.getPushNoFWT(_tlsURN);
        assertEquals(1, col.getAltLocsSize());
	}
	
}
