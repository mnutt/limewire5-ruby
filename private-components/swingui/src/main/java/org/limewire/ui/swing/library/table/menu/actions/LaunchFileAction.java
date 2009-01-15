package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.NativeLaunchUtils;

/**
 * Tries to safely launch the given file.
 * If it is an allowed file type it will be launched, 
 * otherwise explorer will be opened to the files location
 */
public class LaunchFileAction extends AbstractAction {
    
    private final LocalFileItem localFile;
    
    public LaunchFileAction(String name, LocalFileItem localFile) {
        super(name);
        this.localFile = localFile;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NativeLaunchUtils.safeLaunchFile(localFile.getFile());
    }
}