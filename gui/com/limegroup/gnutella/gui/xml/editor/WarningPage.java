package com.limegroup.gnutella.gui.xml.editor;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.TitledPaddedPanel;
import com.limegroup.gnutella.gui.BoxPanel;


import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;

import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.Dimension;

/**
 * Creative Commons publishing warning wizard page for
 * creating a new license or editing an existing license
 */
class WarningPage extends CCPublishWizardPage {

    private final String WARNING_MESSAGE_CREATE = I18n
            .tr("I understand that to publish a file, I must either own its copyrights or be authorized to publish them under a Creative Commons license.");

    private final String WARNING_MESSAGE_MODIFY = I18n
            .tr("This file already has a license. If you want to modify it, click the checkbox to attest that you either own its copyrights or are authorized to publish them under a Creative Commons license.");

    private final JCheckBox WARNING_CHECKBOX = new JCheckBox();

    private final TitledPaddedPanel MODE_SELECTION_PANEL = new TitledPaddedPanel();

    final JRadioButton MODIFY_LICENSE = new JRadioButton(I18n
            .tr("Modify the license of this file"));

    final JRadioButton REMOVE_LICENSE = new JRadioButton(I18n
            .tr("Permanently remove the license from this file"));

    public WarningPage() {
        super("warningPage", I18nMarker.marktr("Publish License"), I18nMarker
                .marktr("This tool helps you publish audio under a Creative Commons license."));

        setURL(SharingSettings.CREATIVE_COMMONS_INTRO_URL.getValue(),
                I18nMarker.marktr("How does it work?"));
    }

    @Override
    protected void createPageContent(JPanel parent) {
        parent.setLayout(new BoxLayout(parent, BoxLayout.Y_AXIS));

        WARNING_CHECKBOX.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateActions();
            }
        });
        // XXX make sure the dialog does not get too wide
        WARNING_CHECKBOX.setPreferredSize(new Dimension(540, -1));
        parent.add(WARNING_CHECKBOX);

        // make sure the panel expands horizontally
        MODE_SELECTION_PANEL.add(Box.createHorizontalGlue());
        parent.add(MODE_SELECTION_PANEL);

        MODIFY_LICENSE.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateActions();
            }
        });
        REMOVE_LICENSE.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateActions();
            }
        });
        ButtonGroup bg = new ButtonGroup();
        bg.add(MODIFY_LICENSE);
        bg.add(REMOVE_LICENSE);

        BoxPanel buttonPanel = new BoxPanel();
        buttonPanel.add(MODIFY_LICENSE);
        buttonPanel.add(REMOVE_LICENSE);
        MODE_SELECTION_PANEL.add(buttonPanel);

        // set defaults
        MODIFY_LICENSE.setSelected(true);
        updateActions();
        setLicenseAvailable(true);
    }

    public void setLicenseAvailable(boolean available) {
        if (available) {
            WARNING_CHECKBOX.setText("<html>" + WARNING_MESSAGE_MODIFY
                    + "</html>");
            MODE_SELECTION_PANEL.setVisible(true);
        } else {
            WARNING_CHECKBOX.setText("<html>" + WARNING_MESSAGE_CREATE
                    + "</html>");
            MODIFY_LICENSE.setSelected(true);
            MODE_SELECTION_PANEL.setVisible(false);
        }
    }

    @Override
    public boolean canFlipToNextPage() {
        return MODIFY_LICENSE.isSelected();
    }

    @Override
    public boolean isPageComplete() {
        return WARNING_CHECKBOX.isSelected();
    }

    private void updateActions() {
        MODIFY_LICENSE.setEnabled(WARNING_CHECKBOX.isSelected());
        REMOVE_LICENSE.setEnabled(WARNING_CHECKBOX.isSelected());
        updateButtons();
    }

}
