package com.limegroup.gnutella.downloader;

import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;

public class DownloadTLSTest extends DownloadTestCase {

    private static final Log LOG = LogFactory.getLog(DownloadTLSTest.class);

    private TestUploader[] testTlsUploaders = new TestUploader[5];

    private int[] TPORTS = { 6421, 6422, 6423, 6424, 6425 };

    public DownloadTLSTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DownloadTLSTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        for (int i = 0; i < testTlsUploaders.length; i++) {
            testTlsUploaders[i] = injector.getInstance(TestUploader.class);
            testTlsUploaders[i].start("TPORT_" + i, TPORTS[i], true);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        for (int i = 0; i < testTlsUploaders.length; i++) {
            if (testTlsUploaders[i] != null) {
                testTlsUploaders[i].reset();
                testTlsUploaders[i].stopThread();
            }
        }
    }

    public void testSimpleTLSDownload10() throws Exception {
        LOG.info("-Testing non-swarmed download...");
        networkManager.setOutgoingTLSEnabled(true);
        RemoteFileDesc rfd = newRFD(TPORTS[0], true);
        RemoteFileDesc[] rfds = { rfd };
        tGeneric(rfds);
    }

    public void testSimpleTLSDownload11() throws Exception {
        LOG.info("-Testing non-swarmed download...");
        networkManager.setOutgoingTLSEnabled(true);
        RemoteFileDesc rfd = newRFDWithURN(TPORTS[0], true);
        RemoteFileDesc[] rfds = { rfd };
        tGeneric(rfds);
    }

    public void testSimpleTLSDownload10OutgoingOff() throws Exception {
        LOG.info("-Testing non-swarmed download...");
        networkManager.setOutgoingTLSEnabled(false);
        RemoteFileDesc rfd = newRFD(PORTS[0], true);
        RemoteFileDesc[] rfds = { rfd };
        tGeneric(rfds);
    }

    public void testSimpleTLSDownload11OutgoingOff() throws Exception {
        LOG.info("-Testing non-swarmed download...");
        networkManager.setOutgoingTLSEnabled(false);
        RemoteFileDesc rfd = newRFDWithURN(PORTS[0], true);
        RemoteFileDesc[] rfds = { rfd };
        tGeneric(rfds);
    }

    public void testSimpleTLSSwarm() throws Exception {
        LOG.info("-Testing swarming from two sources...");
        networkManager.setOutgoingTLSEnabled(true);

        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE = 500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR = RATE * 1024;
        testTlsUploaders[0].setRate(RATE);
        testTlsUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1 = newRFDWithURN(TPORTS[0], true);
        RemoteFileDesc rfd2 = newRFDWithURN(TPORTS[1], true);
        RemoteFileDesc[] rfds = { rfd1, rfd2 };

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = testTlsUploaders[0].fullRequestsUploaded();
        int u2 = testTlsUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: " + u1 + "\n");
        LOG.debug("\tu2: " + u2 + "\n");
        LOG.debug("\tTotal: " + (u1 + u2) + "\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length() / 2 + FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length() / 2 + FUDGE_FACTOR, u2);
    }

    public void testTwoTLSAlternateLocations() throws Exception {
        LOG.info("-Testing Two AlternateLocations w/ TLS...");
        networkManager.setOutgoingTLSEnabled(true);

        final int RATE = 50;
        testTlsUploaders[0].setRate(RATE);
        testTlsUploaders[1].setRate(RATE);

        RemoteFileDesc rfd1 = newRFDWithURN(TPORTS[0], TestFile.hash().toString(), true);
        RemoteFileDesc rfd2 = newRFDWithURN(TPORTS[1], TestFile.hash().toString(), true);
        RemoteFileDesc[] rfds = { rfd1, rfd2 };

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        List<AlternateLocation> alt1 = testTlsUploaders[0].getIncomingGoodAltLocs();
        List<AlternateLocation> alt2 = testTlsUploaders[1].getIncomingGoodAltLocs();

        AlternateLocation al1 = alternateLocationFactory.create(rfd1);
        AlternateLocation al2 = alternateLocationFactory.create(rfd2);

        assertTrue("uploader didn't recieve alt", !alt1.isEmpty());
        assertTrue("uploader didn't recieve alt", !alt2.isEmpty());
        assertTrue("uploader got wrong alt", !alt1.contains(al1));
        assertEquals("incorrect number of locs ", 1, alt1.size());
        assertTrue("uploader got wrong alt", !alt2.contains(al2));
        assertEquals("incorrect number of locs ", 1, alt2.size());

        AlternateLocation read1 = alt1.iterator().next();
        AlternateLocation read2 = alt2.iterator().next();
        assertInstanceof(DirectAltLoc.class, read1);
        assertInstanceof(DirectAltLoc.class, read2);
        IpPort ipp1 = ((DirectAltLoc) read1).getHost();
        IpPort ipp2 = ((DirectAltLoc) read2).getHost();
        assertInstanceof(Connectable.class, ipp1);
        assertTrue(((Connectable) ipp1).isTLSCapable());
        assertInstanceof(Connectable.class, ipp2);
        assertTrue(((Connectable) ipp2).isTLSCapable());

    }

    public void testTLSUploaderAlternateLocations() throws Exception {
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        LOG.info("-Testing swarming from two sources one based on alt w/ TLS...");
        networkManager.setOutgoingTLSEnabled(true);

        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE = 500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR = RATE * 1024;
        testUploaders[0].setRate(RATE);
        testTlsUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], TestFile.hash().toString(), false);
        RemoteFileDesc rfd2 = newRFDWithURN(TPORTS[1], TestFile.hash().toString(), true);
        RemoteFileDesc[] rfds = { rfd1 };

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection<AlternateLocation> ualt = AlternateLocationCollection
                .create(rfd2.getSHA1Urn());

        AlternateLocation al2 = alternateLocationFactory.create(rfd2);
        ualt.add(al2);

        testUploaders[0].setGoodAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testTlsUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: " + u1 + "\n");
        LOG.debug("\tu2: " + u2 + "\n");
        LOG.debug("\tTotal: " + (u1 + u2) + "\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length() / 2 + FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length() / 2 + FUDGE_FACTOR, u2);
    }

    public void testSimpleDownloadWithInitialTLSAlts() throws Exception {
        LOG.info("-Testing download with initial TLS alts");
        networkManager.setOutgoingTLSEnabled(true);

        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE = 500;

        final int FUDGE_FACTOR = RATE * 1024;
        testUploaders[0].setRate(RATE);
        testTlsUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2 = newRFDWithURN(TPORTS[1], false);
        RemoteFileDesc[] rfds1 = { rfd1 };
        List<RemoteFileDesc> rfds2 = new LinkedList<RemoteFileDesc>();
        rfds2.add(rfd2);

        tGeneric(rfds1, rfds2);

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testTlsUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: " + u1 + "\n");
        LOG.debug("\tu2: " + u2 + "\n");
        LOG.debug("\tTotal: " + (u1 + u2) + "\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length() / 2 + FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length() / 2 + FUDGE_FACTOR, u2);
    }

}
