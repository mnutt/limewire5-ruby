package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.PlayerSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * This class defines the panel in the options window that allows the user
 * to change the default mp3 player used by limewire.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class PlayerPreferencePaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Player Options");
    
    public final static String LABEL = I18n.tr("You can choose to play media with LimeWire or your system\'s default player. The media player and playlist will appear only if this option is set. A restart will be required when enabling or disabling the media controls.");

	/**
	 * Constant for the key of the locale-specific <tt>String</tt> for the 
	 * PLAYER enabled check box label in the options window.
	 */
	private final String CHECK_BOX_LABEL = 
		I18nMarker.marktr("Use LimeWire Media Player:");

	/**
	 * Constant for the check box that specifies whether or not downloads 
	 * should be automatically cleared.
	 */
	private final JCheckBox CHECK_BOX = new JCheckBox();

	/**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key
	 *            the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate locale-specific keys
	 */
	public PlayerPreferencePaneItem() {
	    super(TITLE, LABEL);
	    
		LabeledComponent comp = new LabeledComponent(CHECK_BOX_LABEL,
				CHECK_BOX, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(comp.getComponent());
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@Override
    public void initOptions() {
        CHECK_BOX.setSelected(PlayerSettings.PLAYER_ENABLED.getValue());
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Applies the options currently set in this window, displaying an
	 * error message to the user if a setting could not be applied.
	 *
	 * @throws IOException if the options could not be applied for some reason
	 */
	@Override
    public boolean applyOptions() throws IOException {
		GUIMediator.instance().setPlayerEnabled(CHECK_BOX.isSelected());
        return false;
	}

    public boolean isDirty() {
        return PlayerSettings.PLAYER_ENABLED.getValue() != CHECK_BOX.isSelected();
    }
}
