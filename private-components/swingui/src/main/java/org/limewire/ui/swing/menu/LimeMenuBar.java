package org.limewire.ui.swing.menu;

import javax.swing.JMenuBar;

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
    }
}
