package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveDirectoryHandler;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

/**
 * Downloads Option View
 */
public class DownloadOptionPanel extends OptionPanel {

    private SharingDownloadsPanel sharingDownloadsPanel;
    private SavingPanel savingPanel;
    private RecentDownloadsPanel recentDownloadsPanel;
    private ITunesPanel iTunesPanel;
    
    @Inject
    public DownloadOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getSharingDownloadsPanel(), "pushx, growx");
        add(getSavingPanel(), "pushx, growx");
        if(OSUtils.isAnyMac() || OSUtils.isWindows()) {
            add(getITunesPanel(), "pushx, growx");
        }
        add(getRecentDownloadsPanel(), "pushx, growx");
    }
    
    private OptionPanel getSharingDownloadsPanel() {
        if(sharingDownloadsPanel == null) {
            sharingDownloadsPanel = new SharingDownloadsPanel();
        }
        return sharingDownloadsPanel;
    }
    
    private OptionPanel getSavingPanel() {
        if(savingPanel == null) {
            savingPanel = new SavingPanel();
        }
        return savingPanel;
    }
    
    private OptionPanel getRecentDownloadsPanel() {
        if(recentDownloadsPanel == null) {
            recentDownloadsPanel = new RecentDownloadsPanel();
        }
        return recentDownloadsPanel;
    }
    
    private OptionPanel getITunesPanel() {
        if(iTunesPanel == null) {
            iTunesPanel = new ITunesPanel();
        }
        return iTunesPanel;
    }

    @Override
    boolean applyOptions() {
        boolean restart = getSharingDownloadsPanel().applyOptions();
        restart |= getSavingPanel().applyOptions();
        restart |= getRecentDownloadsPanel().applyOptions();
        restart |= getITunesPanel().applyOptions();

        return restart;
    }

    @Override
    boolean hasChanged() {
        return getSharingDownloadsPanel().hasChanged() || getSavingPanel().hasChanged() || getRecentDownloadsPanel().hasChanged() || getITunesPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getSharingDownloadsPanel().initOptions();
        getSavingPanel().initOptions();
        getRecentDownloadsPanel().initOptions();
        getITunesPanel().initOptions();
    }
    
    /**
     * Setting for automatically sharing completed and incomplete downloads
     * with the Gnutella network.
     */
    private class SharingDownloadsPanel extends OptionPanel {

        private JCheckBox shareCompletedDownloadsCheckBox;
        
        public SharingDownloadsPanel() {
            super(I18n.tr("Sharing downloads"));
            
            shareCompletedDownloadsCheckBox = new JCheckBox(I18n.tr("Share files downloaded from the P2P Network with the P2P Network"));
            shareCompletedDownloadsCheckBox.setContentAreaFilled(false);
            
            add(shareCompletedDownloadsCheckBox, "wrap");
        }
        
        @Override
        boolean applyOptions() {
            SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.setValue(shareCompletedDownloadsCheckBox.isSelected());
            SharingSettings.ALLOW_PARTIAL_SHARING.setValue(shareCompletedDownloadsCheckBox.isSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            return SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue() != shareCompletedDownloadsCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            shareCompletedDownloadsCheckBox.setSelected(SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
        }
    }
    
    private class SavingPanel extends OptionPanel {

        private String currentSaveDirectory;
        
        private JCheckBox clearDownloadsCheckBox;
        private JTextField downloadSaveTextField;
        private JButton browseSaveLocationButton;
        private JCheckBox autoRenameDuplicateFilesCheckBox;
        
        public SavingPanel() {
            super(I18n.tr("Saving"));
            
            clearDownloadsCheckBox = new JCheckBox(I18n.tr("Clear downloads from list when finished"));
            clearDownloadsCheckBox.setContentAreaFilled(false);
            downloadSaveTextField = new JTextField();
            downloadSaveTextField.setEditable(false);
            //downloadSaveTextField.setBackground(Color.WHITE);
            browseSaveLocationButton = new JButton(new BrowseDirectoryAction(DownloadOptionPanel.this, downloadSaveTextField));
            autoRenameDuplicateFilesCheckBox = new JCheckBox(I18n.tr("If the file already exists, download it with a different name"));
            autoRenameDuplicateFilesCheckBox.setContentAreaFilled(false);
            
            add(clearDownloadsCheckBox, "split 2, wrap");
            
            add(new JLabel(I18n.tr("Save downloads to:")), "split 3");
            add(downloadSaveTextField, "span, growx, push");
            add(browseSaveLocationButton, "wrap");
            
            add(autoRenameDuplicateFilesCheckBox, "gapleft 25, split 2");
        }
        
        @Override
        boolean applyOptions() {
            SharingSettings.CLEAR_DOWNLOAD.setValue(clearDownloadsCheckBox.isSelected());
            DownloadSettings.AUTO_RENAME_DUPLICATE_FILES.setValue(autoRenameDuplicateFilesCheckBox.isSelected());
            final String save = downloadSaveTextField.getText();
            if(!save.equals(currentSaveDirectory)) {
                try {
                    File saveDir = new File(save);
                    if(!SaveDirectoryHandler.isDirectoryValid(saveDir)) {
                        if (!saveDir.mkdirs())
                            throw new IOException();
                    }
                    SharingSettings.setSaveDirectory(saveDir);
                    currentSaveDirectory = save;
                } catch(Exception ioe) {
                    FocusJOptionPane.showMessageDialog(DownloadOptionPanel.this, 
                            I18n.tr("Could not save download directory, reverted to old directory"),
                            I18n.tr("Save Folder Error"),
                            JOptionPane.ERROR_MESSAGE);
                    downloadSaveTextField.setText(currentSaveDirectory);
                }
            }
            return false;
        }

        @Override
        boolean hasChanged() { 
            return SharingSettings.CLEAR_DOWNLOAD.getValue() != clearDownloadsCheckBox.isSelected()
                    || !currentSaveDirectory.equals(downloadSaveTextField.getText()) 
                    || DownloadSettings.AUTO_RENAME_DUPLICATE_FILES.getValue() != autoRenameDuplicateFilesCheckBox.isSelected();
        }

        @Override
        public void initOptions() { 
            autoRenameDuplicateFilesCheckBox.setSelected(DownloadSettings.AUTO_RENAME_DUPLICATE_FILES.getValue());
            clearDownloadsCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
            
            try {
                File file = SharingSettings.getSaveDirectory();
                if (file == null) {
                    file = SharingSettings.DEFAULT_SAVE_DIR;
                    if(file == null)
                        throw (new FileNotFoundException());
                }
                currentSaveDirectory = file.getCanonicalPath();
                downloadSaveTextField.setText(file.getCanonicalPath());
            } catch (FileNotFoundException fnfe) {
                // simply use the empty string if we could not get the save
                // directory.
                currentSaveDirectory = "";
                downloadSaveTextField.setText("");
            } catch (IOException ioe) {
                currentSaveDirectory = "";
                downloadSaveTextField.setText("");
            }
        }
    }
    
    /**
     * Setting for automatically importing songs into your iTunes library. 
     * NOTE: this is only available on OSX and Windows
     */
    private class ITunesPanel extends OptionPanel {

        private JCheckBox addToITunesCheckBox;
        
        public ITunesPanel() {
            super(I18n.tr("iTunes"));
            
            addToITunesCheckBox = new JCheckBox(I18n.tr("Add audio files I downloaded from LimeWire to iTunes"));
            addToITunesCheckBox.setContentAreaFilled(false);
            
            add(addToITunesCheckBox, "wrap");
        }
        
        @Override
        boolean applyOptions() {
            iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(addToITunesCheckBox.isSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            return iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue() != addToITunesCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            addToITunesCheckBox.setSelected(iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue());
        }        
    }

    private class RecentDownloadsPanel extends OptionPanel {

        private JCheckBox rememberDownloadsCheckBox;
        private JButton clearButton;
        
        public RecentDownloadsPanel() {
            super(I18n.tr("Recent downloads"));
            
            rememberDownloadsCheckBox = new JCheckBox(I18n.tr("Remember recent downloads"));
            rememberDownloadsCheckBox.setContentAreaFilled(false);
            
            clearButton = new JButton(I18n.tr("Clear Now"));
            clearButton.addActionListener(new ActionListener() {
               @Override
                public void actionPerformed(ActionEvent e) {
                   DownloadSettings.RECENT_DOWNLOADS.clear();
                   clearButton.setEnabled(false);
                } 
            });
            
            add(rememberDownloadsCheckBox);
            add(clearButton);
            
        }
        
        @Override
        boolean applyOptions() {
            DownloadSettings.REMEMBER_RECENT_DOWNLOADS.setValue(rememberDownloadsCheckBox.isSelected());
            if(!rememberDownloadsCheckBox.isSelected()) {
                DownloadSettings.RECENT_DOWNLOADS.clear();
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return DownloadSettings.REMEMBER_RECENT_DOWNLOADS.getValue() != rememberDownloadsCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            clearButton.setEnabled(true);
            rememberDownloadsCheckBox.setSelected(DownloadSettings.REMEMBER_RECENT_DOWNLOADS.getValue());
        }
    }
}
