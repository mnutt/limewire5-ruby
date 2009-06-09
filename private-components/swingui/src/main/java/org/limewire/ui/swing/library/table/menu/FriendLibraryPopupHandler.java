package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.table.TablePopupHandler;

public class FriendLibraryPopupHandler implements TablePopupHandler {

    final private LibraryTable<RemoteFileItem> table;

    final private FriendLibraryPopupMenu popupMenu;

    public FriendLibraryPopupHandler(LibraryTable<RemoteFileItem> table, FriendLibraryPopupMenu popupMenu) {
        this.table = table;
        this.popupMenu = popupMenu;
    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        int popupRow = table.rowAtPoint(new Point(x, y));
        List<RemoteFileItem> selectedItems = table.getSelectedItems();
        RemoteFileItem selectedItem = table.getLibraryTableModel().getFileItem(popupRow);
        
        if (selectedItems.size() <= 1 || !selectedItems.contains(selectedItem)) {
            table.setRowSelectionInterval(popupRow, popupRow);
            selectedItems = table.getSelectedItems();
        }

        popupMenu.setFileItems(selectedItems);
        popupMenu.show(component, x, y);
    }

}
