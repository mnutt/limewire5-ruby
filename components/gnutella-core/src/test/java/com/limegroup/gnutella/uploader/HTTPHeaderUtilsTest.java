package com.limegroup.gnutella.uploader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.limewire.collection.BitNumbers;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.StrictIpPortSet;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionManagerImpl;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.simpp.SimppManager;

public class HTTPHeaderUtilsTest extends BaseTestCase {
    
    private StubConnectionManager connectionManager;
    private AltLocManager altLocManager;
    private HTTPHeaderUtils httpHeaderUtils;
    private AlternateLocationFactory alternateLocationFactory;
    private NetworkManagerStub networkManager;

    public HTTPHeaderUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPHeaderUtilsTest.class);
    }

    @Override
    public void setUp() throws Exception {
        networkManager = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManager);
                bind(ConnectionManager.class).to(StubConnectionManager.class);
            }
        });
        alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        connectionManager = (StubConnectionManager) injector.getInstance(ConnectionManager.class);
        connectionManager.proxies = new StrictIpPortSet<Connectable>();
        altLocManager = new AltLocManager();
        httpHeaderUtils = injector.getInstance(HTTPHeaderUtils.class);
    }

    @Override
    public void tearDown() throws Exception {
    }
        
    public void testWritesAltsWhenEmpty() throws Exception {
        StubAltLocTracker altLocTracker = new StubAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");

        altLocTracker.setNextSetOfAltsToSend(new ArrayList<DirectAltLoc>());
        httpHeaderUtils.addAltLocationsHeader(response, altLocTracker, altLocManager);
        assertNull(response.getLastHeader("X-Alt"));
    }
    
    public void testWritesAltsNoTLS() throws Exception {
        StubAltLocTracker altLocTracker = new StubAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");

        altLocTracker.setNextSetOfAltsToSend(altsFor("1.2.3.4:5", "2.3.4.6", "7.3.2.1", "2.1.5.3:6201", "1.2.65.2"));
        httpHeaderUtils.addAltLocationsHeader(response, altLocTracker, altLocManager);
        Header header = response.getLastHeader("X-Alt");
        assertNotNull("Missing X-Alt header", header);
        assertEquals("1.2.3.4:5,2.3.4.6,7.3.2.1,2.1.5.3:6201,1.2.65.2", header.getValue());
    }
    
    public void testWritesAltsWithTLS() throws Exception {
        StubAltLocTracker altLocTracker = new StubAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        
        altLocTracker.setNextSetOfAltsToSend(altsFor("1.2.3.4:5", "T2.3.4.6", "T7.3.2.1", "2.1.5.3:6201", "T1.2.65.2"));
        httpHeaderUtils.addAltLocationsHeader(response, altLocTracker, altLocManager);
        Header header = response.getLastHeader("X-Alt");
        assertNotNull("Missing X-Alt header", header);
        assertEquals("tls=68,1.2.3.4:5,2.3.4.6,7.3.2.1,2.1.5.3:6201,1.2.65.2", header.getValue());
    }
    
    public void testWritePushProxiesWhenEmpty() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        httpHeaderUtils.addProxyHeader(response);
        assertNull(response.getLastHeader("X-Push-Proxy"));
    }
    
    public void testWritePushProxiesNoTLS() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        httpHeaderUtils.addProxyHeader(response);
        
        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("1.2.3.4:5,2.3.4.5:6", header.getValue());
    }
    
    public void testWritePushProxiesSomeTLS() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        httpHeaderUtils.addProxyHeader(response);
        
        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("pptls=A,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7", header.getValue());
    }
    
    public void testWritePushProxiesLimitsAt4() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        connectionManager.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        connectionManager.proxies.add(new ConnectableImpl("5.6.7.8", 9, false));
        httpHeaderUtils.addProxyHeader(response);

        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", header.getValue());
    }
    
    public void testWritePushProxiesLimitsAt4NoTLSIfLater() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        connectionManager.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        connectionManager.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        httpHeaderUtils.addProxyHeader(response);

        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", header.getValue());
    }
    
    public void testWritePushProxiesLimitsAt4TLSRight() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, true));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        connectionManager.proxies.add(new ConnectableImpl("4.5.6.7", 8, true));
        connectionManager.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        httpHeaderUtils.addProxyHeader(response);
        
        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("pptls=F,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", header.getValue());
    }
    
    public void testGetFullFirewalledHeaders() throws Exception {
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, true));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        connectionManager.proxies.add(new ConnectableImpl("4.5.6.7", 8, true));
        connectionManager.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        networkManager.setCanDoFWT(true);
        networkManager.setStableUDPPort(4545);
        List<Header> headers = httpHeaderUtils.getFirewalledHeaders();
        assertEquals(2, headers.size());
        assertEquals("pptls=F,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", headers.get(0).getValue());
        assertEquals("4545", headers.get(1).getValue());
    }
    
    public void testGetEmptyFirewalledHeaders() {
        assertTrue(httpHeaderUtils.getFirewalledHeaders().isEmpty());
    }
    
    public void testGetTLSIndices() throws Exception {
       Collection<? extends IpPort> proxies = Arrays.asList(new ConnectableImpl("localhost", 4545, true),
               new IpPortImpl("helloword.com", 6666),
               new ConnectableImpl("192.168.0.1", 7777, true));
       BitNumbers bn = HTTPHeaderUtils.getTLSIndices(proxies);
       assertFalse(bn.isEmpty());
       assertTrue(bn.isSet(0));
       assertFalse(bn.isSet(1));
       assertTrue(bn.isSet(2));
       assertEquals(3, bn.getMax());
       
       bn = HTTPHeaderUtils.getTLSIndices(proxies, 2);
       assertFalse(bn.isEmpty());
       assertTrue(bn.isSet(0));
       assertFalse(bn.isSet(1));
       assertFalse(bn.isSet(2));
       assertEquals(2, bn.getMax());
       
       // empty set
       proxies = Collections.emptyList();
       bn = HTTPHeaderUtils.getTLSIndices(proxies);
       assertTrue(bn.isEmpty());
    }
    
    public void testFirewalledHeadersNoFWTPort() throws Exception {
        connectionManager.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        connectionManager.proxies.add(new ConnectableImpl("5.6.7.8", 9, false));
        networkManager.setCanDoFWT(false);
        networkManager.setStableUDPPort(4545);
        List<Header> headers = httpHeaderUtils.getFirewalledHeaders();
        assertEquals(1, headers.size());
    }
    
    private Collection<DirectAltLoc> altsFor(String... locs) throws Exception {
        List<DirectAltLoc> alts = new ArrayList<DirectAltLoc>(locs.length);
        for(String loc : locs) {
            boolean tls = false;
            if(loc.startsWith("T")) {
                tls = true;
                loc = loc.substring(1);
            }
            alts.add((DirectAltLoc)alternateLocationFactory.create(loc, UrnHelper.SHA1, tls));
        }
        return alts;
    }
        
    /** A fake ConnectionManager with custom proxies. */
    @Singleton
    private static class StubConnectionManager extends ConnectionManagerImpl {

        @Inject
        public StubConnectionManager(NetworkManager networkManager,
                Provider<HostCatcher> hostCatcher,
                @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                Provider<SimppManager> simppManager,
                CapabilitiesVMFactory capabilitiesVMFactory,
                RoutedConnectionFactory managedConnectionFactory,
                Provider<QueryUnicaster> queryUnicaster,
                SocketsManager socketsManager, ConnectionServices connectionServices,
                Provider<NodeAssigner> nodeAssigner,  
                 Provider<IPFilter> ipFilter,
                ConnectionCheckerManager connectionCheckerManager,
                PingRequestFactory pingRequestFactory,
                NetworkInstanceUtils networkInstanceUtils) {
            super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor, simppManager,
                    capabilitiesVMFactory, managedConnectionFactory, queryUnicaster,
                    socketsManager, connectionServices, nodeAssigner, ipFilter, connectionCheckerManager,
                    pingRequestFactory, networkInstanceUtils);
            
        }

        private Set<Connectable> proxies;
        
        @Override
        public Set<Connectable> getPushProxies() {
            return proxies;
        }
        
    }
    
}
