package org.limewire.xmpp.api.client;

/**
 * Contains the information necessary to display an incoming friend request
 * to the user and return the user's decision to the XMPP connection.
 */
public class FriendRequest {

    public enum EventType {REQUESTED}

    private final String friendUsername;
    private final FriendRequestDecisionHandler decisionHandler;

    public FriendRequest(String friendUsername,
            FriendRequestDecisionHandler decisionHandler) {
        this.friendUsername = friendUsername;
        this.decisionHandler = decisionHandler;
    }

    public String getFriendUsername() {
        return friendUsername;
    }
    
    public FriendRequestDecisionHandler getDecisionHandler() {
        return decisionHandler;
    }
}
