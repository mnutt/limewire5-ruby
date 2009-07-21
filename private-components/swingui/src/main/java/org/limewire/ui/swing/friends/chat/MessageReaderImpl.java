package org.limewire.ui.swing.friends.chat;

import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.MessageReader;

class MessageReaderImpl implements MessageReader {
    private final ChatFriend chatFriend;

    MessageReaderImpl(ChatFriend chatFriend) {
        this.chatFriend = chatFriend;
    }

    @Override
    public void readMessage(final String message) {
        if (message != null) {
            final Message msg = newMessage(message, Message.Type.RECEIVED);
            new MessageReceivedEvent(msg).publish();
        }
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageTextImpl(chatFriend.getName(), chatFriend.getID(), type, message);
    }

    @Override
    public void newChatState(ChatState chatState) {
        new ChatStateEvent(chatFriend, chatState).publish();
    }
    
    @Override
    public void error(String errorMessage) {
        ErrorMessage errMsg = new ErrorMessage(chatFriend.getID(), 
            errorMessage, Message.Type.SERVER);
        new MessageReceivedEvent(errMsg).publish();
    }
}
