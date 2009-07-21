package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.library.table.DefaultLibraryRenderer;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/**
 * Displays a date in a month/day/year format.
 */
@LazySingleton
public class CalendarRenderer extends DefaultLibraryRenderer {
    
    @Inject
    public CalendarRenderer(){
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if(!(value instanceof Long)) {
            setText("");
        } else {
            setText(GuiUtils.msec2DateTime((Long)value)); 
        }
        return this;
    }

}
