package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.FileOfferFeature;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.friend.impl.FileMetaDataImpl;
import org.limewire.friend.impl.FileMetaDataImpl.Element;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressRegistry;
import org.limewire.inject.AbstractModule;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.net.address.AddressEvent;
import org.xmlpull.v1.XmlPullParserException;

import com.google.inject.Injector;
import com.google.inject.Module;

public class XMPPServiceTest extends XmppBaseTestCase {

    private static final String USERNAME_1 = "automatedtestfriend1@gmail.com";
    private static final String USERNAME_2 = "automatedtestfriend2@gmail.com";
    private static final String PASSWORD_1 = "automatedtestfriend123";
    private static final String PASSWORD_2 = "automatedtestfriend234";

    private RosterListenerMock aliceRosterListener;
    private RosterListenerMock bobRosterListener;
    private FileOfferHandlerMock fileOfferHandler;
    private FriendConnection friendConnectionAlice;
    private FriendConnection friendConnectionBob;

    public XMPPServiceTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aliceRosterListener = new RosterListenerMock();
        bobRosterListener = new RosterListenerMock();
        FriendConnectionConfiguration alice = new FriendConnectionConfigurationMock(USERNAME_1, PASSWORD_1,
                SERVICE, aliceRosterListener);
        FriendConnectionConfiguration bob = new FriendConnectionConfigurationMock(USERNAME_2, PASSWORD_2,
                SERVICE, bobRosterListener);
        friendConnectionAlice = factories[0].login(alice).get();
        friendConnectionBob = factories[1].login(bob).get();
        // Allow login, roster, presence, library messages to be sent, received
        Thread.sleep(SLEEP * 2); // TODO wait()/notify()
        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));
        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        assertEquals(0, aliceRosterListener.getRosterSize());
        assertEquals(0, bobRosterListener.getRosterSize());
    }

    @Override
    protected List<Module> getServiceModules() {
        List<Module> defaultServiceModules = super.getServiceModules();
        List<Module> serviceModules = new ArrayList<Module>();
        fileOfferHandler = new FileOfferHandlerMock();
        serviceModules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FileOfferHandlerMock.class).toInstance(fileOfferHandler);
            }
        });        
        serviceModules.addAll(defaultServiceModules);
        return serviceModules;
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

        // send address update of Bob's address
        AddressEventTestBroadcaster addressEventBroadcaster = getAddressBroadcaster(injectors[1]);
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                AddressEvent.Type.ADDRESS_CHANGED));
        
        // send address update of Alices's address
        addressEventBroadcaster = getAddressBroadcaster(injectors[0]);
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                AddressEvent.Type.ADDRESS_CHANGED));

        Thread.sleep(SLEEP);

        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));
        FriendPresence buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        LimewireFeature limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(FriendPresence.Type.available, buddy2.getType());
        assertEquals(FriendPresence.Mode.available, buddy2.getMode());
        AddressFeature addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        FriendAddress friendAddress = (FriendAddress)addressFeature.getFeature();
        assertEquals(buddy2.getPresenceId(), friendAddress.getFullId());
        Connectable address = (Connectable)injectors[0].getInstance(FriendAddressRegistry.class).get(friendAddress);
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        AuthTokenFeature authTokenFeature = (AuthTokenFeature)buddy2.getFeature(AuthTokenFeature.ID);
        assertNotNull(authTokenFeature);

        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));
        FriendPresence buddy1 = bobRosterListener.getFirstPresence(USERNAME_1);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(FriendPresence.Type.available, buddy1.getType());
        assertEquals(FriendPresence.Mode.available, buddy2.getMode());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        friendAddress = (FriendAddress)addressFeature.getFeature();
        assertEquals(buddy1.getPresenceId(), friendAddress.getFullId());
        address = (Connectable)injectors[1].getInstance(FriendAddressRegistry.class).get(friendAddress);
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
    public void testUserLogout() throws InterruptedException, ExecutionException {
        assertTrue(friendConnectionAlice.isLoggedIn());
        friendConnectionAlice.logout().get();
        assertFalse(friendConnectionAlice.isLoggedIn());
    }

    /**
     * Tests that friends receive one another's status updates
     */
    public void testStatusChanges() throws InterruptedException, UnknownHostException, FriendException, ExecutionException {
        assertEquals(1, aliceRosterListener.getRosterSize());
        assertEquals(USERNAME_2, aliceRosterListener.getFirstRosterEntry());
        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));

        assertEquals(1, bobRosterListener.getRosterSize());
        assertEquals(USERNAME_1, bobRosterListener.getFirstRosterEntry());
        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));

        FriendPresence buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        assertEquals(FriendPresence.Type.available, buddy2.getType());
        assertEquals(FriendPresence.Mode.available, buddy2.getMode());

        friendConnectionBob.setMode(FriendPresence.Mode.away).get();

        Thread.sleep(SLEEP);

        buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        assertEquals(FriendPresence.Type.available, buddy2.getType());
        assertEquals(FriendPresence.Mode.away, buddy2.getMode());
    }

    /**
     * Tests that friends receive one another's chat messages
     */
    public void testChat() throws InterruptedException, FriendException, IOException {
        MessageReaderMock reader = new MessageReaderMock();
        FriendPresence automatedtestfriend2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        MessageWriter writer = automatedtestfriend2.getFriend().createChat(reader);
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
    public void testOfferFile() throws InterruptedException, IOException, XmlPullParserException, FriendException {

        FriendPresence automatedtestfriend2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        Map<Element, String> data = new EnumMap<Element, String>(Element.class);
        data.put(Element.id, new Random().nextInt() + "");
        data.put(Element.name, "a_cool_file.txt");
        data.put(Element.size, 1000 + "");
        data.put(Element.createTime, Long.toString(new Date().getTime()));
        data.put(Element.description, "cool file");
        data.put(Element.urns, "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        data.put(Element.index, "455");
        FileMetaDataImpl metaData = new FileMetaDataImpl(data);
        FileOfferFeature feature = (FileOfferFeature)automatedtestfriend2.getFeature(FileOfferFeature.ID);
        assertNotNull(feature);
        FeatureTransport<FileMetaData> fileOfferer = automatedtestfriend2.getTransport(FileOfferFeature.class);
        fileOfferer.sendFeature(automatedtestfriend2, metaData);

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

        getAddressBroadcaster(injectors[0]).listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                AddressEvent.Type.ADDRESS_CHANGED));
        getAddressBroadcaster(injectors[1]).listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                AddressEvent.Type.ADDRESS_CHANGED));

        Thread.sleep(SLEEP);

        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));
        FriendPresence buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        LimewireFeature limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(FriendPresence.Type.available, buddy2.getType());
        AddressFeature addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        FriendAddress friendAddress = (FriendAddress)addressFeature.getFeature();
        assertEquals(buddy2.getPresenceId(), friendAddress.getFullId());
        Connectable address = (Connectable)injectors[0].getInstance(FriendAddressRegistry.class).get(friendAddress);
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());

        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));
        FriendPresence buddy1 = bobRosterListener.getFirstPresence(USERNAME_1);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(FriendPresence.Type.available, buddy1.getType());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        friendAddress = (FriendAddress)addressFeature.getFeature();
        assertEquals(buddy1.getPresenceId(), friendAddress.getFullId());
        address = (Connectable)injectors[1].getInstance(FriendAddressRegistry.class).get(friendAddress);
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());

        getAddressBroadcaster(injectors[0]).listeners.broadcast(new AddressEvent(new ConnectableImpl("200.200.200.200", 5000, false),
                AddressEvent.Type.ADDRESS_CHANGED));
        getAddressBroadcaster(injectors[1]).listeners.broadcast(new AddressEvent(new ConnectableImpl("200.200.200.200", 5000, false),
                AddressEvent.Type.ADDRESS_CHANGED));

        Thread.sleep(SLEEP);

        assertEquals(1, aliceRosterListener.countPresences(USERNAME_2));
        buddy2 = aliceRosterListener.getFirstPresence(USERNAME_2);
        limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(FriendPresence.Type.available, buddy2.getType());
        addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        friendAddress = (FriendAddress)addressFeature.getFeature();
        assertEquals(buddy2.getPresenceId(), friendAddress.getFullId());
        address = (Connectable)injectors[0].getInstance(FriendAddressRegistry.class).get(friendAddress);
        assertEquals("200.200.200.200", address.getAddress());
        assertEquals(5000, address.getPort());
        assertEquals(false, address.isTLSCapable());

        assertEquals(1, bobRosterListener.countPresences(USERNAME_1));
        buddy1 = bobRosterListener.getFirstPresence(USERNAME_1);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(FriendPresence.Type.available, buddy1.getType());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        friendAddress = (FriendAddress)addressFeature.getFeature();
        assertEquals(buddy1.getPresenceId(), friendAddress.getFullId());
        address = (Connectable)injectors[1].getInstance(FriendAddressRegistry.class).get(friendAddress);
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
            throws InterruptedException, FriendException, UnknownHostException, ExecutionException {
        Injector injector = createInjector(getModules());
        ServiceRegistry registry = injector.getInstance(ServiceRegistry.class);
        registry.start();
        XMPPConnectionFactoryImpl factory = injector.getInstance(XMPPConnectionFactoryImpl.class);
        try {
            // Create a second presence for Bob
            RosterListenerMock bob2RosterListener = new RosterListenerMock();
            FriendConnectionConfiguration bob2 = 
                new FriendConnectionConfigurationMock(USERNAME_2, PASSWORD_2,
                        SERVICE, bob2RosterListener);
            factory.login(bob2).get();
            Thread.sleep(SLEEP);
            
            getAddressBroadcaster(injectors[0]).listeners.broadcast(new AddressEvent(
                    new ConnectableImpl("199.199.199.199", 2048, true),
                    AddressEvent.Type.ADDRESS_CHANGED));
            Thread.sleep(SLEEP);
            
            // Simulate Alice talking to two presences of Bob
            MessageReaderMock aliceFromBob = new MessageReaderMock();
            FriendPresence bobPresence = aliceRosterListener.getFirstPresence(USERNAME_2);
            MessageWriter aliceToBob = bobPresence.getFriend().createChat(aliceFromBob);
            
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
        } finally {
            factory.stop();
            registry.stop();
        }
    }
}