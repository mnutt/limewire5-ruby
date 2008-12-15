package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JTable;

import org.jdesktop.swingx.JXLabel;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Draws the quality of a search result in a Classic Search Result table cell. 
 * If the result is considered spam, the text is displayed in a grayed out 
 * fashion.
 */
public class OpaqueQualityRenderer extends OpaqueTableCellRenderer {

    private final JXLabel label = new JXLabel();
    
    public OpaqueQualityRenderer() {
        super(FlowLayout.LEFT);
        addComponent(label);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if(value == null || !(value instanceof Number)) {
            label.setText("");
        } else {
            Number num = (Number)value;
            label.setText(GuiUtils.toQualityString(num.longValue())); 
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
