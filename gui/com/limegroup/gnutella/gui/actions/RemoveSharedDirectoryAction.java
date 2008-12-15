package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.library.RecursiveSharingPanel;

/**
 * Removes selected root folders from a {@link RecursiveSharingPanel}.
 */
public class RemoveSharedDirectoryAction extends AbstractAction {

    private final RecursiveSharingPanel recursiveSharingPanel;

    public RemoveSharedDirectoryAction(RecursiveSharingPanel recursiveSharingPanel) {
        super(I18n.tr("Remove"));
        this.recursiveSharingPanel = recursiveSharingPanel;
        setEnabled(false);
        recursiveSharingPanel.getTree().addTreeSelectionListener(new EnablementSelectionListener());
    }
    
    public void actionPerformed(ActionEvent e) {
        File dir = (File) recursiveSharingPanel.getTree().getSelectionPath().getLastPathComponent();
        recursiveSharingPanel.removeRoot(dir);
    }
    
    /**
     * Enables action when a root folder is selected.
     */
    private class EnablementSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent e) {
            Object obj = e.getPath().getLastPathComponent();
            if (obj instanceof File) {
                setEnabled(recursiveSharingPanel.isRoot(((File)obj)));
            } else {
                setEnabled(false);
            }
        }

    }

}