package org.limewire.ui.swing.friends.chat;


public class MessageTextImpl extends AbstractMessageImpl implements MessageText {

    private final String message;

    public MessageTextImpl(String senderName, ChatFriend chatFriend, Type type, String message) {
        this(senderName, chatFriend.getName(), chatFriend.getID(), type, message);
    }

    public MessageTextImpl(String senderName, String friendName, String friendId, Type type, String message) {
        super(senderName, friendName, friendId, type);
        this.message = message;
    }

    public String getMessageText() {
        return message;
    }

    public String toString() {
        return getMessageText();
    }

    public String format() {
        return URLWrapper.wrap(message.replace("<", "&lt;").replace(">", "&gt;"));
    }
}
