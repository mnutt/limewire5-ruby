package org.limewire.facebook.service.livemessage;

import org.limewire.facebook.service.FacebookFriendConnection;


public interface ConnectBackRequestHandlerFactory {
    ConnectBackRequestHandler create(FacebookFriendConnection connection);
}
