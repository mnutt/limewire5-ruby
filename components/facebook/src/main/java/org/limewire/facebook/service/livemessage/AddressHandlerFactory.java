package org.limewire.facebook.service.livemessage;

import org.limewire.facebook.service.FacebookFriendConnection;

public interface AddressHandlerFactory {
    AddressHandler create(FacebookFriendConnection connection);
}
