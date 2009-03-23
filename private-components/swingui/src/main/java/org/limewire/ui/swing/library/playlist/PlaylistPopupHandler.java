package org.limewire.ui.swing.library.playlist;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;

/**
 * A popup handler for the playlist table.
 */
public class PlaylistPopupHandler implements TablePopupHandler {
    
    private final LibraryTable<? extends LocalFileItem> table;
    private final PlaylistPopupMenu popupMenu;

    /**
     * Constructs a PlaylistPopupHandler for the specified playlist table,
     * playlist, library navigator, and properties factory.
     */
    public PlaylistPopupHandler(LibraryTable<? extends LocalFileItem> table,
            Playlist playlist, LibraryNavigator libraryNavigator,
            PropertiesFactory<LocalFileItem> propertiesFactory) {
        this.table = table;
        this.popupMenu = new PlaylistPopupMenu(playlist, libraryNavigator, propertiesFactory);
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        // Get popup row and item.
        int popupRow = table.rowAtPoint(new Point(x, y));
        LocalFileItem popupItem = table.getLibraryTableModel().getFileItem(popupRow);
        
        // Get selected items.
        List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(table.getSelectedItems());

        // Select popup item if not selected.
        if ((selectedItems.size() <= 1) || !selectedItems.contains(popupItem)) {
            selectedItems.clear();
            table.setRowSelectionInterval(popupRow, popupRow);
            selectedItems.add(popupItem);
        }
        
        // Pass selected items to menu and display.
        popupMenu.setFileItems(selectedItems);
        popupMenu.show(component, x, y);
    }

}
