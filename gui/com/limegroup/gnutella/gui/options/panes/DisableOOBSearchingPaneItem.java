package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.SearchSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * This class gives the user the option of whether or not the user wants to use
 * OOB queries (if even possible).
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class DisableOOBSearchingPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("OOB Searching");
    
    public final static String LABEL = I18n.tr("Out-Of-Band Searching results in faster result delivery to your client, though some internet connections may not work well with this feature.");

	/**
	 * Constant for the key of the locale-specific <tt>String</tt> for the 
	 * check box that allows the user to connect automatically or not
	 */
	private final String CHECK_BOX_LABEL = 
		I18nMarker.marktr("Enable OOB Searching:");

	/**
	 * Constant for the check box that determines whether or not 
	 * to send OOB searches.
	 */
	private final JCheckBox CHECK_BOX = new JCheckBox();

	public DisableOOBSearchingPaneItem() {
	    super(TITLE, LABEL);

		LabeledComponent comp = new LabeledComponent(CHECK_BOX_LABEL,
				CHECK_BOX, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(comp.getComponent());
	}

	@Override
    public void initOptions() {
        CHECK_BOX.setSelected(SearchSettings.OOB_ENABLED.getValue());
	}

	@Override
    public boolean applyOptions() throws IOException {
		SearchSettings.OOB_ENABLED.setValue(CHECK_BOX.isSelected());
        return false;
	}

    public boolean isDirty() {
        return SearchSettings.OOB_ENABLED.getValue() != CHECK_BOX.isSelected();
    }
}
