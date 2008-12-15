package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JTable;

import org.jdesktop.swingx.JXLabel;
import org.limewire.util.CommonUtils;

/**
 * Draws the running time in a Classic Search Result table cell. If the result
 * is considered spam, the text is displayed in a grayed out fashion.
 */
public class OpaqueTimeRenderer extends OpaqueTableCellRenderer {

    private final JXLabel label = new JXLabel();
    
    public OpaqueTimeRenderer() {
        super(FlowLayout.RIGHT);
        addComponent(label);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if(value == null || !(value instanceof Long)) {
            label.setText("");
        } else {
            label.setText(CommonUtils.seconds2time((Long)value)); 
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
