package org.limewire.xmpp.client.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendRequest;
import org.limewire.friend.api.FriendRequestEvent;
import org.limewire.friend.api.RosterEvent;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.TypeLiteral;
import com.google.inject.Key;

/**
 * Test cases pertaining to friend subscriptions
 * (friend requests, and friend accept/decline/removes)
 */
public class XmppFriendSubscriptionTest extends XmppBaseTestCase {
    
    private static final Log LOG = LogFactory.getLog(XmppFriendSubscriptionTest.class);
    private static final int MAX_WAIT_FRIEND_REQUEST = 20;
    
       
    static {
        // get the hour (00 - 23) and modulo 4 in order to cycle thru
        // gmail usernames because of server friend request limitations
        Calendar cal = new GregorianCalendar();
        int numUser = cal.get(Calendar.HOUR_OF_DAY) % 4;
        USERNAME_A = "automatedtestfrienda" + numUser + "@gmail.com";
        USERNAME_B = "automatedtestfriendb" + numUser + "@gmail.com";
    }
    
    private static final String USERNAME_A;
    private static final String USERNAME_B;
    private static final String PASSWORD = "automatedtestfriend";

    private RosterListenerMock autoFriendARosterListener;
    private RosterListenerMock autoFriendBRosterListener;
    private EventListener<FriendRequestEvent> acceptFriendListener;
    private XMPPFriendConnectionImpl connectionA;
    private XMPPFriendConnectionImpl connectionB;

    public XmppFriendSubscriptionTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        autoFriendARosterListener = new RosterListenerMock();
        autoFriendBRosterListener = new RosterListenerMock();

        FriendConnectionConfiguration configFive = new FriendConnectionConfigurationMock(USERNAME_A, PASSWORD,
                SERVICE, autoFriendARosterListener);
        FriendConnectionConfiguration configSix = new FriendConnectionConfigurationMock(USERNAME_B, PASSWORD,
                SERVICE, autoFriendBRosterListener);

        connectionA = (XMPPFriendConnectionImpl)factories[0].login(configFive).get();
        connectionB = (XMPPFriendConnectionImpl)factories[1].login(configSix).get();

        // Allow login, roster, presence, library messages to be sent, received
        Thread.sleep(SLEEP);

        // Make sure that both friend5 and friend6 have empty rosters before beginning
        if (autoFriendARosterListener.getRosterSize() != 0) {
            removeAllUsersFromRoster(connectionA);
        }
        if (autoFriendARosterListener.getRosterSize() != 0) {
            removeAllUsersFromRoster(connectionB);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        removeFriendRequestListener();
        connectionA.removeFriend(USERNAME_B);
        connectionB.removeFriend(USERNAME_A);
        super.tearDown();
    }

    private EventListener<FriendRequestEvent> createFriendRequestListener(final String userName, final boolean accept) {
        return new EventListener<FriendRequestEvent>() {
            @Override
            public void handleEvent(FriendRequestEvent event) {
                LOG.debugf("handling friend request: {0}", event);
                FriendRequest friendRequest = event.getData();
                if (friendRequest.getFriendUsername().equals(userName)) {
                    friendRequest.getDecisionHandler().handleDecision(userName, accept);
                }
            }
        };
    }

    private void setFriendRequestListener(String userName, boolean accept) {
        acceptFriendListener = createFriendRequestListener(userName, accept);
        injectors[1].getInstance(Key.get(new TypeLiteral<ListenerSupport<FriendRequestEvent>>(){})).
                addListener(acceptFriendListener);
    }

    private void removeFriendRequestListener() {
        injectors[1].getInstance(Key.get(new TypeLiteral<ListenerSupport<FriendRequestEvent>>(){})).
                removeListener(acceptFriendListener);
        acceptFriendListener = null;
    }


    /**
     * Test friend request and confirmation
     */
    public void testFriendRequestConfirmed() throws Exception {

        setFriendRequestListener(USERNAME_A, true);

        // automatedtestfrienda requests automatedtestfriendb
        connectionA.addNewFriend(USERNAME_B, USERNAME_B).get();

        // sleep to wait for automatedtestfriendb to confirm, friends to exchange roster packets, etc
        waitForFriendRequest(true);

        // check that both automatedtestfrienda and automatedtestfriendb have each other on their roster
        assertEquals(1, autoFriendARosterListener.getRosterSize());
        assertEquals(1, autoFriendBRosterListener.getRosterSize());
        assertEquals(USERNAME_B, autoFriendARosterListener.getFirstRosterEntry());
        assertEquals(USERNAME_A, autoFriendBRosterListener.getFirstRosterEntry());
    }

    /**
     * Test rejection of friend request
     *
     * 1. automatedtestfrienda requests automatedtestfriendb
     * 2. automatedtestfriendb rejects the friend request
     * 3. both users logout and log back in
     * 4. confirm that no presence packet is ever sent to either user 
     */
    public void testFriendRequestDenied() throws Exception {

        setFriendRequestListener(USERNAME_A, false);

        // automatedtestfrienda requests automatedtestfriendb
        connectionA.addNewFriend(USERNAME_B, USERNAME_B).get();

        // sleep to wait for friend6 to deny
        waitForFriendRequest(false);

        connectionA.logout().get();
        connectionB.logout().get();

        autoFriendARosterListener = new RosterListenerMock();
        autoFriendBRosterListener = new RosterListenerMock();

        FriendConnectionConfiguration configFive = new FriendConnectionConfigurationMock(USERNAME_A, PASSWORD,
                SERVICE, autoFriendARosterListener);
        FriendConnectionConfiguration configSix = new FriendConnectionConfigurationMock(USERNAME_B, PASSWORD,
                SERVICE, autoFriendBRosterListener);

        connectionA = (XMPPFriendConnectionImpl) factories[0].login(configFive).get();
        connectionB = (XMPPFriendConnectionImpl) factories[1].login(configSix).get();

        waitForConnection();

        // check that both A and B are not aware of each other (not subscribed)
        // Both users are signed in, but since they are not friends (subscribed to each others' statuses)
        // neither receives a Presence available packet from the other.
        assertTrue(connectionA.isLoggedIn());
        assertTrue(connectionB.isLoggedIn());

        assertNull(autoFriendARosterListener.getFirstPresence(USERNAME_B));
        assertNull(autoFriendBRosterListener.getFirstPresence(USERNAME_A));
    }

    /**
     * Test that friend removal works
     *
     * 1. automatedtestfrienda requests automatedtestfriendb successfully
     * 2. automatedtestfriendb removes automatedtestfrienda as friend
     * 3. confirm that upon login, automatedtestfrienda does not receive
     *    any automatedtestfriendb Presence available packets.
     */
    public void testRemoveFriend() throws Exception {

        setFriendRequestListener(USERNAME_A, true);

        // automatedtestfriend5* requests automatedtestfriendb
        connectionA.addNewFriend(USERNAME_B, USERNAME_B).get();

        // wait for automatedtestfriends to confirm, friends to exchange roster packets, etc
        waitForFriendRequest(true);

        // check that both automatedtestfriends have each other on their roster
        assertEquals(1, autoFriendARosterListener.getRosterSize());
        assertEquals(1, autoFriendBRosterListener.getRosterSize());
        assertEquals(USERNAME_B, autoFriendARosterListener.getFirstRosterEntry());
        assertEquals(USERNAME_A, autoFriendBRosterListener.getFirstRosterEntry());

        // test friend removal
        connectionA.removeFriend(USERNAME_B).get();
        waitForFriendRequest(false);

        connectionA.logout().get();
        connectionB.logout().get();
        
        autoFriendARosterListener = new RosterListenerMock();
        autoFriendBRosterListener = new RosterListenerMock();

        FriendConnectionConfiguration configFive = new FriendConnectionConfigurationMock(USERNAME_A, PASSWORD,
                SERVICE, autoFriendARosterListener);
        FriendConnectionConfiguration configSix = new FriendConnectionConfigurationMock(USERNAME_B, PASSWORD,
                SERVICE, autoFriendBRosterListener);

        connectionA = (XMPPFriendConnectionImpl) factories[0].login(configFive).get();
        connectionB = (XMPPFriendConnectionImpl) factories[1].login(configSix).get();

        waitForConnection();
        
        assertTrue(connectionA.isLoggedIn());
        assertTrue(connectionB.isLoggedIn());

        assertNull(autoFriendARosterListener.getFirstPresence(USERNAME_B));
        assertNull(autoFriendBRosterListener.getFirstPresence(USERNAME_A));
    }
    
    private void waitForFriendRequest(boolean confirmed) throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        final RosterEvent.Type expectedType = confirmed ? RosterEvent.Type.FRIENDS_ADDED :
            RosterEvent.Type.FRIENDS_DELETED;
        
        class RosterEventListener implements EventListener<RosterEvent> {
            RosterEventListener(String expectedFriend) {
                this.expectedFriend = expectedFriend;    
            }
            AtomicBoolean friendInRoster = new AtomicBoolean(false);
            String expectedFriend;
            
            @Override
            public void handleEvent(RosterEvent event) {
                if (event.getType() == expectedType) {
                    assertEquals(1, event.getData().size());
                    if (expectedFriend.equals(event.getData().iterator().next().getId())) {
                        if (!friendInRoster.getAndSet(true)) {
                            latch.countDown();
                        }
                    }
                }
            }
        }
        injectors[1].getInstance(Key.get(new TypeLiteral<ListenerSupport<RosterEvent>>(){})).
                addListener(new RosterEventListener(USERNAME_A));
        injectors[0].getInstance(Key.get(new TypeLiteral<ListenerSupport<RosterEvent>>(){})).
                addListener(new RosterEventListener(USERNAME_B));
        latch.await(MAX_WAIT_FRIEND_REQUEST, TimeUnit.SECONDS);
    }
    
    private void waitForConnection() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        
        class FriendConnectionEventListener implements EventListener<FriendConnectionEvent> {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                if (event.getType() == FriendConnectionEvent.Type.CONNECTED) {
                    latch.countDown();
                }
            }    
        }
        injectors[1].getInstance(Key.get(new TypeLiteral<ListenerSupport<FriendConnectionEvent>>(){})).
                addListener(new FriendConnectionEventListener());
        injectors[0].getInstance(Key.get(new TypeLiteral<ListenerSupport<FriendConnectionEvent>>(){})).
                addListener(new FriendConnectionEventListener());
        assertTrue(latch.await(SLEEP, TimeUnit.SECONDS));
    }

    private void removeAllUsersFromRoster(FriendConnection conn) throws FriendException, ExecutionException, InterruptedException {
        for (Friend friend : conn.getFriends()) {
            conn.removeFriend(friend.getId()).get();
        }
    }
}
