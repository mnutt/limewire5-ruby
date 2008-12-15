package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTable;

public class CalendarRenderer extends DefaultLibraryRenderer {

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if(value == null || !(value instanceof Long)) {
            setText("");
        } else {
            setText(DATE_FORMAT.format(new Date((Long)value))); 
        }
        return this;
    }

}
