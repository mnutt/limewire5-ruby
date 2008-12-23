package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.util.StringUtils;

public class FriendShareListEvent {
    
    public enum Type {
        FRIEND_SHARE_LIST_ADDED, FRIEND_SHARE_LIST_REMOVED, FRIEND_SHARE_LIST_DELETED;
    }
    
    private final Type type;
    private final LocalFileList fileList;
    private final Friend friend;
    
    public FriendShareListEvent(Type type, LocalFileList fileList, Friend friend) {
        this.type = type;
        this.fileList = fileList;
        this.friend = friend;
    }

    public Type getType() {
        return type;
    }

    public LocalFileList getFileList() {
        return fileList;
    }

    public Friend getFriend() {
        return friend;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    
    
    

}
