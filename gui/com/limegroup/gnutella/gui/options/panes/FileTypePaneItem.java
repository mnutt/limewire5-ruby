package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.sharing.FileTypeSharingPanelManager;

/**
 * Composed abstract pane for the options window that uses a FileTypeSharingPanelManager to
 *  manage file type extensions sharing.  
 */
public final class FileTypePaneItem extends AbstractPaneItem {

    private FileTypeSharingPanelManager manager;

    public FileTypePaneItem() {
        super(I18n.tr(FileTypeSharingPanelManager.TITLE), I18n.tr(FileTypeSharingPanelManager.LABEL), 
                FileTypeSharingPanelManager.URL);
        
        this.manager = new FileTypeSharingPanelManager(this.getContainer());
        
        this.add(this.manager.getContainer());
    }
    
    @Override
    public void initOptions() {
        this.manager.initOptions();
    }

    @Override
    public boolean applyOptions() throws IOException {
        return this.manager.applyOptions();
    }

    public boolean isDirty() {
        return this.manager.isDirty();
    }

}
