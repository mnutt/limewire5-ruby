package org.limewire.ui.swing.friends.settings;

import java.util.Collections;
import java.util.List;

import org.limewire.io.UnresolvedIpPort;
import org.limewire.util.BaseTestCase;

public class XMPPAccountConfigurationImplTest extends BaseTestCase {

    public XMPPAccountConfigurationImplTest(String name) {
        super(name);
    }

    public void testSetUsernameUpdatesCanonicalId() {
        XMPPAccountConfigurationImpl configuration = new XMPPAccountConfigurationImpl("vmail.com", "GMail", "resource");
        assertEquals("", configuration.getCanonicalizedLocalID());
        configuration.setUsername("Julia");
        assertEquals("julia@vmail.com", configuration.getCanonicalizedLocalID());
        configuration.setUsername("romeo@capulet.com");
        assertEquals("romeo@capulet.com", configuration.getCanonicalizedLocalID());
        
        List<UnresolvedIpPort> servers = Collections.emptyList();
        configuration = new XMPPAccountConfigurationImpl(true, "montague.org", "Montague", null, "resource", servers);
        assertEquals("", configuration.getCanonicalizedLocalID());
        configuration.setUsername("Julia");
        assertEquals("julia@montague.org", configuration.getCanonicalizedLocalID());
        configuration.setUsername("romeo@capulet.com");
        assertEquals("romeo@capulet.com", configuration.getCanonicalizedLocalID());
        
        configuration = new XMPPAccountConfigurationImpl(false, "montague.org", "Montague", null, "resource", servers);
        assertEquals("", configuration.getCanonicalizedLocalID());
        configuration.setUsername("Julia");
        assertEquals("julia@montague.org", configuration.getCanonicalizedLocalID());
        configuration.setUsername("romeo@capulet.com");
        assertEquals("romeo@capulet.com", configuration.getCanonicalizedLocalID());
    }
}
