package org.limewire.ui.swing.downloads.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.download.DownloadItem;

public class DownloadTableRenderer implements TableCellRenderer {

    private DownloadTableCell cellComponent;  
        
    public DownloadTableRenderer(DownloadTableCell cellComponent) {
        this.cellComponent = cellComponent;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        cellComponent.update((DownloadItem) value);
        return cellComponent.getComponent();
    }
    
}
