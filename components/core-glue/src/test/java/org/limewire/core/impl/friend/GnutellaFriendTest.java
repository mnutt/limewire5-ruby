package org.limewire.core.impl.friend;

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.Address;
import org.limewire.util.BaseTestCase;

public class GnutellaFriendTest extends BaseTestCase {

    public GnutellaFriendTest(String name) {
        super(name);
    }

    public void testGetters() {
        Mockery context = new Mockery();
        final FriendPresence friendPresence1 = context.mock(FriendPresence.class);
        final Address address = context.mock(Address.class);
        final String id1 = "id1";

        context.checking(new Expectations() {
            {
                exactly(2).of(friendPresence1).getPresenceId();
                will(returnValue(id1));
                allowing(address).getAddressDescription();
                will(returnValue("description"));
            }
        });

        GnutellaFriend gnutellaFriend =
            new GnutellaFriend(address, friendPresence1);
        
        assertTrue(gnutellaFriend.isAnonymous());
        assertEquals("description", gnutellaFriend.getFirstName());
        assertEquals("description", gnutellaFriend.getName());
        assertEquals("description", gnutellaFriend.getRenderName());
        assertEquals(id1, gnutellaFriend.getId());
        assertNull(gnutellaFriend.getNetwork());
        Map<String, FriendPresence> friendPresences = gnutellaFriend.getPresences();
        assertEquals(friendPresence1, friendPresences.get(id1));
        
        context.assertIsSatisfied();
    }

}
