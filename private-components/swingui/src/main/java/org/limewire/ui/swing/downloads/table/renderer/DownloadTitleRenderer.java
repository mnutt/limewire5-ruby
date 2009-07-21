package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PropertiableFileUtils;

import com.google.inject.Inject;

public class DownloadTitleRenderer extends JLabel implements TableCellRenderer {
    @Resource
    private Icon warningIcon;
    @Resource
    private Icon downloadingIcon;
    
    
    private CategoryIconManager categoryIconManager;
    
    @Inject
    public DownloadTitleRenderer(CategoryIconManager categoryIconManager){
        
        GuiUtils.assignResources(this);
        
        //row highlighters only work on opaque renderers
        setOpaque(true);
        
        this.categoryIconManager = categoryIconManager;
        
        new DownloadRendererProperties().decorateComponent(this);
        setIconTextGap(6);
        setBorder(new EmptyBorder(0,4,0,0));
    }
    

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof DownloadItem) {
            DownloadItem item = (DownloadItem)value;
            updateIcon(item.getState(), item);
            updateTitle(item);
        } else {
            setIcon(null);
            setText("");
        }
        return this;
    }
    
    private void updateIcon(DownloadState state, DownloadItem item) {
        switch (state) {
        case ERROR:
            setIcon(warningIcon);
            break;

        case FINISHING:
        case DONE:
            setIcon(categoryIconManager.getIcon(item.getCategory()));
            break;
            
        default:
            setIcon(downloadingIcon);
        }
    }
    
    private void updateTitle(DownloadItem item){
        setText(PropertiableFileUtils.getNameProperty(item, true));
    }
}
