package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.service.ErrorService;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.helpers.AlternateLocationHelper;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;

public class DownloadAltLocTest extends DownloadTestCase {
    
    private static final Log LOG = LogFactory.getLog(DownloadAltLocTest.class);
    
    public DownloadAltLocTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DownloadAltLocTest.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // need some extra time for these tests
        setDownloadWaitTime(2 * 60 * 1000L);
    }

    public void testTwoAlternateLocations() throws Exception {  
        LOG.info("-Testing Two AlternateLocations...");
        
        final int RATE = 50;
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        
        RemoteFileDesc rfd1=
                         newRFDWithURN(PORTS[0], TestFile.hash().toString(), false);
        RemoteFileDesc rfd2=
                         newRFDWithURN(PORTS[1], TestFile.hash().toString(), false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        List<AlternateLocation> alt1 = testUploaders[0].getIncomingGoodAltLocs();
        List<AlternateLocation> alt2 = testUploaders[1].getIncomingGoodAltLocs();

        AlternateLocation al1 = alternateLocationFactory.create(rfd1);
        AlternateLocation al2 = alternateLocationFactory.create(rfd2);
        
        assertTrue("uploader didn't recieve alt", !alt1.isEmpty());
        assertTrue("uploader didn't recieve alt", !alt2.isEmpty());
        assertTrue("uploader got wrong alt", !alt1.contains(al1));
        assertEquals("incorrect number of locs ",1,alt1.size());
        assertTrue("uploader got wrong alt", !alt2.contains(al2));
        assertEquals("incorrect number of locs ",1,alt2.size());
        
        AlternateLocation read1 = alt1.iterator().next();
        AlternateLocation read2 = alt2.iterator().next();
        assertInstanceof(DirectAltLoc.class, read1);
        assertInstanceof(DirectAltLoc.class, read2);
        IpPort ipp1 = ((DirectAltLoc)read1).getHost();
        IpPort ipp2 = ((DirectAltLoc)read2).getHost();
        if(ipp1 instanceof Connectable)
            assertFalse(((Connectable)ipp1).isTLSCapable());
        if(ipp2 instanceof Connectable)
            assertFalse(((Connectable)ipp2).isTLSCapable());
    }

    public void testUploaderAlternateLocations() throws Exception {  
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        LOG.info("-Testing swarming from two sources one based on alt...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=
                          newRFDWithURN(PORTS[0],TestFile.hash().toString(), false);
        RemoteFileDesc rfd2=
                          newRFDWithURN(PORTS[1],TestFile.hash().toString(), false);
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection<AlternateLocation> ualt = 
            AlternateLocationCollection.create(rfd2.getSHA1Urn());

        
        AlternateLocation al2 =
            alternateLocationFactory.create(rfd2);
        ualt.add(al2);

        testUploaders[0].setGoodAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }
    
    public void testAlternateLocationsAreRemoved() throws Exception {
        DOWNLOAD_WAIT_TIME = 2 * 60 * 1000;
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        LOG.info("-Testing swarming from two sources one based on alt...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=5;
        // Make sure uploader2 will never complete an upload
        final int STOP_AFTER = 0;
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[1].stopAfter(STOP_AFTER);
        testUploaders[2].setRate(RATE);
        RemoteFileDesc rfd1=
                        newRFDWithURN(PORTS[0], TestFile.hash().toString(), false);
        RemoteFileDesc rfd2=
                        newRFDWithURN(PORTS[1], TestFile.hash().toString(), false);
        RemoteFileDesc rfd3=
                        newRFDWithURN(PORTS[2], TestFile.hash().toString(), false);
        
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection<AlternateLocation> ualt = 
                         AlternateLocationCollection.create(rfd2.getSHA1Urn());


        AlternateLocation al1 = alternateLocationFactory.create(rfd1);
        AlternateLocation al2 = alternateLocationFactory.create(rfd2);
        AlternateLocation al3 = alternateLocationFactory.create(rfd3);
        ualt.add(al2);
        ualt.add(al3);

        testUploaders[0].setGoodAlternateLocations(ualt);
        
        saveAltLocs = true;
        tGeneric(rfds);

        //Now let's check that the uploaders got the correct AltLocs.
        //Uploader 1: Must have al3. 
        //Uploader 1 got correct Alts?
        List alts = testUploaders[0].getIncomingGoodAltLocs();
        assertTrue(alts.contains(al3));
        
        // al2 should have been sent to uploader1 as NAlt header
        assertTrue(testUploaders[0].getIncomingBadAltLocs().contains(al2));

        // uploader3 should contain only al1
        alts = testUploaders[2].getIncomingGoodAltLocs();
        assertTrue(alts.contains(al1));
        assertFalse(alts.contains(al2));
        
        // Test Downloader has correct alts: the downloader should have 
        // 2 or 3. If two they should be u1 and u3. If 3 u2 should be demoted
        assertTrue(validAlts.contains(al1)); 
        assertTrue(validAlts.contains(al3)); 
        Iterator iter = validAlts.iterator(); 
        while(iter.hasNext()) { 
            AlternateLocation loc = (AlternateLocation)iter.next(); 
            if(loc.equals(al2)) 
                assertTrue("failed loc not demoted",loc.isDemoted()); 
        }
        
        // ManagedDownloader clears validAlts and invalidAlts after completion
        assertEquals(DownloadStatus.COMPLETE, managedDownloader.getState());
        assertTrue(((Set)PrivilegedAccessor.getValue(managedDownloader, "validAlts")).isEmpty());
        assertTrue(((Set)PrivilegedAccessor.getValue(managedDownloader, "invalidAlts")).isEmpty());
    }    

    public void testWeirdAlternateLocations() throws Exception {  
        LOG.info("-Testing AlternateLocation weird...");
        AlternateLocationHelper alternateLocationHelper =
            new AlternateLocationHelper(alternateLocationFactory);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0],TestFile.hash().toString(), false);
        RemoteFileDesc[] rfds = {rfd1};
        
        
        //Prebuild some uploader alts
        AlternateLocationCollection<AlternateLocation> ualt = 
            AlternateLocationCollection.create(
                    alternateLocationHelper.EQUAL_SHA1_LOCATIONS[0].getSHA1Urn());

        ualt.add(alternateLocationHelper.EQUAL_SHA1_LOCATIONS[0]);
        ualt.add(alternateLocationHelper.EQUAL_SHA1_LOCATIONS[1]);
        ualt.add(alternateLocationHelper.EQUAL_SHA1_LOCATIONS[2]);
        ualt.add(alternateLocationHelper.EQUAL_SHA1_LOCATIONS[3]);
        testUploaders[0].setGoodAlternateLocations(ualt);

        saveAltLocs = true;
        tGeneric(rfds);
        
        //Check to check the alternate locations
        List alt1 = testUploaders[0].getIncomingGoodAltLocs();
        assertEquals("uploader got bad alt locs",0,alt1.size());
        
        AlternateLocation agood = alternateLocationFactory.create(rfd1);
        assertTrue(validAlts.contains(agood)); 
        assertFalse(validAlts.contains(alternateLocationHelper.EQUAL_SHA1_LOCATIONS[0])); 
        assertFalse(validAlts.contains(alternateLocationHelper.EQUAL_SHA1_LOCATIONS[1])); 
        assertFalse(validAlts.contains(alternateLocationHelper.EQUAL_SHA1_LOCATIONS[2]));
        
        // ManagedDownloader clears validAlts and invalidAlts after completion
        assertEquals(DownloadStatus.COMPLETE, managedDownloader.getState());
        assertTrue(((Set)PrivilegedAccessor.getValue(managedDownloader, "validAlts")).isEmpty());
        assertTrue(((Set)PrivilegedAccessor.getValue(managedDownloader, "invalidAlts")).isEmpty());
    }

    public void testAddSelfToMeshWithTree() throws Exception {
        LocalSocketAddressProviderStub localSocketAddressProvider = (LocalSocketAddressProviderStub) injector.getInstance(LocalSocketAddressProvider.class);
        localSocketAddressProvider.setLocalAddress(new byte[] { (byte)129, 0, 0, 1 });
        localSocketAddressProvider.setLocalPort(6996);
        
        // change the minimum required bytes so it'll be added.
        HTTPDownloader.MIN_PARTIAL_FILE_BYTES = 1;
        networkManager.setAcceptedIncomingConnection(true);

        LOG.info("-Testing that downloader adds itself to the mesh if it has a tree");

        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                SpeedConstants.MODEM_SPEED_INT);

        List u1Alt = testUploaders[0].getIncomingGoodAltLocs();
        List u2Alt = testUploaders[1].getIncomingGoodAltLocs();

        // neither uploader knows any alt locs.
        u1Alt.clear();
        u2Alt.clear();

        // the rate must be absurdly slow for the incomplete file.length()
        // check in HTTPDownloader to be updated.
        final int RATE=50;
        final int STOP_AFTER = ((TestFile.length()*2)/3)+1;
        testUploaders[0].setRate(RATE);
        testUploaders[0].stopAfter(STOP_AFTER);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        testUploaders[1].setRate(RATE);
        testUploaders[1].stopAfter(STOP_AFTER);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0],TestFile.hash().toString(), false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1],TestFile.hash().toString(), false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        // make sure there was enough swarming opportunity
        int u1 = testUploaders[0].getRequestsReceived();
        int u2 = testUploaders[1].getRequestsReceived();
        // if not, start the test over.  Worst case this hits the junit timeout.
        if (u1 < 5 || u2 < 5) {
            tearDown();
            setUp();
            testAddSelfToMeshWithTree();
            return;
        }

        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //both uploaders should know that this downloader is an alt loc.
        u1Alt = testUploaders[0].getIncomingGoodAltLocs();
        u2Alt = testUploaders[1].getIncomingGoodAltLocs();
        assertFalse(u1Alt.isEmpty());
        assertFalse(u2Alt.isEmpty());

        AlternateLocation al = alternateLocationFactory.create(TestFile.hash());
        assertTrue(u1Alt.toString()+" should contain "+al+" u1: "+u1+" u2 "+u2, u1Alt.contains(al) );
        assertTrue(u2Alt.toString()+" should contain "+al+" u1: "+u1+" u2 "+u2,  u2Alt.contains(al) );        

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertLessThanOrEquals("u1 did too much work", STOP_AFTER, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }

    public void testNotAddSelfToMeshIfNoTree() throws Exception {
        // change the minimum required bytes so it'll be added.
        PrivilegedAccessor.setValue(HTTPDownloader.class,
                "MIN_PARTIAL_FILE_BYTES", new Integer(1) );
        PrivilegedAccessor.setValue(acceptor,
            "_acceptedIncoming", Boolean.TRUE );
            
        LOG.info("-Testing that downloader does not add itself to the mesh if it has no tree");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
        
        List u1Alt = testUploaders[0].getIncomingGoodAltLocs();
        List u2Alt = testUploaders[1].getIncomingGoodAltLocs();
                    
        // neither uploader knows any alt locs.
        assertTrue(u1Alt.isEmpty());
        assertTrue(u2Alt.isEmpty());

        // the rate must be absurdly slow for the incomplete file.length()
        // check in HTTPDownloader to be updated.
        final int RATE=50;
        final int STOP_AFTER = TestFile.length()/2;
        testUploaders[0].setRate(RATE);
        testUploaders[0].stopAfter(STOP_AFTER);
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0],TestFile.hash().toString(), false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1],TestFile.hash().toString(), false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //both uploaders should know that the other uploader is an alt loc.
        u1Alt = testUploaders[0].getIncomingGoodAltLocs();
        u2Alt = testUploaders[1].getIncomingGoodAltLocs();
        assertEquals(1,u1Alt.size());
        assertEquals(1,u2Alt.size());
        assertTrue(u1Alt.contains(alternateLocationFactory.create(rfd2)));
        assertTrue(u2Alt.contains(alternateLocationFactory.create(rfd1)));

        // but should not know about me.
        AlternateLocation al = alternateLocationFactory.create(TestFile.hash());
        assertFalse( u1Alt.contains(al) );
        assertFalse( u2Alt.contains(al) );        

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertLessThanOrEquals("u1 did too much work", STOP_AFTER, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testPartialAddsAltsActiveDownload() throws Exception {
        altBootstrapTest(false);
    }
    
    public void testPartialBootstrapsInactiveDownload() throws Exception {
        // this is different from testResumePartialWithAlternateLocations where the
        // download is resumed manuall
        altBootstrapTest(true);
    }
    
    private void altBootstrapTest(final boolean complete) throws Exception {
        LOG.info("-Testing a shared partial funnels alt locs to downloader");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
            
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = TestFile.length()/10;
        testUploaders[0].setRate(RATE);
        testUploaders[0].stopAfter(STOP_AFTER);
        testUploaders[1].setRate(RATE);
        testUploaders[1].stopAfter(STOP_AFTER);
        testUploaders[2].setRate(RATE);
        final RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], TestFile.hash().toString(), false);
        final RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], TestFile.hash().toString(), false);
        final RemoteFileDesc rfd3=newRFDWithURN(PORTS[2], TestFile.hash().toString(), false);
        
        //Start with only RFD1.
        RemoteFileDesc[] rfds = {rfd1};
        
        // Add RFD2 and 3 to the IncompleteFileDesc, make sure we use them.
        Thread locAdder = new ManagedThread( new Runnable() {
            public void run() {
                try {
                    Thread.sleep(complete ? 4000 : 1500);
                    FileDesc fd = fileManager.getManagedFileList().getFileDescsMatching(TestFile.hash()).get(0);
                    assertTrue(fd instanceof IncompleteFileDesc);
                    altLocManager.add(
                            alternateLocationFactory.create(rfd2),this);
                    altLocManager.add(
                            alternateLocationFactory.create(rfd3),this);
                } catch(Throwable e) {
                    ErrorService.error(e);
                }
            }
       });
       locAdder.start();
        
        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].getAmountUploaded();
        int u2 = testUploaders[1].getAmountUploaded();
        int u3 = testUploaders[2].getAmountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tu3: "+u3+"\n");
        LOG.debug("\tTotal: "+(u1+u2+u3)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did too much work", STOP_AFTER, u1);
        assertEquals("u2 did too much work", STOP_AFTER, u2);
        assertGreaterThan("u3 did no work", 0, u3);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testResumeFromPartialWithAlternateLocations() throws Exception {
        LOG.info("-Testing alt locs from partial bootstrap resumed download");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
            
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = TestFile.length()/10;
        testUploaders[0].setRate(RATE);
        testUploaders[0].stopAfter(STOP_AFTER);
        testUploaders[1].setRate(RATE);
        testUploaders[1].stopAfter(STOP_AFTER);
        testUploaders[2].setRate(RATE);
        final RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], TestFile.hash().toString(), false);
        final RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], TestFile.hash().toString(), false);
        final RemoteFileDesc rfd3=newRFDWithURN(PORTS[2], TestFile.hash().toString(), false);
        AlternateLocation al1 = alternateLocationFactory.create(rfd1);
        AlternateLocation al2 = alternateLocationFactory.create(rfd2);
        AlternateLocation al3 = alternateLocationFactory.create(rfd3);
        
        IncompleteFileManager ifm = downloadManager.getIncompleteFileManager();
        // put the hash for this into IFM.
        File incFile = ifm.getFile(rfd1);
        incFile.createNewFile();
        // add the entry, so it's added to FileManager.
        ifm.addEntry(incFile, verifyingFileFactory.createVerifyingFile(TestFile.length()), true);
        
        // Get the IncompleteFileDesc and add these alt locs to it.
        FileDesc fd = fileManager.getManagedFileList().getFileDescsMatching(TestFile.hash()).get(0);
        assertNotNull(fd);
        assertInstanceof(IncompleteFileDesc.class, fd);
        altLocManager.add(al1, null);
        altLocManager.add(al2, null);
        altLocManager.add(al3, null);
        
        tResume(incFile);

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].getAmountUploaded();
        int u2 = testUploaders[1].getAmountUploaded();
        int u3 = testUploaders[2].getAmountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tu3: "+u3+"\n");
        LOG.debug("\tTotal: "+(u1+u2+u3)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did wrong work", STOP_AFTER, u1);
        assertEquals("u2 did wrong work", STOP_AFTER, u2);
        assertGreaterThan("u3 did no work", 0, u3);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    } 


    /**
     * Test to make sure that we read the alternate locations from the
     * uploader response headers even if the response code is a 503,
     * try again later.
     */
    public void testAlternateLocationsExchangedWithBusy() throws Exception {
        //tests that a downloader reads alternate locations from the
        //uploader even if it receives a 503 from the uploader.
        LOG.info("-Testing dloader gets alt from 503 uploader...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        testUploaders[0].setBusy(true);
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection<AlternateLocation> ualt = 
            AlternateLocationCollection.create(rfd1.getSHA1Urn());

        AlternateLocation al2 =
            alternateLocationFactory.create(rfd2);
        ualt.add(al2);

        testUploaders[0].setGoodAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertEquals("u1 did too much work", 0, u1);
        assertLessThan("u2 did all the work", TestFile.length()+FUDGE_FACTOR, u2);
    }
    
    public void testSimpleDownloadWithInitialAlts() throws Exception {
        LOG.info("-Testing download with initial alts");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;

        final int FUDGE_FACTOR=RATE*1024;  
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds1 = {rfd1};
        List<RemoteFileDesc> rfds2 = new LinkedList<RemoteFileDesc>();
        rfds2.add(rfd2);
        
        tGeneric(rfds1, rfds2);
        
        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }    
    
}
