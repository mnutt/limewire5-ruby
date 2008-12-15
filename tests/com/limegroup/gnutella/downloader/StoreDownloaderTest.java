package com.limegroup.gnutella.downloader;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.io.Connectable;
import org.limewire.io.LocalSocketAddressProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerImpl;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 *  Tests downloading from the store site
 */
public class StoreDownloaderTest extends LimeTestCase{

    private RemoteFileDescFactory remoteFileDescFactory;
    private DownloadManagerImpl downloadManager;
    private Injector injector; 
    
    public StoreDownloaderTest(String name) {
        super(name);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(StoreDownloaderTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
        doSetUp();
    }
    
    private void doSetUp(Module... modules) throws Exception {
        final LocalSocketAddressProviderStub localSocketAddressProviderStub = new LocalSocketAddressProviderStub();
        localSocketAddressProviderStub.setLocalAddressPrivate(false);
        List<Module> allModules = new LinkedList<Module>();
        allModules.add(new AbstractModule() {
           @Override
            protected void configure() {
               bind(ConnectionManager.class).to(ConnectionManagerStub.class);
               bind(MessageRouter.class).to(MessageRouterStub.class);
               bind(FileManager.class).to(FileManagerStub.class);
               bind(NetworkManager.class).to(NetworkManagerStub.class);
               bind(LocalSocketAddressProvider.class).toInstance(localSocketAddressProviderStub);
            } 
        });
        allModules.addAll(Arrays.asList(modules));
        injector = LimeTestUtils.createInjector(allModules.toArray(new Module[0]));
        remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
        ConnectionManagerStub connectionManager = (ConnectionManagerStub)injector.getInstance(ConnectionManager.class);
        connectionManager.setConnected(true);
        
        downloadManager = (DownloadManagerImpl)injector.getInstance(DownloadManager.class);       
        downloadManager.start();
        RequeryManager.NO_DELAY = false;
    }
    
    /**
     * Tests optimized overrides that return immediately since they're never used
     */
    public void testEmptyOverrides() throws Exception{
        URN urn = UrnHelper.URNS[0];
        URL url = new URL("http://test.com");
        RemoteFileDesc rfd = remoteFileDescFactory.createUrlRemoteFileDesc(url, "test.txt", urn, 10L);
        
        //create a valid download
        StoreDownloaderImpl downloader = (StoreDownloaderImpl) downloadManager.downloadFromStore(rfd, false, _storeDir, "test.txt" );
        
        assertNull(downloader.newRequery() );
        
        assertFalse(downloader.allowAddition(null));
        
        assertFalse(downloader.canSendRequeryNow());
        
        assertNull(downloader.getChatEnabledHost());
        
        assertFalse(downloader.hasChatEnabledHost());
        
        assertFalse(downloader.hasBrowseEnabledHost());
        
        assertEquals(0, downloader.getNumberOfAlternateLocations());
        
        assertEquals(0, downloader.getNumberOfInvalidAlternateLocations());
    }

    
    /**
     *  Tests creating a RemoteFileDesc from a url
     */
    public void testRFDCreation() throws Exception {
        URN urn = UrnHelper.URNS[0];
        URL url = new URL("http://test.com");
        
        // test invalid rfd
        try {
            remoteFileDescFactory.createUrlRemoteFileDesc(null, "", null, -1);
            fail("expected NPE");
        } catch(NullPointerException expected) {}
        
        RemoteFileDesc rfd = remoteFileDescFactory.createUrlRemoteFileDesc(url, "test.txt", urn, 10L);
        
        assertTrue(rfd.getUrns().contains(urn));
        
        assertEquals("", rfd.getUrlPath());
        
        Connectable connectable = (Connectable) rfd.getAddress();
        assertEquals("test.com", connectable.getAddress());
        assertEquals(80, connectable.getPort());
        assertFalse(connectable.isTLSCapable());
        
        assertEquals( "test.txt", rfd.getFileName());
        
        assertEquals( 10L, rfd.getSize() );
        
        
    }
    
    /**
     * Tests creating an invalid store download
     */
    public void testInvalidStoreDownloads() throws Exception {

        URN urn = UrnHelper.URNS[0];
        URL url = new URL("http://test.com");
        RemoteFileDesc rfd = remoteFileDescFactory.createUrlRemoteFileDesc(url, "test.txt", urn, 10L);
                
        //create a valid download
        downloadManager.downloadFromStore(rfd, false, _storeDir, "test.txt" );
        
        //test redownloading same file
        try {
            downloadManager.downloadFromStore(rfd, false, _storeDir, "test.txt" );
            fail("File already downloading");
        }
        catch(SaveLocationException e) {
            
        }
    }
    
    /**
     * Tests creating a valid store download
     */
    public void testValidStoreDownloads() throws Exception {
        
        URN urn = UrnHelper.URNS[0];
        URL url = new URL("http://test.com");
        RemoteFileDesc rfd = remoteFileDescFactory.createUrlRemoteFileDesc(url, "test.txt", urn, 10L);
        
        
        downloadManager.downloadFromStore(rfd, false, _storeDir, "test.txt" );
    }
}
