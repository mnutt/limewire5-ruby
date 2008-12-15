package com.limegroup.gnutella.gui.init;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.limewire.setting.FileSetting;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.InstallSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.net.FirewallService;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.FramedDialog;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.Line;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.SplashWindow;
import com.limegroup.gnutella.gui.wizard.Wizard;
import com.limegroup.gnutella.gui.wizard.WizardPage;
import com.limegroup.gnutella.gui.shell.LimeAssociations;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;

/**
 * This class manages the setup wizard.  It constructs all of the primary
 * classes and acts as the mediator between the various objects in the
 * setup windows.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class SetupManager extends Wizard {

    private static final boolean CREATE_PAGE_UPON_ADDING = false;


    /**
	 * the dialog window that holds all other gui elements for the setup.
	 */
	private FramedDialog dialogFrame;

    private final FirewallService firewallService;

    public SetupManager(FirewallService firewallService) {
        super(CREATE_PAGE_UPON_ADDING);
        this.firewallService = firewallService;
    }


    /**
     * Adds the appropriate setup windows if needed.
     */
    protected List<WizardPage> createWizardPages() {

        List<WizardPage> windows = new LinkedList<WizardPage>();

        // TODO: REPLACE the should methods with enums, impl. comparable
        // enum called SetupWindowType that contains shouldShowPage method 
        SaveStatus saveDirectoryStatus = shouldShowSaveDirectoryWindow();
        if(saveDirectoryStatus != SaveStatus.NO)
            windows.add(new SaveWindow(dialogFrame, saveDirectoryStatus == SaveStatus.MIGRATE));
            
        if (shouldShowMiscWindow()) {
            windows.add(new MiscWindow());
        }
        
        if(shouldShowFirewallWindow()) {
            windows.add(new FirewallWindow());
        }
        
        if (shouldShowAssociationsWindow()) {
        	windows.add(new AssociationsWindow());
        }

        if( !InstallSettings.EXTENSION_OPTION.getValue())
            windows.add(new FileTypeWindow(dialogFrame));        

        // KEEP THIS OPTION LAST!
        IntentWindow intentWindow = new IntentWindow();
        if(!intentWindow.isConfirmedWillNot())
            windows.add(intentWindow);

        boolean partial = ApplicationSettings.INSTALLED.getValue();
        if (shouldShowWelcomeWindow(windows, partial)) {
            windows.add(0, new WelcomeWindow(partial));
        }
        return windows;
    }

    /*
      * Creates the main <tt>JDialog</tt> instance and
      * creates all of the setup window classes, buttons, etc.
      */
	protected void showDialog(JPanel setupWindowHolder) {

        dialogFrame = new FramedDialog();
        dialogFrame.setTitle(I18n.tr("LimeWire Setup"));
        dialogFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performCancel();
            }
        });

        JDialog dialog = dialogFrame.getDialog();
        dialog.setModal(true);
        dialog.setTitle(I18n.tr("LimeWire Setup Wizard"));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performCancel();
            }
        });

        // set the layout of the content pane
        Container container = dialog.getContentPane();
        GUIUtils.addHideAction((JComponent) container);
        BoxLayout containerLayout = new BoxLayout(container, BoxLayout.Y_AXIS);
        container.setLayout(containerLayout);

        // create the main panel
        JPanel setupPanel = new JPanel();
        setupPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
        BoxLayout layout = new BoxLayout(setupPanel, BoxLayout.Y_AXIS);
        setupPanel.setLayout(layout);

        Dimension d = new Dimension(SetupWindow.SETUP_WIDTH, SetupWindow.SETUP_HEIGHT);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation((screenSize.width - d.width) / 2, (screenSize.height - d.height) / 2);
        dialogFrame.setLocation(screenSize.width / 2, screenSize.height  / 2);

        // create the setup buttons panel
        setupPanel.add(setupWindowHolder);
        setupPanel.add(Box.createVerticalStrut(17));

        JPanel bottomRow = new JPanel();
        bottomRow.setLayout(new BoxLayout(bottomRow, BoxLayout.X_AXIS));
        ButtonRow buttons = new ButtonRow(getButtonActions(), ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
        LanguagePanel languagePanel = new LanguagePanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLanguage();
            }
        });
        bottomRow.add(languagePanel);
        bottomRow.add(Box.createHorizontalGlue());
        bottomRow.add(buttons);
        bottomRow.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        setupPanel.add(new Line());
        setupPanel.add(bottomRow);

        // add the panel and make it visible
        container.add(setupPanel);

        container.setPreferredSize(new Dimension(SetupWindow.SETUP_WIDTH, SetupWindow.SETUP_HEIGHT));
        dialog.pack();

        SplashWindow.instance().setVisible(false);
        MessageService.setCurrentFrame(dialogFrame);
        dialogFrame.showDialog();
        MessageService.setCurrentFrame(null);
        SplashWindow.instance().setVisible(true);
	}

    /**
	 * Displays the next window in the setup sequence.
	 */
    public void performNext() {
        WizardPage newWindow = getNextPage();
		try {
            if (newWindow != null) {
                applySettings(true);
			    show(newWindow);
            }
        } catch(ApplySettingsException ase) {
			// there was a problem applying the settings from
			// the current window, so display the error message 
			// to the user.
            if (ase.getMessage() != null && ase.getMessage().length() > 0)
                GUIMediator.showError(ase.getMessage());			
		}
	}

	/**
	 * Displays the previous window in the setup sequence.
	 */
    public void performPrevious() {
        WizardPage newWindow = getPreviousPage();
        try {
            if (newWindow != null) {
                applySettings(false);
                show(newWindow);
            }
        } catch(ApplySettingsException ase) {
            // ignore errors when going backwards
        }
	}

	
	/**
	 * Cancels the setup.
	 */
    public void performCancel() {
		dialogFrame.disposeDialog();
		System.exit(0);
	}

    public void performFinish() {

	    try {       
            applySettings(true);
        } catch(ApplySettingsException ase) {
            // there was a problem applying the settings from
            // the current window, so display the error message 
            // to the user.
            if (ase.getMessage() != null && ase.getMessage().length() > 0)
                GUIMediator.showError(ase.getMessage()); 
            //don't finish if there is an exception
            return;
        }
        
		ApplicationSettings.INSTALLED.setValue(true);

        InstallSettings.SAVE_DIRECTORY.setValue(true);
        InstallSettings.SPEED.setValue(true);
        InstallSettings.SCAN_FILES.setValue(true);
        InstallSettings.LANGUAGE_CHOICE.setValue(true);
        InstallSettings.FILTER_OPTION.setValue(true);
        InstallSettings.EXTENSION_OPTION.setValue(true);

        if (GUIUtils.shouldShowStartOnStartupWindow())
            InstallSettings.START_STARTUP.setValue(true);
        if (OSUtils.isWindows())
            InstallSettings.FIREWALL_WARNING.setValue(true);
        InstallSettings.ASSOCIATION_OPTION.setValue(LimeAssociations.CURRENT_ASSOCIATIONS);
		
		Future<Void> future = BackgroundExecutorService.submit(new Callable<Void>() {
            public Void call() {
                SettingsGroupManager.instance().save();
                return null;
            }
        });

        WizardPage currentWindow = getCurrentPage();
        if(currentWindow instanceof IntentWindow) {
		    IntentWindow intent = (IntentWindow)currentWindow;
		    if(!intent.isConfirmedWillNot()) {
		        GUIMediator.showWarning("Lime Wire LLC does not distribute LimeWire to people who intend to use it for the purposes of copyright infringement.\n\nThank you for your interest; however, you cannot continue to use LimeWire at this time.");
		        try {
		            future.get();
		        } catch(Exception ignored) {}
		        System.exit(1);
		    }
		}

        dialogFrame.disposeDialog();
    }


    /************************************************************************/
    /* Helper methods                                                       */
    /************************************************************************/

    /**
     * Determines if the 'a firewall warning may be displayed' window should be shown.
     */
    public boolean shouldShowFirewallWindow() {
        if(InstallSettings.FIREWALL_WARNING.getValue())
            return false;

        // Only show the firewall warning if this is windows, and if
        // we're not capable of automatically changing the firewall.
        return OSUtils.isWindows() && !firewallService.isProgrammaticallyConfigurable();
    }

    /**
     * Returns true if any of the options on the misc window should be presented
     * to the user.
     */
    public boolean shouldShowMiscWindow() {
        if (!InstallSettings.SPEED.getValue()) {
            return true;
        }
        if (!InstallSettings.FILTER_OPTION.getValue()) {
            return true;
        }
        if (!InstallSettings.START_STARTUP.getValue()) {
            return GUIUtils.shouldShowStartOnStartupWindow();
        }
        return false;
    }

    private boolean shouldShowAssociationsWindow() {
        if (InstallSettings.ASSOCIATION_OPTION.getValue() == LimeAssociations.CURRENT_ASSOCIATIONS)
            return false;

    	// display a window if silent grab failed.
    	return !GUIMediator.getAssociationManager().checkAndGrab(false);
    }

    private static enum SaveStatus { NO, NEEDS, MIGRATE };
    private SaveStatus shouldShowSaveDirectoryWindow() {
        // If it's not setup, definitely show it!
        if(!InstallSettings.SAVE_DIRECTORY.getValue())
            return SaveStatus.NEEDS;

        // Otherwise, if it has been setup, it might need
        // additional tweaking because defaults have changed,
        // and we want to move the save directory to somewhere
        // else.
        FileSetting saveSetting = SharingSettings.DIRECTORY_FOR_SAVING_FILES;
        if(saveSetting.isDefault()) {
            // If the directory is default, it could be because older versions
            // of LW didn't write out their save directory (if it was default).
            // Check to see if the new one doesn't exist, but the old one does.
            File oldDefaultDir = new File(CommonUtils.getUserHomeDir(), "Shared");
            if(!saveSetting.getValue().exists() && oldDefaultDir.exists())
                return SaveStatus.MIGRATE;
        }

        return SaveStatus.NO;
    }


    // If the INSTALLED value is set, that means that a previous
    // installer has already been run.

    // We need to ask the user's language very very first,
    // so make sure that if the LanguageWindow is the first item,
    // that the WelcomeWindow is inserted second.
    // It's a little more tricky than that, though, because
    // it could be possible that the LanguageWindow was the only
    // item to be installed -- if that's the case, don't even
    // insert the WelcomeWindow & FinishWindow at all.
    private boolean shouldShowWelcomeWindow(List<WizardPage> windows, boolean partial) {
        if (windows.size() == 0) {
            return false;
        }

        // If the INSTALLED app setting is set AND
        // intentWindow is not the one and only window added
        if (partial &&
            !( (windows.size() == 1) && (windows.get(0) instanceof IntentWindow) )
            ) {
            return true;
        }
        return false;
    }
}






