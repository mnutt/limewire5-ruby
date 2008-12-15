package com.limegroup.gnutella.gui.xml.editor;

import com.limegroup.gnutella.gui.wizard.WizardPage;
import com.limegroup.gnutella.gui.wizard.Status;
import com.limegroup.gnutella.gui.GUIMediator;

import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Parent class for Creative Commons wizard pages
 */
abstract class CCPublishWizardPage extends WizardPage {

    /** The label displaying the status icon and message. */
    private JLabel statusLabel;

    protected CCPublishWizardPage(String key, String titleKey, String descriptionKey) {
        super(key, titleKey, descriptionKey);
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
    }

    protected void setOtherComponents() {
        add(statusLabel, BorderLayout.SOUTH);
    }

    protected void createPageContent() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(10, 5, 10, 5));
        add(mainPanel, BorderLayout.CENTER);

        createPageContent(mainPanel);

        revalidate();
    }

    protected abstract void createPageContent(JPanel panel);

    /**
     * Displays an empty message and icon in the status area
     * of the wizard page.
     *
     */
    public void setEmptyStatus() {
        statusLabel.setText(" ");
        statusLabel.setIcon(null);
    }


    private void setStatusMessage(Status.Severity severity, String message) {
        statusLabel.setText(message);
        if (severity == Status.Severity.ERROR) {
            statusLabel.setIcon(GUIMediator.getThemeImage("stop_small"));
        } else if (severity == Status.Severity.INFO) {
            statusLabel.setIcon(GUIMediator.getThemeImage("annotate_small"));
        } else {
            statusLabel.setIcon(null);
        }
    }

    /**
     * Displays Status message with severity {@link Status}
     * (using the appropriate icon) in the status area of
     * the wizard page.
     *
     * @param status Status information.
     * if null or status.length == 0, no status.
     */
    public void setStatusMessage(Status... status) {
        if (status == null || status.length == 0) {
            setEmptyStatus();
        } else {
            // TODO: How are supposed to handle multiple statuses?
            setStatusMessage(status[0].getSeverity(), status[0].getMessage());
        }
    }
}
