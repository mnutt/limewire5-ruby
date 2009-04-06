package com.limegroup.gnutella.bootstrap;

import static com.limegroup.gnutella.bootstrap.BootstrapperImpl.MULTICAST_INTERVAL;
import static com.limegroup.gnutella.bootstrap.BootstrapperImpl.TCP_FALLBACK_DELAY;
import static com.limegroup.gnutella.bootstrap.BootstrapperImpl.TCP_INTERVAL;
import static com.limegroup.gnutella.bootstrap.BootstrapperImpl.UDP_FALLBACK_DELAY;
import static com.limegroup.gnutella.bootstrap.BootstrapperImpl.UDP_INTERVAL;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.util.BaseTestCase;

import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.MulticastService;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;

public class BootstrapperImplTest extends BaseTestCase {

    Mockery context;
    ConnectionServices connectionServices;
    MulticastService multicastService;
    PingRequestFactory pingRequestFactory;
    Bootstrapper.Listener listener;
    TcpBootstrap tcpBootstrap;
    UDPHostCache udpHostCache;
    PingRequest pingRequest;
    BootstrapperImpl bootstrapper;

    public BootstrapperImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BootstrapperImplTest.class);
    }

    @Override
    public void setUp() {
        // Reduce the timing constants so the tests run faster
        MULTICAST_INTERVAL /= 10;
        TCP_FALLBACK_DELAY /= 10;
        TCP_INTERVAL /= 10;
        UDP_FALLBACK_DELAY /= 10;
        UDP_INTERVAL /= 10;
        ConnectionSettings.BOOTSTRAP_DELAY.setValue(
                ConnectionSettings.BOOTSTRAP_DELAY.getValue() / 10);

        context = new Mockery();
        connectionServices = context.mock(ConnectionServices.class);
        multicastService = context.mock(MulticastService.class);
        Provider<MulticastService> mcast = new Provider<MulticastService>() {
            @Override
            public MulticastService get() {
                return multicastService;
            }
        };
        pingRequestFactory = context.mock(PingRequestFactory.class);
        listener = context.mock(Bootstrapper.Listener.class);
        tcpBootstrap = context.mock(TcpBootstrap.class);
        udpHostCache = context.mock(UDPHostCache.class);
        pingRequest = context.mock(PingRequest.class);
        bootstrapper = new BootstrapperImpl(connectionServices,
                mcast, pingRequestFactory, listener, tcpBootstrap,
                udpHostCache);
        ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.setValue(false);
    }

    @Override
    public void tearDown() {
        // Restore the timing constants
        MULTICAST_INTERVAL *= 10;
        TCP_FALLBACK_DELAY *= 10;
        TCP_INTERVAL *= 10;
        UDP_FALLBACK_DELAY *= 10;
        UDP_INTERVAL *= 10;
        ConnectionSettings.BOOTSTRAP_DELAY.setValue(
                ConnectionSettings.BOOTSTRAP_DELAY.getValue() * 10);
    }

    public void testMulticastTriedFirst() throws Exception {
        context.checking(new Expectations() {{
            // The listener needs hosts
            one(listener).needsHosts();
            will(returnValue(true));
            // The bootstrapper should create and send a multicast ping
            one(pingRequestFactory).createMulticastPing();
            will(returnValue(pingRequest));
            one(multicastService).send(pingRequest);
        }});
        bootstrapper.run();
        context.assertIsSatisfied();
    }

    public void testUDPTriedFirstIfMulticastDisabled() throws Exception {
        ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.setValue(true);
        context.checking(new Expectations() {{
            // The listener needs hosts
            one(listener).needsHosts();
            will(returnValue(true));
            // Multicast is disabled - the bootstrapper should try UDP
            one(udpHostCache).fetchHosts();
            will(returnValue(true));
        }});
        bootstrapper.run();
        context.assertIsSatisfied();
    }

    public void testUDPFallbackDelay() throws Exception {
        context.checking(new Expectations() {{
            // First call should use multicast
            one(listener).needsHosts();
            will(returnValue(true));
            one(pingRequestFactory).createMulticastPing();
            will(returnValue(pingRequest));
            one(multicastService).send(pingRequest);
            // Second call shouldn't use anything (too soon for UDP)
            one(listener).needsHosts();
            will(returnValue(true));
            // Third call should use UDP
            one(listener).needsHosts();
            will(returnValue(true));
            one(udpHostCache).fetchHosts();
            will(returnValue(true));
        }});
        bootstrapper.run(); // Multicast
        Thread.sleep(UDP_FALLBACK_DELAY - 10);
        bootstrapper.run(); // Too soon for UDP
        Thread.sleep(20);
        bootstrapper.run(); // UDP
        context.assertIsSatisfied();
    }

    public void testUDPTriedTwiceBeforeTCP() throws Exception {
        // Sanity check: second UDP attempt comes before first TCP attempt
        assertGreaterThan(UDP_INTERVAL, TCP_FALLBACK_DELAY);
        // Sanity check: second UDP attempt comes before second multicast attempt
        assertGreaterThan(UDP_INTERVAL, MULTICAST_INTERVAL - UDP_FALLBACK_DELAY);
        context.checking(new Expectations() {{
            // First call should use multicast
            one(listener).needsHosts();
            will(returnValue(true));
            one(pingRequestFactory).createMulticastPing();
            will(returnValue(pingRequest));
            one(multicastService).send(pingRequest);
            // Second call should use UDP
            one(listener).needsHosts();
            will(returnValue(true));
            one(udpHostCache).fetchHosts();
            will(returnValue(true));
            // Third call should use UDP again
            one(listener).needsHosts();
            will(returnValue(true));
            one(udpHostCache).fetchHosts();
            will(returnValue(true));
            // Fourth call should use TCP
            one(listener).needsHosts();
            will(returnValue(true));
            one(tcpBootstrap).fetchHosts(listener);
            will(returnValue(true));
        }});
        bootstrapper.run(); // Multicast
        Thread.sleep(UDP_FALLBACK_DELAY + 10);
        bootstrapper.run(); // UDP
        Thread.sleep(UDP_INTERVAL + 10);
        bootstrapper.run(); // UDP again
        Thread.sleep(TCP_FALLBACK_DELAY - UDP_INTERVAL);
        bootstrapper.run(); // TCP
        context.assertIsSatisfied();        
    }

    public void testBootstrapDelay() throws Exception {
        context.checking(new Expectations() {{
            // The listener has hosts but we're not connected yet
            one(listener).needsHosts();
            will(returnValue(false));
            one(connectionServices).isConnected();
            will(returnValue(false));
            // Still not connected but it's too soon to bootstrap
            one(listener).needsHosts();
            will(returnValue(false));
            one(connectionServices).isConnected();
            will(returnValue(false));
            // Give up on the listener's hosts and bootstrap
            one(listener).needsHosts();
            will(returnValue(false));
            one(connectionServices).isConnected();
            will(returnValue(false));
            one(pingRequestFactory).createMulticastPing();
            will(returnValue(pingRequest));
            one(multicastService).send(pingRequest);
        }});
        bootstrapper.run(); // Too soon to bootstrap
        Thread.sleep(ConnectionSettings.BOOTSTRAP_DELAY.getValue() - 10);
        bootstrapper.run(); // Still too soon to bootstrap
        Thread.sleep(20);
        bootstrapper.run(); // Bootstrap
        context.assertIsSatisfied();
    }

    public void testDoesNotBootstrapWhenConnected() throws Exception {
        context.checking(new Expectations() {{
            one(listener).needsHosts();
            will(returnValue(false));
            one(connectionServices).isConnected();
            will(returnValue(true));
        }});
        bootstrapper.run();
        context.assertIsSatisfied();
    }
}
