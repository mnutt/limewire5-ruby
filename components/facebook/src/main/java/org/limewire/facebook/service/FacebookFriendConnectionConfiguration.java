package org.limewire.facebook.service;

import org.limewire.friend.api.FriendConnectionConfiguration;

public interface FacebookFriendConnectionConfiguration extends FriendConnectionConfiguration {
    /**
     * Loads the cookies from the browser where
     * the user logged in to facebook.
     */
    void loadCookies();

    /**
     * Clears the cookies from the browser so that
     * facebook sessions are NOT maintained across LW sessions
     */
    void clearCookies();

    /**
     * @param autoLogin whether this connection should be automatically
     * created on LW startup
     */
    void setAutoLogin(boolean autoLogin);

    /**
     * @return whether this connection should be automatically
     * created on LW startup
     */
    boolean isAutologin();
}
