package com.limegroup.gnutella.gui.xml.editor;

import com.limegroup.gnutella.gui.wizard.WizardPageModificationHandler;
import com.limegroup.gnutella.gui.wizard.Status;
import com.limegroup.gnutella.gui.SizedTextField;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.LabeledComponent;


import org.limewire.i18n.I18nMarker;

import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.JScrollPane;

/**
 * Creative Commons publishing wizard page that takes input for:
 *
 * <ul>
 *    <li>Copyright Holder</li>
 *    <li>Copyright Year</li>
 *    <li>Title of Work</li>
 *    <li>Comment</li>
 * </ul>
 *
 */
class DetailsPage extends CCPublishWizardPage {

    final JTextField COPYRIGHT_HOLDER = new SizedTextField(24, GUIUtils.SizePolicy.RESTRICT_HEIGHT);

    final JTextField WORK_TITLE = new SizedTextField(24, GUIUtils.SizePolicy.RESTRICT_HEIGHT);

    final JTextField COPYRIGHT_YEAR = new SizedTextField(6, GUIUtils.SizePolicy.RESTRICT_HEIGHT);

    final JTextArea DESCRIPTION = new JTextArea(4, 24);

    final JCheckBox SAVE_DETAILS_CHECKBOX = new JCheckBox(I18n.tr("Save details to file"));

    private boolean complete = false;

    public DetailsPage() {
        super("detailsPage", I18nMarker.marktr("Publish License"), I18nMarker
                .marktr("This tool helps you publish audio under a Creative Commons license."));
    }

    @Override
    protected void createPageContent(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        BoxPanel holderPanel = new BoxPanel(BoxPanel.X_AXIS);
        panel.add(holderPanel);
        LabeledComponent c = new LabeledComponent(
                I18nMarker.marktr("Copyright Holder:"), COPYRIGHT_HOLDER,
                LabeledComponent.NO_GLUE, LabeledComponent.TOP_LEFT);
        COPYRIGHT_HOLDER.getDocument().addDocumentListener(new WizardPageModificationHandler(this)) ;
        holderPanel.add(c.getComponent());
        holderPanel.add(Box
                .createRigidArea(BoxPanel.HORIZONTAL_COMPONENT_GAP));
        c = new LabeledComponent(I18nMarker.marktr("Copyright Year:"),
                COPYRIGHT_YEAR, LabeledComponent.NO_GLUE,
                LabeledComponent.TOP_LEFT);
        COPYRIGHT_YEAR.getDocument().addDocumentListener(new WizardPageModificationHandler(this)) ;
        holderPanel.add(c.getComponent());
        panel.add(Box.createRigidArea(BoxPanel.LINE_GAP));

        c = new LabeledComponent(I18nMarker.marktr("Title of Work:"),
                WORK_TITLE, LabeledComponent.NO_GLUE,
                LabeledComponent.TOP_LEFT);
        WORK_TITLE.getDocument().addDocumentListener(new WizardPageModificationHandler(this)) ;
        panel.add(c.getComponent());
        panel.add(Box.createRigidArea(BoxPanel.LINE_GAP));

        c = new LabeledComponent(I18nMarker.marktr("Comment:"),
                new JScrollPane(DESCRIPTION), LabeledComponent.NO_GLUE,
                LabeledComponent.TOP_LEFT);
        panel.add(c.getComponent());

        panel.add(c.getComponent());
        panel.add(Box.createRigidArea(BoxPanel.LINE_GAP));
//			panel.add(SAVE_DETAILS_CHECKBOX);
    }

    @Override
    public boolean isPageComplete() {
        return complete;
    }

    @Override
    public void validateInput() {
        complete = true;
        if ("".equals(COPYRIGHT_HOLDER.getText())
                || "".equals(COPYRIGHT_YEAR.getText())
                || "".equals(WORK_TITLE.getText())) {
            setStatusMessage(new Status(I18n.tr("Please enter the copyright holder, copright year and title."), Status.Severity.INFO));
            complete = false;
        } else {
            try {
                Integer.parseInt(COPYRIGHT_YEAR.getText());
            } catch(NumberFormatException e) {
                setStatusMessage(new Status(I18n.tr("Please enter a valid year for the file you want to publish."), Status.Severity.ERROR));
                complete = false;
            }
        }

        if (complete) {
            setEmptyStatus();
        }

        updateButtons();
    }

}
