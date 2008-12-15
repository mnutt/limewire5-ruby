package com.limegroup.gnutella.downloader;

import java.net.URL;

import junit.framework.Test;

import org.limewire.io.ConnectableImpl;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;

public class URLRemoteFileDescImplTest extends BaseTestCase {
    
    public URLRemoteFileDescImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(URLRemoteFileDescImplTest.class);
    }
    
    public void testFileDescsWithSameURLAreEqual() throws Exception {
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        URL url = new URL("http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        RemoteFileDesc desc1 = new UrlRemoteFileDescImpl(new ConnectableImpl("host1.com", 80, false), "filename", 1000, new UrnSet(urn), url, null);
        RemoteFileDesc desc2 = new UrlRemoteFileDescImpl(new ConnectableImpl("host1.com", 80, false), "filename", 1000, new UrnSet(urn), url, null);
        assertEquals(desc1, desc2);
        assertEquals(desc1.hashCode(), desc2.hashCode());
    }

}
