package org.limewire.ui.swing.friends.chat;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.friend.client.ChatState;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.client.FriendException;

class MessageWriterImpl implements MessageWriter {
    private final String localID;
    private final ChatFriend chatFriend;
    private final MessageWriter writer;

    MessageWriterImpl(String localID, ChatFriend chatFriend, MessageWriter writer) {
        this.localID = localID;
        this.chatFriend = chatFriend;
        this.writer = writer;
    }

    @Override
    public void writeMessage(final String message) throws FriendException {
        ThreadExecutor.startThread(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.writeMessage(message);
                } catch (FriendException e) {
                    e.printStackTrace();
                }
            }
        }, "send-message");
        new MessageReceivedEvent(newMessage(message, Message.Type.Sent)).publish();
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageTextImpl(localID, chatFriend.getID(), type, message);
    }

    @Override
    public void setChatState(ChatState chatState) throws FriendException {
        writer.setChatState(chatState);
    }
}
