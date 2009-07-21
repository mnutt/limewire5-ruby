package org.limewire.facebook.service.livemessage;

import org.limewire.facebook.service.FacebookFriendConnection;

public interface LibraryRefreshHandlerFactory {
    LibraryRefreshHandler create(FacebookFriendConnection connection);
}
