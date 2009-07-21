package org.limewire.ui.swing.friends.chat;

import java.util.Map;
import java.util.HashMap;

import org.limewire.util.BaseTestCase;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.jmock.Mockery;
import org.jmock.Expectations;

/**
 * Test of {@link ChatFriend} interface
 */
public class ChatFriendTest extends BaseTestCase {

    private Mockery context;


    public ChatFriendTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        context = new Mockery();
    }


    /**
     * Test that calling {@link ChatFriend#isChatting()} returns the appropriate result
     * after the chat is started/stopped by calling {@link ChatFriend#startChat()} and
     * {@link ChatFriend#stopChat()}.
     */
    public void testIsChatting() throws Exception {
        Friend user = new MockUser("userId", "User Name");
        FriendPresence presence = new MockPresence(user, FriendPresence.Mode.available, null, user.getId() + "/presence123");

        ChatFriend chatFriend = new ChatFriendImpl(presence);
        assertFalse(chatFriend.isChatting());

        chatFriend.startChat();
        assertTrue(chatFriend.isChatting());

        chatFriend.stopChat();
        assertFalse(chatFriend.isChatting());
    }


    /**
     * Test {@link ChatFriend#update()} and verify that status/mode changes in a ChatFriend's
     * presences get reflected in the status and mode of the ChatFriend.  Only the top ranked
     * presence's mode and status get used.  Available presences take top rank,
     * and the remainder of the presences get ranked by jabber presence priority.  If multiple presences
     * have a mode of "available", the available presences are ranked by jabber priority.
     * 
     */
    public void testUpdateOnChatFriendAfterPresenceModeStatusChange() throws Exception {
        String userId = "limebuddy1@gmail.com";
        String jid1 = userId + "/presence123";
        String jid2 = userId + "/presence125";
        String jid3 = userId + "/presence129";

        final Map<String, FriendPresence> presencesMap = new HashMap<String, FriendPresence>();

        final Friend user = context.mock(Friend.class);
        context.checking(new Expectations() {{
            allowing(user).getPresences();
            will(returnValue(presencesMap));
            one(user).isSignedIn();
            will(returnValue(false));
        }});

        // setting up the presences that we will be using
        MockPresence jid1AvailPresence = new MockPresence(user, FriendPresence.Mode.available, "Ready 1", jid1);
        presencesMap.put(jid1, jid1AvailPresence);

        ChatFriend chatFriend = new ChatFriendImpl(jid1AvailPresence);
        assertEquals(FriendPresence.Mode.available, chatFriend.getMode());
        assertEquals("Ready 1", chatFriend.getStatus());

        // test updating the 1 presence (jid1)
        // This simulates 1 login going from "available" to "away"
        presencesMap.put(jid1, new MockPresence(user, FriendPresence.Mode.away, "Away 1", jid1));
        chatFriend.update();

        assertEquals(FriendPresence.Mode.away, chatFriend.getMode());
        assertEquals("Away 1", chatFriend.getStatus());

        // login goes back to "available"
        presencesMap.put(jid1, jid1AvailPresence);

        // simulate additional presence signing in
        // Since this presence is "Away", the original "available" presence still takes precedence
        presencesMap.put(jid2, new MockPresence(user, FriendPresence.Mode.away, "Away", jid2));
        chatFriend.update();

        assertEquals(FriendPresence.Mode.available, chatFriend.getMode());
        assertEquals("Ready 1", chatFriend.getStatus());

        // simulate the 3rd login (presence) coming online as "available".
        // Make sure the top ranked presence has the highest jabber priority
        // out of the "available" presences
        MockPresence jid3AvailPresence = new MockPresence(user, FriendPresence.Mode.available, "Ready Three", jid3);
        jid3AvailPresence.setPriority(100);
        jid1AvailPresence.setPriority(98);
        presencesMap.put(jid3, jid3AvailPresence);

        // jid3 should determine the chatfriend's status and mode
        chatFriend.update();
        assertEquals(FriendPresence.Mode.available, chatFriend.getMode());
        assertEquals("Ready Three", chatFriend.getStatus());
        

        // simulate presence "logouts".
        presencesMap.remove(jid3);
        chatFriend.update();
        assertEquals(FriendPresence.Mode.available, chatFriend.getMode());
        assertEquals("Ready 1", chatFriend.getStatus());

        presencesMap.remove(jid1);
        chatFriend.update();
        assertEquals(FriendPresence.Mode.away, chatFriend.getMode());
        assertEquals("Away", chatFriend.getStatus());

        presencesMap.remove(jid2);
        chatFriend.update();
        assertFalse(chatFriend.isSignedIn());

        // after all presences go offline, the chatfriend status/mode is the
        // status/mode of the last remaining presence.  Not sure if this is the desired behavior!
        assertEquals(FriendPresence.Mode.away, chatFriend.getMode());
        assertEquals("Away", chatFriend.getStatus());
    }

}
