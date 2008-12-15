package com.limegroup.gnutella.gui.options.panes;

import java.awt.Dimension;
import java.io.IOException;

import javax.swing.JComboBox;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.GUIConstants;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * This class defines the panel in the options window that allows the user
 * to change their connection speed.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class SpeedPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Connection Speed");
    
    public final static String LABEL = I18n.tr("You can specify your connection speed so that LimeWire can optimize performance.");

	/**
	 * Array of constant names for connection speeds used throughout the gui.
	 */
    private final String[] SPEED_MENU_ITEMS = { GUIConstants.MODEM_SPEED, GUIConstants.CABLE_SPEED };

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label on the component that allows to user to change the setting for
	 * this <tt>PaneItem</tt>.
	 */
	private final String OPTION_LABEL = I18nMarker.marktr("Speed:");


	/**
	 * Handle to the <tt>JComboBox</tt> where the user selects the
	 * their connection speed.
	 */
	private final JComboBox SPEED_BOX =  
		new JComboBox(SPEED_MENU_ITEMS);

	/**
	 * Handle to the speed to allow rolling back changes.
	 */
    private String _speed;

	/**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key
	 *            the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate strings
	 */
	public SpeedPaneItem() {
	    super(TITLE, LABEL);
	    
		SPEED_BOX.setMaximumSize(new Dimension(10, 25));

		LabeledComponent comp = new LabeledComponent(OPTION_LABEL, SPEED_BOX,
				LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);

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
        _speed = getSpeedString(ConnectionSettings.CONNECTION_SPEED.getValue());
        SPEED_BOX.setSelectedItem(_speed);
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
        final String speed = (String)SPEED_BOX.getSelectedItem();
        if(speed != null && !speed.equals(_speed)) {
            int speedInt = getSpeedInt(speed);
            
            if (speedInt < SpeedConstants.MIN_SPEED_INT || SpeedConstants.MAX_SPEED_INT < speedInt) {
                throw (new IllegalArgumentException());
            }
                
            ConnectionSettings.CONNECTION_SPEED.setValue(speedInt);
            _speed = speed;
        }
        return false;
	}
	
	public boolean isDirty() {
	    String speed = (String)SPEED_BOX.getSelectedItem();
	    return speed != null &&
	           ConnectionSettings.CONNECTION_SPEED.getValue() != getSpeedInt(speed);
    }

    private String getSpeedString(int i) {
        String speed;
        if (i == SpeedConstants.MODEM_SPEED_INT) {
            speed = GUIConstants.MODEM_SPEED;
        } else if (i == SpeedConstants.CABLE_SPEED_INT) {
            speed = GUIConstants.CABLE_SPEED;
        } else {
            speed = GUIConstants.CABLE_SPEED;
        }

        return speed;
    }

    private int getSpeedInt(String s) {
        int i;
        if (s.equals(GUIConstants.MODEM_SPEED)) {
            i = SpeedConstants.MODEM_SPEED_INT;
        } else if (s.equals(GUIConstants.CABLE_SPEED)) {
            i = SpeedConstants.CABLE_SPEED_INT;
        } else {
            i = SpeedConstants.CABLE_SPEED_INT;
        }
        return i;
    }
}


