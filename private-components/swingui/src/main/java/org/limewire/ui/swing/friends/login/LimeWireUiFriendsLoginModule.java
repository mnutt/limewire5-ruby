package org.limewire.ui.swing.friends.login;

import com.google.inject.AbstractModule;

public class LimeWireUiFriendsLoginModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendActions.class).to(FriendsSignInPanel.class);
    }

}
