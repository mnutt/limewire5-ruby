package com.limegroup.gnutella.gui.playlist;

import java.awt.event.ActionListener;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.ButtonRow;

/**
 * The buttons of the playlist.
 */
final class PlaylistButtons {


	/**
	 * The row of buttons for the donwload window.
	 */
	private ButtonRow BUTTONS;

	/**
	 * Index of the load button.
	 */
	static final int LOAD_BUTTON  = 0;

	/**
	 * Index of the save button.
	 */
	static final int SAVE_BUTTON = 1;
	
	/**
	 * Index of the delete button.
	 */
	static final int REMOVE_BUTTON = 2;
    
    /**
     * Index of the clear button.
     */
    static final int CLEAR_BUTTON = 3;
	
	/**
	 * The constructor creates the row of buttons with their associated
	 * listeners.
	 */
	PlaylistButtons(final PlaylistMediator pm) {

  		String[] buttonLabelKeys = {
			I18nMarker.marktr("Open..."),
            I18nMarker.marktr("Save As..."),
            I18nMarker.marktr("Remove"),
            I18nMarker.marktr("Clear")
		};
		String[] toolTipKeys = {
			I18nMarker.marktr("Open an Existing Playlist"),
            I18nMarker.marktr("Save the Current Playlist to a File"),
            I18nMarker.marktr("Remove Selected File from Playlist"),
            I18nMarker.marktr("Removes all Files from Playlist")
		};		
		ActionListener[] listeners = {
            pm.LOAD_LISTENER,
            pm.SAVE_LISTENER,
            pm.REMOVE_LISTENER,
            pm.CLEAR_LISTENER
		};
		String[] iconNames = {
		    "PLAYLIST_LOAD",
		    "PLAYLIST_SAVE",
		    "PLAYLIST_DELETE",
            "PLAYLIST_CLEAR"
		};

		BUTTONS = new ButtonRow(buttonLabelKeys, toolTipKeys, listeners,
		                        iconNames, ButtonRow.X_AXIS, ButtonRow.NO_GLUE);
	}
	
	ButtonRow getComponent() { return BUTTONS; }
	
}
