package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;

public class ConfigureOptionsAction extends AbstractAction {

	/**
	 * The title of the 
	 */
	private String paneTitle;
	
	public ConfigureOptionsAction(String pane) {
		paneTitle = pane;
	}
	
	public ConfigureOptionsAction(String pane, String menu, String tooltip) {
		this(pane);
		putValue(Action.NAME, I18n.tr(menu));
		putValue(Action.SHORT_DESCRIPTION,
				I18n.tr(tooltip));
	}

	/**
	 * Launches LimeWire's options with the Sharing options pane selected.
	 */
	public void actionPerformed(ActionEvent e) {
		GUIMediator.instance().setOptionsVisible(true, paneTitle);
	}
}
