package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.TablePopupHandler;

/**
 * Popup handler for the download display tables.
 */
public class DownloadPopupHandler implements TablePopupHandler {
   
    
    private DownloadActionHandler actionHandler;
    private DownloadTable table;

    /**
     * Constructs a DownloadPopupHandler using the specified action handler and
     * display table.
     */
    public DownloadPopupHandler(DownloadActionHandler actionHandler, DownloadTable table) {
        this.actionHandler = actionHandler;
        this.table = table;
    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        adjustTableSelection(x, y);
        showMenu(component, x, y);
    }

    /**
     * Returns the index of the table row at the specified (x,y) location.
     */
    private void adjustTableSelection(int x, int y) {

        int popupRow = table.rowAtPoint(new Point(x, y));
        DownloadItem selectedItem = table.getDownloadItem(popupRow);
        if(selectedItem == null)
            return;
        
        List<DownloadItem> selectedItems = table.getSelectedItems();

        if (selectedItems.size() <= 1 || !selectedItems.contains(selectedItem)) {
            selectedItems.clear();
            table.setRowSelectionInterval(popupRow, popupRow);
        }
     
    }
    
    private void showMenu(Component component, int x, int y){
        new DownloadTableMenu(actionHandler, table).show(component, x, y);
    }


}
