package com.limegroup.gnutella.net.address;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.BlockingAddressResolutionObserver;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.util.BaseTestCase;


public class SameNATAddressResolverTest extends BaseTestCase {

    private NetworkManagerStub networkManagerStub;
    private SameNATAddressResolver resolver;
    private Mockery context;
    private EventBroadcaster eventBroadcaster;

    public SameNATAddressResolverTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SameNATAddressResolverTest.class);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        networkManagerStub = new NetworkManagerStub();
        eventBroadcaster = context.mock(EventMulticaster.class);
        resolver = new SameNATAddressResolver(networkManagerStub, eventBroadcaster);
        networkManagerStub.setExternalAddress(new byte[] { (byte)129, 0, 0, 1 });
        networkManagerStub.setAddress(new byte[] { (byte)192, (byte)168, 0, 1});
    }
    
    public void testCanResolve() throws Exception {
        assertTrue(resolver.canResolve(createAddress("129.0.0.1", "192.168.0.5")));
        assertFalse(resolver.canResolve(createAddress("129.0.0.1", "10.0.0.1")));
        assertFalse(resolver.canResolve(createAddress("128.0.0.1", "192.168.0.5")));
    }
    
    @SuppressWarnings("unchecked")
    public void testConnectivityEventOnlyFiredOnce() {
        context.checking(new Expectations() {{
            one(eventBroadcaster).broadcast(with(any(ConnectivityChangeEvent.class)));
        }});
        resolver.handleEvent(new AddressEvent(context.mock(Address.class), AddressEvent.Type.ADDRESS_CHANGED));
        resolver.handleEvent(new AddressEvent(context.mock(Address.class), AddressEvent.Type.ADDRESS_CHANGED));
        context.assertIsSatisfied();
    }

    public void testResolvesSameNatAddressToConnectable() throws Exception {
        BlockingAddressResolutionObserver observer = new BlockingAddressResolutionObserver();
        FirewalledAddress behindSameNatAddress = createAddress("129.0.0.1", "192.168.0.2");
        resolver.resolve(behindSameNatAddress, observer);
        Address resolvedAddress = observer.getAddress();
        Connectable connectable = (Connectable) resolvedAddress;
        assertEquals("192.168.0.2", connectable.getAddress());
    }
    
    public void testDoesNotResolveDifferentSiteLocalNetwork() throws Exception {
        BlockingAddressResolutionObserver observer = new BlockingAddressResolutionObserver();
        FirewalledAddress samePublicAddressDifferentSiteLocalNetwork = createAddress("129.0.0.1", "172.16.0.5");
        try {
            resolver.resolve(samePublicAddressDifferentSiteLocalNetwork, observer);
            fail("expected assertion error");
        } catch (AssertionError ae) {
        }
    }
    
    public void testDoesNotResolveDifferentPublicAddress() throws Exception {
        BlockingAddressResolutionObserver observer = new BlockingAddressResolutionObserver();
        FirewalledAddress samePublicAddressDifferentSiteLocalNetwork = createAddress("128.0.0.1", "191.168.0.2");
        try {
            resolver.resolve(samePublicAddressDifferentSiteLocalNetwork, observer);
            fail("expected assertion error");
        } catch (AssertionError ae) {
        }
    }
    
    private FirewalledAddress createAddress(String publicAddress, String privateAddress) throws Exception {
        return new FirewalledAddress(new ConnectableImpl(publicAddress, 5555, false), 
                new ConnectableImpl(privateAddress, 6666, true),
                null, null, 0);
    }

}
