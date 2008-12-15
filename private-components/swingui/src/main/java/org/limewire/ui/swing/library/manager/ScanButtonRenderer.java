package org.limewire.ui.swing.library.manager;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class ScanButtonRenderer extends JCheckBox implements TableCellRenderer {
    
    @Resource private Icon icon;
    @Resource private Icon selectedIcon;
    
    public ScanButtonRenderer() {
        
        GuiUtils.assignResources(this);

        this.setIcon(icon);
        this.setSelectedIcon(selectedIcon);
        
        setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        LibraryManagerItem item = (LibraryManagerItem) value;
        setSelected(item.isScanned());
        
        if(isSelected)
            setBackground(table.getSelectionBackground());
        else
            setBackground(table.getBackground());
       
        
        return this;
    }
}
