package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressProviderStub;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.dht.db.PushEndpointService;
import com.limegroup.gnutella.library.LibraryStubModule;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;

/**
 * Tests the magnet downloader class.
 * 
 * TODO: lots more tests to add here!!!
 */
public class MagnetDownloaderTest extends LimeTestCase {

	//private static final Log LOG = LogFactory.getLog(MagnetDownloaderTest.class);
    private DownloadManager downloadManager;
    private Mockery context;
    private PushEndpointService pushEndpointService;
	
    /**
     * Creates a new test instance.
     *
     * @param name the test name
     */
    public MagnetDownloaderTest(String name) {
        super(name);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(MagnetDownloaderTest.class);
    }
    
    
    @Override
    public void setUp() throws Exception {
        final LocalSocketAddressProviderStub localSocketAddressProviderStub = new LocalSocketAddressProviderStub();
        localSocketAddressProviderStub.setLocalAddressPrivate(false);
        
        context = new Mockery();
        pushEndpointService = context.mock(PushEndpointService.class);
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
           @Override
            protected void configure() {
               bind(MessageRouter.class).to(MessageRouterStub.class);
               bind(ConnectionManager.class).to(ConnectionManagerStub.class);
               bind(LocalSocketAddressProvider.class).toInstance(localSocketAddressProviderStub);
               bind(PushEndpointService.class).annotatedWith(Names.named("pushEndpointManager")).toInstance(pushEndpointService);
            } 
        }, new LibraryStubModule());
        
        ConnectionManagerStub connectionManager = (ConnectionManagerStub)injector.getInstance(ConnectionManager.class);
        connectionManager.setConnected(true);
        
        downloadManager = injector.getInstance(DownloadManager.class);
        downloadManager.start();
    }

    public void testInvalidMagnetDownloads() throws Exception {
    	// is invalid because we don't have a url
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			downloadManager.download(opts[0], false, null, null);
			fail("No illegal argument exception thrown");
		}
		catch (IllegalArgumentException iae) {
		} 
		
		// invalid: has empty kt
		opts = MagnetOptions.parseMagnet("magnet:?kt=&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			downloadManager.download(opts[0], false, null, null);
			fail("No illegal argument exception thrown");
		}
		catch (IllegalArgumentException iae) {
		} 
		
		// invalid: has only a display name
		opts = MagnetOptions.parseMagnet("magnet:?dn=me");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			downloadManager.download(opts[0], false, null, null);
			fail("No illegal argument exception thrown");
		}
		catch (IllegalArgumentException iae) {
		} 
    }
    
    public void testValidMagnetDownloads() throws Exception {
    	// valid: has a url and a sha1
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
				
		// valid: has a url and keyword topic
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
		
		// valid: has everything
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:KRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
	
		
		// downloadable: has kt and hash
		opts = MagnetOptions.parseMagnet("magnet:?kt=test2&xt=urn:sha1:MRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
		
		// downloadable: has dn and hash
		opts = MagnetOptions.parseMagnet("magnet:?dn=test3&xt=urn:sha1:TRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
		
		// downloadable hash only magnet
		opts = MagnetOptions.parseMagnet("magnet:?xt=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:YRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		assertTrue("Should be hash only", opts[0].isHashOnly());
		downloadManager.download(opts[0], true, null, null);
    }

}
