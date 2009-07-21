package org.limewire.ui.swing.friends.chat;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.impl.AbstractFriendPresence;

public class MockPresence extends AbstractFriendPresence implements FriendPresence {
    private String status;
    private final Friend friend;
    private Mode mode;
    private String jid;
    private int priority;
    
    MockPresence(Friend friend, Mode mode, String status, String jid) {
        this.friend = friend;
        this.mode = mode;
        this.status = status;
        this.jid = jid;
        this.priority = 0;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return priority;
    }

    // package private for unit tests
    void setPriority(int priority) {
        this.priority = priority;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Type getType() {
        return Type.available;
    }

    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return jid;
    }
}
