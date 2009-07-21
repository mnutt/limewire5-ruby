package org.limewire.ui.swing.friends.settings;

import java.util.Collections;
import java.util.List;

import org.limewire.friend.api.Network.Type;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.util.BaseTestCase;

public class FriendAccountConfigurationImplTest extends BaseTestCase {

    public FriendAccountConfigurationImplTest(String name) {
        super(name);
    }

    public void testSetUsernameUpdatesCanonicalId() {
        FriendAccountConfigurationImpl configuration = new FriendAccountConfigurationImpl("vmail.com", "GMail", "resource", Type.XMPP, null, null);
        assertEquals("", configuration.getCanonicalizedLocalID());
        configuration.setUsername("Julia");
        assertEquals("julia@vmail.com", configuration.getCanonicalizedLocalID());
        configuration.setUsername("romeo@capulet.com");
        assertEquals("romeo@capulet.com", configuration.getCanonicalizedLocalID());
        
        List<UnresolvedIpPort> servers = Collections.emptyList();
        configuration = new FriendAccountConfigurationImpl(true, "montague.org", "Montague", null, null, "resource", servers, Type.XMPP);
        assertEquals("", configuration.getCanonicalizedLocalID());
        configuration.setUsername("Julia");
        assertEquals("julia@montague.org", configuration.getCanonicalizedLocalID());
        configuration.setUsername("romeo@capulet.com");
        assertEquals("romeo@capulet.com", configuration.getCanonicalizedLocalID());
        
        configuration = new FriendAccountConfigurationImpl(false, "montague.org", "Montague", null, null, "resource", servers, Type.XMPP);
        assertEquals("", configuration.getCanonicalizedLocalID());
        configuration.setUsername("Julia");
        assertEquals("julia@montague.org", configuration.getCanonicalizedLocalID());
        configuration.setUsername("romeo@capulet.com");
        assertEquals("romeo@capulet.com", configuration.getCanonicalizedLocalID());
    }
}
