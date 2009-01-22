package org.limewire.ui.swing.options;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.daap.DaapManager;
import org.limewire.core.settings.DaapSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LabelTextField;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;

/**
 * Files Option View
 */
public class FilesOptionPanel extends OptionPanel {
    
    private final ManageSaveFoldersOptionPanelFactory manageFoldersOptionPanelFactory;
    private final ManageFileExtensionsOptionPanel manageFileExtensionsOptionPanel;
    private final DaapManager daapManager;
    private final IconManager iconManager;
    
    private ManageExtensionsPanel manageExtensionsPanel;
    private SaveFoldersPanel saveFoldersPanel;
    private LimeWireStorePanel limeWireStorePanel;
    private ITunesPanel iTunesPanel;
    
    @Inject
    FilesOptionPanel(ManageSaveFoldersOptionPanelFactory manageFoldersOptionPanelFactory, 
            ManageFileExtensionsOptionPanel manageFileExtensionsOptionPanel, DaapManager daapManager,
            IconManager iconManager) { 
        
        this.manageFoldersOptionPanelFactory = manageFoldersOptionPanelFactory;
        this.manageFileExtensionsOptionPanel = manageFileExtensionsOptionPanel;
        this.daapManager = daapManager;
        this.iconManager = iconManager;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
    
        setOpaque(false);
        
        add(getManageExtensionsPanel(), "pushx, growx");
        add(getSaveOptionPanel(), "pushx, growx");
        add(getLimeWireStorePanel(), "pushx, growx");
        add(getITunesPanel(), "pushx, growx");
    }
    
    private OptionPanel getManageExtensionsPanel() {
        if(manageExtensionsPanel == null) {
            manageExtensionsPanel = new ManageExtensionsPanel();
        }
        return manageExtensionsPanel;
    }
    
    private OptionPanel getSaveOptionPanel() {
        if(saveFoldersPanel == null) {
            saveFoldersPanel = new SaveFoldersPanel();
        }
        return saveFoldersPanel;
    }
    
    private OptionPanel getLimeWireStorePanel() {
        if(limeWireStorePanel == null) {
            limeWireStorePanel = new LimeWireStorePanel();
        }
        return limeWireStorePanel;
    } 
    
    private OptionPanel getITunesPanel() {
        if(iTunesPanel == null) {
            iTunesPanel = new ITunesPanel();
        }
        return iTunesPanel;
    }

    @Override
    boolean applyOptions() {
        boolean restart = getManageExtensionsPanel().applyOptions();
        restart |= getSaveOptionPanel().applyOptions();
        restart |= getLimeWireStorePanel().applyOptions();
        restart |= getITunesPanel().applyOptions();

        return restart;
    }

    @Override
    boolean hasChanged() {
        return getManageExtensionsPanel().hasChanged() || 
                getSaveOptionPanel().hasChanged() ||
                getLimeWireStorePanel().hasChanged() ||
                getITunesPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getManageExtensionsPanel().initOptions();
        getSaveOptionPanel().initOptions();
        getLimeWireStorePanel().initOptions();
        getITunesPanel().initOptions();
    }
    
    private class ManageExtensionsPanel extends OptionPanel {

        private ManageFileExtensionsOptionPanel extensionsPanel;
        private JButton manageButton;
        
        public ManageExtensionsPanel() {
            super(I18n.tr("File Extensions"));
            
            extensionsPanel = manageFileExtensionsOptionPanel;
            
            manageButton = new JButton(new DialogDisplayAction(FilesOptionPanel.this,
                    extensionsPanel, I18n.tr("Manage File Extensions"),
                    I18n.tr("Manage..."),I18n.tr("Manage file extensions to load")));
            
            add(new JLabel(I18n.tr("Choose the file extensions that belong in each category")), "push");
            add(manageButton);
        }
        
        @Override
        boolean applyOptions() {
            return extensionsPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return extensionsPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            extensionsPanel.initOptions();
        }
    }
    
    private class SaveFoldersPanel extends OptionPanel {

        private ManageSaveFoldersOptionPanel saveFolderPanel;
        private JButton configureButton;
        
        public SaveFoldersPanel() {
            super(I18n.tr("Download Folders"));
            
            saveFolderPanel = manageFoldersOptionPanelFactory.create(new OKDialogAction(), new CancelDialogAction());
            saveFolderPanel.setSize(new Dimension(400,500));
            
            configureButton = new JButton(new DialogDisplayAction(FilesOptionPanel.this, saveFolderPanel, 
                    I18n.tr("Download Folders"),I18n.tr("Configure..."),I18n.tr("Configure where different categories are downloaded")));
            
            add(new JLabel(I18n.tr("Choose where specific categories are downloaded")), "push");
            add(configureButton);
        }
        
        @Override
        boolean applyOptions() {
            return saveFolderPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return saveFolderPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            saveFolderPanel.initOptions();
        }
    }
    
    private class LimeWireStorePanel extends OptionPanel {

        private String currentSaveDirectory;
        private LWSFileNamingOptionPanel storeOptionPanel;
        
        private LabelTextField storePathTextField;
        private JButton browseStorePathButton;
        private JButton configureNamingButton;
        
        public LimeWireStorePanel() {
            super(I18n.tr("LimeWire Store"));
            
            storeOptionPanel = new LWSFileNamingOptionPanel(new OKDialogAction(), new CancelDialogAction());
            storeOptionPanel.setPreferredSize(new Dimension(350, 140));
            
            storePathTextField = new LabelTextField(iconManager);
            storePathTextField.setEditable(false);
            
            BrowseDirectoryAction directoryAction = new BrowseDirectoryAction(FilesOptionPanel.this, storePathTextField);
            storePathTextField.addMouseListener(directoryAction);
            browseStorePathButton = new JButton(directoryAction);
            configureNamingButton = new JButton(new DialogDisplayAction(FilesOptionPanel.this,
                    storeOptionPanel,I18n.tr("Store File Organization"),
                    I18n.tr("Configure File Organization..."),I18n.tr("Configure how files downloaded from the LimeWire Store are organized")));
            
            add(new JLabel(I18n.tr("Save store downloads to:")), "split");
            add(storePathTextField);
            add(browseStorePathButton, "wrap");
            
            add(configureNamingButton);
        }
        
        @Override
        boolean applyOptions() {
            final String save = storePathTextField.getText();
            if(!save.equals(currentSaveDirectory)) {
                try {
                    File saveDir = new File(save);
                    if(!saveDir.isDirectory()) {
                        if (!saveDir.mkdirs())
                            throw new IOException();
                    }
                    SharingSettings.setSaveLWSDirectory(saveDir);
                    currentSaveDirectory = save;
                } catch(Exception ioe) {
                    FocusJOptionPane.showMessageDialog(FilesOptionPanel.this, 
                            I18n.tr("Could not save Store download directory, reverted to old directory"),
                            I18n.tr("Save Folder Error"),
                            JOptionPane.ERROR_MESSAGE);
                    storePathTextField.setText(currentSaveDirectory);
                }
            }
            
            return storeOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return !currentSaveDirectory.equals(storePathTextField.getText()) || 
                    storeOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            storeOptionPanel.initOptions();
            try {
                File file = SharingSettings.getSaveLWSDirectory();
                if (file == null) {
                    file = SharingSettings.DEFAULT_SAVE_LWS_DIR;
                    if(file == null)
                        throw (new FileNotFoundException());
                }
                currentSaveDirectory = file.getCanonicalPath();
                storePathTextField.setText(file.getCanonicalPath());
            } catch (FileNotFoundException fnfe) {
                // simply use the empty string if we could not get the save
                // directory.
                currentSaveDirectory = "";
                storePathTextField.setText("");
            } catch (IOException ioe) {
                currentSaveDirectory = "";
                storePathTextField.setText("");
            }
        }
    }
    
    private class ITunesPanel extends OptionPanel {

        private JCheckBox shareWithITunesCheckBox;
        private JCheckBox requirePassWordCheckBox;
        private JPasswordField passwordField;
        
        public ITunesPanel() {
            super(I18n.tr("iTunes"));
            
            shareWithITunesCheckBox = new JCheckBox(I18n.tr("Share the audio section of My Library on my local network using iTunes"));
            shareWithITunesCheckBox.setContentAreaFilled(false);
            shareWithITunesCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    setPasswordVisible(shareWithITunesCheckBox.isSelected());
                }
            });
            requirePassWordCheckBox = new JCheckBox(I18n.tr("Require password"));
            requirePassWordCheckBox.setContentAreaFilled(false);
            requirePassWordCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    passwordField.setEnabled(requirePassWordCheckBox.isSelected());
                }
            });
            passwordField = new JPasswordField(30);
            passwordField.setEnabled(false);
            
            add(shareWithITunesCheckBox, "split, wrap");
            
            add(requirePassWordCheckBox, "gapleft 25, split");
            add(passwordField);
            
            setPasswordVisible(false);
        }
        
        @Override
        boolean applyOptions() {
            final boolean prevEnabled = DaapSettings.DAAP_ENABLED.getValue();
           
            final boolean prevRequiresPassword = DaapSettings.DAAP_REQUIRES_PASSWORD.getValue();
            final String prevPassword = DaapSettings.DAAP_PASSWORD.getValue();
            
            final boolean requiresPassword = requirePassWordCheckBox.isSelected();
            String password = new String(passwordField.getPassword());
            
            if (password.equals("") && requiresPassword) { 
                FocusJOptionPane.showMessageDialog(FilesOptionPanel.this, 
                        I18n.tr("Daap Password cannot be null, iTunes settings not saved"),
                        I18n.tr("iTunes Error"),
                        JOptionPane.ERROR_MESSAGE);
                
                initOptions();
                return false;
            }
            
            //enable daap setting
            DaapSettings.DAAP_ENABLED.setValue(shareWithITunesCheckBox.isSelected());
            
            //save password value
            if (!DaapSettings.DAAP_PASSWORD.equals(password)) {
                DaapSettings.DAAP_PASSWORD.setValue(password);
            }           
  
            try {               
                if (requiresPassword != prevRequiresPassword || (requiresPassword && !password.equals(prevPassword))) {
                    DaapSettings.DAAP_REQUIRES_PASSWORD.setValue(requiresPassword);
    
                    // A password is required now or password has changed, 
                    // disconnect all users...
                    if (requiresPassword) { 
                        daapManager.disconnectAll();
                    }
                    daapManager.updateService();
    
                }
                
                if (shareWithITunesCheckBox.isSelected()) {              
                    if (!prevEnabled) 
                        daapManager.restart();
                } else if (prevEnabled) {
                    daapManager.stop();
                }
                
            } catch (IOException err) {               
                DaapSettings.DAAP_ENABLED.setValue(prevEnabled);
                DaapSettings.DAAP_REQUIRES_PASSWORD.setValue(prevRequiresPassword);
                DaapSettings.DAAP_PASSWORD.setValue(prevPassword);

                daapManager.stop();
                initOptions();

                FocusJOptionPane.showMessageDialog(FilesOptionPanel.this, 
                        I18n.tr("Could not restart the Daap connection"),
                        I18n.tr("Daap Error"),
                        JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return  DaapSettings.DAAP_ENABLED.getValue() != shareWithITunesCheckBox.isSelected() ||
                    DaapSettings.DAAP_REQUIRES_PASSWORD.getValue() != requirePassWordCheckBox.isSelected() ||
                    DaapSettings.DAAP_PASSWORD.getValue() != requirePassWordCheckBox.getText();
        }

        @Override
        public void initOptions() {
            shareWithITunesCheckBox.setSelected(DaapSettings.DAAP_ENABLED.getValue());

            requirePassWordCheckBox.setSelected(DaapSettings.DAAP_REQUIRES_PASSWORD.getValue());
            if(requirePassWordCheckBox.isSelected()) {
                passwordField.setText(DaapSettings.DAAP_PASSWORD.getValue());
            }
            
            setPasswordVisible(shareWithITunesCheckBox.isSelected());
        }
        
        private void setPasswordVisible(boolean value) {
            requirePassWordCheckBox.setVisible(value);
            passwordField.setVisible(value);
        }
    }
}
