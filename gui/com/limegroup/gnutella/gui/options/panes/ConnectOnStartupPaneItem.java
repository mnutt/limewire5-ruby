package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * This class gives the user the option of whether or not to automatically
 * connect to the network when the program first starts.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class ConnectOnStartupPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Connect on Startup");
    
    public final static String LABEL = I18n.tr("You can choose whether or not to automatically connect to the network when the application starts up.");

	/**
	 * Constant for the key of the locale-specific <tt>String</tt> for the 
	 * check box that allows the user to connect automatically or not
	 */
	private final String CHECK_BOX_LABEL = 
		I18nMarker.marktr("Connect on Startup:");

	/**
	 * Constant for the check box that determines whether or not 
	 * to connect automatically on startup
	 */
	private final JCheckBox CHECK_BOX = new JCheckBox();

	public ConnectOnStartupPaneItem() {
	    super(TITLE, LABEL);

		LabeledComponent comp = new LabeledComponent(CHECK_BOX_LABEL,
				CHECK_BOX, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(comp.getComponent());
	}

	@Override
    public void initOptions() {
        CHECK_BOX.setSelected(ConnectionSettings.CONNECT_ON_STARTUP.getValue());
	}

	@Override
    public boolean applyOptions() throws IOException {
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(CHECK_BOX.isSelected());
        return false;
	}
	
	public boolean isDirty() {
	    return ConnectionSettings.CONNECT_ON_STARTUP.getValue() != CHECK_BOX.isSelected();
    }
}
