package org.limewire.ui.swing.options;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HorizonalCheckBoxListPanel;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.library.manager.LibraryManagerItemImpl;
import org.limewire.ui.swing.library.manager.LibraryManagerModel;
import org.limewire.ui.swing.library.manager.LibraryManagerTreeTable;
import org.limewire.ui.swing.library.manager.NoChildrenLibraryManagerItem;
import org.limewire.ui.swing.library.manager.RootLibraryManagerItem;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.wizard.AutoDirectoryManageConfig;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/** Library Option View */
public class LibraryOptionPanel extends OptionPanel {

    private final IconManager iconManager;    
    private final LibraryManager libraryManager;
    private final Provider<ShareListManager> shareListManager;
    private final Collection<Friend> knownFriends;

    private ManualImportPanel manualImportPanel;
    private LibraryManagerOptionPanel libraryManagerPanel;    
    private ShareCategoryPanel shareCategoryPanel;
    
    
    @Inject
    public LibraryOptionPanel(LibraryManager libraryManager, 
            Provider<ShareListManager> shareListManager,
            IconManager iconManager,
            @Named("known") Collection<Friend> knownFriends) {
        this.iconManager = iconManager;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.knownFriends = knownFriends;
        
        setLayout(new MigLayout("insets 15, fillx, wrap", "", ""));
        
        add(getLibraryManagerPanel(), "pushx, growx");
        add(getManualImportPanel(), "pushx, growx, hidemode 2");
        add(getShareCategoryPanel(), "pushx, growx");
    }
    
    private LibraryManagerOptionPanel getLibraryManagerPanel() {
        if(libraryManagerPanel == null) {
            libraryManagerPanel = new LibraryManagerOptionPanel(libraryManager.getLibraryData());
        }
        return libraryManagerPanel;
    }
    
    private OptionPanel getShareCategoryPanel() {
        if (shareCategoryPanel == null) {
            shareCategoryPanel = new ShareCategoryPanel();
        }
        return shareCategoryPanel;
    }
    
    private ManualImportPanel getManualImportPanel() {
        if(manualImportPanel == null) {
            manualImportPanel = new ManualImportPanel(libraryManager.getLibraryData());
        }
        return manualImportPanel;
    }

    @Override
    boolean applyOptions() {
        return getLibraryManagerPanel().applyOptions() ||
               getShareCategoryPanel().applyOptions() ||
               getManualImportPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getLibraryManagerPanel().hasChanged() ||
               getShareCategoryPanel().hasChanged() ||
               getManualImportPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getLibraryManagerPanel().initOptions();
        getShareCategoryPanel().initOptions();
        getManualImportPanel().initOptions();
    }
    
    
    /**
     * Share Category Panel
     */
    private class ShareCategoryPanel extends OptionPanel {

        private JRadioButton shareSnapshot;

        private JRadioButton shareCollection;

        private ButtonGroup buttonGroup;

        public ShareCategoryPanel() {
            super(I18n.tr("Sharing Categories"));
            setLayout(new MigLayout("", "", "[][][]"));

            shareSnapshot = new JRadioButton(I18n.tr("Only share files in that category at that point"));
            shareSnapshot.setContentAreaFilled(false);
            shareCollection = new JRadioButton(I18n.tr("Share my entire collection, including new files that automatically get added to My Library"));
            shareCollection.setContentAreaFilled(false);
            
            buttonGroup = new ButtonGroup();
            buttonGroup.add(shareSnapshot);
            buttonGroup.add(shareCollection);
            
            add(new JLabel(I18n.tr("When I share a category:")), "wrap");
            add(shareSnapshot, "gapleft 15, wrap");
            add(shareCollection, "gapleft 15");
        }
        
        @Override
        boolean applyOptions() {
            LibrarySettings.SNAPSHOT_SHARING_ENABLED.setValue(shareSnapshot.isSelected());
            // revert to defaults for collection sharing if true
            if(shareSnapshot.isSelected()) {
                ShareListManager shareManager = shareListManager.get();
                // Set friends & gnutella explicitly, so events fire properly.
                shareManager.getGnutellaShareList().setCategoryAutomaticallyAdded(Category.AUDIO, false);
                shareManager.getGnutellaShareList().setCategoryAutomaticallyAdded(Category.VIDEO, false);
                shareManager.getGnutellaShareList().setCategoryAutomaticallyAdded(Category.IMAGE, false);
                for(Friend friend : knownFriends) {
                    FriendFileList fileList = shareManager.getFriendShareList(friend);
                    if(fileList != null) {                    
                        fileList.setCategoryAutomaticallyAdded(Category.AUDIO, false);
                        fileList.setCategoryAutomaticallyAdded(Category.VIDEO, false);
                        fileList.setCategoryAutomaticallyAdded(Category.IMAGE, false);
                    }
                }
                // clear everyone else, so if you're not signed on,
                // when you sign on it doesn't continue auto-sharing.
                LibrarySettings.SHARE_NEW_IMAGES_ALWAYS.revertToDefault();
                LibrarySettings.SHARE_NEW_AUDIO_ALWAYS.revertToDefault();
                LibrarySettings.SHARE_NEW_VIDEO_ALWAYS.revertToDefault();
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return shareSnapshot.isSelected() != LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue();
        }

        @Override
        public void initOptions() {
            if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue())
                shareSnapshot.setSelected(true);
            else
                shareCollection.setSelected(true);
        }
    }
    
    /**
     * Library Manager Panel
     */
    public class LibraryManagerOptionPanel extends OptionPanel {

        private final LibraryData libraryData;
        
        private JButton addFolderButton;
        private HorizonalCheckBoxListPanel<Category> checkBoxes;
        private JCheckBox programCheckBox;
        
        private LibraryManagerTreeTable treeTable;
        
        public LibraryManagerOptionPanel(LibraryData libraryData) {
            this.libraryData = libraryData;
            
            setLayout(new MigLayout("insets 0, gap 0, fill"));
            
            setOpaque(false);
            
            createComponents();
            
            add(new JLabel(I18n.tr("LimeWire will watch the following folders...")), "gapbottom 5, wrap");
        
            add(new JScrollPane(treeTable), "growy, growx, gapbottom 5, wrap");            
            add(new JLabel(I18n.tr("and add the following categories to My Library:")), "split, alignx left");
            add(addFolderButton, "gapbefore push, alignx right, wrap");
            add(checkBoxes, "gapbefore 10, wrap");            
            add(new MultiLineLabel(I18n.tr("Scanning these folders into your Library will not automatically share your files unless you explicitly choose to share a collection.")), "growx");
        }
        
        private void createComponents() {
            treeTable = new LibraryManagerTreeTable(iconManager, libraryData);
            addFolderButton = new JButton(new AddDirectoryAction(this));

            checkBoxes = new HorizonalCheckBoxListPanel<Category>(Category.getCategoriesInOrder());
            programCheckBox = checkBoxes.getCheckBox(Category.PROGRAM);
            
            LibrarySettings.ALLOW_PROGRAMS.addSettingListener( new SettingListener() {
                @Override
                 public void settingChanged(SettingEvent evt) {
                    enablePrograms(LibrarySettings.ALLOW_PROGRAMS.getValue());
                }
             });
        }
        
        
        public void enablePrograms(boolean enable) {
            if(enable) {
                programCheckBox.setEnabled(true);
            } else {
                programCheckBox.setSelected(false);
                programCheckBox.setEnabled(false);
            }
        }
      
        @Override
        public boolean applyOptions() {
            LibraryManagerModel model = treeTable.getLibraryModel();
            Collection<File> manage = model.getRootChildrenAsFiles();
            Collection<File> exclude = model.getAllExcludedSubfolders();
            libraryData.setManagedOptions(manage, exclude, getManagedCategories());
            return false;
        }

        private Collection<Category> getManagedCategories() {
            return checkBoxes.getSelected();
        }

        @Override
        boolean hasChanged() {
            LibraryManagerModel model = treeTable.getLibraryModel();
            Collection<File> manage = model.getRootChildrenAsFiles();
            Collection<File> exclude = model.getAllExcludedSubfolders();
            Collection<Category> categories = getManagedCategories();
            return !manage.equals(libraryData.getDirectoriesToManageRecursively())
                || !exclude.equals(libraryData.getDirectoriesToExcludeFromManaging())
                || !categories.equals(libraryData.getManagedCategories());
        }

        @Override
        public void initOptions() {
            RootLibraryManagerItem root = new RootLibraryManagerItem(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
            for(File file : libraryData.getDirectoriesToManageRecursively()) {
                root.addChild(new LibraryManagerItemImpl(root, libraryData, file, false));
            }

            treeTable.setTreeTableModel(new LibraryManagerModel(root));
            
            Collection<Category> categories = libraryData.getManagedCategories();
            checkBoxes.setSelected(categories);
            
            programCheckBox.setEnabled(libraryData.isProgramManagingAllowed());
            
        }
        
        private class AddDirectoryAction extends AbstractAction {

            private Container parent;
            
            public AddDirectoryAction(Container parent) {
                this.parent = parent;
                
                putValue(Action.NAME, I18n.tr("Add New Folder..."));
                putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a folder to automatically scan for changes"));
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                File folder = FileChooser.getInputDirectory(parent);            
                if(folder == null) {
                    return;
                } else {
                    if(libraryData.isDirectoryAllowed(folder)) {
                        treeTable.addDirectory(folder);
                    } else {
                    FocusJOptionPane.showMessageDialog(LibraryManagerOptionPanel.this,
                            I18n.tr("You selected: {0}\n\nLimeWire cannot manage " +
                                    "this folder because it is either not a folder " +
                                    "or cannot be read.\n\nPlease select another " +
                                    "folder to manage.", folder),
                            I18n.tr("Library Manager Error"),
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }    
    }
    
    /**
     * The manual import panel
     */
    public class ManualImportPanel extends OptionPanel {

        private final Collection<File> initialList;
        private final LibraryData libraryData;       
        private LibraryManagerTreeTable treeTable;
        
        public ManualImportPanel(LibraryData libraryData) {
            this.libraryData = libraryData;
            this.initialList = new HashSet<File>();
            
            treeTable = new LibraryManagerTreeTable(iconManager, libraryData);
            treeTable.setShowsRootHandles(false);
            treeTable.putClientProperty("JTree.lineStyle", "None");
            
            setLayout(new MigLayout("insets 0, gap 0, fill"));
            
            setOpaque(false);
            
            add(Line.createHorizontalLine(), "growx, gapbottom 4, wrap");
            add(new JLabel(I18n.tr("At some point, you manually imported files from some folders into My Library.")), "gapbottom 5, split");
            add(new JButton(new AbstractAction(I18n.tr("View Folders...")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JScrollPane scroller = new JScrollPane(treeTable);
                    scroller.setPreferredSize(new Dimension(500, 300));
                    FocusJOptionPane.showMessageDialog(ManualImportPanel.this,
                            scroller, I18n.tr("Manage Folders"), JOptionPane.PLAIN_MESSAGE);
                }
            }), "alignx left, gapafter push");
            
            
        }

        @Override
        public boolean applyOptions() {
            Collection<File> current = new HashSet<File>(initialList);
            current.removeAll(treeTable.getLibraryModel().getRootChildrenAsFiles());
            libraryData.removeFolders(current);
            return false;
        }

        @Override
        boolean hasChanged() {
            return !initialList.equals(treeTable.getLibraryModel().getRootChildrenAsFiles());
        }

        @Override
        public void initOptions() {
            initialList.clear();
            RootLibraryManagerItem root = new RootLibraryManagerItem(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
            for(File file : libraryData.getDirectoriesWithImportedFiles()) {
                initialList.add(file);
                root.addChild(new NoChildrenLibraryManagerItem(root, file));
            }

            treeTable.setTreeTableModel(new LibraryManagerModel(root));
            setVisible(!initialList.isEmpty());
        }
    }

}
