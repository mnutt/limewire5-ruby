package com.limegroup.gnutella;

import java.util.Collections;
import java.util.Set;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;

public class SelfEndpointTest extends BaseTestCase {

    private NetworkManagerStub networkManagerStub;
    private SelfEndpoint selfEndpoint;
    private Mockery context;
    private ConnectionManager connectionManager;
    private Injector injector;

    public SelfEndpointTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(SelfEndpointTest.class); 
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        connectionManager = context.mock(ConnectionManager.class);
        networkManagerStub = new NetworkManagerStub();
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
                bind(ConnectionManager.class).toInstance(connectionManager);
            }
        });
        selfEndpoint = injector.getInstance(SelfEndpoint.class);
    }
    
    /**
     * Ensures that new values in the network manager are reflected immediately
     * in the self endpoint.
     */
    public void testNetworkManagerChangesAreReflected() {
        // for fwt version
        networkManagerStub.setSupportsFWTVersion(0);
        assertEquals(0, selfEndpoint.getFWTVersion());
        networkManagerStub.setSupportsFWTVersion(1);
        assertEquals(1, selfEndpoint.getFWTVersion());
        
        // for external address
        networkManagerStub.setExternalAddress(new byte[4]);
        assertEquals(RemoteFileDesc.BOGUS_IP, selfEndpoint.getAddress());
        networkManagerStub.setExternalAddress(new byte[] { (byte)192, (byte)168, 0, 1 });
        assertEquals(RemoteFileDesc.BOGUS_IP, selfEndpoint.getAddress());
    }
    
    /**
     * This is a whitebox test, if it doesn't make sense, feel free to change
     * the implementation.
     */
    public void testPrivateNetworkAddressResultsInBogusIp() {
        networkManagerStub.setExternalAddress(new byte[] { (byte)192, (byte)168, 0, 1 });
        assertEquals(RemoteFileDesc.BOGUS_IP, selfEndpoint.getAddress());
    }
    
    /**
     * Ensures proxies are encoded in http string. 
     */
    public void testHttpStringValueContainsProxiesWithoutValidExternalAddress() throws Exception {
        networkManagerStub.setExternalAddress(new byte[] { (byte)192, (byte)168, 0, 1 });
        final Set<? extends Connectable> proxies = Collections.singleton(new ConnectableImpl("222.222.222.222", 2222, false));
        context.checking(new Expectations() {{
            allowing(connectionManager).getPushProxies();
            will(returnValue(proxies));
        }});

        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        String expected = new GUID(applicationServices.getMyGUID()).toHexString();
        expected += ";";
        expected += HTTPHeaderUtils.encodePushProxies(proxies, ";", PushEndpoint.MAX_PROXIES);
        System.out.println(expected);
        assertEquals(expected, selfEndpoint.httpStringValue());
    }
}