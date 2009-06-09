package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.table.TablePopupHandler;

public class MyLibraryPopupHandler implements TablePopupHandler {
    private LibraryTable<LocalFileItem> table;

    private MyLibraryPopupMenu popupMenu;

    public MyLibraryPopupHandler(LibraryTable<LocalFileItem> table, MyLibraryPopupMenu popupMenu) {
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
       LocalFileItem selectedItem = table.getLibraryTableModel().getFileItem(popupRow);
       List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(table.getSelectedItems());

       if (selectedItems.size() <= 1 || !selectedItems.contains(selectedItem)) {
           selectedItems.clear();
           table.setRowSelectionInterval(popupRow, popupRow);
       }
       
       popupMenu.setSelectable(table);
       popupMenu.show(component, x, y);
    }
}
