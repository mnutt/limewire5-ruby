package org.limewire.xmpp.client.impl;

import java.util.concurrent.ExecutionException;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendRequest;
import org.limewire.friend.api.FriendRequestEvent;
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

    private static final String USERNAME_5 = "automatedtestfriend5@gmail.com";
    private static final String USERNAME_6 = "automatedtestfriend6@gmail.com";
    private static final String PASSWORD_56 = "automatedtestfriend56";

    private RosterListenerMock autoFiveRosterListener;
    private RosterListenerMock autoSixRosterListener;
    private EventListener<FriendRequestEvent> acceptFriendListener;
    private XMPPFriendConnectionImpl connectionFive;
    private XMPPFriendConnectionImpl connectionSix;

    public XmppFriendSubscriptionTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        autoFiveRosterListener = new RosterListenerMock();
        autoSixRosterListener = new RosterListenerMock();

        FriendConnectionConfiguration configFive = new FriendConnectionConfigurationMock(USERNAME_5, PASSWORD_56,
                SERVICE, autoFiveRosterListener);
        FriendConnectionConfiguration configSix = new FriendConnectionConfigurationMock(USERNAME_6, PASSWORD_56,
                SERVICE, autoSixRosterListener);

        connectionFive = (XMPPFriendConnectionImpl)factories[0].login(configFive).get();
        connectionSix = (XMPPFriendConnectionImpl)factories[1].login(configSix).get();

        // Allow login, roster, presence, library messages to be sent, received
        Thread.sleep(SLEEP);

        // Make sure that both friend5 and friend6 have empty rosters before beginning
        if (autoFiveRosterListener.getRosterSize() != 0) {
            removeAllUsersFromRoster(connectionFive);
        }
        if (autoFiveRosterListener.getRosterSize() != 0) {
            removeAllUsersFromRoster(connectionSix);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        removeFriendRequestListener();
        connectionFive.removeFriend(USERNAME_6);
        connectionSix.removeFriend(USERNAME_5);
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

        setFriendRequestListener(USERNAME_5, true);

        // automatedtestfriend5 requests automatedtestfriend6
        connectionFive.addNewFriend(USERNAME_6, USERNAME_6).get();

        // sleep to wait for automatedtestfriend6 to confirm, friends to exchange roster packets, etc
        Thread.sleep(SLEEP);

        // check that both automatedtestfriend5 and automatedtestfriend6 have each other on their roster
        assertEquals(1, autoFiveRosterListener.getRosterSize());
        assertEquals(1, autoSixRosterListener.getRosterSize());
        assertEquals(USERNAME_6, autoFiveRosterListener.getFirstRosterEntry());
        assertEquals(USERNAME_5, autoSixRosterListener.getFirstRosterEntry());
    }

    /**
     * Test rejection of friend request
     *
     * 1. automatedtestfriend5 requests automatedtestfriend6
     * 2. automatedtestfriend6 rejects the friend request
     * 3. both users logout and log back in
     * 4. confirm that no presence packet is ever sent to either user 
     */
    public void testFriendRequestDenied() throws Exception {

        setFriendRequestListener(USERNAME_5, false);

        // automatedtestfriend5 requests automatedtestfriend6
        connectionFive.addNewFriend(USERNAME_6, USERNAME_6).get();

        // sleep to wait for friend6 to deny
        Thread.sleep(SLEEP);

        connectionFive.logout().get();
        connectionSix.logout().get();

        autoFiveRosterListener = new RosterListenerMock();
        autoSixRosterListener = new RosterListenerMock();

        FriendConnectionConfiguration configFive = new FriendConnectionConfigurationMock(USERNAME_5, PASSWORD_56,
                SERVICE, autoFiveRosterListener);
        FriendConnectionConfiguration configSix = new FriendConnectionConfigurationMock(USERNAME_6, PASSWORD_56,
                SERVICE, autoSixRosterListener);

        connectionFive = (XMPPFriendConnectionImpl) factories[0].login(configFive).get();
        connectionSix = (XMPPFriendConnectionImpl) factories[1].login(configSix).get();

        Thread.sleep(SLEEP);

        // check that both friend5 and friend6 are not aware of each other (not subscribed)
        // Both users are signed in, but since they are not friends (subscribed to each others' statuses)
        // neither receives a Presence available packet from the other.
        assertTrue(connectionFive.isLoggedIn());
        assertTrue(connectionSix.isLoggedIn());

        assertNull(autoFiveRosterListener.getFirstPresence(USERNAME_6));
        assertNull(autoSixRosterListener.getFirstPresence(USERNAME_5));
    }

    /**
     * Test that friend removal works
     *
     * 1. automatedtestfriend5 requests automatedtestfriend6 successfully
     * 2. automatedtestfriend5 removes automatedtestfriend6 as friend
     * 3. confirm that upon login, automatedtestfriend5 does not receive
     *    any automatedtestfriend6 Presence available packets.
     */
    public void testRemoveFriend() throws Exception {

        setFriendRequestListener(USERNAME_5, true);

        // automatedtestfriend5 requests automatedtestfriend6
        connectionFive.addNewFriend(USERNAME_6, USERNAME_6).get();

        // sleep to wait for automatedtestfriend6 to confirm, friends to exchange roster packets, etc
        Thread.sleep(SLEEP);

        // check that both automatedtestfriend5 and automatedtestfriend6 have each other on their roster
        assertEquals(1, autoFiveRosterListener.getRosterSize());
        assertEquals(1, autoSixRosterListener.getRosterSize());
        assertEquals(USERNAME_6, autoFiveRosterListener.getFirstRosterEntry());
        assertEquals(USERNAME_5, autoSixRosterListener.getFirstRosterEntry());

        // test friend removal
        connectionFive.removeFriend(USERNAME_6).get();
        Thread.sleep(SLEEP);

        connectionFive.logout().get();
        connectionSix.logout().get();
        
        autoFiveRosterListener = new RosterListenerMock();
        autoSixRosterListener = new RosterListenerMock();

        FriendConnectionConfiguration configFive = new FriendConnectionConfigurationMock(USERNAME_5, PASSWORD_56,
                SERVICE, autoFiveRosterListener);
        FriendConnectionConfiguration configSix = new FriendConnectionConfigurationMock(USERNAME_6, PASSWORD_56,
                SERVICE, autoSixRosterListener);

        connectionFive = (XMPPFriendConnectionImpl) factories[0].login(configFive).get();
        connectionSix = (XMPPFriendConnectionImpl) factories[1].login(configSix).get();

        Thread.sleep(SLEEP);
        
        assertTrue(connectionFive.isLoggedIn());
        assertTrue(connectionSix.isLoggedIn());

        assertNull(autoFiveRosterListener.getFirstPresence(USERNAME_6));
        assertNull(autoSixRosterListener.getFirstPresence(USERNAME_5));
    }

    private void removeAllUsersFromRoster(FriendConnection conn) throws FriendException, ExecutionException, InterruptedException {
        for (Friend friend : conn.getFriends()) {
            conn.removeFriend(friend.getId()).get();
        }
    }
}
