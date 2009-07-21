package org.limewire.core.impl.connection;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.connection.ConnectionItem.Status;
import org.limewire.friend.api.FriendPresence;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.connection.ConnectionBandwidthStatistics;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.ConnectionMessageStatistics;
import com.limegroup.gnutella.connection.ConnectionRoutingStatistics;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.handshaking.HandshakeResponse;

/**
 * Test methods for CoreConnectionItem.  Mostly for confirming the linkage of 
 *  delegate methods.
 */
public class CoreConnectionItemTest extends BaseTestCase {

    public CoreConnectionItemTest(String name) {
        super(name);
    }
    
    /**
     * Tests the default value of the resolved field and makes sure it is 
     *  properly modifiable with its setter method.
     */
    public void testResolved() {
        Mockery context = new Mockery();
        final RoutedConnection connection = context.mock(RoutedConnection.class);        
        
        context.checking(new Expectations() {{
            allowing(connection);
        }});
        
        CoreConnectionItem item = new CoreConnectionItem(connection);
        
        assertFalse(item.isAddressResolved());
        item.setAddressResolved(true);
        assertTrue(item.isAddressResolved());
        item.setAddressResolved(false);
        assertFalse(item.isAddressResolved());
    }
    
    /**
     * Ensures the method is able to return a valid FriendPresence and tests that
     *  instance for correctness.
     */
    public void testGetFriendPresence() throws UnknownHostException {
        Mockery context = new Mockery();
        final RoutedConnection connection = context.mock(RoutedConnection.class);        
        
        context.checking(new Expectations() {{
            allowing(connection).getAddress();
            will(returnValue("127.0.0.1"));
            allowing(connection).getPort();
            will(returnValue(444));
            allowing(connection).getInetAddress();
            will(returnValue(InetAddress.getLocalHost()));
            allowing(connection).getInetSocketAddress();
            will(returnValue(new InetSocketAddress(InetAddress.getLocalHost(), 444)));
            allowing(connection).isTLSCapable();
            will(returnValue(true));
        }});
        
        CoreConnectionItem item = new CoreConnectionItem(connection);
     
        FriendPresence presence = item.getFriendPresence();
        assertNotNull(presence);
        assertNotNull(presence.getFriend());
        
        context.assertIsSatisfied();
    }

    /**
     * Test the get header properties method.
     */
    public void testGetHeaderProperties() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RoutedConnection connection = context.mock(RoutedConnection.class); 
        final Properties props = context.mock(Properties.class);
        
        context.checking(new Expectations() {{
            ConnectionCapabilities capabilities = context.mock(ConnectionCapabilities.class);
            allowing(connection).getConnectionCapabilities();
            will(returnValue(capabilities));
            HandshakeResponse response = context.mock(HandshakeResponse.class);
            allowing(capabilities).getHeadersRead();
            will(returnValue(response));
            allowing(response).props();
            will(returnValue(props));
            
            allowing(connection);
        }});
        
        CoreConnectionItem item = new CoreConnectionItem(connection);
        assertSame(props, item.getHeaderProperties());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests hostname, confirms the default value is set and that the field is
     *  properly modifiable.
     */
    public void testHostName() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
                
        final RoutedConnection connection = context.mock(RoutedConnection.class);        
        
        context.checking(new Expectations() {{
            allowing(connection).getAddress();
            will(returnValue("initial"));
            
            InetAddress inetAddress = context.mock(InetAddress.class);
            allowing(connection).getInetAddress();
            will(returnValue(inetAddress));
            allowing(inetAddress).getHostAddress();
            will(returnValue("reseted"));
        }});
        
        CoreConnectionItem item = new CoreConnectionItem(connection);
        assertEquals("initial", item.getHostName());
        item.setHostName("override");
        assertEquals("override", item.getHostName());
        item.setAddressResolved(true);
        item.resetHostName();
        assertEquals("reseted", item.getHostName());
        assertFalse(item.isAddressResolved());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests for consistent internal status before and after the update method is called.
     */
    public void testUpdate() {
        long testStartTime = System.currentTimeMillis();
        
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RoutedConnection connection = context.mock(RoutedConnection.class);        
        
        context.checking(new Expectations() {{
            one(connection).isOutgoing();
            will(returnValue(false));
            one(connection).isOutgoing();
            will(returnValue(true));
            
            allowing(connection).getConnectionTime();
            will(returnValue(101010L));
            
            allowing(connection);
        }});
        
        CoreConnectionItem item = new CoreConnectionItem(connection);
        
        assertSame(connection, item.getRoutedConnection());
        
        assertTrue(item.isConnecting());
        assertFalse(item.isConnected());
        assertEquals(Status.CONNECTING, item.getStatus());
        
        // Ensure the initialisation time value is somewhat sane, aka something bigger
        //  than the start time of the test.
        assertGreaterThanOrEquals(testStartTime, item.getTime());
                
        item.update();
        assertFalse(item.isConnecting());
        assertTrue(item.isConnected());
        assertEquals(Status.INCOMING, item.getStatus());
        
        
        item.update();
        assertFalse(item.isConnecting());
        assertTrue(item.isConnected());
        assertEquals(Status.OUTGOING, item.getStatus());
        assertEquals(101010, item.getTime());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests for proper linkage with methods of CoreConnectionItem that 
     *  pull their data directly from the routed connection.
     */
    public void testDelegateMethods() {
        final Mockery context = new Mockery();
        
        final RoutedConnection connection = context.mock(RoutedConnection.class);        
        
        context.checking(new Expectations() {{
            
            allowing(connection).getMeasuredDownstreamBandwidth();
            will(returnValue(6523f));
            allowing(connection).getMeasuredUpstreamBandwidth();
            will(returnValue(6524f));
            
            ConnectionMessageStatistics messageStats = context.mock(ConnectionMessageStatistics.class);
            allowing(connection).getConnectionMessageStatistics();
            will(returnValue(messageStats));
            allowing(messageStats).getNumMessagesReceived();
            will(returnValue(6525));
            allowing(messageStats).getNumMessagesSent();
            will(returnValue(6526));
            allowing(messageStats).getNumReceivedMessagesDropped();
            will(returnValue(6527L));
            allowing(messageStats).getNumSentMessagesDropped();
            will(returnValue(6528));
            
            allowing(connection).getPort();
            will(returnValue(6529));
            
            ConnectionRoutingStatistics routingStats = context.mock(ConnectionRoutingStatistics.class);
            allowing(connection).getRoutedConnectionStatistics();
            will(returnValue(routingStats));
            allowing(routingStats).getQueryRouteTableEmptyUnits();
            will(returnValue(6530));
            allowing(routingStats).getQueryRouteTablePercentFull();
            will(returnValue(6531d));
            allowing(routingStats).getQueryRouteTableSize();
            will(returnValue(6532));
            allowing(routingStats).getQueryRouteTableUnitsInUse();
            will(returnValue(6533));
            
            ConnectionBandwidthStatistics bandwithStats = context.mock(ConnectionBandwidthStatistics.class);
            allowing(connection).getConnectionBandwidthStatistics();
            will(returnValue(bandwithStats));
            allowing(bandwithStats).getReadLostFromSSL();
            will(returnValue(6534f));
            allowing(bandwithStats).getReadSavedFromCompression();
            will(returnValue(6535f));
            allowing(bandwithStats).getSentLostFromSSL();
            will(returnValue(6536f));
            allowing(bandwithStats).getSentSavedFromCompression();
            will(returnValue(6537f));
            
            ConnectionCapabilities capabilities = context.mock(ConnectionCapabilities.class);
            allowing(connection).getConnectionCapabilities();
            will(returnValue(capabilities));
            allowing(capabilities).getUserAgent();
            will(returnValue("6538"));
            
            one(connection).isSupernodeClientConnection();
            will(returnValue(false));
            one(connection).isSupernodeClientConnection();
            will(returnValue(true));
            one(connection).isOutgoing();
            will(returnValue(false));
            one(connection).isOutgoing();
            will(returnValue(true));
            one(capabilities).isSupernodeSupernodeConnection();
            will(returnValue(false));
            one(capabilities).isSupernodeSupernodeConnection();
            will(returnValue(true));
            one(capabilities).isSupernodeConnection();
            will(returnValue(false));
            one(capabilities).isSupernodeConnection();
            will(returnValue(true));
            one(capabilities).isClientSupernodeConnection();
            will(returnValue(false));
            one(capabilities).isClientSupernodeConnection();
            will(returnValue(true));
            
            allowing(connection);
        }});
        
        CoreConnectionItem item = new CoreConnectionItem(connection);
        
        assertEquals(6523f, item.getMeasuredDownstreamBandwidth());
        assertEquals(6524f, item.getMeasuredUpstreamBandwidth());
        assertEquals(6525, item.getNumMessagesReceived());
        assertEquals(6526, item.getNumMessagesSent());
        assertEquals(6527L, item.getNumReceivedMessagesDropped());
        assertEquals(6528, item.getNumSentMessagesDropped());
        assertEquals(6529, item.getPort());
        assertEquals(6530, item.getQueryRouteTableEmptyUnits());
        assertEquals(6531d, item.getQueryRouteTablePercentFull());
        assertEquals(6532, item.getQueryRouteTableSize());
        assertEquals(6533, item.getQueryRouteTableUnitsInUse());
        assertEquals(6534f, item.getReadLostFromSSL());
        assertEquals(6535f, item.getReadSavedFromCompression());
        assertEquals(6536f, item.getSentLostFromSSL());
        assertEquals(6537f, item.getSentSavedFromCompression());
        assertEquals("6538", item.getUserAgent());
        
        assertFalse(item.isLeaf());
        assertFalse(item.isOutgoing());
        assertFalse(item.isPeer());
        assertFalse(item.isUltrapeerConnection());
        assertFalse(item.isUltrapeer());
        assertTrue(item.isLeaf());
        assertTrue(item.isOutgoing());
        assertTrue(item.isPeer());
        assertTrue(item.isUltrapeerConnection());
        assertTrue(item.isUltrapeer());
    }
  
}
