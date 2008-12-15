package org.limewire.ui.swing.friends.chat;

/**
 * Abstract shell impl of Message
 */
public abstract class AbstractMessageImpl implements Message {

    private final String friendName;
    private final String friendID;
    private final String senderName;
    private final Type type;
    private final long messageTimeMillis;

    public AbstractMessageImpl(String senderName, String friendName, String friendId, Type type) {
        this.friendName = friendName;
        this.friendID = friendId;
        this.senderName = senderName;
        this.type = type;
        this.messageTimeMillis = System.currentTimeMillis();
    }

    @Override
    public String getFriendName() {
        return friendName;
    }

    @Override
    public String getFriendID() {
        return friendID;
    }

    @Override
    public String getSenderName() {
        return senderName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getMessageTimeMillis() {
        return messageTimeMillis;
    }
}
