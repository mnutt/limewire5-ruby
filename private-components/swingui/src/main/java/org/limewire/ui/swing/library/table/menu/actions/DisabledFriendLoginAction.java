package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;

/**
 * Menu Action that redirects to the friend login panel when pressed.
 */
public class DisabledFriendLoginAction extends AbstractAction {
    private final FriendsSignInPanel friendsSignInPanel;
    
    public DisabledFriendLoginAction(String menuText, FriendsSignInPanel friendsSignInPanel) {
        super(menuText);
        
        this.friendsSignInPanel = friendsSignInPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        friendsSignInPanel.signIn();
    }
}
