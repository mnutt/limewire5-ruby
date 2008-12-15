package com.limegroup.gnutella.gui.library;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MultiLineLabel;

/**
 * Dialog that shows which subfolders will be shared when a set of folders
 * are selected by the user to be shared.
 */
public class RecursiveSharingDialog extends JDialog {

    private static final long serialVersionUID = 5104889057583433880L;

    /**
     * The return state of the dialog.
     */
    public static enum State { OK, CANCELLED };
    /**
     * The state that is returned.
     */    
    private State state = State.CANCELLED;
    /**
     * The tree panel that contains all the logic.
     */
    private RecursiveSharingPanel sharingPanel;
    
    private MultiLineLabel titleLabel;
    
    /**
     * Constructs a new recursive sharing dialog with a list of roots. 
     */
    public RecursiveSharingDialog(Frame owner, File... roots) {
        super(owner);
        initialize(roots);
    }
    
    public RecursiveSharingDialog(Dialog owner, File...roots) {
        super(owner);
        initialize(roots);
    }
    
    private void initialize(File... roots) {
        sharingPanel = new RecursiveSharingPanel(roots);
        sharingPanel.setRootsExpanded();
        
        JTree tree = sharingPanel.getTree();
        tree.setRootVisible(false);

        setTitle(I18n.tr("Folders to share"));

        JComponent content = (JComponent) getContentPane();
        content.setLayout(new BorderLayout());
        content.setBorder(new EmptyBorder(6, 6, OSUtils.isMacOSX() ? 12 : 6, 6));
        
        // title label
        titleLabel =
            new MultiLineLabel(I18n.tr("The following folders and subfolders will be shared.  You can uncheck any folders you do not want to share.  (You can change your shared folders at any time in LimeWire\'s options.)"), 400, true);
        titleLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
        content.add(titleLabel, BorderLayout.NORTH);

        
        content.add(sharingPanel, BorderLayout.CENTER);
        
        ButtonRow buttonRow = new ButtonRow(new Action[] { new OkayAction(), new CancelAction() }, 
                ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
        buttonRow.setBorder(new EmptyBorder(6, 0, 0, 0));
        content.add(buttonRow, BorderLayout.SOUTH);
        
        GUIUtils.addHideAction(this);
        
        pack();
    }
    
    public void setTitleText(String text) {
        titleLabel.setText(text, 400);
    }
    
    /**
     * Only shows the dialog when one of the folders contains a subfolder the
     * user should be informed about.
     * 
     * @return {@link State#OK} if there are no subfolders or if the user okayed 
     * sharing after they made there selection, or {@link State#CANCELLED} otherwise.
     */
    public State showChooseDialog(Component c) {
        return showChooseDialog(c, true);
    }
    
    public State showChooseDialog(Component c, boolean dontShowDialogIfNoSubfolders)
    {
    	Set<File> roots = getRootsToShare();
    	
    	if (dontShowDialogIfNoSubfolders && roots.size() == sharingPanel.getTree().getRowCount()) {
    		return State.OK;
    	}
    	
        setModal(true);
        setLocationRelativeTo(c);
        setVisible(true);

        // get roots again
        roots = getRootsToShare();
        if (roots.isEmpty()) {
            state = State.CANCELLED;
        }
        
        return state;
    }
    
    /**
     * Returns the folders to share. See {@link RecursiveSharingPanel#getRootsToShare()}. 
     */
    public Set<File> getRootsToShare() {
    	return sharingPanel.getRootsToShare();
    }
    
    /**
     * Returns the sub folders to exclude from sharing.
     * See {@link RecursiveSharingPanel#getFoldersToExclude()}.
     */
    public Set<File> getFoldersToExclude() {
        return sharingPanel.getFoldersToExclude();
    }
    
    
    private class OkayAction extends AbstractAction {
        
        public OkayAction() {
            super(I18n.tr("OK"));
        }
        public void actionPerformed(ActionEvent e) {
            state = State.OK;
            dispose();
        }
    }
    
    private class CancelAction extends AbstractAction  {
    	
    	public CancelAction() {
    		super(I18n.tr("Cancel"));
    	}
    	
    	public void actionPerformed(ActionEvent e) {
    		state = State.CANCELLED;
    		dispose();
    	}
    }
}