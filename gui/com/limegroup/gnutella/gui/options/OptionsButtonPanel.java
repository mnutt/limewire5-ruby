package com.limegroup.gnutella.gui.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JPanel;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.ButtonRow;

/**
 * This class contains the <tt>ButtonRow</tt> instance for the options 
 * window.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class OptionsButtonPanel {

	/**
	 * Handle to the enclosed <tt>ButtonRow</tt> instance.
	 */
	private ButtonRow _buttonRow;
	
	/**
	 * Handle to the other button row.
	 */
	private ButtonRow _revertRow;

	/**
	 * The constructor creates the <tt>ButtonRow</tt>.
	 */
	OptionsButtonPanel() {
        String[] buttonLabelKeys = {
			I18nMarker.marktr("OK"),
			I18nMarker.marktr("Cancel"),
			I18nMarker.marktr("Apply")
		};

        String[] toolTipKeys = {
			I18nMarker.marktr("Apply Operation"),
			I18nMarker.marktr("Cancel Operation"),
			I18nMarker.marktr("Apply Operation"),
		};
        ActionListener[] listeners = {
			new OKListener(), new CancelListener(), new ApplyListener()
		};
		_buttonRow= new ButtonRow(buttonLabelKeys,toolTipKeys,listeners,
								  ButtonRow.X_AXIS,ButtonRow.LEFT_GLUE);
								  
        buttonLabelKeys = new String[] { I18nMarker.marktr("Restore Defaults") };
        toolTipKeys = new String[] { I18nMarker.marktr("Revert All Settings to the Factory Defaults") };
        listeners = new ActionListener[] { new RevertListener() };
        _revertRow = new ButtonRow(buttonLabelKeys, toolTipKeys, listeners,
                                   ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
            
			
	}

	/**
	 * Returns the <tt>Component</tt> that contains the <tt>ButtonRow</tt>.
	 */
	Component getComponent() {
	    JPanel box = new BoxPanel(BoxPanel.X_AXIS);
	    box.add(Box.createHorizontalStrut(50));
	    box.add(_revertRow);
	    //box.add(Box.createHorizontalGlue());
	    _buttonRow.setAlignmentX(1f);
	    box.add(_buttonRow);
	    //box.add(Box.createHorizontalGlue());
	    return box;
	}
	
	/**
	 * Listener for the revert to default option.
	 * Reverts all options to their factory defaults.
	 */
	private class RevertListener implements ActionListener {
	    public void actionPerformed(ActionEvent e) {
	        OptionsMediator.instance().revertOptions();
	        OptionsMediator.instance().setOptionsVisible(false);
        }
    }

    /** 
	 * The listener for the ok button.  Applies the current options and 
	 * makes the window not visible.
	 */
    private class OKListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
			// close the window only if the new settings 
			// work correctly, as the user may need to
			// change the settings before closing.
			try {
				OptionsMediator.instance().applyOptions();
				OptionsMediator.instance().setOptionsVisible(false);
			} catch(IOException ioe) {
				// nothing we should do here.  a message should
				// have been displayed to the user with more information
			}
        }
    }

    /** 
	 * The listener for the cancel button.
	 */
    private class CancelListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            OptionsMediator.instance().setOptionsVisible(false);
        }
    }

    /** 
	 * The listener for the apply button.  Applies the current settings.
	 */
    private class ApplyListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
			try {
				OptionsMediator.instance().applyOptions();
			} catch(IOException ioe) {
				// nothing we should do here.  a message should
				// have been displayed to the user with more information
            }
        }
    }
}
