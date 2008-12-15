package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedWholeNumberField;
import com.limegroup.gnutella.gui.WholeNumberField;

/**
 * This class defines the panel in the options window that allows the user
 * to change the maximum number of dowloads to allow at any one time.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class MaximumDownloadsPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Maximum Downloads");
    
    public final static String LABEL = I18n.tr("You can set the maximum number of simultaneous downloads.");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label on the component that allows to user to change the setting for
	 * this <tt>PaneItem</tt>.
	 */
	private final String OPTION_LABEL = I18nMarker.marktr("Maximum Downloads:");


	/**
	 * Handle to the <tt>WholeNumberField</tt> where the user selects the
	 * maximum number of downloads.
	 */
	private WholeNumberField _maxDownloadsField;


	/**
	 * The stored value to allow rolling back changes.
	 */
    private int _maximumDownloads;

	/**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key
	 *            the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate strings
	 */
	public MaximumDownloadsPaneItem() {
	    super(TITLE, LABEL);

		_maxDownloadsField = new SizedWholeNumberField();
		LabeledComponent comp = new LabeledComponent(OPTION_LABEL,
				_maxDownloadsField, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);

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
		_maximumDownloads = DownloadSettings.MAX_SIM_DOWNLOAD.getValue();
        _maxDownloadsField.setValue(_maximumDownloads);
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
        final int maxDownloads  = _maxDownloadsField.getValue();
        if(maxDownloads != _maximumDownloads) {
            DownloadSettings.MAX_SIM_DOWNLOAD.setValue(maxDownloads);
            _maximumDownloads = maxDownloads;
        }
        return false;
	}
	
    public boolean isDirty() {
        return DownloadSettings.MAX_SIM_DOWNLOAD.getValue() != _maxDownloadsField.getValue();
    }
}
