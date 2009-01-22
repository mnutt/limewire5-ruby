package org.limewire.ui.swing.library;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MetaDataManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class LocalFileItemPropertiesFactory implements PropertiesFactory<LocalFileItem> {
    private final ThumbnailManager thumbnailManager;
    private final MetaDataManager metaDataManager;
    private final Collection<Friend> allFriends;
    private final ShareListManager shareListManager;
    private final DialogParam dialogParam;

    @Inject
    public LocalFileItemPropertiesFactory(ThumbnailManager thumbnailManager,
            IconManager iconManager,
            MetaDataManager metaDataManager, @Named("known") Collection<Friend> allFriends, 
            ShareListManager shareListManager,
            DialogParam dialogParam) {
        this.thumbnailManager = thumbnailManager;
        this.metaDataManager = metaDataManager;
        this.allFriends = allFriends;
        this.shareListManager = shareListManager;
        this.dialogParam = dialogParam;
    }

    @Override
    public Properties<LocalFileItem> newProperties() {
        return new LocalFileItemProperties(thumbnailManager, metaDataManager, allFriends,
                shareListManager, dialogParam);
    }

    private static class LocalFileItemProperties extends AbstractFileItemDialog implements
            Properties<LocalFileItem> {
        private final ThumbnailManager thumbnailManager;
        private final CategoryIconManager categoryIconManager;
        private final MetaDataManager metaDataManager;
        private final Map<FilePropertyKey, Object> changedProps = new HashMap<FilePropertyKey, Object>();
        private final Collection<Friend> allFriends;
        private final ShareListManager shareListManager;
        private final LibraryNavigator libraryNavigator;
        private final JPanel sharing = new JPanel();
        private LocalFileItem displayedItem;
        
        @Resource private Icon removeIcon;
        @Resource private Icon removeIconRollover;
        @Resource private Icon removeIconPressed;

        private List<Friend> unsharedFriendList = new ArrayList<Friend>();

        private LocalFileItemProperties(ThumbnailManager thumbnailManager,
                MetaDataManager metaDataManager, Collection<Friend> allFriends,
                ShareListManager shareListManager, DialogParam dialogParam) {
            super(dialogParam);
            
            GuiUtils.assignResources(this);
            
            this.libraryNavigator = dialogParam.getLibraryNavigator();
            this.thumbnailManager = thumbnailManager;
            this.categoryIconManager = dialogParam.getCategoryIconManager();
            this.metaDataManager = metaDataManager;
            this.allFriends = allFriends;
            this.shareListManager = shareListManager;
        }

        @Override
        protected void commit() {
            if (changedProps.size() != 0) {
                for (FilePropertyKey key : changedProps.keySet()) {
                    displayedItem.setProperty(key, changedProps.get(key));
                }
                metaDataManager.save(displayedItem);
            }      
            
            if(unsharedFriendList.size() > 0){
                for(Friend friend : unsharedFriendList) {
                    shareListManager.getOrCreateFriendShareList(friend).removeFile(displayedItem.getFile());
                }
                //ensure table values are updated
                GuiUtils.getMainFrame().repaint();
            }
        }

        private void connectTextDocumentListener(final JTextComponent comp, final FilePropertyKey key) {
            comp.getDocument().addUndoableEditListener(new UndoableEditListener() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    changedProps.put(key, comp.getText());
                }
            });
        }

        private void connectComboSelectionListener(final JComboBox comp, final FilePropertyKey key) {
            comp.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    changedProps.put(key, comp.getSelectedItem().toString());
                }
            });
        }

        @Override
        public void showProperties(final LocalFileItem propertiable) {
            this.displayedItem = propertiable;
            
            icon.setIcon(getIcon(propertiable));
            populateCommonFields(propertiable);
            populateCopyToClipboard(propertiable);
            fileLocation.setText(propertiable.getFile().getAbsolutePath());
            
            List<Friend> sharedWithList = getSharedWithList(propertiable);
            boolean isShared = sharedWithList.size() > 0;
            
            if (isShared) {                
                sharing.setLayout(new MigLayout("fillx, nogrid, nocache, gap 0! 0!, insets 0 0 0 0"));
                for(Friend friend : sharedWithList) {
                    final Friend shareFriend = friend;
                    final JLabel friendLabel = new JLabel(friend.getRenderName());
                    final JButton friendButton = new IconButton(removeIcon, removeIconRollover, removeIconPressed);
                    friendButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            unsharedFriendList.add(shareFriend);
                            sharing.remove(friendLabel);
                            sharing.remove(friendButton);
                            //make sure the friend actually disappears
                            mainPanel.revalidate();
                            //make sure we don't get stuck on the hand cursor
                            setCursor(Cursor.getDefaultCursor());
                        }
                    });
                    
                    sharing.add(friendButton);
                    sharing.add(friendLabel, "gapright 20, wrap");
                }
                JScrollPane scroll = new JScrollPane(sharing);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                mainPanel.add(box(tr("Sharing with"), scroll), "grow, cell 0 3, wmin 200");
            }
            
            location.setLayout(new MigLayout("", "[]10[]15[]", isShared ? "[top][]" : "[top]"));
            location.add(fileLocation, "gapbottom 5,push");
            location.add(locateOnDisk);
            location.add(locateInLibrary, isShared ? "wrap" : "");

            locateOnDisk.setAction(new AbstractAction(I18n.tr("locate on disk")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NativeLaunchUtils.launchExplorer(propertiable.getFile());
                }
            });

            locateInLibrary.setAction(new AbstractAction(I18n.tr("locate in library")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    libraryNavigator.selectInLibrary(propertiable.getFile(), propertiable.getCategory());
                }
            });
            
            connectComboSelectionListener(genre, FilePropertyKey.GENRE);
            connectComboSelectionListener(rating, FilePropertyKey.RATING);
            connectComboSelectionListener(platform, FilePropertyKey.PLATFORM);
            connectTextDocumentListener(album, FilePropertyKey.ALBUM);
            connectTextDocumentListener(author, FilePropertyKey.AUTHOR);
            connectTextDocumentListener(artist, FilePropertyKey.AUTHOR);
            connectTextDocumentListener(company, FilePropertyKey.COMPANY);
            connectTextDocumentListener(year, FilePropertyKey.YEAR);
            connectTextDocumentListener(title, FilePropertyKey.TITLE);
            connectTextDocumentListener(track, FilePropertyKey.TRACK_NUMBER);
            connectTextDocumentListener(description, FilePropertyKey.DESCRIPTION);
            
            showDialog(propertiable.getFileName(), propertiable.getCategory());
        }
        
        private List<Friend> getSharedWithList(LocalFileItem fileItem) {
            List<Friend> sharedWith = new ArrayList<Friend>();
            for(Friend friend : allFriends) {       
                boolean isShared = shareListManager.getOrCreateFriendShareList(friend).contains(fileItem.getFile());
                if (isShared) {
                    sharedWith.add(friend);
                }
            }
            return sharedWith;
        }

        private Icon getIcon(LocalFileItem propertiable) {
            switch (propertiable.getCategory()) {
            case IMAGE:
                return thumbnailManager.getThumbnailForFile(propertiable.getFile());
            case AUDIO:
            case VIDEO:
            case PROGRAM:
                return categoryIconManager.getIcon(propertiable.getCategory());
            default: // OTHER, DOCUMENT
                return iconManager.getIconForFile(propertiable.getFile());
            }
        }
    }
}
