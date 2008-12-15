package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * Allows the user to change whether or not partial files are shared.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class PartialFileSharingPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Partial Files");
    
    public final static String LABEL = I18n.tr("You can choose whether or not to automatically share partially downloaded files.");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * upload pane check box label in the options window.
	 */
	private final String CHECK_BOX_LABEL = 
		I18nMarker.marktr("Allow Partial Sharing:");

	/**
	 * Constant for the check box that specifies whether or not partial
	 * files should be shared.
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
	public PartialFileSharingPaneItem() {
	    super(TITLE, LABEL);
		
		LabeledComponent comp = new LabeledComponent(CHECK_BOX_LABEL,
				CHECK_BOX, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(comp.getComponent());
	}

	@Override
    public void initOptions() {
	    CHECK_BOX.setSelected(SharingSettings.ALLOW_PARTIAL_SHARING.getValue());
	}

	@Override
    public boolean applyOptions() throws IOException {
	    SharingSettings.ALLOW_PARTIAL_SHARING.setValue(CHECK_BOX.isSelected());
        return false;
	}
	
    public boolean isDirty() {
        return SharingSettings.ALLOW_PARTIAL_SHARING.getValue() != CHECK_BOX.isSelected();
    }
}

