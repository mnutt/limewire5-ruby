package org.limewire.ui.swing.library.image;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JList;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.images.ImageCellRenderer;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.table.Configurable;

public class LibraryImageCellRenderer extends ImageCellRenderer {
    private Configurable configurable;
    
    public LibraryImageCellRenderer(int width, int height, ThumbnailManager thumbnailManager) {
        super(width, height, thumbnailManager);
    }
    
    
    public void setButtonComponent(JComponent buttonPanel) {
        super.setButtonComponent(buttonPanel);
        if(buttonPanel instanceof Configurable){
            configurable = (Configurable)buttonPanel;
        }
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        if (configurable != null){
            configurable.configure((LocalFileItem)value, isSelected);
        }
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }

}
