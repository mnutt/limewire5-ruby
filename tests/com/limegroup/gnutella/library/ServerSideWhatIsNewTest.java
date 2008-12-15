package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.listener.EventListener;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.ClientSideTestCase;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.downloader.TestFile;
import com.limegroup.gnutella.downloader.TestUploader;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Tests that What is new support is fully functional.  We use a leaf here - we
 * assume that an Ultrapeer will be equally functional.
 * 
 */

public class ServerSideWhatIsNewTest 
    extends ClientSideTestCase {
    private final int PORT=6669;
    private final int TIMEOUT=1000;

    private File berkeley = null;
    private File susheel = null;
    private File tempFile1 = null;
    private File tempFile2 = null;
    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;
    private CapabilitiesVMFactory capabilitiesVMFactory;
    private FileManager fileManager;
    private CreationTimeCache creationTimeCache;
    private QueryRequestFactory queryRequestFactory;
    private Injector injector;
    private DownloadServices downloadServices;
    
    public ServerSideWhatIsNewTest(String name) {
        super(name);
    }
    
    @Override
    public int getNumberOfPeers() {
        return 1;
    }

    public static Test suite() {
        return buildTestSuite(ServerSideWhatIsNewTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setSettings() throws Exception {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        NetworkSettings.PORT.setValue(PORT);
        ConnectionSettings.DO_NOT_BOOTSTRAP.setValue(true);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        //  Required so that the "swarmDownloadCatchesEarlyCreationTest" actually works  =)
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.T3_SPEED_INT);
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }        
    
    @Override
    public void setUp() throws Exception  {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        super.setUp(injector);
        
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        capabilitiesVMFactory = injector.getInstance(CapabilitiesVMFactory.class);
        fileManager = injector.getInstance(FileManager.class);
        creationTimeCache = injector.getInstance(CreationTimeCache.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        downloadServices = injector.getInstance(DownloadServices.class);
        
        fileManager.getGnutellaFileList().remove(berkeleyFD);
        fileManager.getGnutellaFileList().remove(susheelFD);
        
        berkeley = new File(_scratchDir, berkeleyFD.getFileName());
        CommonUtils.copyFile(berkeleyFD.getFile(), berkeley);
        
        susheel = new File(_scratchDir, susheelFD.getFileName());
        CommonUtils.copyFile(susheelFD.getFile(), susheel);
        
        // Make sure mod times of each file are different.
        berkeley.setLastModified(susheel.lastModified()-1000);
        
        berkeleyFD = fileManager.getGnutellaFileList().add(berkeley).get(1, TimeUnit.SECONDS);
        susheelFD = fileManager.getGnutellaFileList().add(susheel).get(1, TimeUnit.SECONDS);
        assertNotNull(berkeleyFD);
        assertNotNull(susheelFD);
        
        exchangeCapabilitiesMessage();
    }
    
    @Override
    public boolean shouldRespondToPing() {
        return false;
    }

    public void exchangeCapabilitiesMessage() throws Exception {
        //testUP = connect(rs, 6355, true);
        BlockingConnection connection = testUP[0];

        // send a MessagesSupportedMessage and capabilities VM
        connection.send(messagesSupportedVendorMessage);
        connection.send(capabilitiesVMFactory.getCapabilitiesVM());
        connection.flush();

        Thread.sleep(100);
        
        // we expect to get a CVM back
        Message m = null;
        do {
            m = connection.receive(TIMEOUT);
        } while (!(m instanceof CapabilitiesVM)) ;
        assertTrue(((CapabilitiesVM)m).supportsWhatIsNew());

        // client side seems to follow the setup process A-OK
    }

    // test that the CreationTimeCache is as expected
    public void testCreationTimeCacheInitialState() throws Exception {
        // we should be sharing two files - two text files.
        assertEquals(2, fileManager.getGnutellaFileList().size());

        FileManager fm = fileManager;
        URN berkeleyURN = fm.getGnutellaFileList().getFileDesc(berkeley).getSHA1Urn();
        URN susheelURN = fm.getGnutellaFileList().getFileDesc(susheel).getSHA1Urn();

        Map urnToLong =  creationTimeCache.getUrnToTime();
        assertEquals(2, urnToLong.size());
        assertNotNull(""+urnToLong, urnToLong.get(berkeleyURN));
        assertNotNull(""+urnToLong, urnToLong.get(susheelURN));

        Map longToUrns = creationTimeCache.getTimeToUrn();
        if (longToUrns.size() == 1) {
            Iterator iter = longToUrns.entrySet().iterator();
            Set urnSet = (Set)((Map.Entry)iter.next()).getValue();
            assertTrue(urnSet.contains(berkeleyURN));
            assertTrue(urnSet.contains(susheelURN));
            assertEquals(2, urnSet.size());
        }
        else if (longToUrns.size() == 2) {
            Iterator iter = longToUrns.entrySet().iterator();
            Set urnSet = (Set)((Map.Entry)iter.next()).getValue();
            assertTrue(
                    ( urnSet.contains(berkeleyURN) && 
                            !urnSet.contains(susheelURN) )
                            ||
                            ( !urnSet.contains(berkeleyURN) && 
                                    urnSet.contains(susheelURN) )
            );
            assertEquals(1, urnSet.size());
            urnSet = (Set)((Map.Entry)iter.next()).getValue();
            assertTrue(
                    ( urnSet.contains(berkeleyURN) && 
                            !urnSet.contains(susheelURN) )
                            ||
                            ( !urnSet.contains(berkeleyURN) && 
                                    urnSet.contains(susheelURN) )
            );
            assertEquals(1, urnSet.size());
        }
        else assertTrue("Bad Cache!", false);
    }

    // make sure that a what is new query is answered correctly
    public void testWhatIsNewQueryBasic() throws Exception {
        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        whatIsNewQuery.hop();
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(2, reply.getResultCount());
        Iterator iter = reply.getResults();
        Response currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        assertFalse(iter.hasNext());
    }

    // make sure that a what is new query meta query is answered correctly
    public void testWhatIsNewQueryMeta() throws Exception {

        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        {
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW, false, 0 | QueryRequest.AUDIO_MASK);
        whatIsNewQuery.hop();
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(2000);

        QueryReply reply = 
             BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNull(reply);
        }

        {
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW, false, 0 | QueryRequest.DOC_MASK);
        whatIsNewQuery.hop();
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(2, reply.getResultCount());
        Iterator iter = reply.getResults();
        Response currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        assertFalse(iter.hasNext());
        }
    }


    // test that the creation time cache handles the additional sharing of files
    // fine
    public void testAddSharedFiles() throws Exception {
        FileManager fm = fileManager;
        URN berkeleyURN = fm.getGnutellaFileList().getFileDesc(berkeley).getSHA1Urn();
        URN susheelURN = fm.getGnutellaFileList().getFileDesc(susheel).getSHA1Urn();

        // we start with one or two timestamps
        Map longToUrns = creationTimeCache.getTimeToUrn();
        int startTimeStamps = longToUrns.size();
        assertTrue(""+longToUrns, startTimeStamps == 1 || startTimeStamps == 2);
        
        setupAndAddTempFiles();

        URN tempFile1URN = fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn();
        URN tempFile2URN = fm.getGnutellaFileList().getFileDesc(tempFile2).getSHA1Urn();

        Map urnToLong = creationTimeCache.getUrnToTime();
        assertEquals(4, urnToLong.size());
        assertNotNull(""+urnToLong, urnToLong.get(berkeleyURN));
        assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
        assertNotNull(""+urnToLong, urnToLong.get(tempFile1URN));
        assertNotNull(""+urnToLong, urnToLong.get(tempFile2URN));
        
        // we end with 2, 3 or 4 timestamps, depending on fs performance
        assertGreaterThan(startTimeStamps, longToUrns.size());
        assertLessThan(5, longToUrns.size());
    }

    // test that after the sharing of additional files, the what is new query
    // results in something else
    public void testWhatIsNewQueryNewFiles() throws Exception {
        setupAndAddTempFiles();
        
        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(3, reply.getResultCount());
        boolean gotTempFile1 = false, gotTempFile2 = false;
        
        Iterator iter = reply.getResults();
        while (iter.hasNext()) {
            Response currResp = (Response) iter.next();
            if (currResp.getName().equals(tempFile1.getName()))
                gotTempFile1 = true;
            if (currResp.getName().equals(tempFile2.getName()))
                gotTempFile2 = true;
        }
        assertTrue("file 1? " + gotTempFile1 + ", file 2? " +
                   gotTempFile2, gotTempFile1 && gotTempFile2);
    }

    
    // test that the fileChanged method of FM does the right thing.  this isn't
    // a total black box test, but hacking up the changing of ID3 data isn't
    // worth the cost.  this test should be good enough....
    public void testFileChanged() throws Exception {
        setupAndAddTempFiles();
        
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;
        URN tempFile1URN = fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn();
        Long cTime = ctCache.getCreationTime(tempFile1URN);

        FileWriter fw = new FileWriter(tempFile1, true);
        fw.write("extra");
        fw.close();
        tempFile1.setLastModified(tempFile1.lastModified()+3000);
        
        final FileDesc beforeChanged = fm.getGnutellaFileList().getFileDesc(tempFile1);
        assertNotNull(beforeChanged);
        
        final CountDownLatch fileChangedLatch = new CountDownLatch(1);
        fm.getGnutellaFileList().addFileListListener(new EventListener<FileListChangedEvent>() {
            public void handleEvent(FileListChangedEvent evt) {
                if (evt.getType() != FileListChangedEvent.Type.CHANGED)
                    return;
                if (evt.getOldValue() == beforeChanged)
                    fileChangedLatch.countDown();
            }
        });
        fm.getManagedFileList().fileChanged(tempFile1, LimeXMLDocument.EMPTY_LIST);
        assertTrue(fileChangedLatch.await(5, TimeUnit.SECONDS));
        FileDesc afterChanged = fm.getGnutellaFileList().getFileDesc(tempFile1);
        assertNotNull(afterChanged);
        assertNotSame(beforeChanged, afterChanged);
        
        assertNotNull(fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn());
        assertNotEquals(tempFile1URN, fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn());
        assertEquals(ctCache.getCreationTime(fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn()),
                     cTime);

        // now just send another What Is New query and make sure everything
        // is kosher - probably overkill but whatever....
        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(3, reply.getResultCount());
        boolean gotTempFile1 = false, gotTempFile2 = false;
        
        Iterator iter = reply.getResults();
        while (iter.hasNext()) {
            Response currResp = (Response) iter.next();
            if (currResp.getName().equals(tempFile1.getName()))
                gotTempFile1 = true;
            if (currResp.getName().equals(tempFile2.getName()))
                gotTempFile2 = true;
        }
        assertTrue("file 1? " + gotTempFile1 + ", file 2? " +
                   gotTempFile2, gotTempFile1 && gotTempFile2);
    }


    // test that the fileChanged method of FM does the right thing when you 
    // change the file to a existing URN.
    public void testFileChangedToExistingURN() throws Exception {
        setupAndAddTempFiles();
        
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;
        URN tempFile1URN = fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn();
        // we are changing tempFile1 to become tempFile2 - but since we
        // call fileChanged(), then the common URN should get tempFile1's
        // cTime
        Long cTime = ctCache.getCreationTime(tempFile1URN);
        byte[] contents = FileUtils.readFileFully(tempFile2);
        FileOutputStream fos = new FileOutputStream(tempFile1, false);
        fos.write(contents);
        fos.close();
        tempFile1.setLastModified(tempFile1.lastModified()+3000);
        FileDesc beforeChanged = fm.getGnutellaFileList().getFileDesc(tempFile1);
        assertNotNull(beforeChanged);
        
        final CountDownLatch latch = new CountDownLatch(1);
        fm.getGnutellaFileList().addFileListListener(new EventListener<FileListChangedEvent>() {
            public void handleEvent(FileListChangedEvent evt) {
                if(FileListChangedEvent.Type.CHANGED == evt.getType())
                    latch.countDown();
            }
        });
        fm.getManagedFileList().fileChanged(tempFile1, LimeXMLDocument.EMPTY_LIST);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        FileDesc afterChanged = fm.getGnutellaFileList().getFileDesc(tempFile1);
        assertNotNull(afterChanged);
        assertNotSame(beforeChanged, afterChanged);
        assertNotNull(fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn());
        assertNotEquals(tempFile1URN, fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn());
        assertEquals(fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn(), fm.getGnutellaFileList().getFileDesc(tempFile2).getSHA1Urn());
        assertEquals(ctCache.getCreationTime(fm.getGnutellaFileList().getFileDesc(tempFile1).getSHA1Urn()),
                     cTime);
        
        // now just send another What Is New query and make sure everything
        // is kosher - probbably overkill but whatever....
        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
           BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals("results: " + reply.getResultsAsList(), 3, reply.getResultCount());
        boolean gotTempFile1 = false, gotTempFile2 = false;
        
        Iterator iter = reply.getResults();
        while (iter.hasNext()) {
            Response currResp = (Response) iter.next();
            if (currResp.getName().equals(tempFile1.getName()))
                gotTempFile1 = true;
            if (currResp.getName().equals(tempFile2.getName()))
                gotTempFile2 = true;
        }
        assertTrue("file 1? " + gotTempFile1 + ", file 2? " +
                   gotTempFile2, !gotTempFile1 && gotTempFile2);
    }

    // test that the FileManager.removeFileIfShared method works    
    public void testRemoveSharedFile() throws Exception {
        setupAndAddTempFiles();
        
        FileManager fm = fileManager;
        assertEquals(4,fm.getGnutellaFileList().size());
        
        
        // 4 shared files
        assertEquals(4,fm.getGnutellaFileList().size());
        
        // 4 different urns 
        {
            Map urnToLong = creationTimeCache.getUrnToTime();
            assertEquals(urnToLong.toString(), 4, urnToLong.size());
        }
        
        Map longToUrns = creationTimeCache.getTimeToUrn();
        int startTimeStamps = longToUrns.size();
        // 2,3 or 4 different creation times 
        {
            assertGreaterThan(1,longToUrns.size());
            assertLessThan(5, longToUrns.size());
        }
        
        // tempFile1 and 2 have the same URN
        fm.getGnutellaFileList().remove(tempFile1);
        {
            Map urnToLong = creationTimeCache.getUrnToTime();  
            assertEquals(3, urnToLong.size());
        }
        // they may or may not have the same timestamp
        assertLessThanOrEquals(startTimeStamps, longToUrns.size());
        
        // tempFile2 should result in a removal of an URN
        // as well as a timestamp
        fm.getGnutellaFileList().remove(tempFile2);

        {
            Map urnToLong = creationTimeCache.getUrnToTime();
            assertEquals(2, urnToLong.size());
        }
        assertLessThan(startTimeStamps, longToUrns.size());
    }


    // manually delete a file, make sure it isn't shared and that the CTC has
    // the correct sizes, etc...
    public void testManualFileDeleteLoadSettings() throws Exception {
        setupAndAddTempFiles();
        
        FileManager fm = fileManager;

        tempFile1.delete(); tempFile1 = null;
        tempFile2.delete(); tempFile2 = null;
        berkeley.delete(); berkeley = null;

        ((ManagedFileListImpl)fm.getManagedFileList()).loadManagedFiles();
        Thread.sleep(2000);
        assertEquals("num shared files", 1, fileManager.getGnutellaFileList().size());

        URN susheelURN = fm.getGnutellaFileList().getFileDesc(susheel).getSHA1Urn();
        {
            Map urnToLong = creationTimeCache.getUrnToTime(); 
            assertEquals(""+urnToLong, 1, urnToLong.size());
            assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
        }
        {
            Map longToUrns = creationTimeCache.getTimeToUrn();
            assertEquals(""+longToUrns, 1, longToUrns.size());
        }
    }
    

    // download a file and make sure the creation time given back is stored...
    public void testDownloadCapturesCreationTime() throws Exception {
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;
        Map longToUrns = ctCache.getTimeToUrn();
        List<FileDesc> fds = CollectionUtils.listOf(fileManager.getGnutellaFileList());
        for (FileDesc fd : fds) {
            fileManager.getGnutellaFileList().remove(fd.getFile());
        }
        longToUrns = ctCache.getTimeToUrn();
        final int UPLOADER_PORT = 10000;
        byte[] guid = GUID.makeGuid();
        TestUploader uploader = injector.getInstance(TestUploader.class);
        uploader.start("whatever.txt", UPLOADER_PORT, false);
        Long cTime = new Long(2);
        uploader.setCreationTime(cTime);
        Set<URN> urns = new HashSet<URN>();
        urns.add(TestFile.hash());
        RemoteFileDesc rfd = injector.getInstance(RemoteFileDescFactory.class)
                .createRemoteFileDesc(new ConnectableImpl("127.0.0.1", UPLOADER_PORT, false), 1, "whatever.txt", TestFile.length(), guid, 1, false, 3,
                        false, null, urns, false, "LIME", -1);
        
        int sharedBefore = fileManager.getGnutellaFileList().size();
        final CountDownLatch shareLatch = new CountDownLatch(1);
        fileManager.getGnutellaFileList().addFileListListener(new EventListener<FileListChangedEvent>() {
            public void handleEvent(FileListChangedEvent evt) {
                if (evt.getType() == FileListChangedEvent.Type.ADDED)
                    shareLatch.countDown();
            }
        });
        final CountDownLatch downloadedLatch = new CountDownLatch(1);
        final Downloader downloader = downloadServices.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        downloader.addListener(new EventListener<DownloadStatusEvent>() {
            @Override
            public void handleEvent(DownloadStatusEvent event) {
                if(event.getSource().isCompleted()) {
                    downloadedLatch.countDown();
                }
            }
        });
        assertTrue("state: " + downloader.getState(), downloadedLatch.await(30,TimeUnit.SECONDS));
        
        assertTrue("didn't share!", shareLatch.await(5, TimeUnit.SECONDS));
        assertEquals( sharedBefore + 1, fileManager.getGnutellaFileList().size());

        File newFile = new File(_savedDir, "whatever.txt");
        assertTrue(newFile.getAbsolutePath()+" didn't exist", newFile.exists());
        URN newFileURN = fm.getGnutellaFileList().getFileDesc(newFile).getSHA1Urn();
        assertEquals(TestFile.hash(), newFileURN);
        assertEquals(newFileURN.toString(), cTime, ctCache.getCreationTime(newFileURN));

        Map urnToLong = ctCache.getUrnToTime();
        assertEquals(""+urnToLong, sharedBefore + 1, urnToLong.size());
        longToUrns = ctCache.getTimeToUrn();
        assertEquals(""+longToUrns+"  vs "+urnToLong, sharedBefore + 1, longToUrns.size());

    }


    // download a file and make sure the creation time given back is stored...
    public void testSwarmDownloadCapturesOlderCreationTime() throws Exception {
        
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;
        
        List<FileDesc> fds = CollectionUtils.listOf(fileManager.getGnutellaFileList());
        for (FileDesc fd : fds) {
            fileManager.getGnutellaFileList().remove(fd.getFile());
        }

        final int UPLOADER_PORT = 20000;
        byte[] guid = GUID.makeGuid();
        TestUploader uploader[] = new TestUploader[4];
        Long cTime[] = new Long[uploader.length];
        RemoteFileDesc rfds[] = new RemoteFileDesc[uploader.length];
        for (int i = 0; i < uploader.length; i++) {
            uploader[i] = injector.getInstance(TestUploader.class);
            uploader[i].start("anita.txt", UPLOADER_PORT+i, false);
            uploader[i].setRate(50);
            cTime[i] = new Long(5+i);
            uploader[i].setCreationTime(cTime[i]);
            Set<URN> urns = new HashSet<URN>();
            urns.add(TestFile.hash());
            rfds[i] = injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(new ConnectableImpl("127.0.0.1", UPLOADER_PORT+i, false), 1, "anita.txt",
                    TestFile.length(), guid, 1, false, 3, false, null, urns, false, "LIME", -1);
        }


        final CountDownLatch incompleteLatch = new CountDownLatch(1);
        final CountDownLatch shareLatch = new CountDownLatch(1);
        
        fileManager.getIncompleteFileList().addFileListListener(new EventListener<FileListChangedEvent>() {
            @Override
            public void handleEvent(FileListChangedEvent event) {
                if(event.getType() == FileListChangedEvent.Type.ADDED) {
                    incompleteLatch.countDown();
                }
            }
        });
        
        
        fileManager.getGnutellaFileList().addFileListListener(new EventListener<FileListChangedEvent>() {
            public void handleEvent(FileListChangedEvent evt) {
                if (evt.getType() == FileListChangedEvent.Type.ADDED) {
                    shareLatch.countDown();
                }
            }
        });
        

        final CountDownLatch downloadLatch = new CountDownLatch(1);
        Downloader downloader = downloadServices.download(rfds, false, new GUID(guid));
        downloader.addListener(new EventListener<DownloadStatusEvent>() {
            @Override
            public void handleEvent(DownloadStatusEvent event) {
                if(event.getSource().isCompleted()) {
                    downloadLatch.countDown();
                }
            }
        });
        
        assertTrue("incomplete never added", incompleteLatch.await(5, TimeUnit.SECONDS));
        Thread.sleep(2000);
        // make sure that the partial file has a creation time - by now one of
        // the downloaders must have got X-Create-Time
        assertNotNull(ctCache.getCreationTime(TestFile.hash()));
        
        assertTrue("download: " + downloader.getState(), downloadLatch.await(30, TimeUnit.SECONDS));
        assertTrue("never shared", shareLatch.await(5, TimeUnit.SECONDS));        
        assertEquals(1, fileManager.getGnutellaFileList().size());

        {
            Map urnToLong = ctCache.getUrnToTime(); 
            assertEquals(""+urnToLong, 1, urnToLong.size());
        }
        {
            Map longToUrns = ctCache.getTimeToUrn();
            assertEquals(""+longToUrns, 1, longToUrns.size());
        }

        File newFile = new File(_savedDir, "anita.txt");
        assertTrue(newFile.exists());
        URN newFileURN = fm.getGnutellaFileList().getFileDesc(newFile).getSHA1Urn();
        assertEquals(cTime[0], ctCache.getCreationTime(newFileURN));
    }

    public void testInstallersNotSharedInCache() throws Exception {
        //  Make sure that the modified times are not the same
        susheel.setLastModified(123456);
        berkeley.setLastModified(123457);
        
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;

        File winInstaller = TestUtils.getResourceFile("com/limegroup/gnutella/Backend.java");
        File linInstaller = TestUtils.getResourceFile("com/limegroup/gnutella/UrnSetTest.java");
        File osxInstaller = TestUtils.getResourceFile("com/limegroup/gnutella/UrnTest.java");

        //  Gotta make use of the force-share folder for this test
        if( LibraryUtils.PROGRAM_SHARE.exists() ) {
            File [] toDelete = LibraryUtils.PROGRAM_SHARE.listFiles();
            for (int j = 0; j < toDelete.length; j++) {
                toDelete[j].delete();
            }
        } else {
            LibraryUtils.PROGRAM_SHARE.mkdir();
        }

        File winDst = new File(LibraryUtils.PROGRAM_SHARE, "LimeWireWin3.69.0010.exe");
        File linDst = new File(LibraryUtils.PROGRAM_SHARE, "LimeWireLinux.bin");
        File osxDst = new File(LibraryUtils.PROGRAM_SHARE, "LimeWireOSX.dmg");
        
        assertTrue(FileUtils.copy(winInstaller, winDst));
        assertTrue(FileUtils.copy(linInstaller, linDst));
        assertTrue(FileUtils.copy(osxInstaller, osxDst));
        
        winDst.deleteOnExit();
        linDst.deleteOnExit();
        osxDst.deleteOnExit();
        
        try {
            assertNotNull(fileManager.getGnutellaFileList().add(winDst).get(5, TimeUnit.SECONDS));
            assertNotNull(fileManager.getGnutellaFileList().add(linDst).get(5, TimeUnit.SECONDS));
            assertNotNull(fileManager.getGnutellaFileList().add(osxDst).get(5, TimeUnit.SECONDS));
            
            assertEquals(5, fileManager.getGnutellaFileList().size());
    
            {
                Map urnToLong = creationTimeCache.getUrnToTime();
                assertEquals(2, urnToLong.size());
            }
            {
                Map longToUrns = creationTimeCache.getTimeToUrn();
                assertEquals(2, longToUrns.size());
            }
    
            // make sure the installer urns are not in the cache
            {
                assertTrue(winInstaller.exists());
                assertNull(fm.getGnutellaFileList().getFileDesc(winInstaller));
            }
            {
                assertTrue(winInstaller.exists());
                assertNull(fm.getGnutellaFileList().getFileDesc(linInstaller));
            }
            {
                assertTrue(winInstaller.exists());
                assertNull(fm.getGnutellaFileList().getFileDesc(osxInstaller));
            }
            // make sure berkeley and susheel are in the cache.
            {
                assertTrue(berkeley.exists());
                assertNotNull(ctCache.getCreationTime(fm.getGnutellaFileList().getFileDesc(berkeley).getSHA1Urn()));
            }
            {
                assertTrue(susheel.exists());
                assertNotNull(ctCache.getCreationTime(fm.getGnutellaFileList().getFileDesc(susheel).getSHA1Urn()));
            }
        
        } finally {        
            File [] toDelete = LibraryUtils.PROGRAM_SHARE.listFiles();
            for (int j = 0; j < toDelete.length; j++) {
                toDelete[j].delete();
            }
            LibraryUtils.PROGRAM_SHARE.delete();

        }
    }
    
    private void setupAndAddTempFiles() throws Exception {
        tempFile1 = new File(_scratchDir, "tempFile1.txt");
        tempFile2 = new File(_scratchDir, "tempFile2.txt");        

        Map urnToLong = creationTimeCache.getUrnToTime();
        long previousTime = (Long)urnToLong.get(berkeleyFD.getSHA1Urn());
        previousTime = Math.max(previousTime, (Long)urnToLong.get(susheelFD.getSHA1Urn()));
        
        FileWriter writer = new FileWriter(tempFile1);
        writer.write("temp1");
        writer.close();
        
        writer = new FileWriter(tempFile2);
        writer.write("temp2");
        writer.close();
        
        tempFile1.setLastModified(previousTime+1000);
        assertNotEquals("couldn't set up test",tempFile1.lastModified(), previousTime);
        tempFile2.setLastModified(previousTime+2000);
        assertNotEquals("couldn't set up test",tempFile2.lastModified(), previousTime);
        
        // now move them to the share dir
        assertNotNull(fileManager.getGnutellaFileList().add(tempFile1).get(1, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(tempFile2).get(1, TimeUnit.SECONDS));
        assertEquals("Files were not loaded by filemanager", 4, fileManager.getGnutellaFileList().size());
    }

}
