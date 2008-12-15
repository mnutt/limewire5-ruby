package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedWholeNumberField;
import com.limegroup.gnutella.gui.WholeNumberField;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;

/**
 * This class defines the panel in the options window that allows the user
 * to change time before incomplete files are purged.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class PurgeIncompletePaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Incomplete Files");
    
    public final static String LABEL = I18n.tr("You can automatically delete old incomplete download files.");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label on the component that allows to user to change the setting for
	 * this <tt>PaneItem</tt>.
	 */
	private final String OPTION_LABEL = I18nMarker.marktr("Days to Keep Incomplete Files:");


	/**
	 * Handle to the <tt>WholeNumberField</tt> where the user selects the
	 * maximum number of downloads.
	 */
	private WholeNumberField _purgeIncompleteField;

	/**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *        superclass uses to generate strings
	 */
	public PurgeIncompletePaneItem() {
	    super(TITLE, LABEL);

		_purgeIncompleteField = new SizedWholeNumberField(0, 3,
				SizePolicy.RESTRICT_BOTH);
		LabeledComponent comp = new LabeledComponent(OPTION_LABEL,
				_purgeIncompleteField, LabeledComponent.LEFT_GLUE,
				LabeledComponent.LEFT);

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
        _purgeIncompleteField.setValue(SharingSettings.INCOMPLETE_PURGE_TIME.getValue());
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
        SharingSettings.INCOMPLETE_PURGE_TIME.setValue(_purgeIncompleteField.getValue());
        return false;
	}
	
	public boolean isDirty() {
        return SharingSettings.INCOMPLETE_PURGE_TIME.getValue() != _purgeIncompleteField.getValue();
    }
}
