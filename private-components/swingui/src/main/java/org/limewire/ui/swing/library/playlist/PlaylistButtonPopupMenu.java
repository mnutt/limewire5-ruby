package org.limewire.ui.swing.library.playlist;

import java.awt.event.ActionEvent;

import javax.swing.JPopupMenu;

import org.limewire.core.api.playlist.Playlist;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

/**
 * A popup menu for the playlist navigation button.
 */
public class PlaylistButtonPopupMenu extends JPopupMenu {

    private final Playlist playlist;
    
    /**
     * Constructs a PlaylistButtonPopupMenu for the specified playlist.
     */
    public PlaylistButtonPopupMenu(Playlist playlist) {
        this.playlist = playlist;
        initialize();
    }

    /**
     * Initializes the menu items in the menu.
     */
    private void initialize() {
        // Add Clear playlist menu item.
        add(new ClearAction());
    }
    
    /**
     * Menu action to clear the playlist.
     */
    private class ClearAction extends AbstractAction {
        
        public ClearAction() {
            super(I18n.tr("Clear playlist"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            playlist.clear();
        }
    }
}
