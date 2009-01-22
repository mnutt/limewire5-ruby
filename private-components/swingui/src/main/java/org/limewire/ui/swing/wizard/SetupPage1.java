package org.limewire.ui.swing.wizard;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ContentSettings;
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

    private String titleLine = I18n.tr("Please take a minute to configure these options before moving on.");
    
    private String filterTitle = I18n.tr("Content Filters");
    
    private String filterCheckText = I18n.tr("Don't let me download or upload files copyright owners request not be shared.");
    private JCheckBox filterCheck;

    private String associationsAndStartupTitle = I18n.tr("File Associations and Startup");
    private String associationsText = I18n.tr("Associate .magnet and .torrent files with LimeWire");    
    private String startupText = I18n.tr("Launch LimeWire at Startup");
    
    private JCheckBox associationCheck;
    private JCheckBox startupCheck;    
    
    public SetupPage1(SetupComponentDecorator decorator){

        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, nogrid"));
        
        HyperlinkButton learnMoreButton = new HyperlinkButton(new AbstractAction(I18n.tr("Learn more")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL("http://filtered.limewire.com/learnmore/contentFiltering");
            }
        });
        decorator.decorateNormalText(learnMoreButton);
        decorator.decorateLink(learnMoreButton);
        
        filterCheck = new JCheckBox();
        decorator.decorateLargeCheckBox(filterCheck);
        
        associationCheck = new JCheckBox();
        associationCheck.setSelected(true);
        decorator.decorateLargeCheckBox(associationCheck);

        startupCheck = new JCheckBox();  
        decorator.decorateLargeCheckBox(startupCheck);
        decorator.decorateNormalText(startupCheck);
        
        JLabel label;
        
        label = new JLabel(filterTitle);
        decorator.decorateHeadingText(label);
        
        add(label, "gaptop 15, gapleft 14, wrap");
        
        add(filterCheck, "gaptop 10, gapleft 40");
        label = new MultiLineLabel(filterCheckText, 500);
        label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(filterCheck));
        decorator.decorateNormalText(label);       
        JPanel textAndLinkCell = new JPanel(new MigLayout("flowy, gap 2, insets 0, fill"));
        textAndLinkCell.add(label);
        textAndLinkCell.add(learnMoreButton);
        add(textAndLinkCell, "gaptop 28, gapleft 5, wrap");
        
        if (LimeAssociations.isMagnetAssociationSupported() 
               || LimeAssociations.isTorrentAssociationSupported()
               || shouldShowStartOnStartupWindow()) {
            label = new JLabel(associationsAndStartupTitle);
            decorator.decorateHeadingText(label);
            add(label, "gaptop 30, gapleft 14, wrap");
        }
        
        if (LimeAssociations.isMagnetAssociationSupported() 
                || LimeAssociations.isTorrentAssociationSupported()) {
            add(associationCheck, "gaptop 10, gapleft 40");
            label = new MultiLineLabel(associationsText, 500);
            label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(associationCheck));
            decorator.decorateNormalText(label);       
            add(label, "gaptop 9, gapleft 5, wrap");
        }
        
        if (shouldShowStartOnStartupWindow()) {
            startupCheck.setSelected(true);
            add(startupCheck, "gaptop 10, gapleft 40");
            label = new MultiLineLabel(startupText, 500);
            label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(startupCheck));
            decorator.decorateNormalText(label);       
            add(label, "gaptop 9, gapleft 5, wrap");
        }
        
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
        return "";
    }
    
    @Override
    public void applySettings() {
        // filter settings
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(filterCheck.isSelected());
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(filterCheck.isSelected());
        InstallSettings.FILTER_OPTION.setValue(true);

        SwingUiSettings.HANDLE_MAGNETS.setValue(associationCheck.isSelected());
        LimeAssociationOption magnetAssociationOption = LimeAssociations.getMagnetAssociation();
        if (magnetAssociationOption != null) {
            magnetAssociationOption.setEnabled(associationCheck.isSelected());
        }

        SwingUiSettings.HANDLE_TORRENTS.setValue(associationCheck.isSelected());
        LimeAssociationOption torrentAssociationOption = LimeAssociations.getTorrentAssociation();
        if (torrentAssociationOption != null) {
            torrentAssociationOption.setEnabled(associationCheck.isSelected());
        }

        InstallSettings.ASSOCIATION_OPTION.setValue(2);

        // launch at startup
        if (shouldShowStartOnStartupWindow()) {
            if (OSUtils.isMacOSX())
                MacOSXUtils.setLoginStatus(startupCheck.isSelected());
            else if (WindowsUtils.isLoginStatusAvailable())
                WindowsUtils.setLoginStatus(startupCheck.isSelected());

            StartupSettings.RUN_ON_STARTUP.setValue(startupCheck.isSelected());
        } else
            StartupSettings.RUN_ON_STARTUP.setValue(false);
        InstallSettings.START_STARTUP.setValue(true);
    }
    
    /**
     * Determines if the Start On Startup option is available.
     */
    private boolean shouldShowStartOnStartupWindow() {
        return OSUtils.isMacOSX() || OSUtils.isGoodWindows();
    }
}
