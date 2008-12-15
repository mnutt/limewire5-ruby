package com.limegroup.gnutella.gui.xml.editor;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.TitledPaddedPanel;
import com.limegroup.gnutella.gui.BoxPanel;


import org.limewire.i18n.I18nMarker;

import javax.swing.JRadioButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;

/**
 * Creative Commons publishing wizard page with
 * radio buttons for asking user:
 * <p>
 * <ul>
 *    <li>allow commercial use</li>
 *    <li>allow modification</li>
 * </ul>
 * 
 */
class UsagePage extends CCPublishWizardPage {

    final JRadioButton ALLOW_COMMERCIAL_YES = new JRadioButton(I18n.tr("Yes"));

    final JRadioButton ALLOW_COMMERCIAL_NO = new JRadioButton(I18n.tr("No"));

    final JRadioButton ALLOW_MODIFICATIONS_SHAREALIKE = new JRadioButton(
            I18n.tr("ShareAlike"));

    final JRadioButton ALLOW_MODIFICATIONS_YES = new JRadioButton(
            I18n.tr("Yes"));

    final JRadioButton ALLOW_MODIFICATIONS_NO = new JRadioButton(
            I18n.tr("No"));

    public UsagePage() {
        super("usagePage", I18nMarker.marktr("Publish License"), I18nMarker
                .marktr("This tool helps you publish audio under a Creative Commons license."));
    }

    @Override
    protected void createPageContent(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        TitledPaddedPanel commercialUsePanel = new TitledPaddedPanel(I18n.tr("Allow commercial use of your work?"));

        // make sure the panel expands horizontally
        commercialUsePanel.add(Box.createHorizontalGlue());

        ButtonGroup bg = new ButtonGroup();
        bg.add(ALLOW_COMMERCIAL_YES);
        bg.add(ALLOW_COMMERCIAL_NO);

        BoxPanel buttonPanel = new BoxPanel();
        commercialUsePanel.add(buttonPanel);
        buttonPanel.add(ALLOW_COMMERCIAL_YES);
        buttonPanel.add(ALLOW_COMMERCIAL_NO);
        panel.add(commercialUsePanel);

        panel.add(Box.createRigidArea(BoxPanel.LINE_GAP));

        TitledPaddedPanel modificationsPanel = new TitledPaddedPanel(I18n.tr("Allow modification of your work?"));

        // make sure the panel expands horizontally
        modificationsPanel.add(Box.createHorizontalGlue());

        bg = new ButtonGroup();
        bg.add(ALLOW_MODIFICATIONS_SHAREALIKE);
        bg.add(ALLOW_MODIFICATIONS_YES);
        bg.add(ALLOW_MODIFICATIONS_NO);

        buttonPanel = new BoxPanel();
        modificationsPanel.add(buttonPanel);
        buttonPanel.add(ALLOW_MODIFICATIONS_SHAREALIKE);
        buttonPanel.add(ALLOW_MODIFICATIONS_YES);
        buttonPanel.add(ALLOW_MODIFICATIONS_NO);
        panel.add(modificationsPanel);

        // set defaults
        ALLOW_COMMERCIAL_NO.setSelected(true);
        ALLOW_MODIFICATIONS_NO.setSelected(true);
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

}
