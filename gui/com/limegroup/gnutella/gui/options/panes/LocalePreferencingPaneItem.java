package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * option pane for turning on/off locale preferencing and
 * choosing the number of connections to preference
 */
public final class LocalePreferencingPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Locale Preferencing");
    
    public final static String LABEL = I18n.tr("You can choose to preference hosts using the same language as you.");
    
    private final String LOCALE_PREF_LABEL_CHECK_BOX = 
        I18nMarker.marktr("Turn preferencing on:");

    private final JCheckBox CHECK_BOX = new JCheckBox();


    public LocalePreferencingPaneItem() {
        super(TITLE, LABEL);

		LabeledComponent c = new LabeledComponent(LOCALE_PREF_LABEL_CHECK_BOX,
				CHECK_BOX, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);

		add(c.getComponent());
	}


    @Override
    public void initOptions() {
        CHECK_BOX.setSelected(ConnectionSettings.USE_LOCALE_PREF.getValue());
    }

    @Override
    public boolean applyOptions() throws IOException {
        ConnectionSettings.USE_LOCALE_PREF.setValue(CHECK_BOX.isSelected());
        return false;
    }
    
    public boolean isDirty() {
        return ConnectionSettings.USE_LOCALE_PREF.getValue() != CHECK_BOX.isSelected();
    }
}
