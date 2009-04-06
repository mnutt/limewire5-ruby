package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.listener.BlockingEventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.net.address.StrictIpPortSet;
import org.limewire.rudp.RUDPUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

public class NetworkManagerImplTest extends LimeTestCase {
    
    private Injector injector;
    private AddressChangedListener addressChangedListener;
    
    public NetworkManagerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        injector = LimeTestUtils.createInjectorAndStart(new AbstractModule() {
            @Override
            protected void configure() {
                //bind(ConnectionManager.class).toInstance(connectionManager);
                bind(AddressChangedListener.class);               
            }
        });
        addressChangedListener = injector.getInstance(AddressChangedListener.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setListeningPort(0);
    }
    
    @Override
    protected void tearDown() {
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        lifecycleManager.shutdown();    
    }
    
    private void assertEquals(Connectable connectable, Address address) {
        assertEquals(connectable + "!=" + address, 0, ConnectableImpl.COMPARATOR.compare(connectable, (Connectable) address));
    }
    
    public void testDirectConnectionAddressEvent() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName(RemoteFileDesc.BOGUS_IP));
        assertNull(addressChangedListener.getEvent());
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        assertNull(addressChangedListener.getEvent());
        acceptor.setListeningPort(5000);
        assertNull(addressChangedListener.getEvent());
        acceptor.setIncoming(true);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), event.getData());
    }
    
    public void testNewDirectConnectionAddressEventAcceptedIncomingTrigger() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        assertNull(addressChangedListener.getEvent());
        acceptor.setIncoming(true);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), event.getData());
    }
    
    public void testNewDirectConnectionAddressEventExternalAddressTrigger() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);        
        acceptor.setIncoming(true);
        acceptor.setListeningPort(5000);
        assertNull(addressChangedListener.getEvent());
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), event.getData());
    }
    
    public void testNewDirectConnectionAddressEventPortTrigger() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);        
        acceptor.setIncoming(true);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        assertNull(addressChangedListener.getEvent());
        acceptor.setListeningPort(5000);        
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), event.getData());
    }    
    
    public void testDirectConnectionAddressEventNoDups() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), event.getData());
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        assertNull(addressChangedListener.getEvent());
        acceptor.setListeningPort(5000);
        assertNull(addressChangedListener.getEvent());
        acceptor.setIncoming(true);
        assertNull(addressChangedListener.getEvent());
    }
    
    public void testDirectConnectionAddressEventTriggeredAfterAllInfoIsThere() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        
        acceptor.setExternalAddress(InetAddress.getByAddress(new byte[] { (byte)129, 0, 0, 1 }));
        assertNull(addressChangedListener.getEvent());
        acceptor.setListeningPort(5000);
        assertNull(addressChangedListener.getEvent());
        acceptor.setIncoming(true);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("129.0.0.1", 5000, true), event.getData());
    }
    
    public void testDirectConnectionAddressChangedExternalAddressTrigger() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), event.getData());
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("200.200.200.200", 5000, true), event.getData());
    }
    
    public void testDirectConnectionAddressChangedEventPortTrigger() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), event.getData());
        acceptor.setListeningPort(5001);
        event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("199.199.199.199", 5001, true), event.getData());
    }
    
    public void testDirectConnectionAddressSupressesPushProxyAddressEvent() throws IOException {
        assertNull(addressChangedListener.getEvent());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        
        acceptor.setExternalAddress(InetAddress.getByAddress(new byte[] { (byte)129, 0, 0, 1 }));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(new ConnectableImpl("129.0.0.1", 5000, true), event.getData());
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        assertNull(addressChangedListener.getEvent());
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        assertNull(addressChangedListener.getEvent());
    }
    
    public void testPushProxyAddressEvent() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl) injector.getInstance(Acceptor.class);
        assertNull(addressChangedListener.getEvent());
        
        acceptor.setListeningPort(5000);
        acceptor.setExternalAddress(InetAddress.getByName("129.0.0.1"));
        
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        FirewalledAddress firewalledAddress = (FirewalledAddress)event.getData();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
    }
    
    public void testPushProxyAddressChangedEvent() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        // regular setup which usually takes place before first push proxies come in
        acceptor.setListeningPort(5000);
        acceptor.setExternalAddress(InetAddress.getByName("129.0.0.1"));
        assertNull(addressChangedListener.getEvent());
        
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        networkManager.newPushProxies(proxies);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        FirewalledAddress firewalledAddress = (FirewalledAddress)event.getData();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
        
        proxies = new StrictIpPortSet<Connectable>();
        Connectable proxyAddress = new ConnectableImpl("199.199.199.199", 5000, true);
        Connectable proxyAddress2 = new ConnectableImpl("200.200.200.200", 5001, false);
        proxies.add(proxyAddress);
        proxies.add(proxyAddress2);
        networkManager.newPushProxies(proxies);

        event = addressChangedListener.getEvent();
        assertNotNull(event);
        firewalledAddress = (FirewalledAddress)event.getData();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
    }
    
    public void testPushProxyAddressEventNoDups() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        // regular setup which usually takes place before first push proxies come in
        acceptor.setListeningPort(5000);
        acceptor.setExternalAddress(InetAddress.getByName("129.0.0.1"));
        assertNull(addressChangedListener.getEvent());
        
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        FirewalledAddress firewalledAddress = (FirewalledAddress)event.getData();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
                
        proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        assertNull(addressChangedListener.getEvent());
    }
    
    public void testNewPushProxyHolePunchAddressEventFWTStatusTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        // regular setup which usually takes place before first push proxies come in
        acceptor.setListeningPort(5000);
        acceptor.setExternalAddress(InetAddress.getByName("129.0.0.1"));
        assertNull(addressChangedListener.getEvent());
        
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        FirewalledAddress firewalledAddress = (FirewalledAddress)event.getData();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
        assertEquals(0, firewalledAddress.getFwtVersion());
        // set valid external address and port for proper fwt support
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);
        
        // signal fwt capability
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        networkManager.incomingStatusChanged();
        event = addressChangedListener.getEvent();
        assertNotNull(event);
        
        acceptor.setExternalAddress(InetAddress.getByName("100.100.100.100"));
        acceptor.setListeningPort(5001);        
        networkManager.incomingStatusChanged();
        event = addressChangedListener.getEvent();
        assertNotNull(event);
        
        firewalledAddress = (FirewalledAddress)event.getData();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
        assertEquals(RUDPUtils.VERSION, firewalledAddress.getFwtVersion());
        assertEquals(new ConnectableImpl("100.100.100.100", 5001, true), firewalledAddress.getPublicAddress());
    }
    
    public void testNewPushProxyHolePunchAddressEventPushProxyTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertNull(addressChangedListener.getEvent());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        networkManager.incomingStatusChanged();
        assertNull(addressChangedListener.getEvent());
        
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        InetAddress privateAddress = InetAddress.getByName("192.168.0.1");
        acceptor.setAddress(privateAddress);
        acceptor.setListeningPort(5001);   
        
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        FirewalledAddress expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl(new InetSocketAddress(privateAddress, 5001), networkManager.isIncomingTLSEnabled()), new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        assertEquals(expectedAddress, event.getData());
    }
    
    public void testPushProxyHolePunchAddressChangedEventPushProxyTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertNull(addressChangedListener.getEvent());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        InetAddress privateAddress = InetAddress.getByName("192.168.0.1");
        acceptor.setAddress(privateAddress);
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);   
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        
        networkManager.newPushProxies(proxies);
        
        FirewalledAddress expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl(new InetSocketAddress(privateAddress, networkManager.getNonForcedPort()), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(expectedAddress, event.getData());
                
        proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        proxies.add(new ConnectableImpl("201.201.201.201", 5002, false));
        networkManager.newPushProxies(proxies);
        
        expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl(new InetSocketAddress(privateAddress, networkManager.getNonForcedPort()), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(expectedAddress, event.getData());
    }
    
    public void testPushProxyHolePunchAddressChangedEventFWTStatusTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertNull(addressChangedListener.getEvent());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        InetAddress privateAddress = InetAddress.getByName("192.168.0.1");
        acceptor.setAddress(privateAddress);
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);   
        
        Set<Connectable> proxies = new HashSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        
        FirewalledAddress expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl(new InetSocketAddress(privateAddress, networkManager.getNonForcedPort()), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(expectedAddress, event.getData());
        
        udpService.setReceiveSolicited(false);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        networkManager.incomingStatusChanged();
        
        expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl(new InetSocketAddress(privateAddress, networkManager.getNonForcedPort()), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(expectedAddress, event.getData());
    }
    
    public void testPushProxyHolePunchAddressEventNoDups() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertNull(addressChangedListener.getEvent());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        InetAddress privateAddress = InetAddress.getByName("192.168.0.1");
        acceptor.setAddress(privateAddress);
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);   
        
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        
        FirewalledAddress expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl(new InetSocketAddress(privateAddress, networkManager.getNonForcedPort()), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
                
        AddressEvent event = addressChangedListener.getEvent();
        assertNotNull(event);
        assertEquals(expectedAddress, event.getData());
                
        networkManager.incomingStatusChanged();
        assertNull(addressChangedListener.getEvent());
        
        proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        networkManager.newPushProxies(proxies);
        assertNull(addressChangedListener.getEvent());
    }
    
    public void testNewPushProxiesDoesNotTriggerEventIfExternalAddressNotKnownYet() throws Exception {
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        Acceptor acceptor = injector.getInstance(Acceptor.class);
        
        acceptor.setListeningPort(5000);
        // no events, since no valid address yet
        assertNull(addressChangedListener.getEvent());
                
        networkManager.newPushProxies(new StrictIpPortSet<Connectable>(new ConnectableImpl("129.0.0.1", 4545, true)));
        // no events, since no valid address yet
        assertNull(addressChangedListener.getEvent());
        
        // now set external address and test for events again
        acceptor.setExternalAddress(InetAddress.getByName("199.0.0.1"));
        AddressEvent event = addressChangedListener.getEvent();
        FirewalledAddress address = (FirewalledAddress) event.getData();
        assertEquals(new ConnectableImpl("199.0.0.1", 5000, true), address.getPublicAddress());
        assertEquals(new StrictIpPortSet<Connectable>(new ConnectableImpl("129.0.0.1", 4545, true)), address.getPushProxies());
    }

    @Singleton
    public static class AddressChangedListener extends BlockingEventListener<AddressEvent> {
        @Inject
        void register(ListenerSupport<AddressEvent> broadcaster) {
            broadcaster.addListener(this);
        }
    
        AddressEvent getEvent() {
            return getEvent(25, TimeUnit.MILLISECONDS);
        }
    }
}
