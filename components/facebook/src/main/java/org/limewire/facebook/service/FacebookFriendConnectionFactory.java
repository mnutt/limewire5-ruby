package org.limewire.facebook.service;

import org.limewire.friend.api.FriendConnectionConfiguration;

public interface FacebookFriendConnectionFactory {
    FacebookFriendConnection create(FriendConnectionConfiguration configuration);    
}
