package com.limegroup.gnutella.lws.server;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.LWSIntegrationServices;
import com.limegroup.gnutella.downloader.StoreDownloader;

/**
 * Tests the <code>Download</code> command.
 */
public class LWSDownloadTest extends AbstractCommunicationSupportWithNoLocalServer {
    
    private final LWSDownloadTestConstants constants = new LWSDownloadTestConstants();
    
    /**
     * Set up the little web server.
     */
    @Override
    protected void afterSetup() {
        super.afterSetup();

        // Make sure we are downloading from the right spot
        LWSIntegrationServices services = getInstance(LWSIntegrationServices.class);
        services.setDownloadPrefix(constants.HOST + ":" + constants.PORT);

        // Start up the server
        server = new SimpleWebServer(constants);
        server.start();

        // Reset and authenticate
        doAuthenticate();
    }

    /**
     * Tear down the little web server.
     */
    @Override
    protected void afterTearDown() {
        super.afterTearDown();
        server.stop();
        sleep(2000);
    }    
  
    public LWSDownloadTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(LWSDownloadTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
    
    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------
    
    public void testSimple() {
        doDownloadTest(null,null,true,"");
    }
    
    public void testTwoDownloadsAtOnce() {
        final boolean[] done = {false,false};
        final MutableString id1 = new MutableString();
        final MutableString id2 = new MutableString();     
        Thread download1 = new Thread(new Runnable() {
            public void run() {
                doDownloadTest(id1,null,true,"");
                done[0] = true;
            }
        });
        download1.start();
        Thread download2 = new Thread(new Runnable() {
            public void run() {
                doDownloadTest(id2,null,true,"");
                done[1] = true;
            }
        });
        download2.start();
        
        // IDs should be the same
        assertEquals("IDs should be the same", id1.get(), id2.get());
        
        DownloadServices downloadServices = getInstance(DownloadServices.class);
        boolean haveSeenOneDownloader = false;
        while (!done[0]) {
            if (!haveSeenOneDownloader) {
                haveSeenOneDownloader = downloadServices.getNumDownloads() == 1;
            }
            int num = downloadServices.getNumDownloads();
            assertTrue("getNumDownloads should be 0 or 1, not " + num, num == 0 || num == 1);
        }
        
        assertTrue("Should have seen one downloader", haveSeenOneDownloader);
        
        try {
            download1.join();
        } catch (InterruptedException e) {
            LOG.error(e);
        }
        try {
            download2.join();
        } catch (InterruptedException e) {
            LOG.error(e);
        }
    } 
    
    /*
     * These two tests were made non-tests because we don't ever actually pause
     * downloads from the webpage, and these tests, while pass most of the time,
     * occasionally fail as false negatives. In the event we start pausing and
     * resuming downloads from the webpage we'll reinstate them.
     */
    
    public void omitTestPauseResumeStopAll() {
        final boolean[] done = {false,false,false};
        final MutableString[] mids = {new MutableString(),new MutableString(),new MutableString()};
        Thread download1 = new Thread(new Runnable() {
            public void run() {
                doDownloadTest(mids[0],null,false,"");
                done[0] = true;
            }
        });
        Thread download2 = new Thread(new Runnable() {
            public void run() {
                doDownloadTest(mids[1],null,false,"1");
                done[1] = true;
            }
        });
        Thread download3 = new Thread(new Runnable() {
            public void run() {
                doDownloadTest(mids[2],null,false,"2");
                done[2] = true;
            }
        });        
        download1.start();  
        download2.start();  
        download3.start();  
        sleep(1000);
        
        boolean isActive;
        boolean isPaused;
        
        isActive = false;
        for (StoreDownloader storeDownloader : getStoreDownloaders()) {
            if (storeDownloader.isActive()) {
                isActive = true;
                break;
            }
        }
        assertTrue("One should be active", isActive);
        final Map<String, String> args = new HashMap<String, String>();
        
        // Now pause the download
        sendCommandToClientAndWait("PauseAllDownloads", args);
        isPaused = false;
        for (StoreDownloader storeDownloader : getStoreDownloaders()) {
            if (storeDownloader.isPaused()) {
                isPaused = true;
                break;
            }
        }
        assertTrue("One should be paused", isPaused);
        
        // Now resume the download
        sendCommandToClientAndWait("ResumeAllDownloads", args);
        isActive = false;
        for (StoreDownloader storeDownloader : getStoreDownloaders()) {
            if (storeDownloader.isActive()) {
                isActive = true;
                break;
            }
        }
        assertTrue("One should be active", isActive);
        
        // Now pause the download, again
        sendCommandToClientAndWait("PauseAllDownloads", args);
        isPaused = false;
        for (StoreDownloader storeDownloader : getStoreDownloaders()) {
            if (storeDownloader.isPaused()) {
                isPaused = true;
                break;
            }
        }
        
        // Now resume the download, again
        sendCommandToClientAndWait("ResumeAllDownloads", args);
        isActive = false;
        for (StoreDownloader storeDownloader : getStoreDownloaders()) {
            if (storeDownloader.isActive()) {
                isActive = true;
                break;
            }
        }
        assertTrue("One should be active", isActive);
        
        // Now stop the download
        sendCommandToClientAndWait("StopAllDownloads", args);
        assertTrue("Should be empty", getStoreDownloaders(false).isEmpty());
        
        try {
            download1.join();
        } catch (InterruptedException e) {
            LOG.error(e);
        }
        try {
            download2.join();
        } catch (InterruptedException e) {
            LOG.error(e);
        } 
        try {
            download3.join();
        } catch (InterruptedException e) {
            LOG.error(e);
        }         
    }    
    
    public void omitTestPauseResumeStop() {
        final boolean[] done = {false};
        final MutableString mid = new MutableString();
        Thread download1 = new Thread(new Runnable() {
            public void run() {
                doDownloadTest(mid,null,false,"");
                done[0] = true;
            }
        });
        download1.start();  
        sleep(1000);
        
        StoreDownloader storeDownloader = getStoreDownloader();
        assertTrue("Should be active", storeDownloader.isActive());
        String id = mid.get().split(" ")[0];
        final Map<String, String> args = new HashMap<String, String>();
        args.put("id", id);
        
        // Now pause the download
        sendCommandToClientAndWait("PauseDownload", args);
        assertTrue("Should be paused", storeDownloader.isPaused());
        
        // Now resume the download
        sendCommandToClientAndWait("ResumeDownload", args);
        assertTrue("Should be resumed", storeDownloader.isActive() || storeDownloader.isCompleted());
        assertFalse("Should not be paused", storeDownloader.isPaused() || storeDownloader.isCompleted());
        
        // Now pause the download, again
        sendCommandToClientAndWait("PauseDownload", args);
        assertTrue("Should be paused", storeDownloader.isPaused() || storeDownloader.isCompleted());
        
        // Now resume the download, again
        sendCommandToClientAndWait("ResumeDownload", args);
        assertTrue("Should be resumed", storeDownloader.isActive() || storeDownloader.isCompleted());
        assertFalse("Should not be paused", storeDownloader.isPaused() || storeDownloader.isCompleted());
        
        // Now stop the download
        sendCommandToClientAndWait("StopDownload", args);
        assertTrue("Should be empty",getStoreDownloaders(false).isEmpty());
        
        try {
            download1.join();
        } catch (InterruptedException e) {
            LOG.error(e);
        }
    }    
    
    // ------------------------------------------------------------------
    // Support
    // ------------------------------------------------------------------    

    private SimpleWebServer server;
    
    private StoreDownloader getStoreDownloader() {
        StoreDownloader storeDownloader = getStoreDownloaders().get(0);
        assertNotNull(storeDownloader);
        return storeDownloader;
    }
    
    private List<StoreDownloader> getStoreDownloaders() {
        return getStoreDownloaders(true);
    }
    
    private List<StoreDownloader> getStoreDownloaders(boolean checkForEmpty) {
        DownloadManager man = getInstance(DownloadManager.class);
        List<StoreDownloader> res = new ArrayList<StoreDownloader>();
        for (CoreDownloader coreDownloader : man.getAllDownloaders()) {
            if (coreDownloader instanceof StoreDownloader) {
                res.add((StoreDownloader)coreDownloader);
            }
        }        
        if (checkForEmpty) {
            assertFalse("Should not be empty", res.isEmpty());
        }
        return res;
    }    
    
    private final static class MutableString {
        String s;
        public void set(String s) {this.s = s;}
        public String get() {return s;}
        @Override
        public String toString() {return get();}
    }
    
    /**
     * @param inId will hold the id of the downloader -- can be null
     * @param r run this is the loop -- can be null
     * @param checkComplete whether we check that the download finished at the end
     * @param fudge fudge factor to add to the url, file, and ID, so we can have multiple downloaders;
     */
    private void doDownloadTest(MutableString inId, Runnable r, boolean checkComplete, String fudge) {

        long length = constants.LENGTH;

        Map<String, String> args = new HashMap<String, String>();
        String id = constants.ID + fudge;
        args.put("url", constants.URL  + fudge);
        args.put("file", (!fudge.equals("") ? "/" + fudge : "") + constants.FILE);
        args.put("id", id);
        args.put("length", String.valueOf(length));

        // Send the client a command to start the download
        String downloaderIDAndProgressBarID = sendCommandToClient("Download", args);
        if (inId != null) {
            String[] parts = downloaderIDAndProgressBarID.split(" ");
            assertEquals("Should have a downloader ID and progress bar ID", 2, parts.length);
            boolean found = false;
            String downloaderID = parts[0];
            for (StoreDownloader storeDownloader : getStoreDownloaders()) {
                if (downloaderID.equals(String.valueOf(System.identityHashCode(storeDownloader)))) {
                    found = true;
                    break;
                }
            }
            assertTrue("Should have found a store downloader for id " + downloaderID, found);
            assertEquals("Should be the same as given", id, parts[1]);
            inId.set(downloaderIDAndProgressBarID);
        }

        File savedFile = new File(_storeDir, args.get("file"));
        // Do a busy wait until we've run out of time or the file we wanted
        // was downloaded and is the size we wanted
        for (long toStop = System.currentTimeMillis() + constants.DOWNLOAD_WAIT_TIME; System.currentTimeMillis() < toStop;) {
            if (server.getBytesWritten() == length && savedFile.length() == length) {
                break;
            }
            if (r != null) r.run();
            sleep(1000);
        }
        //
        // We may have stopped it
        //
        if (checkComplete) {
            assertEquals(savedFile.getAbsolutePath(), length, savedFile.length());
            assertEquals(String.valueOf(savedFile.length()), length, server.getBytesWritten());
        }
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
            // ignore
        }        
    }
    
    /**
     * This is used, because there is a slight race condition in sending
     * commands. But 1 second is plenty enough time, and if it takes longer,
     * that is a problem as well.
     */    
    private void sleepForASecond() {
        sleep(1000);
    }
    
    /**
     * Sends a command to the client and then waits; returns the result.
     * 
     * @return the result of sending the command to the client.
     */
    private String sendCommandToClientAndWait(String cmd, Map<String,String> args) {
        sleepForASecond();
        String res = sendCommandToClient(cmd, args);
        sleepForASecond();
        return res;
    }
}
