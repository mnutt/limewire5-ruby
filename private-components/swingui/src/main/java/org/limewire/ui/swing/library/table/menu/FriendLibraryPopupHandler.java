package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

public class FriendLibraryPopupHandler implements TablePopupHandler {
    private int popupRow = -1;

    final private LibraryTable<RemoteFileItem> table;

    final private FriendLibraryPopupMenu popupMenu;

    public FriendLibraryPopupHandler(LibraryTable<RemoteFileItem> table, DownloadListManager downloadListManager, 
            MagnetLinkFactory magnetLinkFactory, PropertiesFactory<RemoteFileItem> propertiesFactory, SaveLocationExceptionHandler saveLocationExceptionHandler) {
        this.table = table;
        this.popupMenu = new FriendLibraryPopupMenu(downloadListManager,magnetLinkFactory, propertiesFactory, saveLocationExceptionHandler);
    }

    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = table.rowAtPoint(new Point(x, y));
        List<RemoteFileItem> items = table.getSelectedItems();
        RemoteFileItem selectedItem = table.getLibraryTableModel().getFileItem(popupRow);
        
        if (!items.contains(selectedItem)) {
            table.setRowSelectionInterval(popupRow, popupRow);
            items = table.getSelectedItems();
        }

        popupMenu.setFileItems(items);
        popupMenu.show(component, x, y);
    }

}
