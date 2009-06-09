package org.limewire.core.api.friend.client;

public class FriendException extends Exception{
    public FriendException(Throwable cause) {
        super(cause);
    }

    public FriendException(String message) {
        super(message);
    }

    public FriendException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
