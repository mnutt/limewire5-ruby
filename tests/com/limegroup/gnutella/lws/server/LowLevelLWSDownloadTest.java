package com.limegroup.gnutella.lws.server;

import java.io.File;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.DownloadTestCase;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.LWSIntegrationServices;
import com.limegroup.gnutella.downloader.LWSIntegrationServicesImpl;
import com.limegroup.gnutella.downloader.VerifyingFile;

public class LowLevelLWSDownloadTest extends DownloadTestCase {

    private static final Log LOG = LogFactory.getLog(LowLevelLWSDownloadTest.class);

    public LowLevelLWSDownloadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LowLevelLWSDownloadTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    // -----------------------------------------------------------
    // Tests
    // -----------------------------------------------------------

    public void testSimpleDownload() throws Exception {
        RemoteFileDesc rfd = services.createRemoteFileDescriptor(constants.FILE, constants.URL, constants.LENGTH);
        RemoteFileDesc[] rfds = { rfd };
        runGenericLWSTest(rfds);
    }

    // -----------------------------------------------------------
    // Misc
    // -----------------------------------------------------------

    private final LWSDownloadTestConstants constants = new LWSDownloadTestConstants();
    private LWSIntegrationServicesImpl services;
    private SimpleWebServer server;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        services = (LWSIntegrationServicesImpl)injector.getInstance(LWSIntegrationServices.class);
        services.setDownloadPrefix(constants.HOST + ":" + constants.PORT);
        
        server = new SimpleWebServer(constants);
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    /**
     * This is similar to tGeneric but lacks the descriptive name. Also, it has
     * a different way of testing for completion, since we are saving into the
     * LWS location.
     */
    private void runGenericLWSTest(RemoteFileDesc[] rfds) throws Exception {

        for (RemoteFileDesc rfd : rfds) {
            services.createDownloader(rfd, _storeDir);
        }

        waitForComplete();
        boolean isComplete = true;
        for (RemoteFileDesc rfd : rfds) {
            File f = new File(_storeDir, rfd.getFileName());
            isComplete &= isComplete(f, rfd.getSize());
        }
        if (isComplete)
            LOG.debug("pass" + "\n");
        else
            fail("FAILED: complete corrupt");

        IncompleteFileManager ifm = downloadManager.getIncompleteFileManager();
        for (int i = 0; i < rfds.length; i++) {
            File incomplete = ifm.getFile(rfds[i]);
            VerifyingFile vf = ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
    }
}
