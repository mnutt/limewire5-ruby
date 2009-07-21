package org.limewire.core.impl.support;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.SearchResultList;
import org.limewire.net.SocketsManager;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UploadServices;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.downloader.DiskController;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.uploader.UploadSlotManager;

/**
 * Tests the delegate getter methods in LimeSessionInfo.  Ensure that they are connected and return the correct
 *  results from the correct classes.
 */
public class LimeSessionInfoTest extends BaseTestCase {

    public LimeSessionInfoTest(String name) {
        super(name);
    }

    public void testGetNumberOfPendingTimeouts() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final NIODispatcher dispatcher = context.mock(NIODispatcher.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(dispatcher, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(dispatcher).getNumPendingTimeouts();
            will(returnValue(3));
        }});
        
        assertEquals(3, sessionInfo.getNumberOfPendingTimeouts());
        
        context.assertIsSatisfied();
    }
    
    public void testDownloadManagerDelegates() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, downloadManager, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(downloadManager).getNumWaitingDownloads();
            will(returnValue(5));
            allowing(downloadManager).getNumIndividualDownloaders();
            will(returnValue(6));
        }});
        
        assertEquals(5, sessionInfo.getNumWaitingDownloads());
        assertEquals(6, sessionInfo.getNumIndividualDownloaders());
        
        context.assertIsSatisfied();
    }
    
    public void testGetCurrentUptime() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final Statistics statistics = context.mock(Statistics.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, statistics, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(statistics).getUptime();
            will(returnValue(25L));
        }});
        
        assertEquals(25, sessionInfo.getCurrentUptime());
        
        context.assertIsSatisfied();
    }
    
    public void testConnectionManagerDelagates() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final ConnectionManager connectionManager = context.mock(ConnectionManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, connectionManager, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(connectionManager).getNumInitializedClientConnections();
            will(returnValue(9));
            allowing(connectionManager).getNumClientSupernodeConnections();
            will(returnValue(10));
            allowing(connectionManager).getNumUltrapeerConnections();
            will(returnValue(20));
            allowing(connectionManager).getNumOldConnections();
            will(returnValue(21));
        }});
        
        assertEquals(9, sessionInfo.getNumUltrapeerToLeafConnections());
        assertEquals(10, sessionInfo.getNumLeafToUltrapeerConnections());
        assertEquals(20, sessionInfo.getNumUltrapeerToUltrapeerConnections());
        assertEquals(21, sessionInfo.getNumOldConnections());
        
        context.assertIsSatisfied();
    }
    
    public void testGetContentResponsesSize() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final ContentManager contentManager = context.mock(ContentManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, contentManager, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(contentManager).getCacheSize();
            will(returnValue(55));
        }});
        
        assertEquals(55, sessionInfo.getContentResponsesSize());
        
        context.assertIsSatisfied();
    }
    
    public void testGetCreationCacheSize() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final CreationTimeCache creationTimeCache = context.mock(CreationTimeCache.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, creationTimeCache, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(creationTimeCache).getSize();
            will(returnValue(59));
        }});
        
        assertEquals(59, sessionInfo.getCreationCacheSize());
        
        context.assertIsSatisfied();
    }
    
    public void testDiskControllerDelegates() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final DiskController diskController = context.mock(DiskController.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, diskController,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(diskController).getSizeOfByteCache();
            will(returnValue(77));
            allowing(diskController).getSizeOfVerifyingCache();
            will(returnValue(78));
            allowing(diskController).getNumPendingItems();
            will(returnValue(79));
        }});
        
        assertEquals(77, sessionInfo.getDiskControllerByteCacheSize());
        assertEquals(78, sessionInfo.getDiskControllerVerifyingCacheSize());
        assertEquals(79, sessionInfo.getDiskControllerQueueSize());
        
        context.assertIsSatisfied();
    }
    
    public void testGetByteBufferCacheSize() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final ByteBufferCache byteBufferCache = context.mock(ByteBufferCache.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, byteBufferCache, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(byteBufferCache).getHeapCacheSize();
            will(returnValue(99L));
        }});
        
        assertEquals(99, sessionInfo.getByteBufferCacheSize());
        
        context.assertIsSatisfied();
    }
    
    public void testGetNumberOfWaitingSockets() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                socketsManager, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(socketsManager).getNumWaitingSockets();
            will(returnValue(2399));
        }});
        
        assertEquals(2399, sessionInfo.getNumberOfWaitingSockets());
        
        context.assertIsSatisfied();
    }
    
    public void testUDPServiceDelegates() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final UDPService udpService = context.mock(UDPService.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, udpService, null, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(udpService).lastReportedPort();
            will(returnValue(742));
            allowing(udpService).receivedIpPong();
            will(returnValue(745));
            
            one(udpService).isGUESSCapable();
            will(returnValue(true));
            one(udpService).canReceiveSolicited();
            will(returnValue(true));
            one(udpService).canDoFWT();
            will(returnValue(true));
            one(udpService).portStable();
            will(returnValue(true));
            
            one(udpService).isGUESSCapable();
            will(returnValue(false));
            one(udpService).canReceiveSolicited();
            will(returnValue(false));
            one(udpService).canDoFWT();
            will(returnValue(false));
            one(udpService).portStable();
            will(returnValue(false));
        }});
        
        assertEquals(742, sessionInfo.lastReportedUdpPort());
        assertEquals(745, sessionInfo.receivedIpPong());
        
        assertTrue(sessionInfo.isGUESSCapable());
        assertTrue(sessionInfo.canReceiveSolicited());
        assertTrue(sessionInfo.canDoFWT());
        assertTrue(sessionInfo.isUdpPortStable());
        assertFalse(sessionInfo.isGUESSCapable());
        assertFalse(sessionInfo.canReceiveSolicited());
        assertFalse(sessionInfo.canDoFWT());
        assertFalse(sessionInfo.isUdpPortStable());
        
        context.assertIsSatisfied();
    }
    
    public void testAcceptorDelegates() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final Acceptor acceptor = context.mock(Acceptor.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, acceptor, null, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            one(acceptor).acceptedIncoming();
            will(returnValue(true));
            one(acceptor).acceptedIncoming();
            will(returnValue(false));
            one(acceptor).getPort(true);
            will(returnValue(878));
        }});
        
        assertTrue(sessionInfo.acceptedIncomingConnection());
        assertFalse(sessionInfo.acceptedIncomingConnection());
        assertEquals(878, sessionInfo.getPort());
        
        context.assertIsSatisfied();
    }
    
    public void testGetNumActiveDownloads() {
        Mockery context = new Mockery();
        
        final DownloadServices downloadServices = context.mock(DownloadServices.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, downloadServices, null, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(downloadServices).getNumActiveDownloads();
            will(returnValue(111));
        }});
        
        assertEquals(111, sessionInfo.getNumActiveDownloads());
        
        context.assertIsSatisfied();
    }
    
    public void testUploadServicesDeligates() {
        Mockery context = new Mockery();
        
        final UploadServices uploadServices = context.mock(UploadServices.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, uploadServices, null, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(uploadServices).getNumUploads();
            will(returnValue(531));
            allowing(uploadServices).getNumQueuedUploads();
            will(returnValue(532));
        }});
        
        assertEquals(531, sessionInfo.getNumActiveUploads());
        assertEquals(532, sessionInfo.getNumQueuedUploads());
 
        context.assertIsSatisfied();
    }
    
    public void testGetNumConnectionCheckerWorkarounds() {
        Mockery context = new Mockery();
        
        final ConnectionCheckerManager connectionCheckerManager = context.mock(ConnectionCheckerManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null, connectionCheckerManager, null, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(connectionCheckerManager).getNumWorkarounds();
            will(returnValue(1211));
        }});
        
        assertEquals(1211, sessionInfo.getNumConnectionCheckerWorkarounds());
        
        context.assertIsSatisfied();
    }
    
    public void testGetSelectStats() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final NIODispatcher nioDispatcher = context.mock(NIODispatcher.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, nioDispatcher, null, null, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(nioDispatcher).getSelectStats();
            will(returnValue(new long[] {Long.MAX_VALUE, 2, 3, 88}));
        }});
        
        assertEquals(new long[] {Long.MAX_VALUE, 2, 3, 88}, sessionInfo.getSelectStats());
        
        context.assertIsSatisfied();
    }
    
    public void testFileManagerDeligates() {
        final Mockery context = new Mockery();
        
        final Library library = context.mock(Library.class);
        final FileView gnutella = context.mock(FileView.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, library, null, null, null, null, null, gnutella);

        context.checking(new Expectations() {{
            allowing(gnutella).size();
            will(returnValue(1110));
            allowing(library).size();
            will(returnValue(1111));
        }});
        
        assertEquals(1110, sessionInfo.getSharedFileListSize());
        assertEquals(1111, sessionInfo.getManagedFileListSize());
 
        context.assertIsSatisfied();
    }
    
    public void testGetAllFriendsFileListSize() {
        final Mockery context = new Mockery();
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, remoteLibraryManager, null);

        context.checking(new Expectations() {{
            SearchResultList allFriendsList = context.mock(SearchResultList.class);            
            allowing(remoteLibraryManager).getAllFriendsFileList();
            will(returnValue(allFriendsList));
            allowing(allFriendsList).size();
            will(returnValue(222));
        }});
        
        assertEquals(222, sessionInfo.getAllFriendsFileListSize());
        
        context.assertIsSatisfied();
    }
    
    public void testGetSimppVersion() {
        Mockery context = new Mockery();
        
        final SimppManager simppManager = context.mock(SimppManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, simppManager, null, null, null, null, null);

        context.checking(new Expectations() {{
            allowing(simppManager).getVersion();
            will(returnValue(70));
        }});
        
        assertEquals(70, sessionInfo.getSimppVersion());
        
        context.assertIsSatisfied();
    }
    
    public void testGetUploadSlotManagerInfo() {
        Mockery context = new Mockery();
        
        final UploadSlotManager uploadSlotManager = context.mock(UploadSlotManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, uploadSlotManager, null, null, null, null);

        context.checking(new Expectations() {{
            // None
        }});
        
        assertNotNull(sessionInfo.getUploadSlotManagerInfo());
        assertNotEquals("", sessionInfo.getUploadSlotManagerInfo());
        
        context.assertIsSatisfied();
    }
    
    
    public void testConnectionServicesDeligates() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, connectionServices, null, null, null);

        context.checking(new Expectations() {{
            one(connectionServices).isConnected();
            will(returnValue(true));
            one(connectionServices).isConnected();
            will(returnValue(false));
            
            one(connectionServices).isShieldedLeaf();
            will(returnValue(true));
            one(connectionServices).isShieldedLeaf();
            will(returnValue(false));
            
            one(connectionServices).isSupernode();
            will(returnValue(true));
            one(connectionServices).isSupernode();
            will(returnValue(false));
        }});
        
        assertTrue(sessionInfo.isConnected());
        assertFalse(sessionInfo.isConnected());
        
        assertTrue(sessionInfo.isShieldedLeaf());
        assertFalse(sessionInfo.isShieldedLeaf());
        
        assertTrue(sessionInfo.isSupernode());
        assertFalse(sessionInfo.isSupernode());
        
        context.assertIsSatisfied();
    }   
    
    public void testIsLifecycleLoaded() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final LifecycleManager lifecycleManager = context.mock(LifecycleManager.class);
        
        LimeSessionInfo sessionInfo = new LimeSessionInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, lifecycleManager, null, null);

        context.checking(new Expectations() {{
            one(lifecycleManager).isLoaded();
            will(returnValue(true));
            one(lifecycleManager).isLoaded();
            will(returnValue(false));
        }});
        
        assertTrue(sessionInfo.isLifecycleLoaded());
        assertFalse(sessionInfo.isLifecycleLoaded());
        
        context.assertIsSatisfied();
    }
    
}
