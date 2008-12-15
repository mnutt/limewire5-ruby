package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JTable;

import org.jdesktop.swingx.JXLabel;

/**
 * Draws simple String/Number values in Classic Search Result table cell. If 
 * the result is considered spam, the text is displayed in a grayed out fashion.
 */
public class OpaqueStringRenderer extends OpaqueTableCellRenderer {
    
    private final JXLabel label = new JXLabel();
    
    public OpaqueStringRenderer() {
        super(FlowLayout.LEFT);
        addComponent(label);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if(value == null) {
            label.setText("");
        } else {
            label.setText(value.toString());
            if(value instanceof Number) {
                setLayoutAlignment(FlowLayout.RIGHT);
            } else {
                setLayoutAlignment(FlowLayout.LEFT);
            }
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
