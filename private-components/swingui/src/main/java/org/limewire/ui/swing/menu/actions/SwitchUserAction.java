package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class SwitchUserAction extends AbstractAction {

    private final FriendActions friendActions;

    @Inject
    SwitchUserAction(FriendActions friendActions) {
        // TODO fberger
        // super(I18n.tr("Switch &User"));
        super(I18n.tr("Switch User"));
        this.friendActions = friendActions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        friendActions.signOut(true);
    }
}