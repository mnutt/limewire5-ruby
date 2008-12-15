package org.limewire.ui.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Paints a custom tableHeader
 */
public class CustomTableCellHeaderRenderer extends DefaultTableCellRenderer {

    @Resource
    private Color backgroundColor;
    @Resource
    private Color foregroundColor;
    
    private final Border emptyBorder;
    
    private final Font font;
    
    public CustomTableCellHeaderRenderer() {
        GuiUtils.assignResources(this);
        
        emptyBorder = BorderFactory.createEmptyBorder(0, 6, 0, 0);
        font = getFont().deriveFont(Font.BOLD, 11);
        
        setBackground(backgroundColor);
        setForeground(foregroundColor);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        setPreferredSize(new Dimension(20, getPreferredSize().width));
        setBorder(emptyBorder);
        setFont(font);
        
        return this;
    }
}
