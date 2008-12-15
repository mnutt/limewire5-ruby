package com.limegroup.gnutella.uploader;

import junit.framework.Test;

public class TLSBrowseTest extends BrowseTest {
    
    public TLSBrowseTest(String name) {
        super(name);
        
        protocol = "tls";
    }

    public static Test suite() {
        return buildTestSuite(TLSBrowseTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

}
