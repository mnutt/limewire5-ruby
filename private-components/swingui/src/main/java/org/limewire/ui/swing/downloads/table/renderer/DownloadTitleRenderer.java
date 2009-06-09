package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class DownloadTitleRenderer extends JXPanel implements TableCellRenderer {
    @Resource
    private Icon warningIcon;
    @Resource
    private Icon downloadingIcon;
    
    private JLabel iconLabel;
    private JLabel titleLabel;
    
    private CategoryIconManager categoryIconManager;
    
    @Inject
    public DownloadTitleRenderer(CategoryIconManager categoryIconManager){
        setLayout(new MigLayout("insets 0 0 0 0, gap 0 0 0 0, novisualpadding, nogrid, aligny center"));
        GuiUtils.assignResources(this);
        
        this.categoryIconManager = categoryIconManager;        
              
        iconLabel = new JLabel();
        titleLabel = new JLabel();
        
        new DownloadRendererProperties().decorateComponent(titleLabel);
        
        add(iconLabel, "gapleft 4");
        add(titleLabel, "gapleft 6");
    }
    

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof DownloadItem) {
            DownloadItem item = (DownloadItem)value;
            updateIcon(item.getState(), item);
            updateTitle(item);
        } else {
            iconLabel.setIcon(null);
            titleLabel.setText("");
        }
        return this;
    }
    
    private void updateIcon(DownloadState state, DownloadItem item) {
        switch (state) {
        case ERROR:
            iconLabel.setIcon(warningIcon);
            break;

        case FINISHING:
        case DONE:
            iconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
            break;
            
        default:
            iconLabel.setIcon(downloadingIcon);
        }
    }
    
    private void updateTitle(DownloadItem item){
        titleLabel.setText(item.getTitle());
    }
}
