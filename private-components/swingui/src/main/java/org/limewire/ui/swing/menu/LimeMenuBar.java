package org.limewire.ui.swing.menu;

import javax.swing.JMenuBar;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.limewire.ui.swing.action.MnemonicMenu;

import com.google.inject.Inject;

public class LimeMenuBar extends JMenuBar {

    @Inject
    LimeMenuBar(FileMenu fileMenu, FriendsMenu friendMenu, ViewMenu viewMenu, HelpMenu helpMenu,
            ToolsMenu toolsMenu) {
        
        add(fileMenu);
        add(viewMenu);
        add(friendMenu);
        add(toolsMenu);
        add(helpMenu);
        
        MenuListener listener = new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
                ((MnemonicMenu)e.getSource()).removeAll();
            }
            @Override
            public void menuDeselected(MenuEvent e) {
                ((MnemonicMenu)e.getSource()).removeAll();                
            }
            @Override
            public void menuSelected(MenuEvent e) {
                ((MnemonicMenu)e.getSource()).createMenuItems();
            }
        };
        
        fileMenu.addMenuListener(listener);
        viewMenu.addMenuListener(listener);
        friendMenu.addMenuListener(listener);
        toolsMenu.addMenuListener(listener);
        helpMenu.addMenuListener(listener);
    }
}
