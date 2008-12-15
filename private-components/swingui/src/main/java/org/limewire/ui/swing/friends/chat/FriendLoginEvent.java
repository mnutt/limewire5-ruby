package org.limewire.ui.swing.friends.chat;

import org.limewire.ui.swing.event.AbstractEDTEvent;

class FriendLoginEvent extends AbstractEDTEvent {
    private final ChatFriend chatFriend;
    
    public FriendLoginEvent(ChatFriend chatFriend) {
        this.chatFriend = chatFriend;
    }

    public ChatFriend getFriend() {
        return chatFriend;
    }
}
