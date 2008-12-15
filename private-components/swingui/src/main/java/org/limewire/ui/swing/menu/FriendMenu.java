package org.limewire.ui.swing.menu;

import javax.swing.JMenu;

import org.limewire.ui.swing.action.StatusActions;
import org.limewire.ui.swing.menu.actions.ChatAction;
import org.limewire.ui.swing.menu.actions.SignInOutAction;
import org.limewire.ui.swing.menu.actions.SwitchUserAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class FriendMenu extends JMenu {
    
  
    @Inject
    public FriendMenu(SwitchUserAction switchUserAction, SignInOutAction signInOutAction, ChatAction chatAction, StatusActions statusActions) {
        super(I18n.tr("Friend"));
        add(chatAction);
//        add(new FriendDownloadAction());
//        add(new FriendShareAction());
        addSeparator();
        add(statusActions.getAvailableAction());
        add(statusActions.getDnDAction());
        addSeparator();
        add(switchUserAction);
        add(signInOutAction);
    }
}