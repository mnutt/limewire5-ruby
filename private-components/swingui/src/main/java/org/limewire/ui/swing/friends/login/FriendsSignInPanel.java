package org.limewire.ui.swing.friends.login;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXPanel;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.util.I18n;
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import net.miginfocom.swing.MigLayout;

@Singleton
public class FriendsSignInPanel extends JXPanel implements FriendActions {
    
    private final HyperlinkButton shareLabel;
    private final LoginPanel loginPanel;
    private final LoggedInPanel loggedInPanel;
    private final XMPPService xmppService;
    //TODO: being part of a singleton makes no sense for this to be a provider but
    // this class should not be initialized the way it currently is.
    private final Provider<XMPPAccountConfigurationManager> accountManager;
    
    @Inject
    FriendsSignInPanel(LoginPanel loginPanel,
                       LoggedInPanel loggedInPanel,
                       XMPPService xmppService,
                       Provider<XMPPAccountConfigurationManager> accountManager) {
        this.loggedInPanel = loggedInPanel;
        this.loginPanel = loginPanel;
        this.xmppService = xmppService;
        this.accountManager = accountManager;
        setLayout(new MigLayout("fill, flowy, gap 0, insets 0, hidemode 3, nogrid"));
        setOpaque(false);
        
        shareLabel = new HyperlinkButton(I18n.tr("Share with friends!"), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shareLabel.setVisible(false);
                FriendsSignInPanel.this.loginPanel.setVisible(true);
            }
        });
        shareLabel.setOpaque(false);
        shareLabel.setName("FriendsSignIn.ShareLabel");
        add(shareLabel, "alignx center, gaptop 3, gapbottom 6");
        add(loginPanel, "growx, gaptop 4");
        add(loggedInPanel, "growx, gaptop 4");
        
        // Presetup the UI so that it looks correct until services start.
        XMPPConnectionConfiguration config = accountManager.get().getAutoLoginConfig();
        if(config != null) {
            loggedInPanel.autoLogin(config);
            shareLabel.setVisible(false);
            loginPanel.setVisible(false);
            loggedInPanel.setVisible(true);
        } else {
            shareLabel.setVisible(true);
            loginPanel.setVisible(false);
            loggedInPanel.setVisible(false);
        }
        
        loginPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                if(!FriendsSignInPanel.this.loggedInPanel.isVisible()) {
                    shareLabel.setVisible(true);
                }
            }
        });
    }
    
    @Override
    public boolean isSignedIn() {
        return xmppService.isLoggedIn();
    }
    
    @Override
    public void signIn() {        
        if(!xmppService.isLoggedIn() && !xmppService.isLoggingIn()) {
            XMPPAccountConfiguration config = accountManager.get().getAutoLoginConfig();
            if(config == null) {
                shareLabel.setVisible(false);
                loginPanel.setVisible(true);
                loggedInPanel.setVisible(false);
            } else {
                autoLogin(config);
            }
        }
    }
    
    @Override
    public void signOut(final boolean switchUser) {
        xmppService.logout();
        if(switchUser) {
            // 'Switch User' trumps 'Remember Me'
            accountManager.get().setAutoLoginConfig(null);
            shareLabel.setVisible(false);
            loginPanel.setVisible(true);
            loggedInPanel.setVisible(false);
        } else {
            XMPPAccountConfiguration auto =
                accountManager.get().getAutoLoginConfig();
            if(auto == null) {
                shareLabel.setVisible(true);
                loginPanel.setVisible(false);
                loggedInPanel.setVisible(false);
            } else {
                shareLabel.setVisible(false);
                loginPanel.setVisible(false);
                loggedInPanel.setVisible(true);
                loggedInPanel.disconnected(auto);
            }
        }
    }
    
    private void connecting(XMPPConnectionConfiguration config) {
        loginPanel.connecting(config);
        loggedInPanel.connecting(config);
    }
    
    private void connected(XMPPConnectionConfiguration config) {
        shareLabel.setVisible(false);
        loginPanel.setVisible(false);
        loggedInPanel.setVisible(true);

        loginPanel.connected(config);
        loggedInPanel.connected(config);        
    }
    
    private void disconnected(Exception reason) {
        loginPanel.disconnected(reason);
        shareLabel.setVisible(false);
        loginPanel.setVisible(true);
        loggedInPanel.setVisible(false);
    }
    
    private void autoLogin(XMPPAccountConfiguration config) {
        shareLabel.setVisible(false);
        loginPanel.setVisible(false);
        loggedInPanel.setVisible(true);
        
        loggedInPanel.autoLogin(config);
        loginPanel.autoLogin(config);
    }
        
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(new Service() {
            @Override
            public String getServiceName() {
                return tr("Friend Auto-Login");
            }
            @Override
            public void initialize() {
            }
            
            @Override
            public void start() {
                // If there's an auto-login account, select it and log in
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        XMPPAccountConfiguration auto =
                            accountManager.get().getAutoLoginConfig();
                        if(auto != null) {
                            autoLogin(auto);
                        }
                    }
                });
            }
            
            @Override
            public void stop() {
            }
        });
    }
    
    @Inject
    void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTING:
                    connecting(event.getSource().getConfiguration());
                    break;
                case CONNECTED:
                    connected(event.getSource().getConfiguration());
                    break;
                case DISCONNECTED:
                case CONNECT_FAILED:
                    // Ignore duplicate events caused by authentication
                    // errors and events caused by deliberately signing
                    // out or switching user
                    Exception reason = event.getException();
                    if(reason != null) {
                        disconnected(reason);
                    }
                }
            }
        });
    }
}
