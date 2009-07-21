package org.limewire.ui.swing.friends.chat;

import org.limewire.core.api.download.DownloadState;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendPresence;

public class MockMessageFileOffer implements MessageFileOffer {
    private final String friendID;
    private final String senderName;
    private final Type type;
    private final long messageTimeMillis;
    private final FileMetaData fileOffer;

    public MockMessageFileOffer(ChatFriend chatFriend, long messageTimeMillis, String senderName,
            Type type, FileMetaData fileOffer) {
        this.friendID = chatFriend.getID();
        this.messageTimeMillis = messageTimeMillis;
        this.senderName = senderName;
        this.type = type;
        this.fileOffer = fileOffer;
    }

    @Override
    public String getFriendID() {
        return friendID;
    }

    @Override
    public long getMessageTimeMillis() {
        return messageTimeMillis;
    }

    @Override
    public String format() {
        return fileOffer.getName();
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
    public FileMetaData getFileOffer() {
        return fileOffer;
    }

    @Override
    public void setDownloadState(DownloadState downloadState) {
        // TBD when we wish to test with download states
    }

    @Override
    public FriendPresence getPresence() {
        return null;
    }
}

