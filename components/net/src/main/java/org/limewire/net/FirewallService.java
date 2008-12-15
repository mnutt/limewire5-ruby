package org.limewire.net;

/**
 * Service to manage software firewalls
 */
public interface FirewallService {

    /**
     * @return true if the <code>FireWallService</code> can programmatically manage the software
     * firewall
     */
    public boolean isProgrammaticallyConfigurable();

    /**
     * add an entry in the firewall for Limewire
     * @return true if successful
     */
    public boolean addToFirewall();
}
