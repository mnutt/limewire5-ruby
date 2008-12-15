package com.limegroup.gnutella.gui.playlist;

import java.awt.Component;


import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *  Numbers each row in the table
 */
public class NumberTableCellRenderer extends DefaultTableCellRenderer{

    @Override
    public Component getTableCellRendererComponent(JTable table,
            Object value, 
            boolean isSel, 
            boolean hasFocus,
            int row,
            int column) {

            String formatted = Integer.toString(row + 1);
            return super.getTableCellRendererComponent(table, formatted, isSel, hasFocus, row, column);
}

}
