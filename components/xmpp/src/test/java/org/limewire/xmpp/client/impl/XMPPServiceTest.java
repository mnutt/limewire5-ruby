package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.limewire.common.LimeWireCommonModule;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.friend.feature.features.FileOfferFeature;
import org.limewire.core.api.friend.feature.features.FileOfferer;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.http.auth.LimeWireHttpAuthModule;
import org.limewire.inject.AbstractModule;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.EmptyProxySettings;
import org.limewire.net.EmptySocketBindingSettings;
import org.limewire.net.LimeWireNetModule;
import org.limewire.net.ProxySettings;
import org.limewire.net.SocketBindingSettings;
import org.limewire.net.address.AddressEvent;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.client.impl.messages.FileMetaDataImpl;
import org.xmlpull.v1.XmlPullParserException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

public class XMPPServiceTest extends BaseTestCase {

    private static final String USERNAME_1 = "automatedtestfriend1@gmail.com";
    private static final String USERNAME_2 = "automatedtestfriend2@gmail.com";
    private static final String PASSWORD_1 = "automatedtestfriend123";
    private static final String PASSWORD_2 = "automatedtestfriend234";
    private static final String SERVICE = "gmail.com";

    private static final int SLEEP = 5000; // Milliseconds

    private ServiceRegistry registry;
    private XMPPServiceImpl service;
    private RosterListenerMock aliceRosterListener;
    private RosterListenerMock bobRosterListener;
    private AddressEventTestBroadcaster addressEventBroadcaster;
    private FileOfferHandlerMock fileOfferHandler;
    private XMPPAddressRegistry addressRegistry;

    public XMPPServiceTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        Injector injector = createInjector(getModules());
        addressRegistry = injector.getInstance(XMPPAddressRegistry.class);
        registry = injector.getInstance(ServiceRegistry.class);
        registry.initialize();
        registry.start();
        service = injector.getInstance(XMPPServiceImpl.class);
        service.setMultipleConnectionsAllowed(true);
        aliceRosterListener = new RosterListenerMock();
        bobRosterListener = new RosterListenerMock();
        XMPPConnectionConfiguration alice = new XMPPConnectionConfigurationMock(USERNAME_1, PASSWORD_1,
                SERVICE, aliceRosterListener);
        XMPPConnectionConfiguration bob = new XMPPConnectionConfigurationMock(USERNAME_2, PASSWORD_2,
                SERVICE, bobRosterListener);
        service.login(alice);
        service.login(bob);
        // Allow login, roster, presence, library messages to be sent, received
        Thread.sleep(SLEEP); // TODO wait()/notify()
        assertEquals("another automatedtestfriend2 presence has been detected, test cannnot run", 1, aliceRosterListener.countPresences(USERNAME_2));
        assertEquals("another automatedtestfriend1 presence has been detected, test cannnot run", 1, bobRosterListener.countPresences(USERNAME_1));
    }

    protected Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.PRODUCTION, modules);
    }

    private Module [] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCommonModule());
        modules.add(new LimeWireHttpAuthModule());
        modules.addAll(getServiceModules());
        return modules.toArray(new Module[modules.size()]);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        registry.stop();
        // Allow logout messages to be sent, received before next test
        Thread.sleep(SLEEP);
    }

    protected List<Module> getServiceModules() {
        Module xmppModule = new LimeWireXMPPTestModule();
        addressEventBroadcaster = new AddressEventTestBroadcaster();
        fileOfferHandler = new FileOfferHandlerMock();
        Module m = new AbstractModule() {
            protected void configure() {
                bind(new TypeLiteral<ListenerSupport<AddressEvent>>(){}).toInstance(addressEventBroadcaster);
                bind(FileOfferHandlerMock.class).toInstance(fileOfferHandler);
                bind(XMPPConnectionListenerMock.class);
            }
        };
        return Arrays.asList(xmppModule, m, new LimeWireNetTestModule());
    }

    /**
     * Tests that two friends can see each other
     */
    public void testRosterIsPopulated() throws InterruptedException, UnknownHostException {
        assertEquals(1, aliceRosterListener.getRosterSize());
        assertEquals(USERNAME_2, aliceRosterListener.getFirstRosterEntry());
        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));

        assertEquals(1, bobRosterListener.getRosterSize());
        assertEquals(USERNAME_1, bobRosterListener.getFirstRosterEntry());
        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));
    }

    /**
     * Tests that a friend can be renamed in the roster
     */
    public void testSetName() throws InterruptedException {
        // Reset the name first, in case a previous test left it as "foo"
        aliceRosterListener.getUser(USERNAME_2).setName("buddy2");
        Thread.sleep(SLEEP);
        assertEquals("buddy2", aliceRosterListener.getUser(USERNAME_2).getName());
        aliceRosterListener.getUser(USERNAME_2).setName("foo");
        Thread.sleep(SLEEP);
        assertEquals("foo", aliceRosterListener.getUser(USERNAME_2).getName());
        aliceRosterListener.getUser(USERNAME_2).setName("buddy2");
    }

    /**
     * Tests that friends logged in through LimeWire can detect each other
     * and exchange addresses 
     */
    public void testAddresses() throws InterruptedException, UnknownHostException {
        assertEquals(1, aliceRosterListener.getRosterSize());
        assertEquals(USERNAME_2, aliceRosterListener.getFirstRosterEntry());
        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));

        assertEquals(1, bobRosterListener.getRosterSize());
        assertEquals(USERNAME_1, bobRosterListener.getFirstRosterEntry());
        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));

        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));

        Thread.sleep(SLEEP);

        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));
        Presence buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        LimewireFeature limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());
        AddressFeature addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        XMPPAddress xmppAddress = (XMPPAddress)addressFeature.getFeature();
        assertEquals(buddy2.getJID(), xmppAddress.getFullId());
        Connectable address = (Connectable)addressRegistry.get(xmppAddress);
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        AuthTokenFeature authTokenFeature = (AuthTokenFeature)buddy2.getFeature(AuthTokenFeature.ID);
        assertNotNull(authTokenFeature);

        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));
        Presence buddy1 = bobRosterListener.getFirstPresence(USERNAME_1);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy1.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        xmppAddress = (XMPPAddress)addressFeature.getFeature();
        assertEquals(buddy1.getJID(), xmppAddress.getFullId());
        address = (Connectable)addressRegistry.get(xmppAddress);
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        authTokenFeature = (AuthTokenFeature)buddy1.getFeature(AuthTokenFeature.ID);
        assertNotNull(authTokenFeature);
    }

    /**
     * Tests that logging out removes the connection from the service's
     * list of connections
     */
    public void testUserLogout() throws InterruptedException {
        List<? extends XMPPConnection> connections = service.getConnections();
        assertTrue(connections.get(0).isLoggedIn());
        connections.get(0).logout();
        assertFalse(connections.get(0).isLoggedIn());
    }

    /**
     * Tests that friends receive one another's status updates
     */
    public void testStatusChanges() throws InterruptedException, UnknownHostException {
        assertEquals(1, aliceRosterListener.getRosterSize());
        assertEquals(USERNAME_2, aliceRosterListener.getFirstRosterEntry());
        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));

        assertEquals(1, bobRosterListener.getRosterSize());
        assertEquals(USERNAME_1, bobRosterListener.getFirstRosterEntry());
        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));

        Presence buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());

        for(XMPPConnection connection : service.getConnections()) {
            if(connection.getConfiguration().getUserInputLocalID().equals(USERNAME_2)) {
                connection.setMode(Presence.Mode.away);
            }
        }

        Thread.sleep(SLEEP); 

        buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.away, buddy2.getMode());
    }

    /**
     * Tests that friends receive one another's chat messages
     */
    public void testChat() throws InterruptedException, XMPPException, IOException {
        MessageReaderMock reader = new MessageReaderMock();
        Presence automatedtestfriend2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        MessageWriter writer = automatedtestfriend2.getUser().createChat(reader);
        writer.writeMessage("hello world");

        Thread.sleep(SLEEP);

        IncomingChatListenerMock incomingChatListener2 = bobRosterListener.listener;
        MessageWriter writer2 = incomingChatListener2.writer;
        writer2.writeMessage("goodbye world");

        Thread.sleep(SLEEP);

        assertEquals(1, incomingChatListener2.reader.messages.size());
        assertEquals("hello world", incomingChatListener2.reader.messages.get(0));

        assertEquals(1, reader.messages.size());
        assertEquals("goodbye world", reader.messages.get(0)); 

    }

    /**
     * Tests that friends receive one another's file offers
     */
    public void testOfferFile() throws InterruptedException, IOException, XmlPullParserException {

        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));

        Thread.sleep(SLEEP);

        Presence automatedtestfriend2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        FileMetaDataImpl metaData = new FileMetaDataImpl();
        metaData.setId(new Random().nextInt() + "");
        metaData.setName("a_cool_file.txt");
        metaData.setSize(1000);
        metaData.setCreateTime(new Date());
        metaData.setDescription("cool file");
        FileOfferFeature feature = (FileOfferFeature)automatedtestfriend2.getFeature(FileOfferFeature.ID);
        assertNotNull(feature);
        FileOfferer fileOfferer = feature.getFeature();
        fileOfferer.offerFile(metaData);

        Thread.sleep(SLEEP);

        List<FileMetaData> offers = fileOfferHandler.offers;
        assertEquals(1, offers.size());
        assertEquals("a_cool_file.txt", offers.get(0).getName());
    }

    /**
     * Tests that friends logged in through LimeWire receive one another's
     * updated addresses
     */
    public void testDetectAddressChanges() throws InterruptedException, UnknownHostException {
        assertEquals(1, aliceRosterListener.getRosterSize());
        assertEquals(USERNAME_2, aliceRosterListener.getFirstRosterEntry());
        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));

        assertEquals(1, bobRosterListener.getRosterSize());
        assertEquals(USERNAME_1, bobRosterListener.getFirstRosterEntry());
        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));

        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));

        Thread.sleep(SLEEP);

        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));
        Presence buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        LimewireFeature limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy2.getType());
        AddressFeature addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        XMPPAddress xmppAddress = (XMPPAddress)addressFeature.getFeature();
        assertEquals(buddy2.getJID(), xmppAddress.getFullId());
        Connectable address = (Connectable)addressRegistry.get(xmppAddress);
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());

        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));
        Presence buddy1 = bobRosterListener.getFirstPresence(USERNAME_1);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy1.getType());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        xmppAddress = (XMPPAddress)addressFeature.getFeature();
        assertEquals(buddy1.getJID(), xmppAddress.getFullId());
        address = (Connectable)addressRegistry.get(xmppAddress);
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());

        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("200.200.200.200", 5000, false),
                Address.EventType.ADDRESS_CHANGED));

        Thread.sleep(SLEEP);

        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));
        buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy2.getType());
        addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        xmppAddress = (XMPPAddress)addressFeature.getFeature();
        assertEquals(buddy2.getJID(), xmppAddress.getFullId());
        address = (Connectable)addressRegistry.get(xmppAddress);
        assertEquals("200.200.200.200", address.getAddress());
        assertEquals(5000, address.getPort());
        assertEquals(false, address.isTLSCapable());

        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));
        buddy1 = bobRosterListener.getFirstPresence(USERNAME_1);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy1.getType());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        xmppAddress = (XMPPAddress)addressFeature.getFeature();
        assertEquals(buddy1.getJID(), xmppAddress.getFullId());
        address = (Connectable)addressRegistry.get(xmppAddress);
        assertEquals("200.200.200.200", address.getAddress());
        assertEquals(5000, address.getPort());
        assertEquals(false, address.isTLSCapable());
    }

    /**
     * Tests that when a user is logged in more than once, chat messages
     * are received by all the user's presences until one of them replies,
     * after which they're received by whichever presence replied most recently
     */
    public void testChatWithMultiplePresencesOfSameUser()
    throws InterruptedException, XMPPException, UnknownHostException {
        // Create a second presence for Bob
        RosterListenerMock bob2RosterListener = new RosterListenerMock();
        XMPPConnectionConfiguration bob2 = 
            new XMPPConnectionConfigurationMock(USERNAME_2, PASSWORD_2,
                    SERVICE, bob2RosterListener);
        service.login(bob2);
        Thread.sleep(SLEEP);

        addressEventBroadcaster.listeners.broadcast(new AddressEvent(
                new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        Thread.sleep(SLEEP);

        // Simulate Alice talking to two presences of Bob
        MessageReaderMock aliceFromBob = new MessageReaderMock();
        Presence bobPresence = aliceRosterListener.getFirstPresence(USERNAME_2);
        MessageWriter aliceToBob = bobPresence.getUser().createChat(aliceFromBob);

        // Alice writes a message to Bob
        aliceToBob.writeMessage("Hello Bob");
        aliceToBob.writeMessage("Both Bobs should get this");
        Thread.sleep(SLEEP);

        // Confirm that both presences of Bob get the message
        List<String> receivedByBob = bobRosterListener.listener.reader.messages;
        List<String> receivedByBob2 = bob2RosterListener.listener.reader.messages;

        assertEquals(2, receivedByBob.size());
        assertEquals("Hello Bob", receivedByBob.get(0));
        assertEquals("Both Bobs should get this", receivedByBob.get(1));

        assertEquals(2, receivedByBob2.size());
        assertEquals("Hello Bob", receivedByBob2.get(0));
        assertEquals("Both Bobs should get this", receivedByBob2.get(1));

        // Bob sends a message; Alice should get it
        MessageWriter bobToAlice = bobRosterListener.listener.writer;
        bobToAlice.writeMessage("Bob writing to Alice");
        Thread.sleep(SLEEP);
        assertEquals(1, aliceFromBob.messages.size());
        assertEquals("Bob writing to Alice", aliceFromBob.messages.get(0));

        // When Alice writes back, only Bob should receive the message
        aliceToBob.writeMessage("Only Bob should get this");
        Thread.sleep(SLEEP);
        assertEquals(3, receivedByBob.size()); // One extra message received
        assertEquals("Only Bob should get this", receivedByBob.get(2));
        assertEquals(2, receivedByBob2.size()); // Other presence is unaffected

        // Bob2 writes to Alice; when Alice writes back, only Bob2 should
        // receive the message
        MessageWriter bob2ToAlice = bob2RosterListener.listener.writer;
        bob2ToAlice.writeMessage("Bob2 writing to Alice");
        Thread.sleep(SLEEP);
        assertEquals(2, aliceFromBob.messages.size());
        assertEquals("Bob2 writing to Alice", aliceFromBob.messages.get(1));

        aliceToBob.writeMessage("Only Bob2 should get this");
        Thread.sleep(SLEEP);
        assertEquals(3, receivedByBob2.size()); // One extra message received
        assertEquals("Only Bob2 should get this", receivedByBob2.get(2));
        assertEquals(3,receivedByBob.size()); // Other presence is unaffected
    }

    class LimeWireNetTestModule extends LimeWireNetModule {
        @Override
        protected void configure() {
            super.configure();
            bind(ProxySettings.class).to(EmptyProxySettings.class);
            bind(SocketBindingSettings.class).to(EmptySocketBindingSettings.class);
            bind(NetworkInstanceUtils.class).to(SimpleNetworkInstanceUtils.class);
        }
    }
}