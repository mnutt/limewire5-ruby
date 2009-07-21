package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.filter.Filter;
import org.limewire.setting.Setting;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.options.OptionPanelStateManager.SettingChangedListener;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class UnsafeTypeOptionPanel extends OptionPanel {

    private JCheckBox programCheckBox;
    private JCheckBox documentCheckBox;
    private JButton okButton;

    private final LibraryManager libraryManager;
    private final SharedFileListManager shareListManager;
    private final OptionPanelStateManager manager;
   
    private final Map<Setting, JCheckBox> settingMap;
    
    @Inject
    public UnsafeTypeOptionPanel(LibraryManager libraryManager,
            SharedFileListManager shareListManager,
            UnsafeTypeOptionPanelStateManager manager) {
        
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.manager = manager;

        setLayout(new MigLayout("gapy 10"));

        programCheckBox = new JCheckBox(I18n.tr("Allow me to search for and share Programs with anyone"));
        programCheckBox.setContentAreaFilled(false);
        documentCheckBox = new JCheckBox(I18n.tr("Allow me to add Documents to my Public Shared list and share them with the world"));
        documentCheckBox.setContentAreaFilled(false);
        okButton = new JButton(new OKDialogAction());
    
        settingMap = new HashMap<Setting, JCheckBox>();
        settingMap.put(LibrarySettings.ALLOW_PROGRAMS, programCheckBox);
        settingMap.put(LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING, documentCheckBox);
        
        add(new MultiLineLabel(I18n.tr("Enabling these settings makes you more prone to viruses and accidently sharing private documents. We strongly recommend you don't enable them."), 600), "span 2, wrap");
        
        add(programCheckBox, "split, gapleft 25, wrap");
        add(documentCheckBox, "split, gapbottom 15, gapleft 25, wrap");
        
        //add(new JLabel(I18n.tr("By default, LimeWire allows you to share documents with your friends")), "push");
        add(okButton, "tag ok, gapbefore push");
        
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                savePendingSettings();
            }
        });
    }
    
    @Inject
    public void register() {
        manager.addSettingChangedListener(new SettingChangedListener() {
            @Override
            public void settingChanged(Setting setting) {
                settingMap.get(setting).setSelected((Boolean)UnsafeTypeOptionPanel.this.manager.getValue(setting));
            }
        });
    }
        
    @Override
    boolean applyOptions() {
        manager.saveSettings();

        if(!programCheckBox.isSelected()) {
        	Collection<Category> managedCategories = libraryManager.getLibraryData().getManagedCategories();
        	managedCategories.remove(Category.PROGRAM);
        	libraryManager.getLibraryData().setCategoriesToIncludeWhenAddingFolders(managedCategories);
            libraryManager.getLibraryManagedList().removeFiles(new Filter<LocalFileItem>() {
               @Override
                public boolean allow(LocalFileItem localFileItem) {
                    return localFileItem.getCategory() == Category.PROGRAM;
                } 
            });
        }
        
        if (!documentCheckBox.isSelected()) {
            shareListManager.removeDocumentsFromPublicLists();
        }
        return false;
    }

    @Override
    boolean hasChanged() {
        return manager.hasPendingChanges();
    }

    @Override
    public void initOptions() {
        for ( Setting setting : settingMap.keySet() ) {
            settingMap.get(setting).setSelected((Boolean)manager.getValue(setting));
        }
    }
    
    private void savePendingSettings() {
        for ( Setting setting : settingMap.keySet() ) {
            manager.setValue(setting, settingMap.get(setting).isSelected());
        }
    }
}
