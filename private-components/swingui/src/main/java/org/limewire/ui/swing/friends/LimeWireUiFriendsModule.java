package org.limewire.ui.swing.friends;

import org.limewire.inject.LazyBinder;
import org.limewire.ui.swing.friends.chat.LimeWireUiFriendsChatModule;
import org.limewire.ui.swing.friends.login.LimeWireUiFriendsLoginModule;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManagerImpl;

import com.google.inject.AbstractModule;


public class LimeWireUiFriendsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendRequestNotificationController.class);
        bind(FriendAccountConfigurationManager.class).toProvider(LazyBinder.newLazyProvider(
                FriendAccountConfigurationManager.class, FriendAccountConfigurationManagerImpl.class));
        
        install(new LimeWireUiFriendsLoginModule());
        install(new LimeWireUiFriendsChatModule());
    }

}
