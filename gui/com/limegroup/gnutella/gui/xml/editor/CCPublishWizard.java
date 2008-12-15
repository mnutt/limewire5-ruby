package com.limegroup.gnutella.gui.xml.editor;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.DialogOption;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.wizard.Wizard;
import com.limegroup.gnutella.gui.wizard.WizardPage;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.licenses.CCConstants;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.PublishedCCLicense;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import org.limewire.util.NameValue;
import org.limewire.util.OSUtils;


import javax.swing.border.EmptyBorder;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.Box;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Frame;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class provides a wizard for publishing a Creative Commons license.
 */
public class CCPublishWizard extends Wizard {

    private static final boolean CREATE_PAGE_UPON_ADDING = true;
    private static final int DIALOG_WIDTH  = 540;        // The minimum width of the window.
    private static final int DIALOG_HEIGHT = 360;        // The minimum height of the window.


    private LimeXMLDocument doc;
    private LimeXMLSchema schema;
	private FileDesc fd;

    // pages used by this wizard
    private DetailsPage detailsPage;
	private UsagePage usagePage;
	private VerificationPage verificationPage;
	private WarningPage warningPage;


    /**
	 * the dialog window and its parent frame
     * that holds all other gui elements for the setup.
     */
    private Frame parentFrame;
    private JDialog dialog;


    public CCPublishWizard(FileDesc fd, LimeXMLDocument doc, LimeXMLSchema schema, Frame parentFrame) {
        super(CREATE_PAGE_UPON_ADDING);
        this.fd = fd;
		this.doc = doc;
		this.schema = schema;
        this.parentFrame = parentFrame;

    }

	protected List<WizardPage> createWizardPages() {
		warningPage = new WarningPage();
		detailsPage = new DetailsPage();
		usagePage = new UsagePage();
		verificationPage = new VerificationPage();

        List<WizardPage> wizardPages = new ArrayList<WizardPage>();
        wizardPages.add(warningPage);
		wizardPages.add(usagePage);
		wizardPages.add(detailsPage);
		wizardPages.add(verificationPage);

		return wizardPages;
	}

    /*
	 * Creates the main <tt>JDialog</tt> instance and
     * creates all of the setup window classes, buttons, etc.
     */
    protected void showDialog(JPanel wizardPageContainer) {
        initInfo();

        dialog = new JDialog(parentFrame);
        dialog.setModal(true);

        // JDialog sizing seems to work differently with some Unix
        // systems, so we'll just make it resizable.
        if(!OSUtils.isUnix())
            dialog.setResizable(false);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performCancel();
            }
        });
        GUIUtils.addHideAction((JComponent)dialog.getContentPane());

        // set the layout of the content pane
        Container container = dialog.getContentPane();
        BoxLayout containerLayout = new BoxLayout(container, BoxLayout.Y_AXIS);
        container.setLayout(containerLayout);

        // create the main panel
        JPanel setupPanel = new JPanel();
        setupPanel.setBorder(new EmptyBorder(1, 1, 0, 0));
        BoxLayout layout = new BoxLayout(setupPanel, BoxLayout.Y_AXIS);
        setupPanel.setLayout(layout);

        // create the setup buttons panel
        setupPanel.add(wizardPageContainer);
        setupPanel.add(Box.createVerticalStrut(10));
        ButtonRow buttons = new ButtonRow(getButtonActions(), ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
        buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
        setupPanel.add(buttons);



        // add the panel and make it visible
        container.add(setupPanel);

        int width = Math.max(((JComponent)container).getPreferredSize().width, DIALOG_WIDTH);
        int height = Math.max(((JComponent)container).getPreferredSize().height, DIALOG_HEIGHT);
        ((JComponent)container).setPreferredSize(new Dimension(width, height));
        dialog.pack();

        dialog.setTitle(I18n.tr("Publish License"));
		dialog.setLocationRelativeTo(MessageService.getParentComponent());
		dialog.setVisible(true);
    }


    /**
	 * Displays the next window in the setup sequence.
	 */
	public void performNext() {
		WizardPage page = getNextPage();
        if (page != null) {
            show(page);
        }
    }

    /**
	 * Displays the previous window in the setup sequence.
     */
    public void performPrevious() {
        WizardPage page = getPreviousPage();
        if (page != null) {
            show(page);
        }
    }

    /**
	 * Cancels the setup.
     */
    public void performCancel() {
        dialog.dispose();
    }

	public void performFinish() {
		if (warningPage.MODIFY_LICENSE.isSelected()) {
			// save settings
			MetaDataSaver saver = new MetaDataSaver(new FileDesc[] { fd }, schema,
                    GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(
                            getInputValues(), schema.getSchemaURI()).getXMLString());
            saver.saveMetaData(getFileEventListener());
            dialog.dispose();
		} else {
		    DialogOption answer = GUIMediator.showYesNoMessage(I18n.tr("Are you sure you want to permanently remove the license from your local copy of this file?"), DialogOption.YES);
			if(answer == DialogOption.YES) {
				List<NameValue<String>> valList = CCPublishWizard.getPreviousValList(doc, false);
				valList.add(new NameValue<String>(LimeXMLNames.AUDIO_LICENSE, ""));
				valList.add(new NameValue<String>(LimeXMLNames.AUDIO_LICENSETYPE, ""));

				MetaDataSaver saver = new MetaDataSaver(new FileDesc[] { fd }, schema, 
						GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(
                                valList, schema.getSchemaURI()).getXMLString());
				saver.saveMetaData();
				dialog.dispose();
			}
		}
	}

	/**
	 * Initializes the fieds with the file's Meta Data only if a license does
	 * not exist.If a license exists, it populates the verification URL field
	 * and the license distribution details.
	 */
	private void initInfo() {
		License license = fd.getLicense();
		if (license != null) {
			warningPage.setLicenseAvailable(true);
			if (license.getLicenseURI() != null) {
				verificationPage.VERIFICATION_URL_FIELD.setText(license.getLicenseURI()
						.toString());
				verificationPage.SELF_VERIFICATION.setSelected(true);
				verificationPage.updateVerification();
			}
			String licenseDeed = license.getLicenseDeed(fd.getSHA1Urn()).toString();
			if (licenseDeed != null) {
				if (licenseDeed.equals(CCConstants.ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS_URI)) {
					usagePage.ALLOW_COMMERCIAL_YES.setSelected(false);
					usagePage.ALLOW_MODIFICATIONS_NO.setSelected(true);
				} else if (licenseDeed.equals(CCConstants.ATTRIBUTION_NO_DERIVS_URI)) {
					usagePage.ALLOW_COMMERCIAL_YES.setSelected(true);
					usagePage.ALLOW_MODIFICATIONS_NO.setSelected(true);
				} else if (licenseDeed.equals(CCConstants.ATTRIBUTION_NON_COMMERCIAL_URI)) {
					usagePage.ALLOW_COMMERCIAL_YES.setSelected(false);
					usagePage.ALLOW_MODIFICATIONS_YES.setSelected(true);
				} else if (licenseDeed.equals(CCConstants.ATTRIBUTION_SHARE_NON_COMMERCIAL_URI)) {
					usagePage.ALLOW_COMMERCIAL_YES.setSelected(false);
					usagePage.ALLOW_MODIFICATIONS_SHAREALIKE.setSelected(true);
				} else if (licenseDeed.equals(CCConstants.ATTRIBUTION_SHARE_URI)) {
					usagePage.ALLOW_COMMERCIAL_YES.setSelected(true);
					usagePage.ALLOW_MODIFICATIONS_SHAREALIKE.setSelected(true);
				} else {
					usagePage.ALLOW_COMMERCIAL_YES.setSelected(true);
					usagePage.ALLOW_MODIFICATIONS_YES.setSelected(true);
				}
			}
		} else {
			warningPage.setLicenseAvailable(false);
		}
		
		if (doc != null) {
			// license does not exist and file has XML doc
			detailsPage.COPYRIGHT_HOLDER.setText(doc.getValue(LimeXMLNames.AUDIO_ARTIST));
			detailsPage.COPYRIGHT_YEAR.setText(doc.getValue(LimeXMLNames.AUDIO_YEAR));
			detailsPage.WORK_TITLE.setText(doc.getValue(LimeXMLNames.AUDIO_TITLE));
		}
	}

	private MetaDataEventListener getFileEventListener() {
	    return null;
//		return new CCRDFOuptut(fd, detailsPage.COPYRIGHT_HOLDER.getText(),
//					detailsPage.WORK_TITLE.getText(), 
//					detailsPage.COPYRIGHT_YEAR.getText(),
//					detailsPage.DESCRIPTION.getText(), 
//					verificationPage.VERIFICATION_URL_FIELD.getText(),
//					getLicenseType());
	}

	private int getLicenseType() {
		int type = CCConstants.ATTRIBUTION;
		if (!usagePage.ALLOW_COMMERCIAL_YES.isSelected()) {
			type |= CCConstants.ATTRIBUTION_NON_COMMERCIAL;
		}
		if (usagePage.ALLOW_MODIFICATIONS_SHAREALIKE.isSelected()) {
			type |= CCConstants.ATTRIBUTION_SHARE;
		} else if (usagePage.ALLOW_MODIFICATIONS_NO.isSelected()) {
			type |= CCConstants.ATTRIBUTION_NO_DERIVS;
		}
		return type;
	}

	/**
	 * Returns an ArrayList with the <name,value> and MetaData of the license.
	 * 
	 * @return an ArrayList with the <name,value> tuples for the license and
	 *         licensetype.
	 */
	private List<NameValue<String>> getInputValues() {
		List<NameValue<String>> valList = new ArrayList<NameValue<String>>();
		String holder = detailsPage.COPYRIGHT_HOLDER.getText();
		String year = detailsPage.COPYRIGHT_YEAR.getText();
		String title = detailsPage.WORK_TITLE.getText();
		String description = detailsPage.DESCRIPTION.getText();
		int type = getLicenseType();
		String url = verificationPage.VERIFICATION_URL_FIELD.getText();
		boolean saveDetails = detailsPage.SAVE_DETAILS_CHECKBOX.isSelected();
		valList.addAll(getPreviousValList(doc, saveDetails));
		String embeddedLicense = PublishedCCLicense.getEmbeddableString(
				holder, title, year, url, description, type);
		if (embeddedLicense != null) {
			valList.add(new NameValue<String>(LimeXMLNames.AUDIO_LICENSE, embeddedLicense));
			valList.add(new NameValue<String>(LimeXMLNames.AUDIO_LICENSETYPE, CCConstants.CC_URI_PREFIX));
		}
		
		if (saveDetails) {
			valList.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, title));
			valList.add(new NameValue<String>(LimeXMLNames.AUDIO_YEAR, year));			
			valList.add(new NameValue<String>(LimeXMLNames.AUDIO_ARTIST, holder));
			valList.add(new NameValue<String>(LimeXMLNames.AUDIO_COMMENTS, description));
		}
		
		return valList;
	}

	private static List<NameValue<String>> getPreviousValList(LimeXMLDocument doc, boolean excludeDetails) {
		List<NameValue<String>> valList = new ArrayList<NameValue<String>>();
		if (doc != null) {
			for (Map.Entry<String, String> entry : doc.getNameValueSet()) {
				String key = entry.getKey();
				if (!isLicenseKey(key) && !(excludeDetails && isDetailsKey(key))) {
					valList.add(new NameValue<String>(entry.getKey(), entry
							.getValue()));
				}
				
			}
		}
		return valList;
	}
	
	private static boolean isLicenseKey(String key) {
		return key.equals(LimeXMLNames.AUDIO_LICENSE)
			|| key.equals(LimeXMLNames.AUDIO_LICENSETYPE);
	}
	
	private static boolean isDetailsKey(String key) {
		return key.equals(LimeXMLNames.AUDIO_TITLE)
			|| key.equals(LimeXMLNames.AUDIO_YEAR)
			|| key.equals(LimeXMLNames.AUDIO_ARTIST)
			|| key.equals(LimeXMLNames.AUDIO_COMMENTS);
	}

}
