package org.limewire.facebook.service.livemessage;

import org.limewire.facebook.service.FacebookFriendConnection;

public interface AuthTokenHandlerFactory {
    AuthTokenHandler create(FacebookFriendConnection connection);
}
