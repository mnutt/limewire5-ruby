package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JTextField;

import org.limewire.core.settings.URLHandlerSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedTextField;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;

/**
 * This class defines the panel in the options
 * window that allows the user to select the
 * default audio behavior.
 */
public class AudioPlayerPaneItem extends AbstractPaneItem { 
    
    public final static String TITLE = I18n.tr("Audio Options");
    
    public final static String LABEL = I18n.tr("You can choose which audio player to use.");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label on the component that allows to user to change the setting for
	 * this <tt>PaneItem</tt>.
	 */
	private final String OPTION_LABEL = I18nMarker.marktr("Audio Player");
    
    /** 
     * Handle to the <tt>JTextField</tt> that displays the player name
     */    
    private JTextField _playerField;
    
    /**
	 * Creates new AudioPlayerOptionsPaneItem
	 * 
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *        superclass uses to generate locale-specific keys
	 */
	public AudioPlayerPaneItem() {
		super(TITLE, LABEL);
		
		_playerField = new SizedTextField(25, SizePolicy.RESTRICT_HEIGHT);
		LabeledComponent comp = new LabeledComponent(OPTION_LABEL, _playerField);
		add(comp.getComponent());
	}
    
    /**
     * Applies the options currently set in this <tt>PaneItem</tt>.
     *
     * @throws IOException if the options could not be fully applied
     */
    @Override
    public boolean applyOptions() throws IOException {
        URLHandlerSettings.AUDIO_PLAYER.setValue(_playerField.getText());
        return false;
    }
    
    public boolean isDirty() {
        return !URLHandlerSettings.AUDIO_PLAYER.getValue().equals(_playerField.getText());
    }

    /**
     * Sets the options for the fields in this <tt>PaneItem</tt> when the
     * window is shown.
     */
    @Override
    public void initOptions() {
        _playerField.setText(URLHandlerSettings.AUDIO_PLAYER.getValue());
    }
}    
