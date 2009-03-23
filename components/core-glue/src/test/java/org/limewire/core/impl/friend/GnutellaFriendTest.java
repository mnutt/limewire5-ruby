package org.limewire.core.impl.friend;

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.util.BaseTestCase;

public class GnutellaFriendTest extends BaseTestCase {

    public GnutellaFriendTest(String name) {
        super(name);
    }

    public void testGetters() {
        Mockery context = new Mockery();
        final FriendPresence friendPresence1 = context.mock(FriendPresence.class);
        final String name1 = "name1";
        final String id1 = "id1";
        final String presenceId1 = "presenceId1";

        context.checking(new Expectations() {
            {
                one(friendPresence1).getPresenceId();
                will(returnValue(presenceId1));
            }
        });

        GnutellaFriend gnutellaFriend = new GnutellaFriend(name1, id1, friendPresence1);
        
        assertTrue(gnutellaFriend.isAnonymous());
        assertEquals(name1, gnutellaFriend.getFirstName());
        assertEquals(name1, gnutellaFriend.getName());
        assertEquals(name1, gnutellaFriend.getRenderName());
        assertEquals(id1, gnutellaFriend.getId());
        assertNull(gnutellaFriend.getNetwork());
        Map<String, FriendPresence> friendPresences = gnutellaFriend.getFriendPresences();
        assertEquals(friendPresence1, friendPresences.get(presenceId1));
        
        context.assertIsSatisfied();
    }

}
