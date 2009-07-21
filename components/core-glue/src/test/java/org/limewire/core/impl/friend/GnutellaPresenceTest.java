package org.limewire.core.impl.friend;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.friend.api.Friend;
import org.limewire.io.Address;
import org.limewire.util.BaseTestCase;

public class GnutellaPresenceTest extends BaseTestCase {

    public GnutellaPresenceTest(String name) {
        super(name);
    }

    public void testGetters() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        final Address address1 = context.mock(Address.class);
        final String id1 = "id1";
        final String description1 = "description1";

        context.checking(new Expectations() {
            {
                exactly(2).of(address1).getAddressDescription();
                will(returnValue(description1));
                
                allowing(address1);
            }
        });
        GnutellaPresence gnutellaPresence = new GnutellaPresence.GnutellaPresenceWithString(address1, id1);

        Friend friend1 = gnutellaPresence.getFriend();
        assertEquals(id1, friend1.getId());

        assertEquals(id1, gnutellaPresence.getPresenceId());
    }
}
