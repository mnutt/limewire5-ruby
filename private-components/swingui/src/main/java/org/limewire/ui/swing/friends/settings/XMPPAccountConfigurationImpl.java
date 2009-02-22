package org.limewire.ui.swing.friends.settings;

import java.util.Locale;

import javax.swing.Icon;

import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.listener.EventListener;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.RosterEvent;

/**
 * Stores all the information required to log into an XMPP server and
 * describe the server in the UI.
 */
class XMPPAccountConfigurationImpl implements XMPPAccountConfiguration {

    private final String resource;
    private final boolean isDebugEnabled;
    private final boolean modifyUser;
    private final boolean requiresDomain;
    private final Icon icon;
    
    private volatile String serviceName;
    private volatile String label;
    private volatile String username;
    private volatile String password;
    
    /** Constructs an XMPPAccountConfiguration with the following parameters. */
    public XMPPAccountConfigurationImpl(boolean requireDomain, String serviceName, String label, Icon icon, String resource) {
        this(requireDomain, serviceName, label, icon, resource, true);
    }
    
    /** Constructs a basic XMPPAccountConfiguration that cannot modify the serviceName. */
    public XMPPAccountConfigurationImpl(String serviceName, String label, String resource) {
        this(false, serviceName, label, null, resource, false);
    }
    
    private XMPPAccountConfigurationImpl(boolean requireDomain, String serviceName, String label, Icon icon, String resource, boolean modifyUser) {
        this.resource = resource;
        this.modifyUser = modifyUser;
        this.isDebugEnabled = false;
        this.requiresDomain = requireDomain;
        this.serviceName = serviceName;
        this.label = label;
        if(icon != null) {
            this.icon = icon;
        } else {
            this.icon = new EmptyIcon(16, 16);
        }
        this.username = "";
        this.password = "";
    }

    @Override
    public boolean isDebugEnabled() {
        return isDebugEnabled;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public String getUserInputLocalID() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        if(modifyUser) {
            // Some servers expect the domain to be included, others don't
            int at = username.indexOf('@');
            if(requiresDomain && at == -1)
                username += "@" + getServiceName(); // Guess the domain
            else if(!requiresDomain && at > -1)
                username = username.substring(0, at); // Strip the domain
        }
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getResource() {
        return resource;
    }

    @Override
    public String getCanonicalizedLocalID() {
        // friend and friendpresence ids are
        // canonicalized to be lowercase
        return username.toLowerCase(Locale.US);
    }

    @Override
    public String getNetworkName() {
        return serviceName;
    }

    @Override
    public EventListener<RosterEvent> getRosterListener() {
        return null;
    }

    @Override
    public String toString() {
        return StringUtils.toStringBlacklist(this, password);
    }
}
