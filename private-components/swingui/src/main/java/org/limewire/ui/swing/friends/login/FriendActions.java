package org.limewire.ui.swing.friends.login;

public interface FriendActions {
    
    void signIn();
    
    void signOut(boolean switchUser);
    
    boolean isSignedIn();

}
