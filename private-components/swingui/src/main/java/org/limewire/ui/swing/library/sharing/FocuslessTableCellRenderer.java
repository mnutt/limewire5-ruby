package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

class FocuslessTableCellRenderer extends DefaultTableCellRenderer {
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        return super.getTableCellRendererComponent(table, value, false, false, row, column);
    }
    
}
