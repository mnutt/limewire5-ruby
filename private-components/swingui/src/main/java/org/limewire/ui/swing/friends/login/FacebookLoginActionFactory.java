package org.limewire.ui.swing.friends.login;

import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;

public interface FacebookLoginActionFactory {
    FacebookLoginAction create(FriendAccountConfiguration configuration);
}
