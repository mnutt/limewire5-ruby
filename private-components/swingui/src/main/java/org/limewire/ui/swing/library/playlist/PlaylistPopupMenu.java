package org.limewire.ui.swing.library.playlist;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JPopupMenu;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.Catalog;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.menu.actions.LocateFileAction;
import org.limewire.ui.swing.library.table.menu.actions.PlayAction;
import org.limewire.ui.swing.library.table.menu.actions.ViewFileInfoAction;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.I18n;

/**
 * A popup menu for the playlist table.
 */
public class PlaylistPopupMenu extends JPopupMenu {

    private final Playlist playlist;
    private final LibraryNavigator libraryNavigator;
    private final PropertiesFactory<LocalFileItem> propertiesFactory;
    
    private List<LocalFileItem> fileItemList;

    /**
     * Constructs a PlaylistPopupMenu for the specified playlist, library
     * navigator, and properties factory.
     */
    public PlaylistPopupMenu(Playlist playlist, 
            LibraryNavigator libraryNavigator,
            PropertiesFactory<LocalFileItem> propertiesFactory) {
        this.playlist = playlist;
        this.libraryNavigator = libraryNavigator;
        this.propertiesFactory = propertiesFactory;
    }

    /**
     * Sets the list of file items that the menu operates on.
     */
    public void setFileItems(List<LocalFileItem> fileItemList) {
        this.fileItemList = fileItemList;
        initialize();
    }

    /**
     * Initializes the menu items in the menu.
     */
    private void initialize() {
        boolean singleFile = fileItemList.size() == 1;

        LocalFileItem firstItem = fileItemList.get(0);

        boolean playActionEnabled = singleFile;
        boolean locateActionEnabled = singleFile && !firstItem.isIncomplete();
        boolean viewFileInfoEnabled = singleFile;

        // Remove all menu items.
        removeAll();

        // Add Play menu item.
        add(new PlayAction(libraryNavigator, new Catalog(playlist), firstItem)).setEnabled(playActionEnabled);

        // Add Locate File menu item.
        addSeparator();
        add(new LocateFileAction(firstItem)).setEnabled(locateActionEnabled);

        // Add Remove from and Clear playlist menu items.
        add(new RemoveAction());
        add(new ClearAction());

        // Add View File Info menu item.  Use the underlying file item, usually
        // a CoreLocalFileItem, so the properties dialog can perform the Copy
        // Link action.
        addSeparator();
        LocalFileItem localItem = (firstItem instanceof PlaylistFileItemImpl) ?
                ((PlaylistFileItemImpl) firstItem).getLocalFileItem() : firstItem;
        add(new ViewFileInfoAction(localItem, propertiesFactory)).setEnabled(viewFileInfoEnabled);
    }

    /**
     * Menu action to remove file items from the playlist. 
     */
    private class RemoveAction extends AbstractAction {
        
        public RemoveAction() {
            super(I18n.tr("Remove from playlist"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            File songFile = PlayerUtils.getCurrentSongFile();
            for (LocalFileItem fileItem : fileItemList) {
                playlist.removeFile(fileItem.getFile());
                if (fileItem.getFile().equals(songFile)){
                    PlayerUtils.stop();
                }
            }
        }
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
