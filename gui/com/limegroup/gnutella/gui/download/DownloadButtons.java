package com.limegroup.gnutella.gui.download;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.I18n;

/**
 * This class contains the buttons in the download window, allowing
 * classes in this package to enable or disable buttons at specific
 * indeces in the row.
 */
final class DownloadButtons {
    
    /**
     * The JPanel of the queued up/down buttons.
     */
    private JPanel QUEUE_PANEL;

	/**
	 * The row of buttons for the donwload window.
	 */
	private ButtonRow BUTTONS;
	
	/**
	 * The index of the up button.
	 */
	static final int UP_BUTTON = 0;
	
	/**
	 * The index of the down button.
	 */
	static final int DOWN_BUTTON = 1;


  	DownloadButtons(final DownloadMediator dm) {
    
		BUTTONS = new ButtonRow(dm.getActions(),
		                        ButtonRow.X_AXIS, ButtonRow.NO_GLUE);
		
        JButton up = BUTTONS.getButtonAtIndex(UP_BUTTON);
        JButton down = BUTTONS.getButtonAtIndex(DOWN_BUTTON);
        BUTTONS.remove(up); BUTTONS.remove(down);
        JPanel panel = new BoxPanel(BoxPanel.X_AXIS);
        panel.add(up);
        panel.add(new JLabel(I18n.tr("Queue")));
        panel.add(down);
        BUTTONS.add(panel, 0);
        QUEUE_PANEL = panel;
	}
	
	ButtonRow getComponent() { return BUTTONS; }

    /**
     * Sets the queue panel either visible or invisible.
     */
    void setQueuePanelVisible(boolean visible) {
        QUEUE_PANEL.setVisible(visible);
    }
}
