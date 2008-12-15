package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.event.ActionListener;

import org.limewire.core.api.download.DownloadItem;


public interface DownloadTableCell {
    
    public void update(DownloadItem item);
    public Component getComponent();
    public void setEditorListener(ActionListener editorListener);

}
