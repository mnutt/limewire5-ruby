package com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class NodeAssignerTest extends LimeTestCase {
    
    private Injector injector;
    private NodeAssigner nodeAssigner;
    private BandwidthTracker upTracker, downTracker;
    private DHTManager dhtManager;
    private Runnable assignerRunnable;
    private ConnectionServices connectionServices;
    private NetworkManager networkManager;
    private Mockery mockery;
    private ConnectionManager cManager;
    private SearchServices searchServices;
    private final Executor immediateExecutor = new Executor() {
        public void execute(Runnable r) {
            r.run();
        }
    };
    
    public NodeAssignerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NodeAssignerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        setSettings();
        mockery = new Mockery();
        upTracker = mockery.mock(BandwidthTracker.class);
        downTracker = mockery.mock(BandwidthTracker.class);
        dhtManager = mockery.mock(DHTManager.class);
        connectionServices = mockery.mock(ConnectionServices.class);
        networkManager = mockery.mock(NetworkManager.class);
        searchServices = mockery.mock(SearchServices.class);
        cManager = mockery.mock(ConnectionManager.class);
        
        final ScheduledExecutorService ses = new ScheduledExecutorServiceStub() {

            @Override
            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
                if (delay == NodeAssignerImpl.TIMER_DELAY && initialDelay == 0)
                    assignerRunnable = command;
                return null;
            }
            
        };
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(BandwidthTracker.class).annotatedWith(Names.named("uploadTracker")).toInstance(upTracker);
                bind(BandwidthTracker.class).annotatedWith(Names.named("downloadTracker")).toInstance(downTracker);
                bind(DHTManager.class).toInstance(dhtManager);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(ConnectionServices.class).toInstance(connectionServices);
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(ses);
                bind(ConnectionManager.class).toInstance(cManager);
                bind(SearchServices.class).toInstance(searchServices);
                bind(Executor.class).annotatedWith(Names.named("unlimitedExecutor")).toInstance(immediateExecutor);
            } 
        });
        nodeAssigner = injector.getInstance(NodeAssigner.class);
        
        nodeAssigner.start();
        assertNotNull(assignerRunnable);
    }
    
    
    private void setSettings() throws Exception {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        //Set the local host to not be banned so pushes can go through
        String ip = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {ip, "127.*.*.*"});
        NetworkSettings.PORT.setValue(TEST_PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.MODEM_SPEED_INT+1);
        //reset the node capabilities settings
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        DHTSettings.DISABLE_DHT_USER.setValue(false);
        DHTSettings.DISABLE_DHT_NETWORK.setValue(false);
        DHTSettings.EXCLUDE_ULTRAPEERS.setValue(true);
        DHTSettings.FORCE_DHT_CONNECT.setValue(false);
        DHTSettings.ENABLE_PASSIVE_DHT_MODE.setValue(true);
        DHTSettings.ENABLE_PASSIVE_LEAF_DHT_MODE.setValue(true);
    }

    @Override
    protected void tearDown() throws Exception {
        nodeAssigner.stop();
    }
        
    
    private Expectations buildBandwdithExpectations(final boolean good) throws Exception {
        return new Expectations() {{
            one(upTracker).measureBandwidth();
            one(downTracker).measureBandwidth();
            one(cManager).measureBandwidth();
            
            one(upTracker).getMeasuredBandwidth();
            will(returnValue(UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() + (good ? 2f : -2f)));
            one(downTracker).getMeasuredBandwidth();
            will(returnValue(UltrapeerSettings.MIN_DOWNSTREAM_REQUIRED.getValue() +(good ? 2f : -2f)));
            allowing(cManager).getMeasuredUpstreamBandwidth();
            allowing(cManager).getMeasuredDownstreamBandwidth();
            
        }};
    }
    
    /**
     * Builds an ultrapeer environment.
     * 
     * @param required if the checks are required or optional
     */
    private Expectations buildUltrapeerExpectations(
            final boolean GUESSCapable,
            final boolean acceptedIncoming,
            final long lastQueryTime,
            final boolean DHTEnabled,
            final DHTMode currentMode,
            boolean required
            ) throws Exception {
        
        final int invocations = required ? 1 : 0;
        
        return new Expectations(){{
            
            atLeast(invocations).of(connectionServices).isSupernode();
            will(returnValue(false));
            // annoying workaround for NetworkUtils.isPrivate
            atLeast(invocations).of(networkManager).getAddress();
            will(returnValue(new byte[4]));
            
            
            atLeast(invocations).of(networkManager).isGUESSCapable();
            will(returnValue(GUESSCapable));
            atLeast(invocations).of(networkManager).acceptedIncomingConnection();
            will(returnValue(acceptedIncoming));
            atLeast(invocations).of(searchServices).getLastQueryTime();
            will(returnValue(lastQueryTime)); 
            atLeast(invocations).of(dhtManager).getDHTMode();
            will(returnValue(currentMode));
            atLeast(invocations).of(dhtManager).isEnabled();
            will(returnValue(DHTEnabled));
        }};
    }
    
    private Expectations buildPromotionExpectations(final boolean promote) {
        return new Expectations(){{
            if (promote)
                one(cManager).tryToBecomeAnUltrapeer(4);
            else
                never(cManager).tryToBecomeAnUltrapeer(4);
        }};
    }
    
    public void testPromotesUltrapeer() throws Exception{
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());

        // set up some conditions for ultrapeer-ness
        // all of them are required
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, false, DHTMode.INACTIVE , true));
        mockery.checking(buildPromotionExpectations(true));
        
        assignerRunnable.run();
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfSlow() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        // bandwidth not high enough
        mockery.checking(buildBandwdithExpectations(false));
        // all other conditions match, but not all will be checked
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, false, DHTMode.INACTIVE , false));
        // will not get promoted
        mockery.checking(buildPromotionExpectations(false));

        assignerRunnable.run();
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }

    public void testDoesNotPromoteIfNoUDP() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // no UDP support
        mockery.checking(buildUltrapeerExpectations(false, true, 0l, false, DHTMode.INACTIVE , false));
        // will not get promoted
        mockery.checking(buildPromotionExpectations(false));
        assignerRunnable.run();
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfNoTCP() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // no tcp support
        mockery.checking(buildUltrapeerExpectations(true, false, 0l, false, DHTMode.INACTIVE, false));
        // will not get promoted
        mockery.checking(buildPromotionExpectations(false));
        assignerRunnable.run();
        
        // we are ever_capable because of last time
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfQueryTooSoon() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // last query now
        mockery.checking(buildUltrapeerExpectations(true, true, System.currentTimeMillis(), false, DHTMode.INACTIVE, false));
        // will not get promoted
        mockery.checking(buildPromotionExpectations(false));
        assignerRunnable.run();
        
        // we are ever_capable because of last time
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfAverageUptimeLow() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        // uptime bad
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() - 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildUltrapeerExpectations(true, true, 0L, false, DHTMode.INACTIVE, false));
        // will not get promoted
        mockery.checking(buildPromotionExpectations(false));
        assignerRunnable.run();

        // did not become capable this time either
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfNeverIncoming() throws Exception {
        // never incoming
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // everything else is fine
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, false, DHTMode.INACTIVE, false));
        // will not get promoted
        mockery.checking(buildPromotionExpectations(false));
        assignerRunnable.run();

        // did not become capable this time either
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfSupernodeDisabled() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());

        // user disabled
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
        // everything else fine
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, false, DHTMode.INACTIVE , false));
        mockery.checking(buildPromotionExpectations(false));
        
        assignerRunnable.run();
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteModemSpeed() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());

        // modem connection
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.MODEM_SPEED_INT);
        // everything else fine
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, false, DHTMode.INACTIVE , false));
        mockery.checking(buildPromotionExpectations(false));
        
        assignerRunnable.run();
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfDHTActive() throws Exception {
        // disallow switch to ultrapeer
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(0f);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        // pretend some time passed - the uptime counter in NodeAssigner is very hacky
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime", new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        // enabled and active DHT
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, true, DHTMode.ACTIVE, false));
        
        // but we're not an active ultrapeer and can receive solicited
        // and we've been up long enough tob e active in the DHT
        mockery.checking(new Expectations(){{
            one(connectionServices).isActiveSuperNode();
            will(returnValue(false));
            one(networkManager).canReceiveSolicited();
            will(returnValue(true));
            atLeast(1).of(cManager).getCurrentAverageUptime();
            will(returnValue(DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue() + 1));
        }});
        
        // will not get promoted
        mockery.checking(buildPromotionExpectations(false));
        
        assignerRunnable.run();

        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    public void testPromotesFromActiveDHTIfAllowed() throws Exception {
        // disallow switch to ultrapeer
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(1f);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        // pretend some time passed - the uptime counter in NodeAssigner is very hacky
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime", new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        // enabled and active DHT
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, true, DHTMode.ACTIVE, false));
        
        // but we're not an active ultrapeer and can receive solicited
        // but we've been up long enough tob e active in the DHT
        mockery.checking(new Expectations(){{
            one(connectionServices).isActiveSuperNode();
            will(returnValue(false));
            one(networkManager).canReceiveSolicited();
            will(returnValue(true));
            atLeast(1).of(cManager).getCurrentAverageUptime();
            will(returnValue(DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue() + 1));
        }});
        
        // will get promoted
        mockery.checking(buildPromotionExpectations(true));
        
        assignerRunnable.run();

        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        mockery.assertIsSatisfied();
    }
    
    
    private Expectations buildDHTExpectations(
            final DHTMode currentDHTMode,
            final boolean enabled,
            final boolean canReceiveSolicited,
            final boolean isGUESSCapable,
            final long currentAvgUptime,
            final boolean supernode,
            boolean required
            ) {
        final int invocations = required ? 1 : 0;
        return new Expectations(){{
            // annoying workaround for NetworkUtils.isPrivate
            allowing(networkManager).getAddress();
            will(returnValue(new byte[4]));
            
            // some stuff to prevent us from becoming an ultrapeer
            allowing(networkManager).acceptedIncomingConnection();
            will(returnValue(false));
            allowing(searchServices).getLastQueryTime();
            will(returnValue(System.currentTimeMillis()));
            
            
            atLeast(invocations).of(dhtManager).getDHTMode();
            will(returnValue(currentDHTMode));
            atLeast(invocations).of(dhtManager).isEnabled();
            will(returnValue(enabled));
            
            atLeast(invocations).of(networkManager).canReceiveSolicited();
            will(returnValue(canReceiveSolicited));
            atLeast(invocations).of(networkManager).isGUESSCapable();
            will(returnValue(isGUESSCapable));
            atLeast(invocations).of(cManager).getCurrentAverageUptime();
            will(returnValue(currentAvgUptime));
            
            atLeast(invocations).of(connectionServices).isSupernode();
            will(returnValue(supernode));
            atLeast(invocations).of(connectionServices).isActiveSuperNode();
            will(returnValue(supernode));
            
        }};
    }
    
    public void testAssignsActiveDHT() throws Exception {
        
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, true, 
                DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue()+1, 
                false, false));
        
        mockery.checking(new Expectations(){{
            one(dhtManager).start(DHTMode.ACTIVE);
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testAssignsPassiveDHTIfUltrapeer() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_PASSIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, true, 
                DHTSettings.MIN_PASSIVE_DHT_AVERAGE_UPTIME.getValue()+1, 
                true, false));
        
        mockery.checking(new Expectations(){{
            one(dhtManager).start(DHTMode.PASSIVE);
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testAssignsPassiveLeaf() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_PASSIVE_LEAF_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, false, // can't receive unsolicited 
                DHTSettings.MIN_PASSIVE_LEAF_DHT_AVERAGE_UPTIME.getValue()+1, 
                false, false));
        
        mockery.checking(new Expectations(){{
            one(dhtManager).start(DHTMode.PASSIVE_LEAF);
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotAssignLowInitialUptime() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);

        // no initial uptime
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, true, 
                DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue()+1, 
                false, false));
        
        mockery.checking(new Expectations(){{
            never(dhtManager).start(with(Matchers.any(DHTMode.class)));
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotAssignLowAverageUptime() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));


        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, true, 
                0,         // no average uptime 
                false, false));
        
        mockery.checking(new Expectations(){{
            never(dhtManager).start(with(Matchers.any(DHTMode.class)));
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotAssignActiveIfNotHardcore() throws Exception {
        // not accepted incoming previously therefore not hardcore
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, true, 
                DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue()+1, 
                false, false));
        
        mockery.checking(new Expectations(){{
            one(dhtManager).start(DHTMode.PASSIVE_LEAF);
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testPassiveLeafDoesNotNeedHardCore() throws Exception {
      // not accepted incoming previously therefore not hardcore
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_PASSIVE_LEAF_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, false, // can't receive unsolicited 
                DHTSettings.MIN_PASSIVE_LEAF_DHT_AVERAGE_UPTIME.getValue()+1, 
                false, false));
        
        mockery.checking(new Expectations(){{
            one(dhtManager).start(DHTMode.PASSIVE_LEAF);
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotAssignDHTIfDisabled() throws Exception {
        
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                false, true, true, 
                DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue()+1, 
                false, false));
        
        mockery.checking(new Expectations(){{
            never(dhtManager).start(with(Matchers.any(DHTMode.class)));
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testStopsDHTWhenDisabled() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.ACTIVE, 
                false, true, true, 
                DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue()+1, 
                false, false));
        
        mockery.checking(new Expectations(){{
            one(dhtManager).stop();
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotAssignPassiveIfDisabled() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        DHTSettings.ENABLE_PASSIVE_DHT_MODE.setValue(false);
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_PASSIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, true, 
                DHTSettings.MIN_PASSIVE_DHT_AVERAGE_UPTIME.getValue()+1, 
                true, false));
        
        mockery.checking(new Expectations(){{
            never(dhtManager).start(with(Matchers.any(DHTMode.class)));
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotAssignPassiveLeafIfDisabled() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        DHTSettings.ENABLE_PASSIVE_LEAF_DHT_MODE.setValue(false);
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",new Long(DHTSettings.MIN_PASSIVE_LEAF_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildDHTExpectations(DHTMode.INACTIVE, 
                true, true, false, // can't receive unsolicited 
                DHTSettings.MIN_PASSIVE_LEAF_DHT_AVERAGE_UPTIME.getValue()+1, 
                false, false));
        
        mockery.checking(new Expectations(){{
            never(dhtManager).start(with(Matchers.any(DHTMode.class)));
        }});
        
        assignerRunnable.run();
        mockery.assertIsSatisfied();
    }
}
