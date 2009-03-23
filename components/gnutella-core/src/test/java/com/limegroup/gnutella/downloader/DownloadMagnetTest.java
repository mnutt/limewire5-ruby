package com.limegroup.gnutella.downloader;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.util.MojitoUtils;
import org.limewire.rudp.RUDPUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTTestUtils;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.dht.db.PushProxiesValue;
import com.limegroup.gnutella.dht.db.PushProxiesValueImpl;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Integration tests for magnet downloads.
 */
public class DownloadMagnetTest extends DownloadTestCase {

    private TestUDPAcceptorFactoryImpl testUDPAcceptorFactoryImpl;
    
    private final int PUSH_PROXY_PORT = 6666;

    private DHTManager dhtManager;

    private HostCatcher hostCatcher;
    
    public DownloadMagnetTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(DownloadMagnetTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        DHTSettings.DISABLE_DHT_USER.setValue(false);
        DHTSettings.DISABLE_DHT_NETWORK.setValue(false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        DHTTestUtils.setSettings(NetworkSettings.PORT.getValue());
        
        PrivilegedAccessor.setValue(DHTSettings.DHT_NODE_FETCHER_TIME, "value", 500L);
        super.setUp();
        networkManager.setCanReceiveSolicited(true);
        dhtManager = injector.getInstance(DHTManager.class);
        DHTTestUtils.setLocalIsPrivate(injector, false);
        // make sure address is updated which isn't done by mock network manager
        dhtManager.addressChanged();
        testUDPAcceptorFactoryImpl = injector.getInstance(TestUDPAcceptorFactoryImpl.class);
        hostCatcher = injector.getInstance(HostCatcher.class);
    }
    
    /**
     * 
     * @throws Exception
     */
    public void testDirectAlternateGuidLocationIsFoundAndDownloadedFrom() throws Exception {
        GUID guid = new GUID();

        MagnetOptions magnet = MagnetOptions.createMagnet(createFileDetails(), null, guid.bytes());
 
        List<MojitoDHT> dhts = Collections.emptyList();
        try {
             dhts = MojitoUtils.createBootStrappedDHTs(1);

             MojitoDHT node  = dhts.get(0);
             assertTrue(node.isBootstrapped());
             
             ExtendedEndpoint endpoint = new ExtendedEndpoint((InetSocketAddress)node.getContactAddress());
             endpoint.setDHTMode(DHTMode.ACTIVE);
             endpoint.setDHTVersion(dhtManager.getVersion().shortValue());
             
             hostCatcher.add(endpoint, true);

             publishPushProxyForGuid(node, guid, PORTS[0], PORTS[0]);
             
             DHTTestUtils.waitForBootStrap(dhtManager, 5);

             Downloader downloader = downloadServices.download(magnet, true, saveDir, savedFileName);
             
             tGeneric(downloader, null, null);
             
        } finally {
            IOUtils.close(dhts);
        }
    }
    
    public void testFirewalledAlternateGuidLocationIsFoundAndDownloadedFrom() throws Exception {
        GUID guid = new GUID();

        MagnetOptions magnet = MagnetOptions.createMagnet(createFileDetails(), null, guid.bytes());
                
        List<MojitoDHT> dhts = Collections.emptyList();
        try {
             dhts = MojitoUtils.createBootStrappedDHTs(1);
        
             MojitoDHT node  = dhts.get(0);
             
             ExtendedEndpoint endpoint = new ExtendedEndpoint((InetSocketAddress)node.getContactAddress());
             endpoint.setDHTMode(DHTMode.ACTIVE);
             endpoint.setDHTVersion(dhtManager.getVersion().shortValue());
             
             hostCatcher.add(endpoint, true);

             publishPushProxyForGuid(node, guid, 5555 /* just a random different port */, PUSH_PROXY_PORT);
             
             DHTTestUtils.waitForBootStrap(dhtManager, 5);

             TestUploader uploader = injector.getInstance(TestUploader.class);
             uploader.start("push uploader");
             
             TestUDPAcceptor testUDPAcceptor = testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PUSH_PROXY_PORT, networkManager.getPort(), "filename", uploader, guid, _currentTestName);
             
             Downloader downloader = downloadServices.download(magnet, true, saveDir, savedFileName);
             
             tGeneric(downloader, null, null);
             
             testUDPAcceptor.shutdown();
        } finally {
            IOUtils.close(dhts);
        }
    }
    
    private void publishPushProxyForGuid(MojitoDHT dht, GUID guid, int proxyPort, int clientPort) throws Exception {
        PushProxiesValue value = new PushProxiesValueImpl(dht.getVersion(), guid.bytes(), (byte) 0, RUDPUtils.VERSION, clientPort, Collections.singleton(new IpPortImpl("127.0.0.1", proxyPort)));
        dht.put(KUIDUtils.toKUID(guid), value).get();
    }
    
    private FileDetails createFileDetails() {
        
        FileDetails fileDetails = new FileDetails() {

            public String getFileName() {
                return "filename";
            }

            public long getSize() {
                return TestFile.length();
            }

            public URN getSHA1Urn() {
                return TestFile.hash();
            }

            public Set<URN> getUrns() {
                return null;
            }

            public LimeXMLDocument getXMLDocument() {
                return null;
            }

            public long getIndex() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public long getCreationTime() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        
       return fileDetails;
    }
}
