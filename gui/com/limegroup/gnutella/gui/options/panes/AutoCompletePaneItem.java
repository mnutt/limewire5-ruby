package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.UISettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * Class defineing the options panel that allows the user to enable
 * or disable autocompletion of text fields.
 */
public final class AutoCompletePaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Text Autocompletion");
    
    public final static String LABEL = I18n.tr("You can enable or disable autocompletion of text fields.");

	/**
	 * Constant for the key of the locale-specific <tt>String</tt> for the 
	 * autocompletion enabled check box label..
	 */
    private final String AUTOCOMPLETE_LABEL = 
        I18nMarker.marktr("Enable Autocompletion of Text Fields:");
    
    /**
	 * Constant for the check box that specifies whether to enable or 
	 * disable autocompletion
	 */
    private final JCheckBox CHECK_BOX = new JCheckBox();
    
    /**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *        superclass uses to generate locale-specific keys
	 */
	public AutoCompletePaneItem() {
		super(TITLE, LABEL);
		
		LabeledComponent c = new LabeledComponent(AUTOCOMPLETE_LABEL,
				CHECK_BOX, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(c.getComponent());
	}

    /**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
    @Override
    public void initOptions() {
        CHECK_BOX.setSelected(UISettings.AUTOCOMPLETE_ENABLED.getValue());
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
        UISettings.AUTOCOMPLETE_ENABLED.setValue(CHECK_BOX.isSelected());
        return false;
    }
    
    public boolean isDirty() {
        return UISettings.AUTOCOMPLETE_ENABLED.getValue() != CHECK_BOX.isSelected();   
    }    
}


