package org.limewire.ui.swing.library.table;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

       
public class DefaultLibraryRenderer extends DefaultTableCellRenderer {

    private Border border;
    
    public DefaultLibraryRenderer() {
        border = BorderFactory.createEmptyBorder(0,5,0,5);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    
        setBorder(border);
        
        return this;
    }
}
