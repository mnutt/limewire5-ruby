package org.limewire.facebook.service.livemessage;

import org.limewire.facebook.service.FacebookFriendConnection;

public interface PresenceHandlerFactory {
    PresenceHandler create(FacebookFriendConnection connection);
}
