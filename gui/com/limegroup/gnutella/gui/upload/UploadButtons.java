package com.limegroup.gnutella.gui.upload;

import java.awt.event.ActionListener;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.ButtonRow;

/**
 * This class contains the buttons in the download window, allowing
 * classes in this package to enable or disable buttons at specific
 * indeces in the row.
 */
final class UploadButtons {


	/**
	 * The row of buttons for the donwload window.
	 */
	private ButtonRow BUTTONS;

	/**
	 * The index of the kill button in the button row.
	 */
	static final int KILL_BUTTON   = 0;
	
	/**
	 * The index of the browse button in the button row.
	 */
	static final int BROWSE_BUTTON = 1;

	/**
	 * The index of the clear button in the button row.
	 */
	static final int CLEAR_BUTTON  = 2;
	
	/**
	 * The constructor creates the row of buttons with their associated
	 * listeners.
	 */
	UploadButtons(final UploadMediator um) {
        String[] buttonLabelKeys = {
			I18nMarker.marktr("Kill Upload"),
			I18nMarker.marktr("Browse Host"),
			I18nMarker.marktr("Clear Inactive")
		};
        String[] buttonTipKeys = {
			I18nMarker.marktr("Stop Selected Uploads"),
			I18nMarker.marktr("View All Files on the Selected Computer"),
			I18nMarker.marktr("Remove Inactive Uploads")
		};

		ActionListener[] buttonListeners = {
			um.REMOVE_LISTENER,
			um.BROWSE_LISTENER,
			um.CLEAR_LISTENER
		};
		
		String[] iconNames =  {
		    "UPLOAD_REMOVE",
		    "UPLOAD_BROWSE_HOST",
		    "UPLOAD_CLEAR"
		};				

		BUTTONS = new ButtonRow(buttonLabelKeys,buttonTipKeys,buttonListeners,
		                        iconNames, ButtonRow.X_AXIS, ButtonRow.NO_GLUE);
	}
	
	ButtonRow getComponent() { return BUTTONS; }
	
}
