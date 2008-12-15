package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import org.limewire.core.settings.NetworkSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedWholeNumberField;
import com.limegroup.gnutella.gui.WholeNumberField;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;

/**
 * This class defines the panel in the options window that allows the user
 * to change the listening port.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class PortPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Listening Port");
    
    public final static String LABEL = I18n.tr("You can set the local network port that listens for incoming connections. This port can be freely changed in case of conflict with another service on your system, or if a specific port number is required by the configuration of your firewall to direct incoming connections to your host.");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label on the component that allows to user to change the setting for
	 * this <tt>PaneItem</tt>.
	 */
	private final String OPTION_LABEL = I18nMarker.marktr("Listen on Port:");


	/**
	 * Handle to the <tt>WholeNumberField</tt> where the user selects the
	 * time to live for outgoing searches.
	 */
	private WholeNumberField _portField = new SizedWholeNumberField(0, 5, SizePolicy.RESTRICT_BOTH);

	/**
	 * The stored value to allow rolling back changes.
	 */
	private int _port;
	
	private final NetworkManager networkManager;

	/**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *        superclass uses to generate locale-specific keys
	 */
	public PortPaneItem() {
	    super(TITLE, LABEL);
		LabeledComponent comp = new LabeledComponent(OPTION_LABEL, _portField,
				LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(comp.getComponent());
		this.networkManager = GuiCoreMediator.getNetworkManager();
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.
	 * <p>
	 * 
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the
	 * window is shown.
	 */
	@Override
    public void initOptions() {
		_port = NetworkSettings.PORT.getValue();
		_portField.setValue(_port);
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
		int port = _portField.getValue();
		if(port == _port) return false;
		try {
            NetworkSettings.PORT.setValue(port);
			networkManager.setListeningPort(port);
			_port = port;
			networkManager.portChanged();
		} catch(IOException ioe) {
			GUIMediator.showError(I18n.tr("Port not available. Please select a different port."));
			NetworkSettings.PORT.setValue(_port);
			_portField.setValue(_port);
			throw new IOException("port not available");
		} catch(IllegalArgumentException iae) {
			GUIMediator.showError(I18n.tr("Please enter a port between 1 and 65535."));
			_portField.setValue(_port);
			throw new IOException("invalid port");
		}
        return false;
	}
	
	public boolean isDirty() {
	    return NetworkSettings.PORT.getValue() != _portField.getValue();
    }
}


