package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.ContentSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * Constructs the content filter dialog options pane item.
 */
public class ContentFilterPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Content Filters");
    
    public final static String LABEL = I18n.tr("You can tell LimeWire to filter files that copyright owners request not be shared. By enabling filtering, you are instructing LimeWire to confirm all files you download or share with a list of removed content.");

    private final JCheckBox CHECK_BOX = new JCheckBox();

    public ContentFilterPaneItem() {
		super(TITLE, LABEL, ContentSettings.LEARN_MORE_URL);
		
		LabeledComponent comp = new LabeledComponent(
				I18nMarker.marktr("Enable Content Filters"), CHECK_BOX,
				LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(comp.getComponent());
	}

    @Override
    public void initOptions() {
        CHECK_BOX.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue());
    }

    @Override
    public boolean applyOptions() throws IOException {
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(CHECK_BOX.isSelected());
        return false;
    }
    
    public boolean isDirty() {
        return ContentSettings.USER_WANTS_MANAGEMENTS.getValue() != CHECK_BOX.isSelected();
    }
}
