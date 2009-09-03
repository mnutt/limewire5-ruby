package org.limewire.ui.swing.friends.login;

import org.limewire.ui.swing.friends.settings.FacebookFriendAccountConfiguration;

public interface FacebookLoginActionFactory {
    FacebookLoginAction create(FacebookFriendAccountConfiguration configuration);
}
