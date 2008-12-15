package com.limegroup.gnutella.gui.init;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.ContentSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.core.settings.StartupSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.URLLabel;
import com.limegroup.gnutella.gui.WindowsUtils;
import com.limegroup.gnutella.util.MacOSXUtils;

/**
 * This class displays a window to the user allowing them to specify
 * their connection speed.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class MiscWindow extends SetupWindow {

    /**
     * The four buttons that represent the speeds, and their button group.
     */
    private ButtonGroup _speedGroup;

    private JRadioButton _modem;

    private JRadioButton _cable;

    /*
     * System Startup
     */
    private JCheckBox _startup;

    /**
     * The checkbox that determines whether or not to use content management.
     */
    private JCheckBox _filter;

    /**
     * Creates the window and its components.
     */
    MiscWindow() {
        super(I18nMarker.marktr("Miscellaneous Settings"),
              I18nMarker.marktr("Below, are several options that affect " +
                                "the performance and functionality of LimeWire."));
    }

    @Override
    protected void createPageContent() {

        JPanel mainPanel = new JPanel(new GridBagLayout());

        // Connection Speed
        {
            GridBagConstraints gbc = new GridBagConstraints();
            JPanel buttonPanel = new JPanel(new GridBagLayout());
            
            buttonPanel.setBorder(new TitledBorder(I18n.tr("Network Speed")));
            
            _speedGroup = new ButtonGroup();
            _modem = new JRadioButton(I18n.tr("Dial Up"));
            _cable = new JRadioButton(I18n.tr("Broadband (or unsure)"));
            
            _speedGroup.add(_modem);
            _speedGroup.add(_cable);
            
            WizardMultiLineLabel speedDesc = new WizardMultiLineLabel(
                    I18n.tr("Please choose the speed of your internet connection. Setting this speed correctly is important for optimum network performance."));
            speedDesc.setOpaque(false);
            speedDesc.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 5));
            speedDesc.setForeground(Color.black);
            speedDesc.setFont(speedDesc.getFont().deriveFont(Font.PLAIN));
            
            gbc.anchor = GridBagConstraints.NORTHEAST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            buttonPanel.add(speedDesc, gbc);
            
            gbc.weightx = 0;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            buttonPanel.add(_cable, gbc);
            
            gbc.insets = new Insets(0, 20, 0, 0);
            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            buttonPanel.add(_modem, gbc);
            
            gbc.insets = new Insets(0, 0, 10, 0);
            mainPanel.add(buttonPanel, gbc);
        }

        // System Startup
        if (GUIUtils.shouldShowStartOnStartupWindow()) {
            GridBagConstraints gbc = new GridBagConstraints();
            JPanel startupPanel = new JPanel(new GridBagLayout());
            
            startupPanel.setBorder(new TitledBorder(I18n.tr("System Startup")));
            
            _startup = new JCheckBox(I18n.tr("Start Automatically"));
            _startup.setSelected(StartupSettings.RUN_ON_STARTUP.getValue());
            
            WizardMultiLineLabel desc = new WizardMultiLineLabel(
                    I18n.tr("Would you like LimeWire to start when you log into your computer? This will cause LimeWire to start faster when you use it later."));
            desc.setOpaque(false);
            desc.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 5));
            desc.setForeground(Color.black);
            desc.setFont(desc.getFont().deriveFont(Font.PLAIN));
            
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
            
            startupPanel.add(desc, gbc);
            startupPanel.add(_startup, gbc);
            
            gbc.insets = new Insets(0, 0, 10, 0);
            mainPanel.add(startupPanel, gbc);
        }
        
        // Content Filtering
        {
            GridBagConstraints gbc = new GridBagConstraints();
            JPanel filterPanel = new JPanel(new GridBagLayout());
            
            filterPanel.setBorder(new TitledBorder(I18n.tr("Content Filtering")));
            
            _filter = new JCheckBox(I18n.tr("Enable Content Filtering"));
            _filter.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue());
            
            WizardMultiLineLabel desc = new WizardMultiLineLabel(
                    I18n.tr("LimeWire can filter files that copyright owners request not be shared. By enabling filtering, you are telling LimeWire to confirm all files you download or share with a list of removed content. You can change this at any time by choosing Filters -> Configure Content Filters from the main menu."));
            desc.setOpaque(false);
            desc.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 5));
            desc.setForeground(Color.black);
            desc.setFont(desc.getFont().deriveFont(Font.PLAIN));
            
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
            
            JLabel url= new URLLabel(ContentSettings.LEARN_MORE_URL, I18n.tr("Learn more about this option..."));
            url.setOpaque(false);
            url.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            url.setForeground(Color.black);
            url.setOpaque(false);
            url.setAlignmentY( 1.0f );

            
            filterPanel.add(desc, gbc);
            gbc.insets = new Insets(0, 0, 5, 0);
            filterPanel.add(url, gbc);
            gbc.insets = new Insets(0, 0, 0, 0);
            filterPanel.add(_filter, gbc);
            
            gbc.insets = new Insets(0, 0, 10, 0);
            mainPanel.add(filterPanel, gbc);
        }

        // Vertical Filler
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.add(new JPanel(), gbc);

        setSetupComponent(mainPanel);

        // set the radio button selection state to the
        // current setting, such that it doesn't just
        // get reset every time the window is drawn.
        //
        {
            int speed = ConnectionSettings.CONNECTION_SPEED.getValue();

            if (SpeedConstants.MODEM_SPEED_INT == speed)
                _modem.setSelected(true);
            else if (SpeedConstants.CABLE_SPEED_INT == speed)
                _cable.setSelected(true);
            else
                _cable.setSelected(true);
        }
    }

    /**
     * Overrides applySettings in SetupWindow superclass.
     * Applies the settings handled in this window.
     */
    @Override
    public void applySettings(boolean loadCoreComponents) {
        // Connection Speed
        {
            int speed = getSpeed();
            setDownloadSlots(speed);

            if (speed < SpeedConstants.MIN_SPEED_INT || SpeedConstants.MAX_SPEED_INT < speed) {
                throw new IllegalArgumentException();
            }

            ConnectionSettings.CONNECTION_SPEED.setValue(speed);
        }

        // System Startup
        if (GUIUtils.shouldShowStartOnStartupWindow()) {
            boolean allow = _startup.isSelected();

            if (OSUtils.isMacOSX())
                MacOSXUtils.setLoginStatus(allow);
            else if (WindowsUtils.isLoginStatusAvailable())
                WindowsUtils.setLoginStatus(allow);

            StartupSettings.RUN_ON_STARTUP.setValue(allow);
        }

        // Content Filtering
        {
            ContentSettings.USER_WANTS_MANAGEMENTS.setValue(_filter.isSelected());
        }
    }

    /**
     * Returns the selected speed value.  If no speed was selected, 
     * it returns the MODEM_SPEED.
     *
     * @return the selected speed value.  If no speed was selected, 
     * it returns the MODEM_SPEED
     */
    private int getSpeed() {
        if (_cable.isSelected())
            return SpeedConstants.CABLE_SPEED_INT;
        else
            return SpeedConstants.MODEM_SPEED_INT;
    }

    /**
     * Sets the number of download slots based on the connection
     * speed the user entered.
     * 
     * @param speed the speed of the connection to use for setting
     *  the download slots
     */
    private void setDownloadSlots(int speed) {

        if (speed == SpeedConstants.MODEM_SPEED_INT) {
            DownloadSettings.MAX_SIM_DOWNLOAD.setValue(3);
        } else if (speed == SpeedConstants.CABLE_SPEED_INT) {
            DownloadSettings.MAX_SIM_DOWNLOAD.setValue(8);
        } else {
            DownloadSettings.MAX_SIM_DOWNLOAD.setValue(3);
        }
    }
}
