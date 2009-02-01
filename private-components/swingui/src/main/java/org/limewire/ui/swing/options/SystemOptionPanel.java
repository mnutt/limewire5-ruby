package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.updates.UpdateStyle;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.ui.swing.settings.BugSettings;
import org.limewire.ui.swing.settings.StartupSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.shell.LimeAssociationOption;
import org.limewire.ui.swing.shell.LimeAssociations;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.WindowsUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

/**
 * System Option View
 */
public class SystemOptionPanel extends OptionPanel {

    private final TrayNotifier trayNotifier;
    private FileAssociationPanel fileAssociationPanel;
    private StartupShutdownPanel startupShutdownPanel;
    private BugsPanel bugsPanel;
    
    private JCheckBox betaCheckBox;

    @Inject
    public SystemOptionPanel(TrayNotifier trayNotifier) {
        this.trayNotifier = trayNotifier;
        setLayout(new MigLayout("hidemode 2, insets 15 15 15 15, fillx, wrap", "", ""));

        setOpaque(false);

        betaCheckBox = new JCheckBox(I18n.tr("Tell me about Beta updates"));
        betaCheckBox.setContentAreaFilled(false);
        
        add(getFileAssociationPanel(), "pushx, growx");
        add(getStartupShutdownPanel(), "pushx, growx");
        add(getBugsPanel(), "pushx, growx");
        add(betaCheckBox, "gapleft 15");
    }

    private OptionPanel getFileAssociationPanel() {
        if (fileAssociationPanel == null) {
            fileAssociationPanel = new FileAssociationPanel();
        }
        return fileAssociationPanel;
    }

    private OptionPanel getStartupShutdownPanel() {
        if (startupShutdownPanel == null) {
            startupShutdownPanel = new StartupShutdownPanel();
        }
        return startupShutdownPanel;
    }

    private OptionPanel getBugsPanel() {
        if (bugsPanel == null) {
            bugsPanel = new BugsPanel();
        }
        return bugsPanel;
    }


    @Override
    boolean applyOptions() {
        boolean restart = getFileAssociationPanel().applyOptions();
        restart |= getStartupShutdownPanel().applyOptions();
        restart |= getBugsPanel().applyOptions();
        if(betaCheckBox.isSelected()) {
            UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_BETA);
        } else {
            UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_MINOR);
        }

        return restart;
    }

    @Override
    boolean hasChanged() {
        int expectedUpdateStyle = betaCheckBox.isSelected() ? UpdateStyle.STYLE_BETA : UpdateStyle.STYLE_MINOR;
        
        return getFileAssociationPanel().hasChanged() || getStartupShutdownPanel().hasChanged()
                || UpdateSettings.UPDATE_STYLE.getValue() == expectedUpdateStyle || getBugsPanel().hasChanged();                
    }

    @Override
    public void initOptions() {
        getFileAssociationPanel().initOptions();
        getStartupShutdownPanel().initOptions();
        getBugsPanel().initOptions();
        betaCheckBox.setSelected(UpdateSettings.UPDATE_STYLE.getValue() == 0);
    }

    private class FileAssociationPanel extends OptionPanel {

        private JCheckBox magnetCheckBox;

        private JCheckBox torrentCheckBox;

        private JCheckBox warnCheckBox;

        public FileAssociationPanel() {
            super(I18n.tr("File Associations"));
            setLayout(new MigLayout("insets 0, gap 0"));

            magnetCheckBox = new JCheckBox(I18n.tr(".magnet files"));
            magnetCheckBox.setContentAreaFilled(false);
            magnetCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateView();
                }
            });

            torrentCheckBox = new JCheckBox(I18n.tr(".torrent files"));
            torrentCheckBox.setContentAreaFilled(false);
            torrentCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateView();
                }
            });
            warnCheckBox = new JCheckBox(I18n.tr("Warn me when other programs want to automatically open these types"));
            warnCheckBox.setContentAreaFilled(false);

            add(magnetCheckBox, "gapleft 5, gapbottom 5, wrap");
            add(torrentCheckBox, "gapleft 5, push");
            add(warnCheckBox);
        }

        @Override
        boolean applyOptions() {
            if (hasChanged(magnetCheckBox, SwingUiSettings.HANDLE_MAGNETS)) {
                applyOption(magnetCheckBox, SwingUiSettings.HANDLE_MAGNETS);
                LimeAssociationOption magnetAssociationOption = LimeAssociations
                        .getMagnetAssociation();
                if (magnetAssociationOption != null) {
                    magnetAssociationOption.setEnabled(magnetCheckBox.isSelected());
                }
            }

            if (hasChanged(torrentCheckBox, SwingUiSettings.HANDLE_TORRENTS)) {
                applyOption(torrentCheckBox, SwingUiSettings.HANDLE_TORRENTS);
                LimeAssociationOption torrentAssociationOption = LimeAssociations
                        .getTorrentAssociation();
                if (torrentAssociationOption != null) {
                    torrentAssociationOption.setEnabled(torrentCheckBox.isSelected());
                }
            }

            if (hasChanged(warnCheckBox, SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES)) {
                applyOption(warnCheckBox, SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES);
            }
            return false;
        }

        private void applyOption(JCheckBox checkBox, BooleanSetting booleanSetting) {
            booleanSetting.setValue(checkBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return hasChanged(magnetCheckBox, SwingUiSettings.HANDLE_MAGNETS)
                    || hasChanged(torrentCheckBox, SwingUiSettings.HANDLE_TORRENTS)
                    || hasChanged(warnCheckBox, SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES);
        }

        private boolean hasChanged(JCheckBox checkBox, BooleanSetting booleanSetting) {
            return booleanSetting.getValue() != checkBox.isSelected();
        }

        @Override
        public void initOptions() {
            initOption(magnetCheckBox, SwingUiSettings.HANDLE_MAGNETS.getValue()
                    && LimeAssociations.isMagnetAssociationSupported() && LimeAssociations.getMagnetAssociation().isEnabled());
            initOption(torrentCheckBox, SwingUiSettings.HANDLE_TORRENTS.getValue()
                    && LimeAssociations.isTorrentAssociationSupported() && LimeAssociations.getTorrentAssociation().isEnabled());
            initOption(warnCheckBox, SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES.getValue());
            updateView();
        }

        private void updateView() {
            boolean warnShouldBeVisible = magnetCheckBox.isSelected()
                    || torrentCheckBox.isSelected();
            warnCheckBox.setVisible(warnShouldBeVisible);

            boolean torrentShouldBeVisible = LimeAssociations.isTorrentAssociationSupported();
            torrentCheckBox.setVisible(torrentShouldBeVisible);

            boolean magnetShouldBeVisible = LimeAssociations.isMagnetAssociationSupported();
            magnetCheckBox.setVisible(magnetShouldBeVisible);

            setVisible(torrentShouldBeVisible || magnetShouldBeVisible);
        }

        private void initOption(JCheckBox checkBox, boolean value) {
            checkBox.setSelected(value);
        }
    }

    /**
     * When I press X is not shown for OSX, OSX automatically minimizes on an X
     * If Run at startup || minimize to try is selected, set System tray icon to
     * true
     * 
     * TODO: revisit this and check
     */
    private class StartupShutdownPanel extends OptionPanel {

        private JCheckBox runAtStartupCheckBox;

        private JRadioButton minimizeButton;

        private JRadioButton exitButton;

        private ButtonGroup buttonGroup;

        private boolean displaySystemTrayIcon = false;

        public StartupShutdownPanel() {
            super(I18n.tr("Startup and Shutdown"));
            setLayout(new MigLayout("insets 0, gap 0"));

            runAtStartupCheckBox = new JCheckBox(I18n.tr("Run LimeWire on System Startup"));
            runAtStartupCheckBox.setContentAreaFilled(false);
            minimizeButton = new JRadioButton(I18n.tr("Minimize to system tray"));
            minimizeButton.setContentAreaFilled(false);
            exitButton = new JRadioButton(I18n.tr("Exit program"));
            exitButton.setContentAreaFilled(false);

            buttonGroup = new ButtonGroup();
            buttonGroup.add(minimizeButton);
            buttonGroup.add(exitButton);

            if(OSUtils.isWindows() || OSUtils.isMacOSX())
                add(runAtStartupCheckBox, "split, gapleft 5, wrap");

            if (trayNotifier.supportsSystemTray()) {
                add(new JLabel(I18n.tr("When I press X:")), "wrap, gapleft 5, gaptop 6");
                add(minimizeButton, "gapleft 25, split, gapafter 20");
                add(exitButton);
            }
        }

        @Override
        boolean applyOptions() {
            if (OSUtils.isMacOSX()) {
                MacOSXUtils.setLoginStatus(runAtStartupCheckBox.isSelected());
            } else if (WindowsUtils.isLoginStatusAvailable()) {
                WindowsUtils.setLoginStatus(runAtStartupCheckBox.isSelected());
            }
            StartupSettings.RUN_ON_STARTUP.setValue(runAtStartupCheckBox.isSelected());
            SwingUiSettings.MINIMIZE_TO_TRAY.setValue(minimizeButton.isSelected());
            if(SwingUiSettings.MINIMIZE_TO_TRAY.getValue()) {
                trayNotifier.showTrayIcon();
            } else {
                trayNotifier.hideTrayIcon();
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return StartupSettings.RUN_ON_STARTUP.getValue() != runAtStartupCheckBox.isSelected()
                    || SwingUiSettings.MINIMIZE_TO_TRAY.getValue() != minimizeButton
                            .isSelected() || isIconDisplayed();
        }

        private boolean isIconDisplayed() {
            if ((runAtStartupCheckBox.isSelected() || minimizeButton.isSelected())
                    && OSUtils.supportsTray())
                return displaySystemTrayIcon != true;
            else
                return displaySystemTrayIcon != false;
        }

        @Override
        public void initOptions() {
            runAtStartupCheckBox.setSelected(StartupSettings.RUN_ON_STARTUP.getValue());
            minimizeButton.setSelected(SwingUiSettings.MINIMIZE_TO_TRAY.getValue());
            exitButton.setSelected(!SwingUiSettings.MINIMIZE_TO_TRAY.getValue());
        }
    }

    private class BugsPanel extends OptionPanel {

        private JRadioButton showBugsBeforeSending;
        private JRadioButton alwaysSendBugs;
        private JRadioButton neverSendBugs;


        public BugsPanel() {
            super(I18n.tr("Bugs"));
            setLayout(new MigLayout("gap 0, insets 0"));
            
            showBugsBeforeSending = new JRadioButton(I18n.tr("Let me know about bugs before sending them"));
            showBugsBeforeSending.setContentAreaFilled(false);
            alwaysSendBugs = new JRadioButton(I18n.tr("Always send bugs to Lime Wire"));
            alwaysSendBugs.setContentAreaFilled(false);
            neverSendBugs = new JRadioButton(I18n.tr("Never send bugs to Lime Wire"));
            neverSendBugs.setContentAreaFilled(false);

            ButtonGroup bugsButtonGroup = new ButtonGroup();
            bugsButtonGroup.add(showBugsBeforeSending);
            bugsButtonGroup.add(alwaysSendBugs);
            bugsButtonGroup.add(neverSendBugs);
            add(showBugsBeforeSending, "split, gapleft 5, wrap");
            add(alwaysSendBugs, "split, gapleft 5, wrap");
            add(neverSendBugs, "split, gapleft 5, wrap");
        }

        @Override
        boolean applyOptions() {
            if (showBugsBeforeSending.isSelected()) {
                BugSettings.SHOW_BUGS.setValue(true);
                BugSettings.REPORT_BUGS.setValue(true);
            } else if (alwaysSendBugs.isSelected()) {
                BugSettings.SHOW_BUGS.setValue(false);
                BugSettings.REPORT_BUGS.setValue(true);
            } else {
                BugSettings.SHOW_BUGS.setValue(false);
                BugSettings.REPORT_BUGS.setValue(false);
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return hasChanged(alwaysSendBugs, BugSettings.REPORT_BUGS)
                    || hasChanged(showBugsBeforeSending, BugSettings.SHOW_BUGS);
        }

        private boolean hasChanged(JRadioButton radioButton, BooleanSetting setting) {
            return setting.getValue() != radioButton.isSelected();
        }

        @Override
        public void initOptions() {
            if (BugSettings.SHOW_BUGS.getValue()) {
                showBugsBeforeSending.setSelected(true);
            } else if (BugSettings.REPORT_BUGS.getValue()) {
                alwaysSendBugs.setSelected(true);
            } else {
                neverSendBugs.setSelected(true);
            }
        }
    }
}
