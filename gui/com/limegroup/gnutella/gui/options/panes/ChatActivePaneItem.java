package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.ChatSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * This class defines the panel in the options window that allows the user
 * to change the maximum number of dowloads to allow at any one time.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class ChatActivePaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Host Communication");
    
    public final static String LABEL = I18n.tr("You can turn chat on or off.");

	/**
	 * Constant for the key of the locale-specific <tt>String</tt> for the 
	 * chat enabled check box label in the options window.
	 */
	private final String CHECK_BOX_LABEL = 
		I18nMarker.marktr("Enable Chat:");

	/**
	 * Constant for the check box that specifies whether or not downloads 
	 * should be automatically cleared.
	 */
	private final JCheckBox CHECK_BOX = new JCheckBox();

	/**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *        superclass uses to generate locale-specific keys
	 */
	public ChatActivePaneItem() {
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
        CHECK_BOX.setSelected(ChatSettings.CHAT_ENABLED.getValue());
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
        ChatSettings.CHAT_ENABLED.setValue(CHECK_BOX.isSelected());
        return false;
	}
	
    public boolean isDirty() {
        return ChatSettings.CHAT_ENABLED.getValue() != CHECK_BOX.isSelected();
    }	
}
