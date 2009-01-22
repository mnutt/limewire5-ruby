package org.limewire.ui.swing.menu;

import javax.swing.JComponent;

import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.action.StatusActions;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.menu.actions.ChatAction;
import org.limewire.ui.swing.menu.actions.SignInOutAction;
import org.limewire.ui.swing.menu.actions.SwitchUserAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class FriendMenu extends MnemonicMenu {
    
    private final ChatAction chatAction;
    
  
    @Inject
    public FriendMenu(SwitchUserAction switchUserAction, SignInOutAction signInOutAction, ChatAction chatAction, StatusActions statusActions) {
        // TODO fberger
        //super(I18n.tr("F&riend"));
        super(I18n.tr("Friend"));
        this.chatAction = chatAction;
        add(chatAction);
//        add(new FriendDownloadAction());
//        add(new FriendShareAction());
        addSeparator();
        add(statusActions.getAvailableMenuItem());
        add(statusActions.getDnDMenuItem());
        addSeparator();
        add(switchUserAction);
        add(signInOutAction);
    }

    @Inject void register(Navigator navigator, final LibraryNavigator libraryNavigator) {
        // listen for changes in the selected friend
        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void itemSelected(NavCategory category, NavItem navItem,
                    NavSelectable selectable, JComponent panel) {
                chatAction.setFriend(libraryNavigator.getSelectedFriend());
            }
            
            @Override public void itemAdded(NavCategory category, NavItem navItem, JComponent panel) {}
            @Override public void itemRemoved(NavCategory category, NavItem navItem, JComponent panel) {}
            @Override public void categoryAdded(NavCategory category) {}
            @Override public void categoryRemoved(NavCategory category) {}
        });
    }
}