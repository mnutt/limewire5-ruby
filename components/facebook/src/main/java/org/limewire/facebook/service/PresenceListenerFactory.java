package org.limewire.facebook.service;

public interface PresenceListenerFactory {
    PresenceListener createPresenceListener(FacebookFriendConnection connection);
}
