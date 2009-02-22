package org.limewire.ui.swing.options;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.setting.FileSetting;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LabelTextField;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveDirectoryHandler;
import org.limewire.util.MediaType;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

/**
 * Downloads Option View
 */
public class DownloadOptionPanel extends OptionPanel {

    private final IconManager iconManager;
    
    private SavingPanel savingPanel;
    private JCheckBox clearDownloadsCheckBox;
    
    @Inject
    public DownloadOptionPanel(IconManager iconManager) {
        this.iconManager = iconManager;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getSavingPanel(), "pushx, growx");

        clearDownloadsCheckBox = new JCheckBox(I18n.tr("Clear downloads from list when finished"));
        clearDownloadsCheckBox.setContentAreaFilled(false);
        
        add(clearDownloadsCheckBox, "gapleft 15");
    }
    
    private OptionPanel getSavingPanel() {
        if(savingPanel == null) {
            savingPanel = new SavingPanel();
        }
        return savingPanel;
    }

    @Override
    boolean applyOptions() {
        SharingSettings.CLEAR_DOWNLOAD.setValue(clearDownloadsCheckBox.isSelected());
        return getSavingPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getSavingPanel().hasChanged() 
            || SharingSettings.CLEAR_DOWNLOAD.getValue() != clearDownloadsCheckBox.isSelected();
    }

    @Override
    public void initOptions() {
        getSavingPanel().initOptions();
        clearDownloadsCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
    }
    
    /**
     * Defines the Saving sub-panel in the Download options container.
     */
    private class SavingPanel extends OptionPanel {
        
        private String currentSaveDirectory;
        
        private LabelTextField downloadSaveTextField;
        private JButton browseSaveLocationButton;
        private JCheckBox autoRenameDuplicateFilesCheckBox;
        
        private JCheckBox shareCompletedDownloadsCheckBox;
        private JCheckBox addToITunesCheckBox;
        
        public SavingPanel() {
            super(I18n.tr("Saving"));
            
            downloadSaveTextField = new LabelTextField(iconManager);
            downloadSaveTextField.setEditable(false);

            BrowseDirectoryAction directoryAction = new BrowseDirectoryAction(DownloadOptionPanel.this, downloadSaveTextField);
            downloadSaveTextField.addMouseListener(directoryAction);
            browseSaveLocationButton = new JButton(directoryAction);
            autoRenameDuplicateFilesCheckBox = new JCheckBox(I18n.tr("If the file already exists, download it with a different name"));
            autoRenameDuplicateFilesCheckBox.setContentAreaFilled(false);
            
            shareCompletedDownloadsCheckBox = new JCheckBox(I18n.tr("Share files downloaded from the P2P Network with the P2P Network"));
            shareCompletedDownloadsCheckBox.setContentAreaFilled(false);
            
            addToITunesCheckBox = new JCheckBox(I18n.tr("Add audio files I downloaded from LimeWire to iTunes"));
            addToITunesCheckBox.setContentAreaFilled(false);
            

            
            add(new JLabel(I18n.tr("Save downloads to:")), "split 3");
            add(downloadSaveTextField, "span, growx, push");
            add(browseSaveLocationButton, "wrap");
            
            add(autoRenameDuplicateFilesCheckBox, "gapleft 25, split 2, wrap");
            
            add(shareCompletedDownloadsCheckBox, "split 3, wrap");
            if(OSUtils.isMacOSX() || OSUtils.isWindows()) {
                add(addToITunesCheckBox, "split 3, wrap");
            }
        }
        
        @Override
        boolean applyOptions() {
            SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.setValue(autoRenameDuplicateFilesCheckBox.isSelected());
            final String save = downloadSaveTextField.getText();
            if(!save.equals(currentSaveDirectory)) {
                try {
                    File saveDir = new File(save);
                    if(!SaveDirectoryHandler.isDirectoryValid(saveDir)) {
                        if (!saveDir.mkdirs())
                            throw new IOException();
                    }
                    SharingSettings.setSaveDirectory(saveDir);
                    updateMediaSaveDirectories(currentSaveDirectory, save);
                    currentSaveDirectory = save;
                } catch(Exception ioe) {
                    FocusJOptionPane.showMessageDialog(DownloadOptionPanel.this, 
                            I18n.tr("Could not save download directory, reverted to old directory"),
                            I18n.tr("Save Folder Error"),
                            JOptionPane.ERROR_MESSAGE);
                    downloadSaveTextField.setText(currentSaveDirectory);
                }
            }
            SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.setValue(shareCompletedDownloadsCheckBox.isSelected());
            SharingSettings.ALLOW_PARTIAL_SHARING.setValue(shareCompletedDownloadsCheckBox.isSelected());
            
            if(OSUtils.isMacOSX() || OSUtils.isWindows()) {
                iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(addToITunesCheckBox.isSelected());
            }
            return false;
        }
        
        /**
         * Updates the save location for all media types.
         */
        private void updateMediaSaveDirectories(String oldSaveDir, String newSaveDir) {
            updateMediaSaveDirectory(MediaType.getAudioMediaType(), oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(MediaType.getVideoMediaType(), oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(MediaType.getImageMediaType(), oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(MediaType.getDocumentMediaType(), oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(MediaType.getProgramMediaType(), oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(MediaType.getOtherMediaType(), oldSaveDir, newSaveDir);
        }
        
        /**
         * Update the save location for the specified media type.  If the media
         * save location is equal to the old or new default location, then the
         * media setting is reverted to the default setting.
         */
        private void updateMediaSaveDirectory(MediaType mediaType, 
                String oldSaveDir, String newSaveDir) {
            FileSetting mediaSetting = SharingSettings.getFileSettingForMediaType(mediaType);
            if (!mediaSetting.isDefault()) {
                String mediaSaveDir = mediaSetting.getValue().getAbsolutePath();
                if (oldSaveDir.equals(mediaSaveDir) || newSaveDir.equals(mediaSaveDir)) {
                    mediaSetting.revertToDefault();
                }
            }
        }

        @Override
        boolean hasChanged() { 
            return  !currentSaveDirectory.equals(downloadSaveTextField.getText()) 
                    || SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue() != autoRenameDuplicateFilesCheckBox.isSelected()
                    || SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue() != shareCompletedDownloadsCheckBox.isSelected()
                    || (OSUtils.isMacOSX() || OSUtils.isWindows()) ? iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue() != addToITunesCheckBox.isSelected() : false;
        }

        @Override
        public void initOptions() { 
            autoRenameDuplicateFilesCheckBox.setSelected(SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue());
            
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
            
            shareCompletedDownloadsCheckBox.setSelected(SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
            
            if(OSUtils.isMacOSX() || OSUtils.isWindows()) {
                addToITunesCheckBox.setSelected(iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue());
            }
        }
    }
}
