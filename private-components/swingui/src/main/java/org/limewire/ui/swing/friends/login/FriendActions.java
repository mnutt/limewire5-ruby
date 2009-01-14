package org.limewire.ui.swing.friends.login;


/** Exposes actions for friends. */
public interface FriendActions {
    
    /** Either signs in immediately or shows the sign in dialog. */
    void signIn();
    
    /** Signs out, possibly showing the 'sign in' dialog again if switchUser is true. */
    void signOut(boolean switchUser);
    
    /** Returns true if already signed in. */
    boolean isSignedIn();

}
