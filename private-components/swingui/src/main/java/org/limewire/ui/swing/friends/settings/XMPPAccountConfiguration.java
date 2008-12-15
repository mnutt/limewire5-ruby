package org.limewire.ui.swing.friends.settings;

import javax.swing.Icon;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

/**
 * Extends the XMPPConnectionConfiguration interface with methods for
 * describing and configuring an XMPP account through the UI.
 */
public interface XMPPAccountConfiguration extends XMPPConnectionConfiguration {
    
    /**
     * Returns an icon associated with the account, such as the logo of the
     * service provider. May be null.
     */
    public Icon getIcon();
    
    /**
     * Sets the label that the UI will display to identify the account.
     */
    public void setLabel(String label);
    
    /**
     * Sets the service name of the account.
     */
    public void setServiceName(String serviceName);
    
    /**
     * Sets the username of the account.
     */
    public void setUsername(String username);
    
    /**
     * Sets the password of the account.
     */
    public void setPassword(String password);
}
