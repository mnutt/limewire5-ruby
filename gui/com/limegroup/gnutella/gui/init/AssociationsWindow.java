package com.limegroup.gnutella.gui.init;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.limewire.core.settings.QuestionsHandler;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.DialogOption;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.Line;
import com.limegroup.gnutella.gui.shell.LimeAssociationOption;
import com.limegroup.gnutella.gui.shell.LimeAssociations;

final class AssociationsWindow extends SetupWindow {
	/** a mapping of checkboxes to associations */
	private Map<JCheckBox, LimeAssociationOption> associations =
		new HashMap<JCheckBox, LimeAssociationOption>();
	
	/** Check box to check associations on startup. */
	private JRadioButton always, never, ask;
	
	AssociationsWindow() {
		super(I18nMarker.marktr("File & Protocol Associations"), I18nMarker.marktr("What type of resources should LimeWire open?"));
	}
	
    protected void createPageContent() {

        // Similar to the options window, except that the radio buttons default to
        // "always" and all supported associations are allowed.

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        gbc.insets = new Insets(1, 4, 1, 0);
        for (LimeAssociationOption option : LimeAssociations.getSupportedAssociations()) {
            JCheckBox box = new JCheckBox(option.getDescription());
            box.setSelected(true);
            associations.put(box, option);
            panel.add(box, gbc);
        }
        
        gbc.insets = new Insets(9, 3, 9, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new Line(), gbc);
        
        gbc.fill = GridBagConstraints.NONE;

        gbc.insets = new Insets(1, 0, 2, 0);
        panel.add(new JLabel(I18n.tr("What should LimeWire do with the selected associations on startup?")), gbc);
        int value = QuestionsHandler.GRAB_ASSOCIATIONS.getValue();
        always = new JRadioButton(I18n.tr("Always take the selected associations."),
                DialogOption.parseInt(value) == DialogOption.YES);
        never = new JRadioButton(I18n.tr("Ignore all missing associations."),
                DialogOption.parseInt(value) == DialogOption.NO);
        ask = new JRadioButton(I18n.tr("Ask me what to do when an association is missing."),
                DialogOption.parseInt(value) != DialogOption.YES && DialogOption.parseInt(value) != DialogOption.NO);
        ButtonGroup grabGroup = new ButtonGroup();
        grabGroup.add(always);
        grabGroup.add(ask);
        grabGroup.add(never);
        always.setSelected(true);

        gbc.insets = new Insets(1, 4, 1, 0);
        panel.add(always, gbc);
        panel.add(ask, gbc);
        panel.add(never, gbc);

        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridheight = GridBagConstraints.REMAINDER;
        panel.add(Box.createGlue(), gbc);

        setSetupComponent(panel);
    }
	
	@Override
    public void applySettings(boolean loadCoreComponents) {
		for (Map.Entry<JCheckBox, LimeAssociationOption>entry : associations.entrySet()) {
			LimeAssociationOption option = entry.getValue();
//			if (entry.getKey().isSelected()) {
				option.setAllowed(true);
				option.setEnabled(true);
//			} else {
//				// only disallow options that were previously enabled.
//				if (option.isEnabled())
//					option.setAllowed(false);
//				option.setEnabled(false);
//			}
		}
		
		DialogOption value = DialogOption.INVALID;
		if (always.isSelected())
			value = DialogOption.YES;
		else if (never.isSelected())
			value = DialogOption.NO;
		QuestionsHandler.GRAB_ASSOCIATIONS.setValue(value.toInt());
	}
}
