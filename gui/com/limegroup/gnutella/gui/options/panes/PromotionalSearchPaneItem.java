package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

public class PromotionalSearchPaneItem extends AbstractPaneItem {
 
    public final static String TITLE = I18n.tr("LimeLinks");
    
    public final static String LABEL = I18n.tr("You can disable LimeLinks from appearing in search results.");

    /**
     * Constant for the key of the locale-specific <code>String</code> for the 
     * label on the component that allows to user to change the setting for
     * this <tt>PaneItem</tt>.
     */
    private final String OPTION_LABEL = I18nMarker.marktr("Disable LimeLinks:");

    private final JCheckBox disableResultsCheckBox = new JCheckBox();

    public PromotionalSearchPaneItem() {
        super(TITLE, LABEL, "http://www.limewire.com/learnMore/limelinks.php");

        LabeledComponent comp = new LabeledComponent(OPTION_LABEL,
                disableResultsCheckBox, LabeledComponent.LEFT_GLUE,
                LabeledComponent.LEFT);

        add(comp.getComponent());
    }

    /**
     * Sets the options for the fields in this <tt>PaneItem</tt> when the 
     * window is shown.
     */
    @Override
    public void initOptions() {
//        disableResultsCheckBox.setSelected(SearchSettings.DISABLE_PROMOTIONAL_RESULTS.getValue());
    }

    @Override
    public boolean applyOptions() throws IOException {
//        SearchSettings.DISABLE_PROMOTIONAL_RESULTS.setValue(disableResultsCheckBox.isSelected());
        return false;
    }
    
    public boolean isDirty() {
        return false;
//        return SearchSettings.DISABLE_PROMOTIONAL_RESULTS.getValue() != disableResultsCheckBox.isSelected();
    }
}
