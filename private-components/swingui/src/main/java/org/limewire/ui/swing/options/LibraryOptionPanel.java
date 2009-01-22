package org.limewire.ui.swing.options;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
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
import org.limewire.ui.swing.library.manager.LibraryManagerItem;
import org.limewire.ui.swing.library.manager.LibraryManagerItemImpl;
import org.limewire.ui.swing.library.manager.LibraryManagerModel;
import org.limewire.ui.swing.library.manager.LibraryManagerTreeTable;
import org.limewire.ui.swing.library.manager.LibraryTreeTableContainer;
import org.limewire.ui.swing.library.manager.NoChildrenLibraryManagerItem;
import org.limewire.ui.swing.library.manager.RootLibraryManagerItem;
import org.limewire.ui.swing.settings.SwingUiSettings;
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
    private final Provider<ShareListManager> shareListManager;
    private final Collection<Friend> knownFriends;
    private final LibraryManagerOptionPanel libraryManagerPanel;    
    private final ShareCategoryPanel shareCategoryPanel;
    private final UsePlayerPanel playerPanel;
    
    @Inject
    public LibraryOptionPanel(LibraryManager libraryManager, 
            Provider<ShareListManager> shareListManager,
            IconManager iconManager,
            @Named("known") Collection<Friend> knownFriends) {
        this.iconManager = iconManager;
        this.shareListManager = shareListManager;
        this.knownFriends = knownFriends;
        this.libraryManagerPanel = new LibraryManagerOptionPanel(libraryManager.getLibraryData()); 
        this.shareCategoryPanel = new ShareCategoryPanel();
        this.playerPanel = new UsePlayerPanel();
        
        setLayout(new MigLayout("insets 15, fillx, wrap", "", ""));
        
        add(libraryManagerPanel, "pushx, growx");
        add(shareCategoryPanel, "pushx, growx");
        add(playerPanel, "pushx, growx");
        
        libraryManagerPanel.syncWith(shareCategoryPanel);
    }

    @Override
    boolean applyOptions() {
        return libraryManagerPanel.applyOptions() ||
               shareCategoryPanel.applyOptions() ||
               playerPanel.applyOptions();
    }

    @Override
    boolean hasChanged() {
        return libraryManagerPanel.hasChanged() ||
               shareCategoryPanel.hasChanged() ||
               playerPanel.hasChanged();
    }

    @Override
    public void initOptions() {
        libraryManagerPanel.initOptions();
        shareCategoryPanel.initOptions();
        playerPanel.initOptions();
    }    
    
    /**
     * Share Category Panel
     */
    private class ShareCategoryPanel extends OptionPanel {

        private final JRadioButton shareSnapshot;
        private final JRadioButton shareCollection;
        private final JPanel buttonPanel; 
        private final ButtonGroup buttonGroup;

        public ShareCategoryPanel() {
            super(I18n.tr("Sharing Categories"));
            setLayout(new MigLayout("gap 0, fill"));

            
            shareSnapshot = new JRadioButton(I18n.tr("Only share files in that category at that point"));
            shareSnapshot.setContentAreaFilled(false);
            shareCollection = new JRadioButton(I18n.tr("Share my entire collection, including new files that automatically get added to My Library"));
            shareCollection.setContentAreaFilled(false);
            
            buttonGroup = new ButtonGroup();
            buttonGroup.add(shareSnapshot);
            buttonGroup.add(shareCollection);
            
            buttonPanel = new JXPanel();
            buttonPanel.setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, novisualpadding"));//new VerticalLayout(2));
            buttonPanel.add(new JLabel(I18n.tr("When I share a category:")), "wrap");
            buttonPanel.add(shareSnapshot,"gapleft 5, gaptop 8, wrap");
            buttonPanel.add(shareCollection, "gapleft 5");
            
            add(new JLabel(I18n.tr("Choose what happens when I share a category")), "alignx left");
            add(new JButton(new AbstractAction(I18n.tr("Configure...")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean snapC = shareSnapshot.isSelected();
                    boolean collC = shareCollection.isSelected();
                    int result = FocusJOptionPane.showConfirmDialog(ShareCategoryPanel.this,
                            buttonPanel, I18n.tr("Sharing Categories"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if(result != JOptionPane.OK_OPTION) {
                        buttonGroup.setSelected(shareSnapshot.getModel(), snapC);
                        buttonGroup.setSelected(shareCollection.getModel(), collC);
                    }
                }
            }), "alignx right, gapbefore push, wrap");
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
        
        private LibraryTreeTableContainer treeTableContainer;
        
        private final ManualImportPanel manualImportPanel;
        
        public LibraryManagerOptionPanel(LibraryData libraryData) {            
            this.libraryData = libraryData;
            this.manualImportPanel = new ManualImportPanel(libraryData);
            
            setBorder(BorderFactory.createTitledBorder(""));
            setLayout(new MigLayout("gap 0, fill"));            
            setOpaque(false);
            
            createComponents();
            
            add(new JLabel(I18n.tr("Automatically add the following categories from folders below to My Library:")), "alignx left, gapbottom 5, wrap");
            add(checkBoxes, "alignx center, gapbottom 5, wrap");
            add(treeTableContainer, "grow, gapbottom 2, wrap");
            add(addFolderButton, "gapbefore push, alignx right, wrap");
            add(manualImportPanel, "growx, hidemode 2");
        }
        
        void syncWith(ShareCategoryPanel shareCategoryPanel) {
            shareCategoryPanel.shareCollection.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(((AbstractButton)e.getSource()).isSelected()) {
                        treeTableContainer.setAdditionalText(I18n.tr("*Adding these folders will automatically share any collections you shared"));
                    }
                }
            });
            shareCategoryPanel.shareSnapshot.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(((AbstractButton)e.getSource()).isSelected()) {
                        treeTableContainer.setAdditionalText(I18n.tr("*Adding these folders will not automatically share your files"));
                    }
                }
            });
        }

        private void createComponents() {
            treeTableContainer = new LibraryTreeTableContainer(iconManager, libraryData);
            addFolderButton = new JButton(new AddDirectoryAction(this));

            checkBoxes = new HorizonalCheckBoxListPanel<Category>(Category.getCategoriesInOrder());
            programCheckBox = checkBoxes.getCheckBox(Category.PROGRAM);
            
            LibrarySettings.ALLOW_PROGRAMS.addSettingListener( new SettingListener() {
                @Override
                 public void settingChanged(SettingEvent evt) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            enablePrograms(LibrarySettings.ALLOW_PROGRAMS.getValue());                            
                        }
                    });
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
            LibraryManagerModel model = treeTableContainer.getTable().getLibraryModel();
            Collection<File> manage = model.getRootChildrenAsFiles();
            Collection<File> exclude = model.getAllExcludedSubfolders();
            libraryData.setManagedOptions(manage, exclude, getManagedCategories());
            
            return manualImportPanel.applyOptions();
        }

        private Collection<Category> getManagedCategories() {
            return checkBoxes.getSelected();
        }

        @Override
        boolean hasChanged() {
            LibraryManagerModel model = treeTableContainer.getTable().getLibraryModel();
            Collection<File> manage = model.getRootChildrenAsFiles();
            Collection<File> exclude = model.getAllExcludedSubfolders();
            Collection<Category> categories = getManagedCategories();
            return !manage.equals(libraryData.getDirectoriesToManageRecursively())
                || !exclude.equals(libraryData.getDirectoriesToExcludeFromManaging())
                || !categories.equals(libraryData.getManagedCategories())
                || manualImportPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            RootLibraryManagerItem root = new RootLibraryManagerItem(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
            for(File file : libraryData.getDirectoriesToManageRecursively()) {
                root.addChild(new LibraryManagerItemImpl(root, libraryData, file, false));
            }

            treeTableContainer.getTable().setTreeTableModel(new LibraryManagerModel(root));
            
            Collection<Category> categories = libraryData.getManagedCategories();
            checkBoxes.setSelected(categories);
            
            programCheckBox.setEnabled(libraryData.isProgramManagingAllowed());
            
            manualImportPanel.initOptions();
            
        }
        
        private class AddDirectoryAction extends AbstractAction {

            private Container parent;
            
            public AddDirectoryAction(Container parent) {
                this.parent = parent;
                
                putValue(Action.NAME, I18n.tr("Add Folder..."));
                putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a folder to automatically add to My Library"));
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                File folder = FileChooser.getInputDirectory(parent);            
                if(folder == null) {
                    return;
                } else {
                    if(libraryData.isDirectoryAllowed(folder)) {
                        treeTableContainer.getTable().addDirectory(folder);
                    } else {
                        FocusJOptionPane.showMessageDialog(LibraryManagerOptionPanel.this, I18n.tr(
                                "You selected: {0}\n\nLimeWire cannot manage "
                                        + "this folder because it is either not a folder "
                                        + "or cannot be read.\n\nPlease select another "
                                        + "folder to manage.", folder), I18n
                                .tr("Library Manager Error"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }    
    }
    
    /** The manual import panel */
    private class ManualImportPanel extends OptionPanel {
        private final Collection<File> initialList;
        private final LibraryData libraryData;
        private final JPanel manPanel; 
        private final JScrollPane scroller;
        private LibraryManagerTreeTable treeTable;
        
        public ManualImportPanel(LibraryData libraryData) {
            this.libraryData = libraryData;
            this.initialList = new HashSet<File>();
            
            treeTable = new LibraryManagerTreeTable(iconManager, libraryData);
            treeTable.setShowsRootHandles(false);
            treeTable.putClientProperty("JTree.lineStyle", "None");
            
            setLayout(new MigLayout("insets 0, gap 0, fill"));            
            setOpaque(false);
            
            scroller = new JScrollPane(treeTable);
            scroller.setPreferredSize(new Dimension(500, 300));
            
            manPanel = new JXPanel();
            manPanel.setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, novisualpadding"));//new VerticalLayout(2));
            manPanel.add(new JLabel(I18n.tr("At some point, you manually added files from the following folders into My Library:")), "wrap");
            manPanel.add(scroller, "gaptop 8");
            
            add(Line.createHorizontalLine(Color.LIGHT_GRAY), "gapbefore 30, gapafter 30, growx, gaptop 10, gapbottom 10, wrap");
            add(new JLabel(I18n.tr("At some point, you manually added files from some folders into My Library.")), "gapbottom 5, split");
            add(new JButton(new AbstractAction(I18n.tr("View Folders...")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LibraryManagerModel model = treeTable.getLibraryModel();
                    List<LibraryManagerItem> children = new ArrayList<LibraryManagerItem>(model.getRoot().getChildren());
                    int result = FocusJOptionPane.showConfirmDialog(ManualImportPanel.this,
                            manPanel, I18n.tr("Manually Added Folders"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                   
                    if(result != JOptionPane.OK_OPTION) {
                        model.setRootChildren(children);
                    }
                }
            }), "alignx left, gapafter push, wrap");
            
            
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
    
    /** Do you want to use the LW player? */
    private class UsePlayerPanel extends OptionPanel {

        private JCheckBox useLimeWirePlayer;

        public UsePlayerPanel() {
            super("");
            setBorder(BorderFactory.createEmptyBorder());
            setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, fill"));

            useLimeWirePlayer = new JCheckBox(I18n.tr("Use the LimeWire player when I play audio files"));
            useLimeWirePlayer.setOpaque(false);
        
            add(useLimeWirePlayer);
        }
        
        @Override
        boolean applyOptions() {
            SwingUiSettings.PLAYER_ENABLED.setValue(useLimeWirePlayer.isSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            return useLimeWirePlayer.isSelected() != SwingUiSettings.PLAYER_ENABLED.getValue();
        }

        @Override
        public void initOptions() {
            useLimeWirePlayer.setSelected(SwingUiSettings.PLAYER_ENABLED.getValue());
        }
    }


}
