package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;

public class MyLibraryPopupHandler implements TablePopupHandler {
    private LibraryTable<LocalFileItem> table;

    private MyLibraryPopupMenu popupMenu;

    public MyLibraryPopupHandler(LibraryTable<LocalFileItem> table, Category category, LibraryManager libraryManager,
            ShareListManager shareListManager, MagnetLinkFactory magnetFactory,
            PropertiesFactory<LocalFileItem> propertiesFactory, ShareWidgetFactory shareFactory) {
        this.table = table;
        this.popupMenu = new MyLibraryPopupMenu(category, libraryManager, shareFactory, propertiesFactory);
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
