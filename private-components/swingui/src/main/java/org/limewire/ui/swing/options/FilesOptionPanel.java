package org.limewire.ui.swing.options;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.miginfocom.swing.MigLayout;

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
import com.google.inject.Provider;

/**
 * Files Option View.
 */
public class FilesOptionPanel extends OptionPanel {
    
    private final ManageFileExtensionsOptionPanel manageFileExtensionsOptionPanel;
    private final Provider<IconManager> iconManager;
    
    private ManageExtensionsPanel manageExtensionsPanel;
    private LimeWireStorePanel limeWireStorePanel;
    
    @Inject
    FilesOptionPanel(ManageFileExtensionsOptionPanel manageFileExtensionsOptionPanel,
            Provider<IconManager> iconManager) { 
        
        this.manageFileExtensionsOptionPanel = manageFileExtensionsOptionPanel;
        this.iconManager = iconManager;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
    
        setOpaque(false);
        
        add(getManageExtensionsPanel(), "pushx, growx");
        add(getLimeWireStorePanel(), "pushx, growx");
    }
    
    private OptionPanel getManageExtensionsPanel() {
        if(manageExtensionsPanel == null) {
            manageExtensionsPanel = new ManageExtensionsPanel();
        }
        return manageExtensionsPanel;
    }
    
    private OptionPanel getLimeWireStorePanel() {
        if(limeWireStorePanel == null) {
            limeWireStorePanel = new LimeWireStorePanel();
        }
        return limeWireStorePanel;
    } 
    
    @Override
    boolean applyOptions() {
        boolean restart = getManageExtensionsPanel().applyOptions();
        restart |= getLimeWireStorePanel().applyOptions();

        return restart;
    }

    @Override
    boolean hasChanged() {
        return getManageExtensionsPanel().hasChanged() || 
                getLimeWireStorePanel().hasChanged();

    }

    @Override
    public void initOptions() {
        getManageExtensionsPanel().initOptions();
        getLimeWireStorePanel().initOptions();
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
    
    private class LimeWireStorePanel extends OptionPanel {

        private String currentSaveDirectory;
        private LWSFileNamingOptionPanel storeOptionPanel;
        
        private LabelTextField storePathTextField;
        private JButton browseStorePathButton;
        private JButton configureNamingButton;
        
        public LimeWireStorePanel() {
            super(I18n.tr("LimeWire Store"));
            
            storeOptionPanel = new LWSFileNamingOptionPanel(new OKDialogAction(), new CancelDialogAction());

            storePathTextField = new LabelTextField(iconManager);
            storePathTextField.setEditable(false);
            
            BrowseDirectoryAction directoryAction = new BrowseDirectoryAction(FilesOptionPanel.this, storePathTextField);
            storePathTextField.addMouseListener(directoryAction);
            browseStorePathButton = new JButton(directoryAction);
            configureNamingButton = new JButton(new DialogDisplayAction(FilesOptionPanel.this,
                    storeOptionPanel,I18n.tr("Store File Organization"),
                    I18n.tr("Configure File Organization..."),I18n.tr("Configure how files downloaded from the LimeWire Store are organized")));
            
            add(new JLabel(I18n.tr("Save Store downloads to:")), "split");
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
}
