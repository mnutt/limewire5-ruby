package com.limegroup.gnutella.uploader;

import junit.framework.Test;

public class TLSUploadTest extends UploadTest {

    public TLSUploadTest(String name) {
        super(name);
        
        this.protocol = "tls";
    }
    
    public static Test suite() {
        return buildTestSuite(TLSUploadTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
    
}
