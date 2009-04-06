package com.limegroup.gnutella.uploader;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.apache.http.message.BasicHeader;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import com.google.inject.Injector;
import com.limegroup.gnutella.altlocs.AltLocListener;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.http.AltLocHeaderInterceptor;

public class AltLocHeaderInterceptorTest extends LimeTestCase {

    private AltLocManager altLocManager;
    private AlternateLocationFactory alternateLocationFactory;

    public AltLocHeaderInterceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AltLocHeaderInterceptorTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        
        altLocManager = injector.getInstance(AltLocManager.class);
        
        alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
    }
    
    private void check(AlternateLocation loc, String ip, int port, boolean tls) {
        DirectAltLoc d = (DirectAltLoc)loc;
        IpPort host = d.getHost();
        assertEquals(ip, host.getAddress());
        assertEquals(port, host.getPort());
        if(tls) {
            assertInstanceof(Connectable.class, host);
            assertTrue(((Connectable)host).isTLSCapable());
        } else {
            if(host instanceof Connectable)
                assertFalse(((Connectable)host).isTLSCapable());
        }
    }
    
    public void testReadAltLocsWithTLSNotifiesListenersAndTracker() throws Exception {
        String key = "X-Alt";
        String value = "tls=3D8,1.2.3.4:5213,5.4.3.2:1,8.3.2.1,6.3.2.1,5.2.1.3:52,5.3.2.6:18,43.41.42.42:41,41.42.42.43:42,89.98.89.98:89,98.89.98.89:98\r\n";
       
        final AtomicInteger received = new AtomicInteger(0);
        AltLocListener listener = new AltLocListener() {
            public void locationAdded(AlternateLocation loc) {
                switch(received.getAndIncrement()) {
                case 0: check(loc, "1.2.3.4",     5213, false); break;
                case 1: check(loc, "5.4.3.2",     1,    false); break;
                case 2: check(loc, "8.3.2.1",     6346, true);  break;
                case 3: check(loc, "6.3.2.1",     6346, true);  break;
                case 4: check(loc, "5.2.1.3",     52,   true);  break;
                case 5: check(loc, "5.3.2.6",     18,   true);  break;
                case 6: check(loc, "43.41.42.42", 41,   false); break;
                case 7: check(loc, "41.42.42.43", 42,   true);  break;
                case 8: check(loc, "89.98.89.98", 89,   true);  break;
                case 9: check(loc, "98.89.98.89", 98,   false); break;
                default: throw new IllegalStateException("unexpected loc: " + loc + ", i: " + received);
                }
            }
        };
        
        MockHTTPUploader uploader = new MockHTTPUploader();
        AltLocHeaderInterceptor interceptor = new AltLocHeaderInterceptor(uploader, altLocManager, alternateLocationFactory);
        List<AlternateLocation> addedLocs = ((StubAltLocTracker)uploader.getAltLocTracker()).getAddedLocs();
        
        altLocManager.addListener(UrnHelper.SHA1, listener);
        try {
            interceptor.process(new BasicHeader(key, value), null);
        } finally {
            altLocManager.removeListener(UrnHelper.SHA1, listener);
        }
        
        assertEquals(10, received.get()); // incremented one+
        
        assertEquals(10, addedLocs.size());
        for(int i = 0; i < 10; i++) {
            AlternateLocation loc = addedLocs.get(i);
            switch(i) {
            case 0: check(loc, "1.2.3.4",     5213, false); break;
            case 1: check(loc, "5.4.3.2",     1,    false); break;
            case 2: check(loc, "8.3.2.1",     6346, true);  break;
            case 3: check(loc, "6.3.2.1",     6346, true);  break;
            case 4: check(loc, "5.2.1.3",     52,   true);  break;
            case 5: check(loc, "5.3.2.6",     18,   true);  break;
            case 6: check(loc, "43.41.42.42", 41,   false); break;
            case 7: check(loc, "41.42.42.43", 42,   true);  break;
            case 8: check(loc, "89.98.89.98", 89,   true);  break;
            case 9: check(loc, "98.89.98.89", 98,   false); break;
            default: throw new IllegalStateException("unexpected loc: " + loc + ", i: " + received);
            }
        }
    }
    
    
}
