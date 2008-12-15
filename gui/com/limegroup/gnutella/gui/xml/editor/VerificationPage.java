package com.limegroup.gnutella.gui.xml.editor;

import com.limegroup.gnutella.gui.SizedTextField;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.wizard.WizardPageModificationHandler;
import com.limegroup.gnutella.gui.wizard.Status;


import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;

import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Creative Commons publishing wizard page concering verification URL
 */
class VerificationPage extends CCPublishWizardPage {

    /**
     * The Verification URL field
     */
    final JTextField VERIFICATION_URL_FIELD = new SizedTextField(20, GUIUtils.SizePolicy.RESTRICT_HEIGHT);

    final String VERIFICATION_ARCHIVE = I18n
            .tr("I want to use the Internet Archive to host the file.");

    final String VERIFICATION_SELF = I18n
            .tr("I want to host the verification file myself at the following URL:");

    final JRadioButton SELF_VERIFICATION = new JRadioButton(
            VERIFICATION_SELF);

    private boolean complete;

    public VerificationPage() {
        super("verificationPage", I18nMarker.marktr("Publish License"),
                I18nMarker
                        .marktr("Where do you want to store the verification URL?"));

        setURL(SharingSettings.CREATIVE_COMMONS_VERIFICATION_URL.getValue(),
                I18nMarker.marktr("What is this?"));
    }

    @Override
    protected void createPageContent(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        ButtonGroup bg = new ButtonGroup();
        bg.add(SELF_VERIFICATION);

        SELF_VERIFICATION.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                updateVerification();
            }
        });
        BoxPanel p = new BoxPanel(BoxPanel.X_AXIS);
        p.add(new JLabel(I18n
                .tr("The verification file is hosted at the following URL:")));
        p.add(Box.createHorizontalGlue());
        panel.add(p);
        panel.add(Box.createRigidArea(BoxPanel.VERTICAL_COMPONENT_GAP));

        VERIFICATION_URL_FIELD.setText("http://");
        p = new BoxPanel(BoxPanel.X_AXIS);
        p.addVerticalComponentGap();
        p.add(VERIFICATION_URL_FIELD);
        VERIFICATION_URL_FIELD.getDocument().addDocumentListener(new WizardPageModificationHandler(this));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, VERIFICATION_URL_FIELD
                .getPreferredSize().height));
        panel.add(p);

        panel.add(Box.createVerticalGlue());

        // set defaults
        SELF_VERIFICATION.setSelected(true);
        updateVerification();
    }

    @Override
    public boolean isPageComplete() {
        return complete;
    }

    void updateVerification() {
        VERIFICATION_URL_FIELD.setEnabled(true);
        validateInput();
    }

    @Override
    public void validateInput() {
        complete = true;
        String url = VERIFICATION_URL_FIELD.getText();
        if (url.equals("") || !url.startsWith("http://") || url.length() < 8) {
            setStatusMessage(new Status(I18n.tr("Please enter a verification URL for the license."), Status.Severity.INFO));
            complete = false;
        }
        try {
            new URL(url);
        } catch(MalformedURLException invalidURL) {
            setStatusMessage(new Status(I18n.tr("Please enter a verification URL for the license."), Status.Severity.ERROR));
            complete = false;
        }

        if (complete) {
            setEmptyStatus();
        }
        updateButtons();
    }
}
