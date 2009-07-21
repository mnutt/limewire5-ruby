package org.limewire.facebook.service.livemessage;

import org.limewire.facebook.service.FacebookFriendConnection;

public interface FileOfferHandlerFactory {
    FileOfferHandler create(FacebookFriendConnection connection);
}
