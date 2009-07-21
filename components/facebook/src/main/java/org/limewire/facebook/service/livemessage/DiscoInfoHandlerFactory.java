package org.limewire.facebook.service.livemessage;

import org.limewire.facebook.service.FacebookFriendConnection;

/**
 * Factory that creates a {@link DiscoInfoHandler} for a {@link FacebookFriendConnection}.
 */
public interface DiscoInfoHandlerFactory {
    DiscoInfoHandler create(FacebookFriendConnection connection);
}
