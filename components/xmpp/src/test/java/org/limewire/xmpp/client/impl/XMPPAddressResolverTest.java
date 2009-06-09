package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.util.Collections;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.friend.feature.features.ConnectBackRequestFeature;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.net.address.BlockingAddressResolutionObserver;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;

public class XMPPAddressResolverTest extends BaseTestCase {

    private Mockery context;
    private XMPPService xmppService;
    private XMPPConnection connection;
    private SocketsManager socketsManager;
    private XMPPAddressRegistry addressRegistry;
    private XMPPFriend user;
    private FriendPresence friendPresence;
    private XMPPAddressResolver xmppAddressResolver;

    public XMPPAddressResolverTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        xmppService = context.mock(XMPPService.class);
        connection = context.mock(XMPPConnection.class);  
        socketsManager = context.mock(SocketsManager.class);
        addressRegistry = new XMPPAddressRegistry();
        user = context.mock(XMPPFriend.class);
        friendPresence = context.mock(FriendPresence.class);
        
        context.checking(new Expectations() {{
            allowing(xmppService).getActiveConnection();
            will(returnValue(connection));
            allowing(connection).getFriend("me@you.com");
            will(returnValue(user));
            allowing(connection).getFriend(with(any(String.class)));
            will(returnValue(null));
            allowing(user).getFriendPresences();
            will(returnValue(Collections.singletonMap("me@you.com/resource", friendPresence)));
            allowing(friendPresence).getFeature(AuthTokenFeature.ID);
            will(returnValue(new AuthTokenFeature(new byte[] {})));
        }});
        
        xmppAddressResolver = new XMPPAddressResolver(xmppService, null, socketsManager, addressRegistry);
    }

    public void testSuccessfulGetPresence() {
        addressRegistry.put(new XMPPAddress("me@you.com/resource"), ConnectableImpl.INVALID_CONNECTABLE);
        // with exact same resource id
        FriendPresence presence = xmppAddressResolver.getPresence(new XMPPAddress("me@you.com/resource"));
        assertNotNull(presence);
        // different, but matching resource id
        presence = xmppAddressResolver.getPresence(new XMPPAddress("me@you.com/resource12345"));
        assertNotNull(presence);
    }
    
    public void testFailedGetPresence() {
        FriendPresence presence = xmppAddressResolver.getPresence(new XMPPAddress("me@you.com/resource"));
        assertNull(presence);
        
        addressRegistry.put(new XMPPAddress("me@you.com/resource"), ConnectableImpl.INVALID_CONNECTABLE);
        presence = xmppAddressResolver.getPresence(new XMPPAddress("me@you.com/differentresource"));
        assertNull(presence);
    }

    public void testCanResolveHasPresence() {
        addressRegistry.put(new XMPPAddress("me@you.com/resource"), ConnectableImpl.INVALID_CONNECTABLE);
        // exact id
        assertTrue(xmppAddressResolver.canResolve(new XMPPAddress("me@you.com/resource")));
        // different, but matching resource id
        assertTrue(xmppAddressResolver.canResolve(new XMPPAddress("me@you.com/resource1234")));
    }
    
    public void testCanResolveNoPresence() {
        assertFalse(xmppAddressResolver.canResolve(new XMPPAddress("me@you.com/resource")));
    }
    
    public void testCanResolveWrongAddressType() {
        assertFalse(xmppAddressResolver.canResolve(ConnectableImpl.INVALID_CONNECTABLE));
    }
    
    public void testResolveNoPresence() {
        try {
            xmppAddressResolver.resolve(new XMPPAddress("hello@world/ivef"), 
                    new BlockingAddressResolutionObserver()).getAddress();
            fail("io exception for failed resolution expected");
        } catch (IOException ie) {
        }
    }
    
    public void testResolveNoAddressForPresence() {
        try {
            xmppAddressResolver.resolve(new XMPPAddress("me@you.com/resource"), 
                    new BlockingAddressResolutionObserver()).getAddress();
            fail("io exception for failed resolution expected");
        } catch (IOException ie) {
        }
    }

    public void testResolveFirewalledAddressReturnsXMPPFirewalledAddress() throws Exception {
        final FirewalledAddress firewalledAddress = new FirewalledAddress(ConnectableImpl.INVALID_CONNECTABLE, ConnectableImpl.INVALID_CONNECTABLE, 
                new GUID(), Connectable.EMPTY_SET, 0);
        addressRegistry.put(new XMPPAddress("me@you.com/resource"), firewalledAddress);
        context.checking(new Expectations() {{
            one(socketsManager).canResolve(firewalledAddress);
            // return false so xmpp firewalled address can be constructed
            will(returnValue(false));
            // advertise connect back feature
            one(friendPresence).hasFeatures(ConnectBackRequestFeature.ID);
            will(returnValue(true));
        }});
        Address address = xmppAddressResolver.resolve(new XMPPAddress("me@you.com/resource"), 
                new BlockingAddressResolutionObserver()).getAddress();
        assertInstanceof(XMPPFirewalledAddress.class, address);
        XMPPFirewalledAddress xmppfiFirewalledAddress = (XMPPFirewalledAddress)address;
        assertSame(firewalledAddress, xmppfiFirewalledAddress.getFirewalledAddress());
        assertEquals(new XMPPAddress("me@you.com/resource"), xmppfiFirewalledAddress.getXmppAddress());
        context.assertIsSatisfied();
    }
    
    public void testResolveFirewalledAddressCheckesSocketsManagerForSameNatAndResolves() throws Exception {
        final FirewalledAddress firewalledAddress = new FirewalledAddress(ConnectableImpl.INVALID_CONNECTABLE, ConnectableImpl.INVALID_CONNECTABLE, 
                new GUID(), Connectable.EMPTY_SET, 0);
        final BlockingAddressResolutionObserver observer = new BlockingAddressResolutionObserver();
        addressRegistry.put(new XMPPAddress("me@you.com/resource"), firewalledAddress);
        context.checking(new Expectations() {{
            one(socketsManager).canResolve(firewalledAddress);
            // return false so xmpp firewalled address can be constructed
            will(returnValue(true));
            one(socketsManager).resolve(firewalledAddress, observer);
            will(new CustomAction("call observer") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    ((AddressResolutionObserver)invocation.getParameter(1)).resolved(ConnectableImpl.INVALID_CONNECTABLE);
                    return null;
                }
            });
        }});
        Address address = xmppAddressResolver.resolve(new XMPPAddress("me@you.com/resource"), 
                observer).getAddress();
        assertInstanceof(Connectable.class, address);
        assertSame(ConnectableImpl.INVALID_CONNECTABLE, address);
        context.assertIsSatisfied();
    }
    
    public void testResolveFirewalledAddressAsIs() throws Exception {
        final FirewalledAddress firewalledAddress = new FirewalledAddress(ConnectableImpl.INVALID_CONNECTABLE, ConnectableImpl.INVALID_CONNECTABLE, 
                new GUID(), Connectable.EMPTY_SET, 0);
        addressRegistry.put(new XMPPAddress("me@you.com/resource"), firewalledAddress);
        context.checking(new Expectations() {{
            one(socketsManager).canResolve(firewalledAddress);
            // return false so xmpp firewalled address can be constructed
            will(returnValue(false));
            // don't connect back feature, to get standard firewalled address
            one(friendPresence).hasFeatures(ConnectBackRequestFeature.ID);
            will(returnValue(false));
        }});
        Address address = xmppAddressResolver.resolve(new XMPPAddress("me@you.com/resource"), 
                new BlockingAddressResolutionObserver()).getAddress();
        assertInstanceof(FirewalledAddress.class, address);
        assertSame(firewalledAddress, address);
        context.assertIsSatisfied();
    }
    
    public void testResolveConnectable() throws Exception {
        addressRegistry.put(new XMPPAddress("me@you.com/resource"), ConnectableImpl.INVALID_CONNECTABLE);
        context.checking(new Expectations() {{
            never(socketsManager).canResolve(ConnectableImpl.INVALID_CONNECTABLE);
            // don't connect back feature, to get standard firewalled address
            never(friendPresence).hasFeatures(ConnectBackRequestFeature.ID);
        }});
        Address address = xmppAddressResolver.resolve(new XMPPAddress("me@you.com/resource"), 
                new BlockingAddressResolutionObserver()).getAddress();
        assertInstanceof(Connectable.class, address);
        assertSame(ConnectableImpl.INVALID_CONNECTABLE, address);
        context.assertIsSatisfied();
    }
}
