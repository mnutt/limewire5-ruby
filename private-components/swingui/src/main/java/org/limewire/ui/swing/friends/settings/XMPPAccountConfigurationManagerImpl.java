package org.limewire.ui.swing.friends.settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.xmpp.XMPPResourceFactory;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.io.UnresolvedIpPortImpl;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.PasswordManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPAccountConfigurationManagerImpl implements XMPPAccountConfigurationManager {
    
    private final PasswordManager passwordManager;
    private final Map<String,XMPPAccountConfiguration> configs; // Indexed by label
    private final String resource;
    
    private XMPPAccountConfiguration autoLoginConfig = null;
    
    @Resource private Icon gmailIcon;
    @Resource private Icon ljIcon;
    
    @Inject
    public XMPPAccountConfigurationManagerImpl(PasswordManager passwordManager,
            XMPPResourceFactory xmppResourceFactory) {
        GuiUtils.assignResources(this);
        this.passwordManager = passwordManager;
        configs = new HashMap<String,XMPPAccountConfiguration>();
        resource = xmppResourceFactory.getResource();
        loadWellKnownServers();
        loadCustomServer();
    }

    private void loadCustomServer() {
        String custom = SwingUiSettings.USER_DEFINED_JABBER_SERVICENAME.get();
        XMPPAccountConfigurationImpl customConfig =
            new XMPPAccountConfigurationImpl(custom, "Jabber", resource);
        configs.put(customConfig.getLabel(), customConfig);
        String autoLogin = SwingUiSettings.XMPP_AUTO_LOGIN.get();
        if(!autoLogin.equals("")) {
            int comma = autoLogin.indexOf(',');
            try {
                String label = autoLogin.substring(0, comma);
                String username = autoLogin.substring(comma + 1);
                String password = passwordManager.loadPassword(username);
                XMPPAccountConfiguration config = configs.get(label);
                if(config != null) {
                    config.setUsername(username);
                    config.setPassword(password);
                    autoLoginConfig = config;
                }
            } catch(IndexOutOfBoundsException ignored) {
                // Malformed string - no soup for you!
            } catch(IllegalArgumentException ignored) {
                // Empty username - no soup for you!
            } catch(IOException ignored) {
                // Error decrypting password - no soup for you!
            }
        }
    }

    private void loadWellKnownServers() {
        XMPPAccountConfiguration gmail =
            new XMPPAccountConfigurationImpl(true, "gmail.com", "Gmail", gmailIcon, resource, getGTalkServers());
        XMPPAccountConfiguration livejournal =
            new XMPPAccountConfigurationImpl(false, "livejournal.com", "LiveJournal", ljIcon, resource, getLiveJournalServers());

        configs.put(gmail.getLabel(), gmail);
        configs.put(livejournal.getLabel(), livejournal);
    }

    private List<UnresolvedIpPort> getLiveJournalServers() {
        List<UnresolvedIpPort> defaultServers = new ArrayList<UnresolvedIpPort>(1);
        defaultServers.add(new UnresolvedIpPortImpl("xmpp.services.livejournal.com", 5222));
        return defaultServers;
    }

    private List<UnresolvedIpPort> getGTalkServers() {
        List<UnresolvedIpPort> defaultServers = new ArrayList<UnresolvedIpPort>(5);
        defaultServers.add(new UnresolvedIpPortImpl("talk.1.google.com", 5222));
        defaultServers.add(new UnresolvedIpPortImpl("talk1.1.google.com", 5222));
        defaultServers.add(new UnresolvedIpPortImpl("talk2.1.google.com", 5222));
        defaultServers.add(new UnresolvedIpPortImpl("talk3.1.google.com", 5222));
        defaultServers.add(new UnresolvedIpPortImpl("talk4.1.google.com", 5222));
        return defaultServers;
    }

    @Override
    public XMPPAccountConfiguration getConfig(String label) {
        return configs.get(label);
    }
    
    @Override
    public List<XMPPAccountConfiguration> getConfigurations() {
        ArrayList<XMPPAccountConfiguration> configurations = new ArrayList<XMPPAccountConfiguration>(configs.values());
        Collections.sort(configurations, new Comparator<XMPPAccountConfiguration>() {
            @Override
            public int compare(XMPPAccountConfiguration o1, XMPPAccountConfiguration o2) {
                return o1.getLabel().compareToIgnoreCase(o2.getLabel());
            }
        });
        return configurations;
    }    
    
    @Override
    public List<String> getLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        for(XMPPAccountConfiguration config : configs.values())
            labels.add(config.getLabel());
        Collections.sort(labels);
        return labels;
    }
    
    @Override
    public XMPPAccountConfiguration getAutoLoginConfig() {
        return autoLoginConfig;
    }
    
    @Override
    public void setAutoLoginConfig(XMPPAccountConfiguration config) {
        // Remove the old configuration, if there is one
        if(autoLoginConfig != null) {
            passwordManager.removePassword(autoLoginConfig.getUserInputLocalID());
            SwingUiSettings.XMPP_AUTO_LOGIN.set("");
            SwingUiSettings.USER_DEFINED_JABBER_SERVICENAME.set("");
            autoLoginConfig = null;
        }
        // Store the new configuration, if there is one
        if(config != null) {
            try {
                passwordManager.storePassword(config.getUserInputLocalID(), config.getPassword());
                SwingUiSettings.XMPP_AUTO_LOGIN.set(config.getLabel() + "," + config.getUserInputLocalID());
                if(config.getLabel().equals("Jabber"))
                    SwingUiSettings.USER_DEFINED_JABBER_SERVICENAME.set(config.getServiceName());
                autoLoginConfig = config;
            } catch (IllegalArgumentException ignored) {
                // Empty username or password - no soup for you!
            } catch (IOException ignored) {
                // Error encrypting password - no more Soup Nazi jokes for you!
            }
        }
    }
}
