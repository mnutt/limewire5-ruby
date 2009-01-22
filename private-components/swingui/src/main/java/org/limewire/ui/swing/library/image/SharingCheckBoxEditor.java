package org.limewire.ui.swing.library.image;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.table.Configurable;
import org.limewire.ui.swing.table.TableRendererEditor;

/**
 * Editor for sharing images view. Creates a selectable checkbox over the image
 * to share/unshare that image
 */
public class SharingCheckBoxEditor extends TableRendererEditor implements Configurable {
    
    private final LocalFileList localFileList;
    private final JCheckBox checkBox;
    private LocalFileItem currentItem = null;
    
    public SharingCheckBoxEditor(LocalFileList fileList) {
        this.localFileList = fileList;
        
        setLayout(new MigLayout());
        
        checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        checkBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentItem != null) {
                    //if the box was checked, add it to the fileList
                    if(checkBox.isSelected()) {
                        localFileList.addFile(currentItem.getFile());
                    } else { // remove it from the fileList
                        localFileList.removeFile(currentItem.getFile());
                    }
                }
            }
        });
        
        add(checkBox, "alignx right");
    }
    
    @Override
    public void configure(LocalFileItem item, boolean isRowSelected) {
        currentItem = item;
        checkBox.setSelected(localFileList.contains(item.getFile()));
    }

    @Override
    public Component doTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        configure((LocalFileItem)value, isSelected);
        return this;
    }

    @Override
    public Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        configure((LocalFileItem)value, isSelected);
        return this;
    }

}
