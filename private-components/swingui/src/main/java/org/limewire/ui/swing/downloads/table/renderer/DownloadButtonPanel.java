package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Used by DownloadButtonRendererEditor
 */
public class DownloadButtonPanel extends JPanel {
		
	private final JButton pauseButton;
    private final JButton cancelButton;
    private final JButton resumeButton;
    private final JButton tryAgainButton;
    private final JButton removeButton;


   
    @Resource
    private Icon cancelIcon;
    @Resource
    private Icon cancelIconPressed;
    @Resource
    private Icon cancelIconRollover;

    public DownloadButtonPanel() {
        this(null);
    }
	/**
	 * Create the panel
	 */
	public DownloadButtonPanel(ActionListener actionListener) {
	    super(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding, fill, aligny center"));
	    
	    GuiUtils.assignResources(this);		
	    
	    setOpaque(false);
	    
	    Font font = new DownloadRendererProperties().getFont();

		pauseButton = new HyperlinkButton(I18n.tr("Pause"));
		pauseButton.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
		pauseButton.addActionListener(actionListener);
		pauseButton.setToolTipText(I18n.tr("Pause download"));		
		pauseButton.setFont(font);

        cancelButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        cancelButton.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelButton.addActionListener(actionListener);
        cancelButton.setToolTipText(I18n.tr("Cancel download"));

        removeButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        removeButton.setActionCommand(DownloadActionHandler.REMOVE_COMMAND);
        removeButton.addActionListener(actionListener);
        removeButton.setToolTipText(I18n.tr("Remove download"));

        tryAgainButton = new HyperlinkButton(I18n.tr("Try again"));
        tryAgainButton.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        tryAgainButton.addActionListener(actionListener);
        tryAgainButton.setToolTipText(I18n.tr("Try again"));    
        tryAgainButton.setFont(font);

		resumeButton =  new HyperlinkButton(I18n.tr("Resume"));;
		resumeButton.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
		resumeButton.addActionListener(actionListener);
		resumeButton.setVisible(false);
        resumeButton.setToolTipText(I18n.tr("Resume download"));    
        resumeButton.setFont(font);
		
		
		add(resumeButton, "hidemode 3, push");
        add(pauseButton, "hidemode 3, push");
        add(tryAgainButton, "hidemode 1, push");		
	
		add(cancelButton, "hidemode 3, gapright 6");
		add(removeButton, "hidemode 3, gapright 6");
	}	
	
	public void addActionListener(ActionListener actionListener){
	    pauseButton.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);
        resumeButton.addActionListener(actionListener);
        removeButton.addActionListener(actionListener);
        tryAgainButton.addActionListener(actionListener);
	}

	

	public void updateButtons(DownloadState state) {
		pauseButton.setVisible(state == DownloadState.DOWNLOADING);  //used to be connecting also. keeping consistent with tray
		resumeButton.setVisible(state.isResumable());
        cancelButton.setVisible(state != DownloadState.DONE);
        removeButton.setVisible(state == DownloadState.DONE);
        tryAgainButton.setVisible(state == DownloadState.STALLED);
	}

}
