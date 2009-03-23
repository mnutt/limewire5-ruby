package com.limegroup.gnutella;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.limegroup.gnutella.uploader.AltLocUploadTest;
import com.limegroup.gnutella.uploader.FileRequestHandlerTest;
import com.limegroup.gnutella.uploader.HTTPUploaderTest;
import com.limegroup.gnutella.uploader.HttpPushRequestHandlerTest;
import com.limegroup.gnutella.uploader.PushUploadTest;
import com.limegroup.gnutella.uploader.UploadQueueingTest;
import com.limegroup.gnutella.uploader.UploadSlotManagerTest;
import com.limegroup.gnutella.uploader.UploadTest;

public class AllUploadTests {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        //$JUnit-BEGIN$
        suite.addTest(FileRequestHandlerTest.suite());
        suite.addTest(HttpPushRequestHandlerTest.suite());
        suite.addTest(HTTPUploaderTest.suite());
        suite.addTest(UploadSlotManagerTest.suite());
        suite.addTest(UrnHttpRequestTest.suite());
        suite.addTest(HTTPAcceptorTest.suite());
        suite.addTest(UploadTest.suite());
        suite.addTest(PushUploadTest.suite());
        suite.addTest(AltLocUploadTest.suite());
        suite.addTest(UploadQueueingTest.suite());
        //$JUnit-END$
        return suite;
    }

}
