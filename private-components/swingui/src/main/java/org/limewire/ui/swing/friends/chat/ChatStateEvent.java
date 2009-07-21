package org.limewire.ui.swing.friends.chat;

import org.limewire.friend.api.ChatState;
import org.limewire.ui.swing.event.AbstractEDTEvent;

public class ChatStateEvent extends AbstractEDTEvent {
    private static final String TOPIC_PREFIX = "chatstate-";
    private final ChatFriend chatFriend;
    private final ChatState state;

    public ChatStateEvent(ChatFriend chatFriend, ChatState state) {
        this.chatFriend = chatFriend;
        this.state = state;
    }
    
    public ChatFriend getFriend() {
        return chatFriend;
    }
    
    public ChatState getState() {
        return state;
    }
    
    public static String buildTopic(String conversationName) {
        return TOPIC_PREFIX + conversationName;
    }
    
    @Override
    public void publish() {
        super.publish(buildTopic(chatFriend.getID()));
    }
}
