package org.limewire.ui.swing.options;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.FileSetting;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LabelTextField;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveDirectoryHandler;
import org.limewire.util.MediaType;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Downloads Option View.
 */
public class DownloadOptionPanel extends OptionPanel {

    private final Provider<IconManager> iconManager;
    private final ManageSaveFoldersOptionPanelFactory manageFoldersOptionPanelFactory;
    
    private SavingPanel savingPanel;
    private JCheckBox clearDownloadsCheckBox;

    //private JCheckBox deleteFileOnCancelCheckBox;
    
    @Inject
    public DownloadOptionPanel(Provider<IconManager> iconManager,
            ManageSaveFoldersOptionPanelFactory manageFoldersOptionPanelFactory) {
        this.iconManager = iconManager;
        this.manageFoldersOptionPanelFactory = manageFoldersOptionPanelFactory;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap, gap 4"));
        
        add(getSavingPanel(), "pushx, growx");

        clearDownloadsCheckBox = new JCheckBox(I18n.tr("Clear downloads from list when finished"));
        clearDownloadsCheckBox.setContentAreaFilled(false);

        //we aren't using deleteFileOnCancelCheckBox yet
        //deleteFileOnCancelCheckBox = new JCheckBox(I18n.tr("When I cancel a download, delete the file"));
        //deleteFileOnCancelCheckBox.setContentAreaFilled(false);
        //deleteFileOnCancelCheckBox.setVisible(false);
        
        add(clearDownloadsCheckBox, "wrap");
        //add(deleteFileOnCancelCheckBox);
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
        //DownloadSettings.DELETE_CANCELED_DOWNLOADS.setValue(deleteFileOnCancelCheckBox.isSelected());
        return getSavingPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getSavingPanel().hasChanged() 
            || SharingSettings.CLEAR_DOWNLOAD.getValue() != clearDownloadsCheckBox.isSelected(); 
         //   || DownloadSettings.DELETE_CANCELED_DOWNLOADS.getValue() != deleteFileOnCancelCheckBox.isSelected();
    }

    @Override
    public void initOptions() {
        getSavingPanel().initOptions();
        clearDownloadsCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
        //deleteFileOnCancelCheckBox.setSelected(DownloadSettings.DELETE_CANCELED_DOWNLOADS.getValue());
    }
    
    /**
     * Defines the Saving sub-panel in the Download options container.
     */
    private class SavingPanel extends OptionPanel {
        
        private String currentSaveDirectory;
        
        private LabelTextField downloadSaveTextField;
        private JButton browseSaveLocationButton;
        private JCheckBox autoRenameDuplicateFilesCheckBox;
        
        private ManageSaveFoldersOptionPanel saveFolderPanel;
        private JButton multiLocationConfigureButton;

        private JRadioButton singleLocationButton;

        private JRadioButton multiLocationButton;
        
        public SavingPanel() {
            super(I18n.tr("Saving Files"));
            
            ButtonGroup downloadOptions = new ButtonGroup();
            singleLocationButton = new JRadioButton(I18n.tr("Save all downloads to one folder:"));
            multiLocationButton = new JRadioButton(I18n.tr("Save different categories to different folders"));
            singleLocationButton.setOpaque(false);
            multiLocationButton.setOpaque(false);
            downloadOptions.add(singleLocationButton);
            downloadOptions.add(multiLocationButton);
            
            downloadSaveTextField = new LabelTextField(iconManager);
            downloadSaveTextField.setEditable(false);

            BrowseDirectoryAction directoryAction = new BrowseDirectoryAction(DownloadOptionPanel.this, downloadSaveTextField);
            downloadSaveTextField.addMouseListener(directoryAction);
            browseSaveLocationButton = new JButton(directoryAction);
            autoRenameDuplicateFilesCheckBox = new JCheckBox(I18n.tr("If the file already exists, download it with a different name"));
            autoRenameDuplicateFilesCheckBox.setContentAreaFilled(false);
            
            add(singleLocationButton);
            add(downloadSaveTextField, "span, growx");
            add(browseSaveLocationButton, "wrap");

            saveFolderPanel = manageFoldersOptionPanelFactory.create(new OKDialogAction(), new CancelDialogAction());
            saveFolderPanel.setSize(new Dimension(400,500));
            
            multiLocationConfigureButton = new JButton(new DialogDisplayAction(this, saveFolderPanel, 
                    I18n.tr("Download Folders"),I18n.tr("Configure..."),I18n.tr("Configure where different categories are downloaded")));
            
            add(multiLocationButton);
            add(multiLocationConfigureButton, "wrap");
            
            add(autoRenameDuplicateFilesCheckBox, "wrap");
            
            ActionListener downloadSwitchAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (singleLocationButton.isSelected()) {
                        downloadSaveTextField.setVisible(true);
                        browseSaveLocationButton.setVisible(true);
                        multiLocationConfigureButton.setVisible(false);
                    } 
                    else {
                        downloadSaveTextField.setVisible(false);
                        browseSaveLocationButton.setVisible(false);
                        multiLocationConfigureButton.setVisible(true);
                    }
                }
            };
            singleLocationButton.addActionListener(downloadSwitchAction);
            multiLocationButton.addActionListener(downloadSwitchAction);
        }
        
        @Override
        boolean applyOptions() {
            
            if (singleLocationButton.isSelected() && saveFolderPanel.isConfigCustom()) {
                saveFolderPanel.revertToDefault();
            }
            
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
            
            return saveFolderPanel.applyOptions();
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
                String mediaSaveDir = mediaSetting.get().getAbsolutePath();
                if (oldSaveDir.equals(mediaSaveDir) || newSaveDir.equals(mediaSaveDir)) {
                    mediaSetting.revertToDefault();
                }
            }
        }

        @Override
        boolean hasChanged() { 
            return  !currentSaveDirectory.equals(downloadSaveTextField.getText()) 
                    || saveFolderPanel.hasChanged()
                    || singleLocationButton.isSelected() && saveFolderPanel.isConfigCustom()
                    || SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue() != autoRenameDuplicateFilesCheckBox.isSelected();
        }

        @Override
        public void initOptions() { 
            autoRenameDuplicateFilesCheckBox.setSelected(SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue());
            saveFolderPanel.initOptions();
            
            if (saveFolderPanel.isConfigCustom()) {
                multiLocationButton.doClick();
            }
            else {
                singleLocationButton.doClick();
            }
            
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
}
