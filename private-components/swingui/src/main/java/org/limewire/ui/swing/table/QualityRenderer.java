package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.DefaultLibraryRenderer;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Displays the quality of a file in a table cell. 
 */
public class QualityRenderer extends DefaultLibraryRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if(value == null) {
            setText("");
        } else if(value instanceof VisualSearchResult){
            
            VisualSearchResult result = (VisualSearchResult)value;
            if(result.isSpam()) {
                setText(I18n.tr("Spam"));
            } else {
                if(!(result.getProperty(FilePropertyKey.QUALITY) instanceof Number))
                    setText("");
                else {
                    Number num = ((Number)result.getProperty(FilePropertyKey.QUALITY));
                    setText(GuiUtils.toQualityStringShort(num.longValue())); 
                }
            }
        } else if(value instanceof RemoteFileItem) {
            RemoteFileItem item = (RemoteFileItem) value;

            if(!(item.getProperty(FilePropertyKey.QUALITY) instanceof Number))
                setText("");
            else {
                Number num = ((Number)item.getProperty(FilePropertyKey.QUALITY));
                setText(GuiUtils.toQualityStringShort(num.longValue())); 
            }
        } else if(value instanceof LocalFileItem) {
            LocalFileItem item = (LocalFileItem) value;

            if(!(item.getProperty(FilePropertyKey.QUALITY) instanceof Number))
                setText("");
            else {
                Number num = ((Number)item.getProperty(FilePropertyKey.QUALITY));
                setText(GuiUtils.toQualityStringShort(num.longValue())); 
            }
        }
        return this;
    }
}
