package org.limewire.ui.swing.friends.settings;

import org.limewire.facebook.service.FacebookFriendConnectionConfiguration;

/**
 * Configuration necessary to create a facebook <code>FriendConnection</code>
 * via <code>FriendConnectionFactory.login(FriendConnectionConfiguration configuration)</code>
 */
public interface FacebookFriendAccountConfiguration extends FriendAccountConfiguration, FacebookFriendConnectionConfiguration {

}
