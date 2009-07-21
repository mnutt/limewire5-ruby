package org.limewire.ui.swing.wizard;

import javax.swing.JCheckBox;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ContentSettings;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.settings.StartupSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.shell.LimeAssociationOption;
import org.limewire.ui.swing.shell.LimeAssociations;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.WindowsUtils;
import org.limewire.util.OSUtils;

public class SetupPage1 extends WizardPage {

    private final JCheckBox associationFileTypeCheckBox;
    private final JCheckBox launchAtStartupCheckBox;    
    private final JCheckBox shareUsageDataCheckBox;
    private final JCheckBox contentFilterCheckBox;
    
    public SetupPage1(SetupComponentDecorator decorator){
        super(decorator);
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0 14 0 0, gap 0"));       
   
        associationFileTypeCheckBox = createAndDecorateCheckBox(true);
        launchAtStartupCheckBox = createAndDecorateCheckBox(true);
        shareUsageDataCheckBox = createAndDecorateCheckBox(true);
        contentFilterCheckBox = createAndDecorateCheckBox(false);  

        //File Associations
        addFileAssociations();        
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
                 add(createAndDecorateMultiLine(I18n.tr("Launch LimeWire at system startup"), launchAtStartupCheckBox), "gaptop 5, gapleft 5, wrap");
             }
        }
    }
    
    /**
     * Adds header for Anonymous Data collection, checkbox, and associated text
     */
    private void addImproving() {
        //TODO: re-enable this code once the setting does something
        /*
        add(createAndDecorateHeader(I18n.tr("Improve LimeWire")), "gaptop 20, span, wrap");
        add(shareUsageDataCheckBox, "gaptop 5, gapleft 26");
        add(createAndDecorateMultiLine(I18n.tr("Help improve LimeWire by sending us anonymous usage data"), shareUsageDataCheckBox), "gapleft 5, gaptop 5");
        add(createAndDecorateHyperlink("http://www.limewire.com/client_redirect/?page=anonymousDataCollection"), "gapleft 10, wrap");
        */
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
        shareUsageDataCheckBox.setSelected(ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.getValue());
        contentFilterCheckBox.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue());  
    }

    @Override
    public String getLine1() {
        return I18n.tr("Please take a minute to configure these options before moving on.");
    }
    
    @Override
    public String getLine2() {
        return "";
    }
    
    @Override
    public String getFooter() {
        return OSUtils.isMacOSX() ? I18n.tr("All settings can be changed later from LimeWire > Preferences") :
            I18n.tr("All settings can be changed later in Tools > Options");
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
        
        //TODO: re-enable this code once the setting does something
        /*
        //Anonymous Usage statics
        ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.setValue(shareUsageDataCheckBox.isSelected());
        InstallSettings.ANONYMOUS_DATA_COLLECTION.setValue(true);
        */
        
        //Content Filters
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(contentFilterCheckBox.isSelected());
        InstallSettings.FILTER_OPTION.setValue(true);
    }
    
    /**
     * Determines if the Start On Startup option is available.
     */
    private boolean shouldShowStartOnStartupWindow() {
        return OSUtils.isMacOSX() || OSUtils.isGoodWindows();
    }
}
