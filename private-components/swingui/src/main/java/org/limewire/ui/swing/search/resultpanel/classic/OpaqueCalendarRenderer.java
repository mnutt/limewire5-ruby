package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTable;

import org.jdesktop.swingx.JXLabel;

/**
 * Draws a date in a Classic Search Result table cell. If the result
 * is considered spam, the text is displayed in a grayed out fashion.
 */
public class OpaqueCalendarRenderer extends OpaqueTableCellRenderer {

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    
    private final JXLabel label = new JXLabel();
    
    public OpaqueCalendarRenderer() {
        super(FlowLayout.LEFT);
        addComponent(label);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if(value == null || !(value instanceof Long)) {
            label.setText("");
        } else {
            label.setText(DATE_FORMAT.format(new Date((Long)value))); 
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
