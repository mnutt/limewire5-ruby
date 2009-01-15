package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;

public class ShareLibraryPopupHandler implements TablePopupHandler {
    private LibraryTable<LocalFileItem> table;

    private ShareLibraryPopupMenu popupMenu;

    public ShareLibraryPopupHandler(LocalFileList friendFileList,
            LibraryTable<LocalFileItem> table, Category category, LibraryManager libraryManager,
            MagnetLinkFactory magnetFactory,
            PropertiesFactory<LocalFileItem> propertiesFactory) {
        this.table = table;
        this.popupMenu = new ShareLibraryPopupMenu(friendFileList, category, libraryManager,
                propertiesFactory);
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
            selectedItems.add(selectedItem);
        }

        popupMenu.setFileItems(selectedItems);
        popupMenu.show(component, x, y);
    }
}
