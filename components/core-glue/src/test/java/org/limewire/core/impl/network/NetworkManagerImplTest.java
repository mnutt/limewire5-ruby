package org.limewire.core.impl.network;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.NetworkManager;

public class NetworkManagerImplTest extends BaseTestCase {

    public NetworkManagerImplTest(String name) {
        super(name);
    }

    public void testAddressChanged() throws Exception {
        Mockery context = new Mockery();
        final NetworkManager networkManager = context.mock(NetworkManager.class);

        NetworkManagerImpl networkManagerImpl = new NetworkManagerImpl(networkManager);

        context.checking(new Expectations() {
            {
                one(networkManager).addressChanged();
                will(returnValue(true));
            }
        });
        assertTrue(networkManagerImpl.addressChanged());

        context.checking(new Expectations() {
            {
                one(networkManager).addressChanged();
                will(returnValue(false));
            }
        });
        assertFalse(networkManagerImpl.addressChanged());
        context.assertIsSatisfied();
    }

    public void testIsIncomingTLSEnabled() throws Exception {
        Mockery context = new Mockery();
        final NetworkManager networkManager = context.mock(NetworkManager.class);

        NetworkManagerImpl networkManagerImpl = new NetworkManagerImpl(networkManager);

        context.checking(new Expectations() {
            {
                one(networkManager).isIncomingTLSEnabled();
                will(returnValue(true));
            }
        });
        assertTrue(networkManagerImpl.isIncomingTLSEnabled());

        context.checking(new Expectations() {
            {
                one(networkManager).isIncomingTLSEnabled();
                will(returnValue(false));
            }
        });
        assertFalse(networkManagerImpl.isIncomingTLSEnabled());
        context.assertIsSatisfied();
    }

    public void testIsOutgoingTLSEnabled() throws Exception {
        Mockery context = new Mockery();
        final NetworkManager networkManager = context.mock(NetworkManager.class);

        NetworkManagerImpl networkManagerImpl = new NetworkManagerImpl(networkManager);

        context.checking(new Expectations() {
            {
                one(networkManager).isOutgoingTLSEnabled();
                will(returnValue(true));
            }
        });
        assertTrue(networkManagerImpl.isOutgoingTLSEnabled());

        context.checking(new Expectations() {
            {
                one(networkManager).isOutgoingTLSEnabled();
                will(returnValue(false));
            }
        });
        assertFalse(networkManagerImpl.isOutgoingTLSEnabled());
        context.assertIsSatisfied();
    }

    public void testSetIncomingTLSEnabled() throws Exception {
        Mockery context = new Mockery();
        final NetworkManager networkManager = context.mock(NetworkManager.class);

        NetworkManagerImpl networkManagerImpl = new NetworkManagerImpl(networkManager);

        context.checking(new Expectations() {
            {
                one(networkManager).setIncomingTLSEnabled(true);
            }
        });
        networkManagerImpl.setIncomingTLSEnabled(true);

        context.checking(new Expectations() {
            {
                one(networkManager).setIncomingTLSEnabled(false);
            }
        });
        networkManagerImpl.setIncomingTLSEnabled(false);
        context.assertIsSatisfied();
    }

    public void testSetOutgoingTLSEnabled() throws Exception {
        Mockery context = new Mockery();
        final NetworkManager networkManager = context.mock(NetworkManager.class);

        NetworkManagerImpl networkManagerImpl = new NetworkManagerImpl(networkManager);

        context.checking(new Expectations() {
            {
                one(networkManager).setOutgoingTLSEnabled(true);
            }
        });
        networkManagerImpl.setOutgoingTLSEnabled(true);

        context.checking(new Expectations() {
            {
                one(networkManager).setOutgoingTLSEnabled(false);
            }
        });
        networkManagerImpl.setOutgoingTLSEnabled(false);
        context.assertIsSatisfied();
    }

    public void testSetListeningPort() throws Exception {
        Mockery context = new Mockery();
        final NetworkManager networkManager = context.mock(NetworkManager.class);

        NetworkManagerImpl networkManagerImpl = new NetworkManagerImpl(networkManager);

        context.checking(new Expectations() {
            {
                one(networkManager).setListeningPort(1234);
            }
        });
        networkManagerImpl.setListeningPort(1234);

        context.checking(new Expectations() {
            {
                one(networkManager).setListeningPort(5678);
            }
        });
        networkManagerImpl.setListeningPort(5678);
        context.assertIsSatisfied();
    }
    
    public void testPortChanged() throws Exception {
        Mockery context = new Mockery();
        final NetworkManager networkManager = context.mock(NetworkManager.class);

        NetworkManagerImpl networkManagerImpl = new NetworkManagerImpl(networkManager);

        context.checking(new Expectations() {
            {
                one(networkManager).portChanged();
            }
        });
        networkManagerImpl.portChanged();
        context.assertIsSatisfied();
    }
}
