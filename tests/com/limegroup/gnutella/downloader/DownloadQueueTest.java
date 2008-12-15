package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SpeedConstants;

import com.limegroup.gnutella.RemoteFileDesc;

public class DownloadQueueTest extends DownloadTestCase {
    private static final Log LOG = LogFactory.getLog(DownloadQueueTest.class);
    
    public DownloadQueueTest(String name) {
        super(name);
    }

    public static Test suite() { 
        return buildTestSuite(DownloadQueueTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    
    public void testQueuedDownloader() throws Exception {
        LOG.info("-Testing queued downloader. \n");
        
        testUploaders[0].setQueue(true);
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc[] rfds = {rfd1};
        //the queued downloader will resend the query after sleeping,
        //and then it shold complete the download, because TestUploader
        //resets queue after sending 503
        tGeneric(rfds);
    }
    
    /**
     * Tests that an uploader offering the file, replaces a queued uploader
     * when even at swarm capacity
     */
    public void testDownloadAtCapacityReplaceQueued() throws Exception {
        LOG.info("-testing that if max threads are queued or downloading, and a "+
              "good location comes along, the queued downloader is dislodged");
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 30;
        testUploaders[0].setRate(RATE);
        testUploaders[2].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[1].setQueue(true);
        testUploaders[1].setUnqueue(false); //never unqueue this uploader.
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};//one good and one queued
        
        RemoteFileDesc rfd3 = newRFDWithURN(PORTS[2], false);
        
        ManagedDownloader downloader = null;
        
        downloader=(ManagedDownloader)downloadServices.download(rfds, false, null);
        //Thread.sleep(1000);
        //downloader.addDownloadForced(rfd2,false);
        Thread.sleep(2 * DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        LOG.debug("about to check swarming");
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1, queued);

        downloader.addDownload(rfd3, true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        
        //make sure we killed the queued
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not replaced ",0, queued);

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");
        
        int u1 = testUploaders[0].getAmountUploaded();
        int u2 = testUploaders[1].getAmountUploaded();
        int u3 = testUploaders[2].getAmountUploaded();
        
        assertEquals("queued uploader uploaded",0,u2);
        assertGreaterThan("u3 not given a chance to run", 0, u3);
        assertLessThan("u1 did all the work",TestFile.length(),u1);  
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }

    /**
     * Tests that when we have max download threads, and there is a queued
     * downloader, it does not get replaced by another queued downloader with a
     * worse position, but does get replaced by a queued downloader that has a
     * better position
     */
    public void testDownloadAtCapacityGetsBetterQueued() throws Exception {
        LOG.info("-testing that if max threads are queued or downloading, and a "+
              "queued downloader gets by a queued downloader only if the new "+
              "one has a better queue position");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 50;
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[1].setQueue(true);
        testUploaders[1].setUnqueue(false); //never unqueue this uploader.
        testUploaders[1].setQueuePos(3);

        testUploaders[2].setRate(RATE);
        testUploaders[2].setQueue(true);
        testUploaders[2].setUnqueue(false); //never unqueue this uploader.
        testUploaders[2].setQueuePos(5);

        testUploaders[3].setRate(RATE);
        testUploaders[3].setQueue(true);
        testUploaders[3].setUnqueue(false); //never unqueue this uploader.
        testUploaders[3].setQueuePos(1);

        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc rfd3=newRFDWithURN(PORTS[2], false);
        RemoteFileDesc rfd4=newRFDWithURN(PORTS[3], false);
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        ManagedDownloaderImpl downloader = null;
        downloader = (ManagedDownloaderImpl)downloadServices.download(rfds, false, null);
        Thread.sleep(2 * DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        int qPos=downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 3 which is worse, nothing should change
        downloader.addDownload(rfd3,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1500);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = downloader.getQueuePosition();
        LOG.debug("queued workers: "+downloader.getQueuedWorkers());
        LOG.debug("active workers: "+downloader.getActiveWorkers());
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 4 which is better, we should drop uploader2
        downloader.addDownload(rfd4,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1500);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 4 not queued ",1,queued);
        assertEquals("incorrect queue pos ",1,qPos);        

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }

    /**
     *  Tests that queued downloads advance on the downloader, this is important
     *  because we use the queue position to decide which downloader to get rid
     *  off when a good uploader shows up
     */
    public void testQueueAdvancementWorks() throws Exception {
        LOG.info("-testing that if queued downloaders advance we downloaders "+
              "register that they did, so that the choice of which downloader"+
              " to replace is made correctly");
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 50;
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[2].setRate(RATE);

        testUploaders[0].setQueue(true);
        testUploaders[0].setUnqueue(false); //never unqueue this uploader.
        testUploaders[0].setQueuePos(5);//the worse one
        testUploaders[1].setQueue(true);
        testUploaders[1].setUnqueue(false); //never unqueue this uploader.
        testUploaders[1].setQueuePos(3);//the better one
        
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc rfd3=newRFDWithURN(PORTS[2], false);
        
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        ManagedDownloader downloader = null;
        downloader = (ManagedDownloader)downloadServices.download(rfds,false,null);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()*2 + 1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("both uploaders should be queued",2,queued);
        
        testUploaders[0].setQueuePos(1);//make uploader1 become better
        //wait for the downloader to make the next requests to uploaders.
        Thread.sleep(testUploaders[0].MIN_POLL+2000);

        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("both uploaders should still be queued",2,queued);
        
        downloader.addDownload(rfd3,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        //now uploader 2 should have been removed.
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        int qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("queued uploader not dropped",1,queued);
        assertEquals("wrong uploader removed",1,qPos);

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }
}
