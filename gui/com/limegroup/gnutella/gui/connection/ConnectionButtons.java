package com.limegroup.gnutella.gui.connection;

import java.awt.event.ActionListener;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.ButtonRow;

/**
 * This class contains the buttons in the connection window, allowing
 * classes in this package to enable or disable buttons at specific
 * indeces in the row.
 */
final class ConnectionButtons {


	/**
	 * The row of buttons for the donwload window.
	 */
	private ButtonRow BUTTONS;

	/**
	 * The index of the add button in the button row.
	 */
	static final int ADD_BUTTON = 0;

	/**
	 * The index of the remove button in the button row.
	 */
	static final int REMOVE_BUTTON = 1;

    /**
     * The index of the browse host button in the button row.
     */
    static final int BROWSE_HOST_BUTTON = 2;

	/**
	 * The constructor creates the row of buttons with their associated
	 * listeners.
	 */
	ConnectionButtons(final ConnectionMediator cm) {
        String[] buttonLabelKeys = {
			I18nMarker.marktr("Add..."),
			I18nMarker.marktr("Remove"),
			I18nMarker.marktr("Browse Host")
		};
        String[] buttonTipKeys = {
			I18nMarker.marktr("Create a New Outgoing Connection"),
			I18nMarker.marktr("Remove the Selected Connections"),
			I18nMarker.marktr("View All Files on the Selected Computer")
		};

		ActionListener[] buttonListeners = {
			cm.ADD_LISTENER,
			cm.REMOVE_LISTENER,
			cm.BROWSE_HOST_LISTENER
		};
		
		String[] buttonNames = {
		    "CONNECTION_ADD",
		    "CONNECTION_REMOVE",
		    "CONNECTION_BROWSE_HOST"
		};

		BUTTONS = new ButtonRow(buttonLabelKeys, buttonTipKeys, buttonListeners,
		                        buttonNames, ButtonRow.X_AXIS, ButtonRow.NO_GLUE);
	}
	
	ButtonRow getComponent() { return BUTTONS; }
	
}
