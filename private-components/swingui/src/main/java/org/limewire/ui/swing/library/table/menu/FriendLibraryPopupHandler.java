package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

public class FriendLibraryPopupHandler implements TablePopupHandler {

    final private LibraryTable<RemoteFileItem> table;

    final private FriendLibraryPopupMenu popupMenu;

    public FriendLibraryPopupHandler(LibraryTable<RemoteFileItem> table, DownloadListManager downloadListManager, 
            PropertiesFactory<RemoteFileItem> propertiesFactory, SaveLocationExceptionHandler saveLocationExceptionHandler, PropertiesFactory<DownloadItem> downloadItemPropertiesFactory, LibraryManager libraryManager) {
        this.table = table;
        this.popupMenu = new FriendLibraryPopupMenu(downloadListManager, propertiesFactory, saveLocationExceptionHandler, downloadItemPropertiesFactory, libraryManager);
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
