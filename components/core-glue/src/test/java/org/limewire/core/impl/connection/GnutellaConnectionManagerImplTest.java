package org.limewire.core.impl.connection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.ConnectionLifecycleEventType;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.friend.api.FriendPresence;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;
import org.limewire.util.PrivateAccessor;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Various tests for GnutellaConnectionManagerImpl.  Tests event listeners,
 *  delegate functions, and ConnectionStrength calculation. 
 */
public class GnutellaConnectionManagerImplTest extends BaseTestCase {

    private Mockery context;
    private ConnectionManager connectionManager;

    public GnutellaConnectionManagerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        connectionManager = context.mock(ConnectionManager.class);
    }
    
    /**
     * Confirms calling registerListener() on an instance of gnutellaConnectionManager
     *  will register the instance as an event listener on ConnectionManager
     */
    public void testRegisterConnectionListener() {
        final GnutellaConnectionManagerImpl gnutellaConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, null);
        
        context.checking(new Expectations() {{
            exactly(1).of(connectionManager).addEventListener(gnutellaConnectionManager);
        }});
        
        gnutellaConnectionManager.registerListener();
        
        context.assertIsSatisfied();
    }
    
    /**
     * An integration test based around registering the service that goes all the way from
     *  the initial register to running the service once.
     */
    public void testRegisterService() {
        
        final GnutellaConnectionManagerImpl gnutellaConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, null);

        final ServiceRegistry registry = context.mock(ServiceRegistry.class);
        final ScheduledExecutorService backgroundExecutor = context.mock(ScheduledExecutorService.class);
        
        final PropertyChangeListener changeListener = context.mock(PropertyChangeListener.class); 
        
        final MatchAndCopy<Service> serviceCollector = new MatchAndCopy<Service>(Service.class);
        final MatchAndCopy<ConnectionLifecycleListener> listenerCollector 
            = new MatchAndCopy<ConnectionLifecycleListener>(ConnectionLifecycleListener.class);
        final MatchAndCopy<Runnable> runnableCollector = new MatchAndCopy<Runnable>(Runnable.class);
        
        context.checking(new Expectations() {{
            exactly(1).of(registry).register(with(serviceCollector));
            exactly(1).of(connectionManager).addEventListener(with(listenerCollector));
            exactly(1).of(backgroundExecutor).scheduleWithFixedDelay(with(runnableCollector),
                    with(any(Long.class)), with(any(Long.class)), with(any(TimeUnit.class)));
            
            // Ensure that the calculation actually takes place by expecting a connection info
            //  query when the periodic service is run.
            exactly(1).of(connectionManager).countConnectionsWithNMessages(with(any(Integer.class)));
        }});
        
        // Should start disconnected
        assertEquals(ConnectionStrength.DISCONNECTED, gnutellaConnectionManager.getConnectionStrength());
        
        // Kick off the register
        gnutellaConnectionManager.registerService(registry, backgroundExecutor);

        // Test stop without initialising
        serviceCollector.getLastMatch().stop();
        
        // Manually Initialise the service
        serviceCollector.getLastMatch().initialize();
        
        // Make sure the service has a valid name
        assertNotNull(serviceCollector.getLastMatch().getServiceName());
        assertNotEquals("", serviceCollector.getLastMatch().getServiceName());
        
        // Make sure the connection listener was properly linked
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.CONNECTED));
        assertEquals(ConnectionLifecycleEventType.CONNECTED, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.NO_INTERNET));
        assertEquals(ConnectionLifecycleEventType.NO_INTERNET, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.CONNECTION_INITIALIZED));
        assertEquals(ConnectionLifecycleEventType.CONNECTION_INITIALIZED, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.DISCONNECTED));
        assertEquals(ConnectionLifecycleEventType.CONNECTION_INITIALIZED, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        // Test start
        serviceCollector.getLastMatch().start();

        // External listener and stop assertions        
        context.checking(new Expectations() {{
            exactly(1).of(connectionManager).removeEventListener(listenerCollector.getLastMatch());
            
            // When service is run the new state should alternate to test listeners
            one(connectionManager).isConnecting();
            will(returnValue(true));
            one(connectionManager).isConnecting();
            will(returnValue(true));
            one(connectionManager).isConnecting();
            will(returnValue(false));
            // Should only be fired the once, the rest are either caches or when the listener is removed
            exactly(1).of(changeListener).propertyChange(with(any(PropertyChangeEvent.class)));
            
            // We don't care about any other calls on connection manager that could happen while
            //  calculating the connection strength.
            ignoring(connectionManager);
        }});
        
        // Add a listener to ensure changes are sent out
        gnutellaConnectionManager.addPropertyChangeListener(changeListener);
        // Test the service - change event
        runnableCollector.getLastMatch().run();
        // Test the service - no change event because caching
        runnableCollector.getLastMatch().run();
        // Remove the listener
        gnutellaConnectionManager.removePropertyChangeListener(changeListener);
        // Test the service - no change event because no listeners conencted
        runnableCollector.getLastMatch().run();
        
        // Test stop
        serviceCollector.getLastMatch().stop();
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test that delegate to ConnectionManager.isConnected() is correctly connected and returns
     *  accordingly in true and false state.
     */
    public void testIsConnected() {
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);
        
        GnutellaConnectionManagerImpl gnutellaConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, connectionServices);

        context.checking(new Expectations() {{
                one(connectionServices).isConnected();
                will(returnValue(true));
        }});
        assertTrue(gnutellaConnectionManager.isConnected());

        context.checking(new Expectations() {{
                one(connectionServices).isConnected();
                will(returnValue(false));
        }});
        assertFalse(gnutellaConnectionManager.isConnected());
        
        context.assertIsSatisfied();
    }

    /**
     * Test that delegate to ConnectionManager.isSupernode() is correctly connected and returns
     *  accordingly in true and false state.
     */
    public void testIsUltraPeer() {
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);
                
        GnutellaConnectionManagerImpl gnutellaConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, connectionServices);
        context.checking(new Expectations() {{
                one(connectionManager).isSupernode();
                will(returnValue(true));
        }});
        assertTrue(gnutellaConnectionManager.isUltrapeer());

        context.checking(new Expectations() {{
                one(connectionManager).isSupernode();
                will(returnValue(false));
        }});
        assertFalse(gnutellaConnectionManager.isUltrapeer());

        context.assertIsSatisfied();
    }
    
    /**
     * Ensures that the various delegate functions to ConnectionServices, LibraryManager,
     *  and core ConnectionManager are correctly linked and pass execution on properly.
     */
    public void testConnectionDelegates() {
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);

        final int portnum = 111111;
        final String hostname = "hostekepucally";
        final ConnectionItem connectionItemToNotRemove = context.mock(ConnectionItem.class);
        final ConnectionItem connectionItemToRemove = context.mock(CoreConnectionItem.class);
        final ConnectionItem connectionItemToBrowse = context.mock(ConnectionItem.class);
        
        GnutellaConnectionManagerImpl gConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, connectionServices);
        
        context.checking(new Expectations() {{
            allowing(connectionItemToRemove);
            
            exactly(1).of(connectionServices).connect();
            exactly(1).of(connectionServices).disconnect();
            exactly(1).of(connectionManager).disconnect(true);
            exactly(1).of(connectionManager).connect();
            exactly(1).of(connectionServices).removeConnection(with(any(RoutedConnection.class)));
            exactly(1).of(connectionServices).connectToHostAsynchronously(hostname, portnum, ConnectType.TLS);
            exactly(1).of(connectionServices).connectToHostAsynchronously(hostname, portnum, ConnectType.PLAIN);
            
            FriendPresence presence = context.mock(FriendPresence.class);
            allowing(connectionItemToBrowse).getFriendPresence();
            will(returnValue(presence));
        }});
        
        gConnectionManager.connect();
        gConnectionManager.disconnect();
        gConnectionManager.restart();
        
        gConnectionManager.removeConnection(connectionItemToNotRemove);
        gConnectionManager.removeConnection(connectionItemToRemove);
        
        gConnectionManager.tryConnection(hostname, portnum, true);
        gConnectionManager.tryConnection(hostname, portnum, false);
        
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the ConnectionLifecycleEvent code under various conditions.  Ensure
     *  ConnectionItems are created, added, updated, and removed according according to
     *  the different event types.
     */
    public void testHandleConnectionLifecycleEvent() {
        
        GnutellaConnectionManagerImpl gnutellaConnectionManager
            = new GnutellaConnectionManagerImpl(connectionManager, null);
        
        EventList<ConnectionItem> list = gnutellaConnectionManager.getConnectionList();
        
        final RoutedConnection routedConnection = context.mock(RoutedConnection.class);
        
        context.checking(new Expectations() {{
            // Key to ensure the RoutedConnection and ConnectionItem match
            allowing(routedConnection).getPort();
            will(returnValue(444));
            
            // Marker to identify with the ConnectionItem is updated
            allowing(routedConnection).getConnectionTime();
            will(returnValue(555L));
            
            allowing(routedConnection);
        }});
        
        assertEmpty(list);
        
        // An event with no routed connection -- should be ignored
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZING));        
        assertEmpty(list);
                
        // Spurious initialised event -- should be ignored
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZED,
                        routedConnection));
        assertEmpty(list);
        
        // Non relevant event -- should be ignored
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_CAPABILITIES,
                        routedConnection));
        assertEmpty(list);
        
        // Initialising -- should result in a new list item
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZING,
                        routedConnection));
        assertEquals(1, list.size());
        assertEquals(444, list.get(0).getPort());

        // Duplicate initialising -- should be ignored
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZING,
                        routedConnection));
        assertEquals(1, list.size());
        assertEquals(444, list.get(0).getPort());
        
        // Make sure the update marker is not set before the update is made
        assertNotEquals(555, list.get(0).getTime());
        
        // Initialised -- Should call update, check that time marker is updated
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZED,
                        routedConnection));
        assertEquals(1, list.size());
        assertEquals(444, list.get(0).getPort());
        // This marker will be propagated into the ConnectionItem when update is called
        assertEquals(555, list.get(0).getTime());
        
        // Closed -- Should remove the item from the list
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_CLOSED,
                        routedConnection));
        assertEmpty(list);

        // Duplicate closed -- Should have no effect
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_CLOSED,
                        routedConnection));
        assertEmpty(list);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the calculate function.  Make sure it identifies disconnected and connecting.
     *  Call with various input and insure all states are returned and no errors are encountered
     */
    public void testConnectionStrength() throws Exception {
        
        // Make sure it correctly identifies disconnected and connecting
        assertEquals(ConnectionStrength.DISCONNECTED, 
                testCalculate(0, false, false, 0, 0, 0, false, false, 0, null));
        assertEquals(ConnectionStrength.CONNECTING, 
                testCalculate(0, true, false, 0, 0, 0, false, false, 0, null));
        
        // Assert NO_INTERNET state is preserved 
        assertEquals(ConnectionStrength.NO_INTERNET, 
                testCalculate(0, true, false, 0, 0, 0, false, false, 0,
                        ConnectionLifecycleEventType.NO_INTERNET));
        
        // Test waking up state
        assertEquals(ConnectionStrength.MEDIUM, 
                testCalculate(0, true, false, 0, 0, 0, false, false, Long.MAX_VALUE-60*1000, null));        
        
        // Prepare a set of states that should be hit in the proceeding calculate calls
        Set<ConnectionStrength> states = new HashSet<ConnectionStrength>();
        for ( ConnectionStrength state : ConnectionStrength.values() ) {
            states.add(state);
        }
        states.remove(ConnectionStrength.DISCONNECTED);
        states.remove(ConnectionStrength.NO_INTERNET);
        
        // Call calculates and if the result is new remove it from the list 
        //  of outstanding states
        states.remove(testCalculate(0, false, false, 11, 0, 0, false, false, 0, null));
        states.remove(testCalculate(0, false, false, 22, 11, 0, false, false, 0, null));
        states.remove(testCalculate(80, false, true, 0, 0, 0, false, false, 0, null));
        states.remove(testCalculate(1, false, false, 0, 0, Integer.MAX_VALUE, false, false, 0, null));
        
        states.remove(testCalculate(1, false, false, 0, 0, Integer.MAX_VALUE, false, false, 0, null));
        
        states.remove(testCalculate(23452346, false, false, 0, 0, 0, false, false, 0, null));
        states.remove(testCalculate(100, false, false, 0, 0, 1, false, false, 0, null));        
        states.remove(testCalculate(-10, false, false, 0, 0, 100, false, false, 0, null));
        states.remove(testCalculate(30, false, false, 0, 0, 99, false, false, 0, null));
        states.remove(testCalculate(31, false, false, 0, 0, 101, false, false, 0, null));
        states.remove(testCalculate(31, false, false, 0, 0, 101, false, false, 0, null));
        
        states.remove(testCalculate(31, false, false, 0, 0, 101, false, true, 0, null));
        states.remove(testCalculate(5, false, false, 0, 0, 101, true, false, Long.MAX_VALUE-60*1000, null));
        states.remove(testCalculate(5, false, false, 0, 0, 101, true, true, 0, null));
        
        for ( int i = 1 ; i < 125 ; i+=4 ) {
            states.remove(testCalculate(i, false, false, 0, 0, 100, false, false, 0, null));
        }
        
        // This should be enough to hit all the connection states, make sure otherwise something 
        //  strange is happening
        assertEmpty(states);        
    }
    
    /**
     * Returns the input of the calculate function when called in environment 
     *  that corresponds to the parameters passed in.
     */
    private ConnectionStrength testCalculate(
            final int countConnectionsWithNMessages, 
            final boolean isConnecting, 
            final boolean isConnectionIdle,
            final int getNumFetchingConnections,
            final int getNumInitializedConnections,
            final int getPreferredConnectionCount,
            final boolean isPro, 
            final boolean isSupernode,
            final long lastIdleTime, 
            final ConnectionLifecycleEventType lastStrengthRelatedEvent) throws Exception {
        
        GnutellaConnectionManagerImpl gnutellaConnectionManager
            = new GnutellaConnectionManagerImpl(connectionManager, null);
        
        gnutellaConnectionManager.lastIdleTime = lastIdleTime;
        gnutellaConnectionManager.lastStrengthRelatedEvent = lastStrengthRelatedEvent;
        
        context.checking(new Expectations() {{
            allowing(connectionManager).countConnectionsWithNMessages(with(any(int.class)));
            will(returnValue(countConnectionsWithNMessages));
            allowing(connectionManager).isConnecting();
            will(returnValue(isConnecting));
            allowing(connectionManager).isConnectionIdle();
            will(returnValue(isConnectionIdle));
            allowing(connectionManager).getNumFetchingConnections();
            will(returnValue(getNumFetchingConnections));
            allowing(connectionManager).getNumInitializedConnections();
            will(returnValue(getNumInitializedConnections));
            allowing(connectionManager).getPreferredConnectionCount();
            will(returnValue(getPreferredConnectionCount));
            allowing(connectionManager).isSupernode();
            will(returnValue(isSupernode));
        }});
        
        // LimeWireUtils.isPro() is hardcoded, use reflection to get it
        PrivateAccessor isProAccessor = new PrivateAccessor(LimeWireUtils.class, null, "_isPro");
        isProAccessor.setValue(isPro);
        
        // Calculate connection strength
        ConnectionStrength strength = gnutellaConnectionManager.calculateStrength();

        // Reset pro field
        isProAccessor.reset();
        
        context.assertIsSatisfied();

        // Reset context
        setUp();
        
        return strength;
    }
}
