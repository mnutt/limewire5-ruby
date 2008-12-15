package org.limewire.ui.swing.friends.settings;

import java.net.URL;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.RosterEvent;

/**
 * Stores all the information required to log into an XMPP server and
 * describe the server in the UI.
 */
class XMPPAccountConfigurationImpl implements XMPPAccountConfiguration {

    private final static String iconPath =
        "org/limewire/ui/swing/mainframe/resources/icons/friends/";
    private final static String iconExtension = ".png";

    private final String resource;
    private final boolean isDebugEnabled;
    private final boolean requiresDomain;
    private volatile String serviceName;
    private volatile String label;
    private final Icon icon;    
    private volatile String username;
    private volatile String password;

    public XMPPAccountConfigurationImpl(String configString, String resource)
    throws IllegalArgumentException {
        this.resource = resource;
        try {
            StringTokenizer st = new StringTokenizer(configString, ",");
            isDebugEnabled = Boolean.valueOf(st.nextToken());
            requiresDomain = Boolean.valueOf(st.nextToken());
            serviceName = st.nextToken();
            label = st.nextToken();
        } catch(NoSuchElementException nsex) {
            throw new IllegalArgumentException("Malformed XMPP server setting");
        }
        // Use the label as the icon name
        String path = iconPath + label + iconExtension;
        URL url = ClassLoader.getSystemResource(path);
        if(url == null)
            icon = new EmptyIcon(16, 16);
        else
            icon = new ImageIcon(url);
        username = "";
        password = "";
    }

    public XMPPAccountConfigurationImpl(String resource) {
        this.resource = resource;
        isDebugEnabled = false;
        requiresDomain = false;
        serviceName = "";
        label = "";
        icon = new EmptyIcon(16, 16); // Naomi Klein
        username = "";
        password = "";
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
        // Some servers expect the domain to be included, others don't
        int at = username.indexOf('@');
        if(requiresDomain && at == -1)
            username += "@" + getServiceName(); // Guess the domain
        else if(!requiresDomain && at > -1)
            username = username.substring(0, at); // Strip the domain
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
        // Return a string that can be parsed back into a config
        return isDebugEnabled + "," + requiresDomain + "," + serviceName + "," + label;
    }
}
