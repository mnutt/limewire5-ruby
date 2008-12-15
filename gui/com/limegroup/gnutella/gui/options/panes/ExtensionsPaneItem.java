package com.limegroup.gnutella.gui.options.panes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JTextField;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedTextField;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;

/**
 * This class defines the panel in the options window that allows the user
 * to change the directory for saving files.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
@SuppressWarnings("deprecation")
public final class ExtensionsPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Shared Extensions");
    
    public final static String LABEL = I18n.tr("You can choose which file extensions you would like to share.");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label on the component that allows to user to change the setting for
	 * this <tt>PaneItem</tt>.
	 */
	private final String OPTION_LABEL = I18nMarker.marktr("Extensions:");

	/**
	 * Handle to the <tt>JTextField</tt> that displays the save directory.
	 */
	private JTextField _extField;

	/**
	 * The stored value to allow rolling back changes.
	 */
	private String _extensions;

	/**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *        superclass uses to generate locale-specific keys
	 */
	public ExtensionsPaneItem() {
	    super(TITLE, LABEL);
	    
		_extField = new SizedTextField(25, SizePolicy.RESTRICT_HEIGHT);
		LabeledComponent comp = new LabeledComponent(OPTION_LABEL, _extField,
				LabeledComponent.NO_GLUE, LabeledComponent.TOP_LEFT);

		String[] labelKeys = { I18nMarker.marktr("Use Defaults"), };

		String[] toolTipKeys = { I18nMarker.marktr("Share the Default File Extensions"), };

		ActionListener[] listeners = { new DefaultExtensionsListener() };

		ButtonRow br = new ButtonRow(labelKeys, toolTipKeys, listeners,
				ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);

		add(comp.getComponent());
		add(getVerticalSeparator());
		add(br);
	}

	/**
	 * This class handles mouse clicks on the "Use Defaults" button of the
	 * extensions panel, setting the current extensions back to the defaults..
	 */
	private class DefaultExtensionsListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
//		    OldLibrarySettings.EXTENSIONS_TO_SHARE.revertToDefault();
//    	    _extField.setText(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue());
		}
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@Override
    public void initOptions() {
//        _extensions = OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue();
//        _extField.setText(_extensions);
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
        final String ext = _extField.getText();

        // Handle a change to the shared directories or list of extensions.
        // The loadSettings method is non-blocking, so threads are needed.
        if(!ext.equals(_extensions)) {
//            OldLibrarySettings.EXTENSIONS_TO_SHARE.setValue(_extField.getText());
//            GuiCoreMediator.getFileManager().loadSettings();
            _extensions = _extField.getText();
        }
        
        return false;
    }		    
    
    public boolean isDirty() {
        return false; //!OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().equals(_extField.getText());
    }
}
