package org.limewire.ui.swing.friends.chat;

import java.util.ArrayList;
import java.util.Collections;

import org.limewire.ui.swing.friends.chat.ChatFriend;
import org.limewire.ui.swing.friends.chat.FriendAvailabilityComparator;
import org.limewire.friend.api.FriendPresence.Mode;

import junit.framework.TestCase;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class FriendAvailabilityComparatorTest extends TestCase {

    private ArrayList<ChatFriend> chatFriends;
    
    @Override
    public void setUp() {
        chatFriends = new ArrayList<ChatFriend>();
    }
    
    private MockChatFriend populateFriends(String name, Mode mode) {
        MockChatFriend friend = new MockChatFriend(name, "foo", mode);
        chatFriends.add(friend);
        return friend;
    }

    public void testModeAlphabeticalSorting() {
        populateFriends("b_xa", Mode.xa);
        populateFriends("b_available", Mode.available);
        populateFriends("b_away", Mode.away);
        populateFriends("b_dnd", Mode.dnd);
        populateFriends("A_away", Mode.away);
        populateFriends("a_xa", Mode.xa);
        populateFriends("b_chat", Mode.chat);
        populateFriends("a_dnd", Mode.dnd);
        populateFriends("a_available", Mode.available);
        
        Collections.sort(chatFriends, new FriendAvailabilityComparator());
        
        String[] sorted = new String[] {"b_chat", 
                                        "a_available", "b_available",
                                        "A_away", 
                                        "a_xa", "b_away", "b_xa",
                                        "a_dnd", "b_dnd"};
        
        assertOrder(sorted);
    }

    private void assertOrder(String[] sorted) {
        for(int i = 0; i < sorted.length; i++) {
            assertEquals(sorted[i], chatFriends.get(i).getName());
        }
    }

    public void testHandleNullFriendName() {
        populateFriends("a_xa", Mode.xa);
        populateFriends(null, Mode.xa);
        populateFriends(null, Mode.chat);
        populateFriends("a_chat", Mode.chat);
        
        Collections.sort(chatFriends, new FriendAvailabilityComparator());
        
        assertOrder(new String[] {"a_chat", null, "a_xa", null});
    }
    
    public void testOrderSortingForChatStatus() {
        populateFriends("b_chat", Mode.chat).chatStartTime = 3l;
        populateFriends("a_chat", Mode.chat).chatStartTime = 1l;
        populateFriends("h_chat", Mode.chat).chatStartTime = 2l;
        
        Collections.sort(chatFriends, new FriendAvailabilityComparator());

        assertOrder(new String[] {"a_chat", "h_chat", "b_chat"});
    }
}
