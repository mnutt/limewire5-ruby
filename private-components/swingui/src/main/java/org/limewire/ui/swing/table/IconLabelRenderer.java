package org.limewire.ui.swing.table;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.FileUtils;

/**
 * Renders a table cell with a string and the system icon representing that
 * file type.
 */
public class IconLabelRenderer extends JXPanel implements TableCellRenderer {

    private final IconManager iconManager;
    private final JLabel label;
    
    public IconLabelRenderer(IconManager iconManager) {
        super(new BorderLayout());
        this.iconManager = iconManager;
        
        setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
        label = new JLabel();        
        label.setIconTextGap(5);
        
        add(label);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if (table.getSelectedRow() == row) {
            this.setBackground(table.getSelectionBackground());
            this.setForeground(table.getSelectionForeground());
        } else {
            this.setBackground(table.getBackground());
            this.setForeground(table.getForeground());
        }
        
        if (value instanceof FileItem) {
            
            FileItem item = (FileItem) value;

            if (item instanceof RemoteFileItem) {
                label.setIcon(iconManager.getIconForExtension(FileUtils.getFileExtension(item.getFileName())));
            } else {
                label.setIcon(iconManager.getIconForFile(((LocalFileItem) item).getFile()));
            }
            label.setText(item.getFileName());
            
        } else if (value instanceof VisualSearchResult){
            
            VisualSearchResult vsr = (VisualSearchResult)value;
            
            label.setText(vsr.getPropertyString(FilePropertyKey.NAME));
            label.setIcon(iconManager.getIconForExtension(vsr.getFileExtension()));

            setAlpha(vsr.isSpam() ? 0.2f : 1.0f);
        } else if (value != null) {
            throw new IllegalArgumentException(value + " must be a FileItem or VisualSearchResult");
        }
        
        return this;
    }
}
