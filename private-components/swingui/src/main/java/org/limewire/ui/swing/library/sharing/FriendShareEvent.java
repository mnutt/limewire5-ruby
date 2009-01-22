package org.limewire.ui.swing.library.sharing;

import java.util.EventObject;

import org.limewire.core.api.friend.Friend;

public class FriendShareEvent extends EventObject {
    
    public enum ShareEventType {SHARE, UNSHARE}
    
    private ShareEventType type;

    public FriendShareEvent(Friend sourceFriend, ShareEventType type) {
        super(sourceFriend);
        this.type = type;
    }
    
    public ShareEventType getShareEventType(){
        return type;
    }

}
