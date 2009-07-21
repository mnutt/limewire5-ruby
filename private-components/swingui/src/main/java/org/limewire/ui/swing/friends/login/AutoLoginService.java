package org.limewire.ui.swing.friends.login;

import javax.swing.SwingUtilities;

import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AutoLoginService implements Service {
    
    private final FriendAccountConfigurationManager accountManager;
    private boolean hasAttemptedLogin = false;
    private final FriendConnectionFactory friendConnectionFactory;
    
    @Inject
    public AutoLoginService(FriendAccountConfigurationManager accountManager,
            FriendConnectionFactory friendConnectionFactory) {
        this.accountManager = accountManager;
        this.friendConnectionFactory = friendConnectionFactory;
    }
    
    /**
     * Used to identify whether or not this service will attempt to automatically login.
     */
    public boolean hasLoginConfig() {
        return accountManager.getAutoLoginConfig() != null;
    }
    
    /**
     * Whether or not the service has attempted a login yet.
     */
    public boolean hasAttemptedLogin() {
        return hasAttemptedLogin;
    }
    
    /**
     * If an auto login is in process.
     */
    public boolean isAttemptingLogin() {
        return !hasAttemptedLogin() && hasLoginConfig(); 
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return "Auto-Login Serivce";
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
                if(hasLoginConfig()) {
                    friendConnectionFactory.login(accountManager.getAutoLoginConfig());
                }
                hasAttemptedLogin = true;
            }
        });
    }
    
    @Override
    public void stop() {
    }
}
