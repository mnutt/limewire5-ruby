package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.Collection;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;

public class MyLibraryPopupHandler implements TablePopupHandler {
    private int popupRow = -1;

    private LibraryTable<LocalFileItem> table;

    private MyLibraryPopupMenu popupMenu;
    private MyLibraryMultipleSelectionPopupMenu multiSelectPopupMenu;

    public MyLibraryPopupHandler(LibraryTable<LocalFileItem> table, Category category, LibraryManager libraryManager,
            ShareListManager shareListManager, MagnetLinkFactory magnetFactory, Collection<Friend> friendList,
            PropertiesFactory<LocalFileItem> propertiesFactory, ShareWidgetFactory shareFactory) {
        this.table = table;
        this.popupMenu = new MyLibraryPopupMenu(category, libraryManager, shareListManager, magnetFactory, table, friendList,
                propertiesFactory);
        this.multiSelectPopupMenu = new MyLibraryMultipleSelectionPopupMenu(category, libraryManager, shareFactory, table);
    }

    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = table.rowAtPoint(new Point(x, y));
        if (!table.isRowDisabled(popupRow)) {
            List<LocalFileItem> items = table.getSelectedItems();
            LocalFileItem selectedItem = table.getLibraryTableModel().getFileItem(popupRow);
            if (items.contains(selectedItem) && items.size() > 1) {
                multiSelectPopupMenu.setFileItems(items);
                multiSelectPopupMenu.show(component, x, y);
            } else {
                table.setRowSelectionInterval(popupRow, popupRow);
                popupMenu.setFileItem(selectedItem);
                popupMenu.show(component, x, y);
            }
        }
    }
}
