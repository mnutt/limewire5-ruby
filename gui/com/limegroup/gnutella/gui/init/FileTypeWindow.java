package com.limegroup.gnutella.gui.init;

import javax.swing.JComponent;

import com.limegroup.gnutella.gui.sharing.FileTypeSharingPanelManager;
import com.limegroup.gnutella.gui.FramedDialog;

/**
 * Composed panel for the first start setup sequence that uses a FileTypeSharingPanelManager to
 *  manage file type extensions sharing.  
 */
final class FileTypeWindow extends SetupWindow {
    
    private FileTypeSharingPanelManager manager;

    FileTypeWindow(FramedDialog framedDialog) {
        super(FileTypeSharingPanelManager.TITLE, FileTypeSharingPanelManager.LABEL,
                FileTypeSharingPanelManager.URL);
        this.manager = new FileTypeSharingPanelManager(framedDialog);
        this.manager.initOptions();
    }
    
    /**
     * Also add the language options.
     */
    protected void createPageContent() {
        manager.buildUI();
        setSetupComponent((JComponent)this.manager.getContainer());
    }
    
    /**
     * Overrides applySettings in SetupWindow superclass.
     * Applies the settings handled in this window.
     */
    @Override
    public void applySettings(boolean loadCoreComponents) {
        this.manager.applyOptions();
    }

}
