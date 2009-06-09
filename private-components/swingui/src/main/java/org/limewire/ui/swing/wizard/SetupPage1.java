package org.limewire.ui.swing.wizard;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ContentSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.settings.StartupSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.shell.LimeAssociationOption;
import org.limewire.ui.swing.shell.LimeAssociations;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.WindowsUtils;
import org.limewire.util.OSUtils;

public class SetupPage1 extends WizardPage {

    private final SetupComponentDecorator decorator;
    
    private final String titleLine = I18n.tr("Please take a minute to configure these options before moving on.");

    private final String footerText = OSUtils.isMacOSX() ? I18n.tr("All settings can be changed later from LimeWire > Preferences") :
                                        I18n.tr("All settings can be changed later from Tools > Options");
    

    private final JCheckBox associationFileTypeCheckBox;
    private final JCheckBox launchAtStartupCheckBox;    
    private final JCheckBox shareDownloadedFilesCheckBox;
    private final JCheckBox shareUsageDataCheckBox;
    private final JCheckBox contentFilterCheckBox;
    
    public SetupPage1(SetupComponentDecorator decorator){
        this.decorator = decorator;
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0 14 0 0, gap 0"));       
   
        associationFileTypeCheckBox = createAndDecorateCheckBox(true);
        launchAtStartupCheckBox = createAndDecorateCheckBox(true);
        shareDownloadedFilesCheckBox = createAndDecorateCheckBox(true);
        shareUsageDataCheckBox = createAndDecorateCheckBox(true);
        contentFilterCheckBox = createAndDecorateCheckBox(false);  

        //File Associations
        addFileAssociations();        
        //Auto Sharing
        addAutoSharing();
        //Improving LimeWire Opt-out
        addImproving();
        //Content Filters
        addContentFilters();
        
        initSettings();
    }
    
    /**
     * Adds header for file association and any appropriate checkboxes and text
     */
    private void addFileAssociations() {
        if (LimeAssociations.isMagnetAssociationSupported() 
                || LimeAssociations.isTorrentAssociationSupported()
                || shouldShowStartOnStartupWindow()) {
             add(createAndDecorateHeader(I18n.tr("File Associations and Startup")), "gaptop 20, span, wrap");

             if (LimeAssociations.isMagnetAssociationSupported() 
                     || LimeAssociations.isTorrentAssociationSupported()) {
                 add(associationFileTypeCheckBox, "gaptop 5, gapleft 26");
                 add(createAndDecorateMultiLine(I18n.tr("Associate .magnet and .torrent files with LimeWire"), associationFileTypeCheckBox), "gaptop 5, gapleft 5, wrap");
             }
             
             if (shouldShowStartOnStartupWindow()) {
                 add(launchAtStartupCheckBox, "gaptop 5, gapleft 26");
                 add(createAndDecorateMultiLine(I18n.tr("Launch LimeWire at Startup"), launchAtStartupCheckBox), "gaptop 5, gapleft 5, wrap");
             }
        }
    }
    
    /**
     * Adds header for Auto-Sharing, checkbox and associated text
     */
    private void addAutoSharing() {
        add(createAndDecorateHeader(I18n.tr("Auto-Sharing")), "gaptop 20, span, wrap");
        add(shareDownloadedFilesCheckBox, "gaptop 5, gapleft 26");
        add(createAndDecorateMultiLine(I18n.tr("Share files downloaded from the P2P Network with the P2P Network"), shareDownloadedFilesCheckBox), "gapleft 5, gaptop 5");
        add(createAndDecorateHyperlink("http://www.limewire.com/client_redirect/?page=autoSharing"), "gapleft 10, wrap");
    }
    
    /**
     * Adds header for Anonymous Data collection, checkbox, and associated text
     */
    private void addImproving() {
        add(createAndDecorateHeader(I18n.tr("Improve LimeWire")), "gaptop 20, span, wrap");
        add(shareUsageDataCheckBox, "gaptop 5, gapleft 26");
        add(createAndDecorateMultiLine(I18n.tr("Help improve LimeWire by sending us anonymous usage data"), shareUsageDataCheckBox), "gapleft 5, gaptop 5");
        add(createAndDecorateHyperlink("http://www.limewire.com/client_redirect/?page=anonymousDataCollection"), "gapleft 10, wrap");
    }
    
    /**
     * Adds header for Copyrighted content filtering, checkbox, and associated text
     */
    private void addContentFilters() {
        add(createAndDecorateHeader(I18n.tr("Content Filters")), "gaptop 20, span, wrap");
        add(contentFilterCheckBox, "gaptop 5, gapleft 26");
        add(createAndDecorateMultiLine(I18n.tr("Don't let me download or upload files copyright owners request not be shared"), contentFilterCheckBox), "gapleft 5, gaptop 5");
        add(createAndDecorateHyperlink("http://www.limewire.com/client_redirect/?page=contentFiltering"), "gapleft 10, wrap");
    }
    
    private void initSettings() {
        associationFileTypeCheckBox.setSelected(SwingUiSettings.HANDLE_MAGNETS.getValue());
        launchAtStartupCheckBox.setSelected(StartupSettings.RUN_ON_STARTUP.getValue());
        shareDownloadedFilesCheckBox.setSelected(SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
        shareUsageDataCheckBox.setSelected(ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.getValue());
        contentFilterCheckBox.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue());  
    }

    @Override
    public String getLine1() {
        return titleLine;
    }
    
    @Override
    public String getLine2() {
        return null;
    }
    
    @Override
    public String getFooter() {
        return footerText;
    }
    
    @Override
    public void applySettings() {
        // File Associations
        SwingUiSettings.HANDLE_MAGNETS.setValue(associationFileTypeCheckBox.isSelected());
        LimeAssociationOption magnetAssociationOption = LimeAssociations.getMagnetAssociation();
        if (magnetAssociationOption != null) {
            magnetAssociationOption.setEnabled(associationFileTypeCheckBox.isSelected());
        }

        SwingUiSettings.HANDLE_TORRENTS.setValue(associationFileTypeCheckBox.isSelected());
        LimeAssociationOption torrentAssociationOption = LimeAssociations.getTorrentAssociation();
        if (torrentAssociationOption != null) {
            torrentAssociationOption.setEnabled(associationFileTypeCheckBox.isSelected());
        }

        InstallSettings.ASSOCIATION_OPTION.setValue(2);

        // launch at startup
        if (shouldShowStartOnStartupWindow()) {
            if (OSUtils.isMacOSX())
                MacOSXUtils.setLoginStatus(launchAtStartupCheckBox.isSelected());
            else if (WindowsUtils.isLoginStatusAvailable())
                WindowsUtils.setLoginStatus(launchAtStartupCheckBox.isSelected());

            StartupSettings.RUN_ON_STARTUP.setValue(launchAtStartupCheckBox.isSelected());
        } else
            StartupSettings.RUN_ON_STARTUP.setValue(false);
        InstallSettings.START_STARTUP.setValue(true);
        
        
        //Auto-Sharing downloaded files Setting
        SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.setValue(shareDownloadedFilesCheckBox.isSelected());
        SharingSettings.ALLOW_PARTIAL_SHARING.setValue(shareDownloadedFilesCheckBox.isSelected());
        InstallSettings.AUTO_SHARING_OPTION.setValue(true);
        
        //Anonymous Usage statics
        ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.setValue(shareUsageDataCheckBox.isSelected());
        InstallSettings.ANONYMOUS_DATA_COLLECTION.setValue(true);
        
        //Content Filters
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(contentFilterCheckBox.isSelected());
        InstallSettings.FILTER_OPTION.setValue(true);
    }
    
    private JLabel createAndDecorateHeader(String text) {
        JLabel label = new JLabel(text);
        decorator.decorateHeadingText(label);
        return label;
    }
    
    private JLabel createAndDecorateMultiLine(String text, JCheckBox checkBox) {
        JLabel label = new MultiLineLabel(text, 500);
        label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(checkBox));
        decorator.decorateNormalText(label); 
        return label;
    }
    
    private JCheckBox createAndDecorateCheckBox(boolean isSelected) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(isSelected);
        decorator.decorateLargeCheckBox(checkBox);
        return checkBox;
    }
    
    private HyperlinkButton createAndDecorateHyperlink(final String url) {
        HyperlinkButton learnMoreButton = new HyperlinkButton(new AbstractAction(I18n.tr("Learn more")) {
            {
                putValue(Action.SHORT_DESCRIPTION, url);
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL(url);
            }
        });
        decorator.decorateNormalText(learnMoreButton);
        decorator.decorateLink(learnMoreButton);
        return learnMoreButton;
    }
    
    /**
     * Determines if the Start On Startup option is available.
     */
    private boolean shouldShowStartOnStartupWindow() {
        return OSUtils.isMacOSX() || OSUtils.isGoodWindows();
    }
}
