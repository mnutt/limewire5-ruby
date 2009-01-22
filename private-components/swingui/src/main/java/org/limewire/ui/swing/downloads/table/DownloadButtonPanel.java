package org.limewire.ui.swing.downloads.table;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Renderer and editor for DownloadTables. Editors must be initialized with 
 * <code>editor.initializeEditor(downloadItems)</code>
 */
public class DownloadButtonPanel extends JPanel {
		
	private JButton pauseButton;
    private JButton cancelButton;
    private JButton resumeButton;
    private JButton removeButton;


    @Resource
    private Icon pauseIcon;    
    @Resource
    private Icon pauseIconPressed;    
    @Resource
    private Icon pauseIconRollover;
    @Resource
    private Icon cancelIcon;
    @Resource
    private Icon cancelIconPressed;
    @Resource
    private Icon cancelIconRollover;
    @Resource
    private Icon resumeIcon;
    @Resource
    private Icon resumeIconPressed;
    @Resource
    private Icon resumeIconRollover;   

	/**
	 * Create the panel
	 */
	public DownloadButtonPanel(ActionListener actionListener) {
	    super(new BorderLayout(0,0));
	    
	    GuiUtils.assignResources(this);		

		pauseButton = new IconButton(pauseIcon, pauseIconRollover, pauseIconPressed);
		pauseButton.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
		pauseButton.addActionListener(actionListener);
		pauseButton.setToolTipText(I18n.tr("Pause"));		


        cancelButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        cancelButton.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelButton.addActionListener(actionListener);
        cancelButton.setToolTipText(I18n.tr("Cancel Download"));

        removeButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        removeButton.setActionCommand(DownloadActionHandler.REMOVE_COMMAND);
        removeButton.addActionListener(actionListener);
        removeButton.setToolTipText(I18n.tr("Remove Download"));

		resumeButton = new IconButton(resumeIcon, resumeIconRollover, resumeIconPressed);
		resumeButton.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
		resumeButton.addActionListener(actionListener);
		resumeButton.setVisible(false);
        resumeButton.setToolTipText(I18n.tr("Resume"));
		
		JPanel leadPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0,0));
		leadPanel.setOpaque(false);
		
		leadPanel.add(resumeButton);
		leadPanel.add(pauseButton);
		
		JPanel cancelPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0,0));
		cancelPanel.setOpaque(false);
		cancelPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,15));
		cancelPanel.add(cancelButton);
		cancelPanel.add(removeButton);
		
				
		add(leadPanel, BorderLayout.WEST);
		add(cancelPanel, BorderLayout.EAST);
	}	
	
	public void setActionListener(ActionListener actionListener){
	    pauseButton.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);
        resumeButton.addActionListener(actionListener);
        removeButton.addActionListener(actionListener);
	}

	

	public void updateButtons(DownloadState state) {
		pauseButton.setVisible(state == DownloadState.DOWNLOADING);  //used to be connecting also. keeping consistent with tray
		resumeButton.setVisible(state.isResumable());
        cancelButton.setVisible(state != DownloadState.DONE);
        removeButton.setVisible(state == DownloadState.DONE);
	}

}
