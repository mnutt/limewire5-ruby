package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.io.UnresolvedIpPortImpl;
import org.limewire.util.BaseTestCase;

public class FallbackConnectionConfigurationFactoryTest extends BaseTestCase {
    public FallbackConnectionConfigurationFactoryTest(String name) {
        super(name);
    }
    
    public void testHasMoreNoFallbacks() {
        FallbackConnectionConfigurationFactory factory = new FallbackConnectionConfigurationFactory();
        Mockery mockery = new Mockery();
        final FriendConnectionConfiguration connectionConfiguration = mockery.mock(FriendConnectionConfiguration.class);
        ConnectionConfigurationFactory.RequestContext requestContext = new ConnectionConfigurationFactory.RequestContext();
        final List<UnresolvedIpPort> fallbackServers = new ArrayList<UnresolvedIpPort>();
        mockery.checking(new Expectations() {{
            allowing(connectionConfiguration).getDefaultServers(); 
            will(returnValue(fallbackServers));
        }});
        assertFalse(factory.hasMore(connectionConfiguration, requestContext));
        mockery.assertIsSatisfied();
    }
    
    public void testHasMoreWithFallbacks() {
        FallbackConnectionConfigurationFactory factory = new FallbackConnectionConfigurationFactory();
        Mockery mockery = new Mockery();
        final FriendConnectionConfiguration connectionConfiguration = mockery.mock(FriendConnectionConfiguration.class);
        ConnectionConfigurationFactory.RequestContext requestContext = new ConnectionConfigurationFactory.RequestContext();
        final List<UnresolvedIpPort> fallbackServers = new ArrayList<UnresolvedIpPort>();
        fallbackServers.add(new UnresolvedIpPortImpl("foo.com", 1234));
        fallbackServers.add(new UnresolvedIpPortImpl("bar.com", 5678));
        mockery.checking(new Expectations() {{
            allowing(connectionConfiguration).getDefaultServers(); 
            will(returnValue(fallbackServers));
        }});
        assertTrue(factory.hasMore(connectionConfiguration, requestContext));
        requestContext.incrementRequests();
        assertTrue(factory.hasMore(connectionConfiguration, requestContext));
        requestContext.incrementRequests();
        assertFalse(factory.hasMore(connectionConfiguration, requestContext));
        mockery.assertIsSatisfied();
    }
    
    public void testGetConnectionConfigurationNoConfigs() {
        FallbackConnectionConfigurationFactory factory = new FallbackConnectionConfigurationFactory();
        Mockery mockery = new Mockery();
        final FriendConnectionConfiguration connectionConfiguration = mockery.mock(FriendConnectionConfiguration.class);
        ConnectionConfigurationFactory.RequestContext requestContext = new ConnectionConfigurationFactory.RequestContext();
        final List<UnresolvedIpPort> fallbackServers = new ArrayList<UnresolvedIpPort>();
        mockery.checking(new Expectations() {{
            allowing(connectionConfiguration).getDefaultServers(); 
            will(returnValue(fallbackServers));
        }});
        try {
            factory.getConnectionConfiguration(connectionConfiguration, requestContext);
            fail();
        } catch (IllegalStateException e) {
        }
        mockery.assertIsSatisfied();
    }
    
    public void testGetConnectionConfiguration() {
        FallbackConnectionConfigurationFactory factory = new FallbackConnectionConfigurationFactory();
        Mockery mockery = new Mockery();
        final FriendConnectionConfiguration connectionConfiguration = mockery.mock(FriendConnectionConfiguration.class);
        ConnectionConfigurationFactory.RequestContext requestContext = new ConnectionConfigurationFactory.RequestContext();
        final List<UnresolvedIpPort> fallbackServers = new ArrayList<UnresolvedIpPort>();
        fallbackServers.add(new UnresolvedIpPortImpl("foo.com", 1234));
        fallbackServers.add(new UnresolvedIpPortImpl("bar.com", 5678));
        mockery.checking(new Expectations() {{
            allowing(connectionConfiguration).getDefaultServers(); 
            will(returnValue(fallbackServers));
            allowing(connectionConfiguration).getServiceName(); 
            will(returnValue("foo"));
        }});     
        ConnectionConfiguration configuration = factory.getConnectionConfiguration(connectionConfiguration, requestContext);
        assertEquals("foo.com", configuration.getHost());
        assertEquals(1234, configuration.getPort());
        requestContext.incrementRequests();
        configuration = factory.getConnectionConfiguration(connectionConfiguration, requestContext);
        assertEquals("bar.com", configuration.getHost());
        assertEquals(5678, configuration.getPort());
        mockery.assertIsSatisfied();
    }
}
