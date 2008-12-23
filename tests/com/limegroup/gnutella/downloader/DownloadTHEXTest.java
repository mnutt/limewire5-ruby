package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.List;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.tigertree.HashTree;

public class DownloadTHEXTest extends DownloadTestCase {
    private static final Log LOG = LogFactory.getLog(DownloadTHEXTest.class);
    
    public DownloadTHEXTest(String name) {
        super(name);
    }

    public static Test suite() { 
        return buildTestSuite(DownloadTHEXTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * tests http11 downloads and the gray area allocation.
     */
    public void testTHEXDownload11() throws Exception {
        LOG.info("-Testing chunk allocation in a thex download...");
        
        
        final RemoteFileDesc rfd=newRFDWithURN(PORTS[0], false);
        final IncompleteFileManager ifm=downloadManager.getIncompleteFileManager();
        RemoteFileDesc[] rfds = {rfd};
        
        HTTP11Listener grayVerifier = new HTTP11Listener() {
            //private int requestNo;
            //public void requestHandled(){}
            //public void thexRequestStarted() {}

            /** The only lease that is DEFAULT_CHUNK_SIZE large */
            private Range firstLease = null;
            
            public void requestHandled(){}
            public void thexRequestStarted() {}
            public void thexRequestHandled() {}
            
            // checks whether we request chunks at the proper offset, etc.
            public void requestStarted(TestUploader uploader) {
                long fileSize = 0;
                
                Range i = null;
                try {
                    IntervalSet leased = null;
                    File incomplete = null;
                    incomplete = ifm.getFile(rfd);
                    assertNotNull(incomplete);
                    VerifyingFile vf = ifm.getEntry(incomplete);
                    fileSize = ((Long)PrivilegedAccessor.getValue(vf, "completedSize")
                                  ).longValue();
                    assertNotNull(vf);
                    leased = (IntervalSet)
                                    PrivilegedAccessor.getValue(vf,"leasedBlocks");
                    assertNotNull(leased);
                    List l = leased.getAllIntervalsAsList();
                    assertEquals(1,l.size());
                    i = (Range)l.get(0);
                } catch (Throwable bad) {
                  fail(bad);
                }
                
                assert i != null;
                
                if (firstLease == null) {
                    // first request, we should have the chunk aligned to
                    // a DEFAULT_CHUNK_SIZE boundary
                    assertEquals("First chunk has improperly aligned low byte.",
                            0, i.getLow() % VerifyingFile.DEFAULT_CHUNK_SIZE);
                    if (i.getHigh() != fileSize-1 &&
                            i.getHigh() % VerifyingFile.DEFAULT_CHUNK_SIZE != 
                                VerifyingFile.DEFAULT_CHUNK_SIZE-1) {
                        assertTrue("First chunk has improperly aligned high byte.",
                                false);
                    }
                    firstLease = i;
                } else {
                    // on all other requests, we have 256k blocks
                    // Check that the low byte is aligned    
                    if (i.getLow() % (256 * 1024) != 0 &&
                            i.getLow() != firstLease.getHigh() + 1) {
                        assertTrue("Un-aligned low byte on chunk that is "+
                                "not adjascent to the DEFAULT_CHUNK_SIZE chunk.",
                                false);
                    }
                    // Check that the high byte is aligned    
                    if (i.getHigh() % (256 * 1024) != 256*1024-1 &&
                            i.getHigh() != firstLease.getLow() - 1 &&
                            i.getHigh() != fileSize-1) {
                        assertTrue("Un-aligned high byte on chunk that is "+
                                "not adjascent to the DEFAULT_CHUNK_SIZE chunk "+
                                "and is not the last chunk of the file",
                                false);
                    }
                } // close of if-else
            } // close of method
        }; // close of inner class
        
        testUploaders[0].setHTTPListener(grayVerifier);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        tigerTreeCache.purgeTree(rfd.getSHA1Urn());
        downloadServices.download(rfds, RemoteFileDesc.EMPTY_LIST, null, false);
        
        waitForComplete();
        assertEquals(6,testUploaders[0].getRequestsReceived());
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        
        for (int i=0; i<rfds.length; i++) {
            File incomplete=ifm.getFile(rfds[i]);
            VerifyingFile vf=ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
        
        assertEquals(1, testUploaders[0].getConnections());
    }

    public void testGetsThex() throws Exception {
        LOG.info("test that a host gets thex");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNotNull(tree);
        assertEquals(TestFile.tree().getRootHash(), tree.getRootHash());
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());     

        // there should be an entry for the sha1 urn.
        URN ttroot = tree.getTreeRootUrn();
        FileManager fm = injector.getInstance(FileManager.class);
        assertNotNull(fm.getGnutellaFileList().getFileDesc(TestFile.hash()));
        
        // and the filedesc should have both
        FileDesc fd = fm.getGnutellaFileList().getFileDesc(TestFile.hash());
        assertTrue(fd.getUrns().contains(TestFile.hash()));
        assertTrue(fd.getUrns().contains(ttroot));
    }
    
    public void testIncompleteDescsUpdated() throws Exception {
        LOG.info("test that the incomplete file desc is updated with the root once we have it");
        testUploaders[0].setRate(500);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        Downloader download = downloadServices.download(new RemoteFileDesc[]{rfd1}, true, null);
        try {
            Thread.sleep(1000);
            IncompleteFileManager ifm = downloadManager.getIncompleteFileManager();
            File incompleteFile = ifm.getFileForUrn(TestFile.hash());
            assertNotNull(incompleteFile);
            VerifyingFile vf = ifm.getEntry(incompleteFile);
            
            // wait till we get the hash tree
            int sleeps = 0;
            while(vf.getHashTree() == null && sleeps++ < 20) 
                Thread.sleep(500);
            assertNotNull(vf.getHashTree());
            URN ttroot = vf.getHashTree().getTreeRootUrn();
            assertEquals(ttroot, tigerTreeCache.getHashTreeRootForSha1(TestFile.hash()));
            
            // the sha1 should point to the filedesc 
            FileManager fm = injector.getInstance(FileManager.class);
            assertNotNull(fm.getIncompleteFileList().getFileDesc(TestFile.hash()));
            
            // and the filedesc should have both
            FileDesc fd = fm.getIncompleteFileList().getFileDesc(TestFile.hash());
            assertInstanceof(IncompleteFileDesc.class, fd);
            assertTrue(fd.getUrns().contains(TestFile.hash()));
            assertTrue(fd.getUrns().contains(ttroot));
            
            IncompleteFileDesc ifd = (IncompleteFileDesc)fd;
            
            // eventually we should become shareable
            sleeps = 0;
            while(!ifd.hasUrnsAndPartialData() && sleeps++ < 20)
                Thread.sleep(500);
            assertTrue(ifd.hasUrnsAndPartialData());
            
            waitForComplete();
        } finally {
            download.stop(false);
        }
    }
    
    public void testQueuedOnThexContinues() throws Exception {
        LOG.info("test that queued on thex continues");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setQueueOnThex(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());
    }
    
    public void testBadHeaderOnThexContinues() throws Exception {
        LOG.info("test bad header on thex continues");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setUseBadThexResponseHeader(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());
    }    
    
    public void testKeepCorrupt() throws Exception {
        LOG.info("-Testing that if the user chooses to keep a corrupt download the download" +
                "will eventually finish");
        final int RATE = 100;
        testUploaders[0].setCorruption(true);
        testUploaders[0].setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        testUploaders[0].setRate(RATE);
        
        testUploaders[1].setCorruption(true);
        testUploaders[1].setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        testUploaders[1].setRate(RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        
        downloadServices.download(new RemoteFileDesc[]{rfd1, rfd2}, RemoteFileDesc.EMPTY_LIST, null, false);
        waitForComplete();

        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        assertTrue(activityCallback.corruptChecked);
        
        // tried once or if twice then failed the second time.
        assertLessThanOrEquals(2, testUploaders[0].getConnections());
        // tried once or twice.
        assertLessThanOrEquals(2, testUploaders[1].getConnections());
        
        assertGreaterThanOrEquals(TestFile.length(), 
                testUploaders[0].getAmountUploaded()+testUploaders[1].getAmountUploaded());
    }
    
    public void testDiscardCorrupt() throws Exception {
        LOG.info("-Testing that if the user chooses to discard a corrupt download it will terminate" +
                "immediately");
        
        final int RATE = 100;
        activityCallback.delCorrupt = true;
        activityCallback.corruptChecked = false;
        testUploaders[0].setCorruption(true);
        testUploaders[0].setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        testUploaders[0].setRate(RATE);
        
        testUploaders[1].setCorruption(true);
        testUploaders[1].setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        testUploaders[1].setRate(RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        
        tGenericCorrupt( new RemoteFileDesc[] { rfd1}, new RemoteFileDesc[] {rfd2} );
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        assertTrue(activityCallback.corruptChecked);
        
        // tried once or if twice then failed the second time.
        assertLessThanOrEquals(2, testUploaders[0].getConnections());
        // tried once or twice.
        assertLessThanOrEquals(2, testUploaders[1].getConnections());
        
        assertGreaterThanOrEquals(TestFile.length(), 
                testUploaders[0].getAmountUploaded()+testUploaders[1].getAmountUploaded());
    }
    
    
    public void testMismatchedVerifyHashNoStopOnCorrupt() throws Exception {
        tMismatchedVerifyHash(false, false);
    }
    
    public void testMismatchedVerifyHashStopOnCorrupt() throws Exception {
        activityCallback.delCorrupt = true;        
        tMismatchedVerifyHash(true, false);
    }
    
    public void testMismatchedVerifyHashWithThexNoStopOnCorrupt()
      throws Exception {
        tMismatchedVerifyHash(false, true);
    }
    
    public void testMismatchedVerifyHashWithThexStopOnCorrupt() throws Exception{
        activityCallback.delCorrupt = true;
        tMismatchedVerifyHash(true, true);
    }

    // note that this test ONLY works because the TestUploader does NOT SEND
    // a Content-Urn header.  if it did, the download would immediately fail
    // when reading the header. 
    private void tMismatchedVerifyHash(boolean deleteCorrupt, boolean getThex )
      throws Exception {
        LOG.info("-Testing file declared corrupt, when hash of "+
                         "downloaded file mismatches bucket hash" +
                         "stop when corrupt "+ deleteCorrupt+" ");
        String badSha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";

        final int RATE=100;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(getThex);
        testUploaders[0].setSendThexTree(getThex);
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0],badSha1, false);
        
        URN badURN = URN.createSHA1Urn(badSha1);
        tigerTreeCache.purgeTree(TestFile.hash());
        tigerTreeCache.purgeTree(badURN);
        
        downloadServices.download(new RemoteFileDesc[] {rfd1}, false, null);
        // even though the download completed, we ignore the tree 'cause the
        // URNs didn't match.
        assertNull(tigerTreeCache.getHashTree(TestFile.hash()));
        assertNull(tigerTreeCache.getHashTree(badURN));

        waitForComplete(deleteCorrupt);
        assertTrue(activityCallback.corruptChecked);
        assertEquals(getThex, testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());
    }

    public void testReuseHostWithBadTree() throws Exception {
        LOG.info("-Testing that a host with a bad tree will be used");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(false);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        // the tree will fail, but it'll pick up the content-length
        // and discard the rest of the bad data.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());        
    }
    
    public void testReuseHostWithBadTreeAndNoContentLength() throws Exception {
        setDownloadWaitTime(2 * DOWNLOAD_WAIT_TIME);
        LOG.info("-Testing that a host with a bad tree will be used");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(false);
        testUploaders[0].setSendContentLength(false);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        // should pass after a bit because it retries the host
        // who gave it the bad length.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(2, testUploaders[0].getConnections());
    }
}
