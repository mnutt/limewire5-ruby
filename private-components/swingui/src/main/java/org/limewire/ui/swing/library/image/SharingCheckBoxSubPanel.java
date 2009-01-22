package org.limewire.ui.swing.library.image;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.table.Configurable;

public class SharingCheckBoxSubPanel extends JPanel implements Configurable {

    private LocalFileList localFileList;
    
    private JCheckBox checkBox;
    
    public SharingCheckBoxSubPanel(LocalFileList localFileList) {
        this.localFileList = localFileList;
        
        setLayout(new MigLayout());
        setOpaque(false);
        
        checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        
        add(checkBox, "alignx right");
    }
    
    @Override
    public void configure(LocalFileItem item, boolean isRowSelected) {
        if(localFileList.contains(item.getFile()))
            checkBox.setSelected(true);
        else
            checkBox.setSelected(false);
    }
}
