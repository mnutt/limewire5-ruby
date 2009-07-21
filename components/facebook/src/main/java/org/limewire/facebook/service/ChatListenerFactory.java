package org.limewire.facebook.service;

public interface ChatListenerFactory {
    ChatListener createChatListener(FacebookFriendConnection connection);
}
