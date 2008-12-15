package com.limegroup.gnutella.gui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.library.RecursiveSharingPanel;
import com.limegroup.gnutella.gui.sharing.ShareManager;

/** 
 * This class shows the <tt>JFileChooser</tt> when the user presses 
 * the button to add a new directory to the shared directories.  It
 * adds the directory only if does not already exist in the list.
 */
public class SelectSharedDirectoryAction extends AbstractAction {
    
    private final RecursiveSharingPanel recursiveSharingPanel;
    private final Component parent;

    /**
     * @param parent the owner of the dialog, the dialog is centered on it.
     */
    public SelectSharedDirectoryAction(RecursiveSharingPanel recursiveSharingPanel, Component parent) {
        super(I18n.tr("Add") + "...");
        this.recursiveSharingPanel = recursiveSharingPanel;
        this.parent = parent;
    }
    
	public void actionPerformed(ActionEvent ae) {
		File dir = FileChooserHandler.getInputDirectory(parent);
		if(ShareManager.checkAndWarnNewSharedFolder(dir)) {
		    recursiveSharingPanel.addRoot(dir);
		}
	}
}