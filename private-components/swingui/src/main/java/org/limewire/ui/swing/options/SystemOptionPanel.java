package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.updates.UpdateStyle;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.BugSettings;
import org.limewire.core.settings.StartupSettings;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.ui.swing.shell.LimeAssociationOption;
import org.limewire.ui.swing.shell.LimeAssociations;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

/**
 * System Option View
 */
public class SystemOptionPanel extends OptionPanel {

    private FileAssociationPanel fileAssociationPanel;

    private StartupShutdownPanel startupShutdownPanel;

    private UpdatesBugsPanel updatesBugsPanel;

    @Inject
    public SystemOptionPanel() {
        setLayout(new MigLayout("hidemode 2, insets 15 15 15 15, fillx, wrap", "", ""));

        setOpaque(false);

        add(getFileAssociationPanel(), "pushx, growx");
        add(getStartupShutdownPanel(), "pushx, growx");
        add(getUpdatesBugsPanel(), "pushx, growx");
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

    private OptionPanel getUpdatesBugsPanel() {
        if (updatesBugsPanel == null) {
            updatesBugsPanel = new UpdatesBugsPanel();
        }
        return updatesBugsPanel;
    }

    @Override
    boolean applyOptions() {
        boolean restart = getFileAssociationPanel().applyOptions();
        restart |= getStartupShutdownPanel().applyOptions();
        restart |= getUpdatesBugsPanel().applyOptions();

        return restart;
    }

    @Override
    boolean hasChanged() {
        return getFileAssociationPanel().hasChanged() || getStartupShutdownPanel().hasChanged()
                || getUpdatesBugsPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getFileAssociationPanel().initOptions();
        getStartupShutdownPanel().initOptions();
        getUpdatesBugsPanel().initOptions();
    }

    private class FileAssociationPanel extends OptionPanel {

        private JCheckBox magnetCheckBox;

        private JCheckBox torrentCheckBox;

        private JCheckBox warnCheckBox;

        private JLabel magnetLabel;

        private JLabel torrentLabel;

        private JLabel warnLabel;

        public FileAssociationPanel() {
            super(I18n.tr("File Associations"));

            magnetCheckBox = new JCheckBox();
            magnetCheckBox.setContentAreaFilled(false);
            magnetCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateView();
                }
            });

            torrentCheckBox = new JCheckBox();
            torrentCheckBox.setContentAreaFilled(false);
            torrentCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateView();
                }
            });
            warnCheckBox = new JCheckBox();
            warnCheckBox.setContentAreaFilled(false);

            add(magnetCheckBox);

            magnetLabel = new JLabel(I18n.tr(".magnet files"));
            add(magnetLabel, "wrap");

            add(torrentCheckBox);

            torrentLabel = new JLabel(I18n.tr(".torrent files"));
            add(torrentLabel, "push");

            add(warnCheckBox);

            warnLabel = new JLabel(I18n.tr("Warn me when other programs want to automatically open these types"));
            add(warnLabel, "wrap");
        }

        @Override
        boolean applyOptions() {
            if (hasChanged(magnetCheckBox, ApplicationSettings.HANDLE_MAGNETS)) {
                applyOption(magnetCheckBox, ApplicationSettings.HANDLE_MAGNETS);
                LimeAssociationOption magnetAssociationOption = LimeAssociations
                        .getMagnetAssociation();
                if (magnetAssociationOption != null) {
                    magnetAssociationOption.setEnabled(magnetCheckBox.isSelected());
                }
            }

            if (hasChanged(torrentCheckBox, ApplicationSettings.HANDLE_TORRENTS)) {
                applyOption(torrentCheckBox, ApplicationSettings.HANDLE_TORRENTS);
                LimeAssociationOption torrentAssociationOption = LimeAssociations
                        .getTorrentAssociation();
                if (torrentAssociationOption != null) {
                    torrentAssociationOption.setEnabled(torrentCheckBox.isSelected());
                }
            }

            if (hasChanged(warnCheckBox, ApplicationSettings.WARN_FILE_ASSOCIATION_CHANGES)) {
                applyOption(warnCheckBox, ApplicationSettings.WARN_FILE_ASSOCIATION_CHANGES);
            }
            return false;
        }

        private void applyOption(JCheckBox checkBox, BooleanSetting booleanSetting) {
            booleanSetting.setValue(checkBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return hasChanged(magnetCheckBox, ApplicationSettings.HANDLE_MAGNETS)
                    || hasChanged(torrentCheckBox, ApplicationSettings.HANDLE_TORRENTS)
                    || hasChanged(warnCheckBox, ApplicationSettings.WARN_FILE_ASSOCIATION_CHANGES);
        }

        private boolean hasChanged(JCheckBox checkBox, BooleanSetting booleanSetting) {
            return booleanSetting.getValue() != checkBox.isSelected();
        }

        @Override
        public void initOptions() {
            initOption(magnetCheckBox, ApplicationSettings.HANDLE_MAGNETS.getValue()
                    && LimeAssociations.isMagnetAssociationSupported() && LimeAssociations.getMagnetAssociation().isEnabled());
            initOption(torrentCheckBox, ApplicationSettings.HANDLE_TORRENTS.getValue()
                    && LimeAssociations.isTorrentAssociationSupported() && LimeAssociations.getTorrentAssociation().isEnabled());
            initOption(warnCheckBox, ApplicationSettings.WARN_FILE_ASSOCIATION_CHANGES.getValue());
            updateView();
        }

        private void updateView() {
            boolean warnShouldBeVisible = magnetCheckBox.isSelected()
                    || torrentCheckBox.isSelected();
            warnCheckBox.setVisible(warnShouldBeVisible);
            warnLabel.setVisible(warnShouldBeVisible);

            boolean torrentShouldBeVisible = LimeAssociations.isTorrentAssociationSupported();
            torrentLabel.setVisible(torrentShouldBeVisible);
            torrentCheckBox.setVisible(torrentShouldBeVisible);

            boolean magnetShouldBeVisible = LimeAssociations.isMagnetAssociationSupported();
            magnetCheckBox.setVisible(magnetShouldBeVisible);
            magnetLabel.setVisible(magnetShouldBeVisible);

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

        private boolean shutdownAfterTransfer = false;

        public StartupShutdownPanel() {
            super(I18n.tr("Startup and Shutdown"));

            runAtStartupCheckBox = new JCheckBox(I18n.tr("Run LimeWire on System Startup"));
            runAtStartupCheckBox.setContentAreaFilled(false);
            minimizeButton = new JRadioButton(I18n.tr("Minimize to system tray"));
            minimizeButton.setContentAreaFilled(false);
            exitButton = new JRadioButton(I18n.tr("Exit program"));
            exitButton.setContentAreaFilled(false);

            buttonGroup = new ButtonGroup();
            buttonGroup.add(minimizeButton);
            buttonGroup.add(exitButton);

            add(runAtStartupCheckBox, "split, wrap");

            if (!OSUtils.isAnyMac()) {
                add(new JLabel(I18n.tr("When I press X:")), "wrap");

                add(minimizeButton, "gapleft 25, split, gapafter 20");
                add(exitButton);
            }
        }

        @Override
        boolean applyOptions() {
            StartupSettings.RUN_ON_STARTUP.setValue(runAtStartupCheckBox.isSelected());
            ApplicationSettings.MINIMIZE_TO_TRAY.setValue(minimizeButton.isSelected());

            // if minimize or run at startup is selected, system tray icon must
            // be shown
            if ((minimizeButton.isSelected() || runAtStartupCheckBox.isSelected())
                    && OSUtils.supportsTray()) {
                ApplicationSettings.DISPLAY_TRAY_ICON.setValue(true);
            } else {
                ApplicationSettings.DISPLAY_TRAY_ICON.setValue(false);
            }
            ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.setValue(false);
            return false;
        }

        @Override
        boolean hasChanged() {
            return StartupSettings.RUN_ON_STARTUP.getValue() != runAtStartupCheckBox.isSelected()
                    || ApplicationSettings.MINIMIZE_TO_TRAY.getValue() != minimizeButton
                            .isSelected() || isIconDisplayed() || shutdownAfterTransfer != false;
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
            // TODO: should shutdown after transfer be set to false here??
            shutdownAfterTransfer = ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.getValue();
            if (shutdownAfterTransfer) {
                ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.setValue(false);
                ApplicationSettings.MINIMIZE_TO_TRAY.setValue(true);
                shutdownAfterTransfer = false;
            }

            runAtStartupCheckBox.setSelected(StartupSettings.RUN_ON_STARTUP.getValue());
            minimizeButton.setSelected(ApplicationSettings.MINIMIZE_TO_TRAY.getValue());
            exitButton.setSelected(!ApplicationSettings.MINIMIZE_TO_TRAY.getValue());

            // load these to ensure that we override them correctly since they
            // aren't
            // directly accessable in 5.0
            displaySystemTrayIcon = ApplicationSettings.DISPLAY_TRAY_ICON.getValue();
        }
    }

    private class UpdatesBugsPanel extends OptionPanel {

        private JCheckBox betaCheckBox;

        private JCheckBox bugCheckBox;

        private JCheckBox bugMessageCheckBox;

        public UpdatesBugsPanel() {
            super(I18n.tr("Updates and Bugs"));

            betaCheckBox = new JCheckBox(I18n.tr("Tell me about Beta updates"));
            betaCheckBox.setContentAreaFilled(false);
            bugCheckBox = new JCheckBox(I18n.tr("Report bugs to LimeWire"));
            bugCheckBox.setContentAreaFilled(false);

            bugMessageCheckBox = new JCheckBox(I18n.tr("Show me the bug report before sending it"));
            bugMessageCheckBox.setContentAreaFilled(false);

            add(betaCheckBox, "split, wrap");
            add(bugCheckBox, "split, wrap");
            add(bugMessageCheckBox, "split, wrap");
        }

        @Override
        boolean applyOptions() {
            applyOption(bugCheckBox, BugSettings.REPORT_BUGS);
            applyOption(bugMessageCheckBox, BugSettings.SHOW_BUGS);
            
            if(betaCheckBox.isSelected()) {
                UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_BETA);
            } else {
                UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_MINOR);
            }
            return false;
        }

        private void applyOption(JCheckBox checkbox, BooleanSetting setting) {
            setting.setValue(checkbox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return hasChanged(bugCheckBox, BugSettings.REPORT_BUGS)
                    || hasChanged(bugMessageCheckBox, BugSettings.SHOW_BUGS);
        }

        private boolean hasChanged(JCheckBox checkbox, BooleanSetting setting) {
            return setting.getValue() != checkbox.isSelected();
        }

        @Override
        public void initOptions() {
            initOption(betaCheckBox, UpdateSettings.UPDATE_STYLE.getValue() == 0);
            initOption(bugCheckBox, BugSettings.REPORT_BUGS.getValue());
            initOption(bugMessageCheckBox, BugSettings.SHOW_BUGS.getValue());
        }

        private void initOption(JCheckBox checkBox, boolean value) {
            checkBox.setSelected(value);
        }
    }
}
