package com.limegroup.gnutella.gui.search;

import java.awt.Component;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

class DateRenderer extends DefaultTableCellRenderer {
    
    private static final DateFormat FORMAT =
        DateFormat.getDateInstance(DateFormat.MEDIUM);

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value, 
                                                   boolean isSel, 
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        Date d = (Date)value;
        if(d == null)
            return super.getTableCellRendererComponent(table, value, isSel, hasFocus, row, column);
            
        String formatted = FORMAT.format(d);
        return super.getTableCellRendererComponent(table, formatted, isSel, hasFocus, row, column);
    }
}
