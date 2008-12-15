package com.limegroup.gnutella.dht.db;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTControllerStub;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.NullDHTController;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class PushProxiesPublisherTest extends LimeTestCase {

    private NetworkManagerStub networkManagerStub;
    private PushProxiesPublisher pushProxiesPublisher;
    private Mockery context;

    private Runnable publishingRunnable;
    private ConnectionManager connectionManager;
    
    public PushProxiesPublisherTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushProxiesPublisherTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        connectionManager = context.mock(ConnectionManager.class);
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
                bind(ConnectionManager.class).toInstance(connectionManager);
            }
        });
        pushProxiesPublisher = injector.getInstance(PushProxiesPublisher.class);
    }
    
    /**
     * An integration test that makes sure we create a storable for a 
     * non-firewalled peer.
     */
    public void testValueToPublishForNonFirewalledPeer() {
        networkManagerStub.setAcceptedIncomingConnection(true);
        assertTrue(NetworkUtils.isValidIpPort(new IpPortImpl(networkManagerStub.getAddress(), networkManagerStub.getPort())));
        
        PushProxiesValue value = pushProxiesPublisher.getValueToPublish();
        assertNull("First value should be null since not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        Connectable expected = new ConnectableImpl(new IpPortImpl(networkManagerStub.getAddress(), networkManagerStub.getPort()), networkManagerStub.isOutgoingTLSEnabled());
        assertEquals(0, IpPort.IP_COMPARATOR.compare(expected, value.getPushProxies().iterator().next()));
    }
    
    /**
     * When the set of push proxies or the fwt capability of this peer change it 
     * should publish its updated info into the DHT the next time the StorableModel
     * is asked for them.
     */
    public void testValueToPublishWhenProxiesValueHasChanged() throws Exception {
        
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setSupportsFWTVersion(1);

        PushProxiesValue value = pushProxiesPublisher.getValueToPublish();
        assertNull("First value should be null, since not considered stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        
        Connectable expected = new ConnectableImpl(new IpPortImpl(networkManagerStub.getAddress(), networkManagerStub.getPort()), networkManagerStub.isOutgoingTLSEnabled());
        assertEquals(0, IpPort.IP_COMPARATOR.compare(expected, value.getPushProxies().iterator().next()));
        assertEquals(1, value.getFwtVersion());
        
        // change fwt support status so we should get a different pushproxy value 
        // for ourselves that needs to be republished
        networkManagerStub.setSupportsFWTVersion(2);
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("first value after change should be null, since not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertEquals(2, value.getFwtVersion());
    }
    
    public void testValueToPublishDoesNotChangeWhenPushProxiesChange() throws Exception {

        final IpPort proxy1 = new IpPortImpl("199.49.4.4", 4545);
        final IpPort proxy2 = new IpPortImpl("205.2.1.1", 1000);
        final IpPort proxy3 = new IpPortImpl("111.34.4.4", 1010);
        final IpPort proxy4 = new IpPortImpl("111.34.4.4", 2020);
        
        final IpPortSet proxies = new IpPortSet(proxy1, proxy2, proxy3);
        
        context.checking(new Expectations() {{
            allowing(connectionManager).getPushProxies();
            will(returnValue(proxies));
        }});
        
        assertFalse(networkManagerStub.acceptedIncomingConnection());
        
        PushProxiesValue value = pushProxiesPublisher.getValueToPublish();
        assertNull("First value should be null, since not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertEquals(new IpPortSet(proxy1, proxy2, proxy3), value.getPushProxies());
        
        // one new proxy, two old proxies
        proxies.remove(proxy3);
        proxies.add(proxy4);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should be null, since changed and not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should still be null, since mostly the same proxies", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should still be null, since mostly the same proxies", value);
        
        // only one old proxy
        proxies.remove(proxy2);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should be null, since changed and not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertEquals("new proxies should be published", new IpPortSet(proxy1, proxy4), value.getPushProxies());
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("no changes, should be null", value);
    }
    
    public void testValueIsPublishedAfterValuesExpire() throws Exception {
        final IpPort proxy1 = new IpPortImpl("199.49.4.4", 4545);
        final IpPort proxy2 = new IpPortImpl("205.2.1.1", 1000);
        final IpPort proxy3 = new IpPortImpl("111.34.4.4", 1010);
        
        final IpPortSet proxies = new IpPortSet(proxy1, proxy2, proxy3);
        
        context.checking(new Expectations() {{
            allowing(connectionManager).getPushProxies();
            will(returnValue(proxies));
        }});
       
        PrivilegedAccessor.setValue(DatabaseSettings.VALUE_REPUBLISH_INTERVAL, "value", 100);
        
        PushProxiesValue value = pushProxiesPublisher.getValueToPublish();
        assertNull("First value should be null, since not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertEquals(new IpPortSet(proxy1, proxy2, proxy3), value.getPushProxies());
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("Value should be null, since just published and not changed", value);
        
        Thread.sleep(200);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertEquals("should not be null, should have been republished due to timeout",
                new IpPortSet(proxy1, proxy2, proxy3), value.getPushProxies());
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("Value should be null, since just published and not changed", value);
    }
    
    public void testValueChangingInBetweenConsecutiveCallsToGetValueToPublish() {
        networkManagerStub.setAcceptedIncomingConnection(true);
        
        PushProxiesValue value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should be null, since not stable as first value", value);
        
        networkManagerStub.setPort(54545);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should be null, since push proxies should have changed and not stable", value);
        
        networkManagerStub.setCanDoFWT(true);
        networkManagerStub.setSupportsFWTVersion(2);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should be null, since push proxies should have changed and not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNotNull("value should not be null, since stable now", value);
    }
    public void testDHTEventHandlerIsInstalledAndTriggersRunnable() {
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setSupportsFWTVersion(1);
        
        final MojitoDHT mojitoDHT = context.mock(MojitoDHT.class);
        final ScheduledExecutorService executorService = context.mock(ScheduledExecutorService.class);
        final ScheduledFuture future = context.mock(ScheduledFuture.class);
        final DHTManager dhtManager = context.mock(DHTManager.class);
        
        
        context.checking(new Expectations() {{
            one(executorService).scheduleAtFixedRate(with(any(Runnable.class)), 
                    with(any(Long.class)),
                    with(equal(DHTSettings.PUSH_PROXY_STABLE_PUBLISHING_INTERVAL.getValue())),
                    with(equal(TimeUnit.MILLISECONDS)));
            will(new CustomAction("save runnable and return future action") {
                public Object invoke(Invocation invocation) throws Throwable {
                    publishingRunnable = (Runnable)invocation.getParameter(0);
                    return future;
                }
            });
        }});
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
                // not binding mocked executor, since it is used in all kinds of places
            } 
        });
        
        DHTSettings.PUBLISH_PUSH_PROXIES.setValue(true);
        
        pushProxiesPublisher = new PushProxiesPublisher(injector.getInstance(PushProxiesValueFactory.class), executorService, dhtManager);

        pushProxiesPublisher.handleDHTEvent(new DHTEvent(new DHTControllerStub(mojitoDHT, DHTMode.PASSIVE), Type.CONNECTED));
        
        assertNotNull(publishingRunnable);

        context.assertIsSatisfied();
        
        // test publishing 
        context.checking(new Expectations() {{
            one(dhtManager).put(with(any(KUID.class)), with(any(DHTValue.class)));
        }});

        publishingRunnable.run();

        // publishing only takes place the second time around
        publishingRunnable.run();
        
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            one(future).cancel(false);
        }});
        
        pushProxiesPublisher.handleDHTEvent(new DHTEvent(new NullDHTController(), Type.STOPPED));
        
        context.assertIsSatisfied();
    }
    
}
