package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.Catalog;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.menu.actions.DeleteAction;
import org.limewire.ui.swing.library.table.menu.actions.LaunchFileAction;
import org.limewire.ui.swing.library.table.menu.actions.LocateFileAction;
import org.limewire.ui.swing.library.table.menu.actions.PlayAction;
import org.limewire.ui.swing.library.table.menu.actions.RemoveAction;
import org.limewire.ui.swing.library.table.menu.actions.ViewFileInfoAction;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Popup menu implementation for Share views 
 */
public class ShareLibraryPopupMenu extends JPopupMenu {

    private List<LocalFileItem> fileItems;

    private final LibraryManager libraryManager;

    private final Category category;

    private final PropertiesFactory<LocalFileItem> propertiesFactory;

    private final LocalFileList friendFileList;
    
    private final LibraryNavigator libraryNavigator;

    public ShareLibraryPopupMenu(LocalFileList friendFileList, Category category,
            LibraryManager libraryManager, PropertiesFactory<LocalFileItem> propertiesFactory,
            LibraryNavigator libraryNavigator) {
        this.friendFileList = friendFileList;
        this.libraryManager = libraryManager;
        this.category = category;
        this.propertiesFactory = propertiesFactory;
        this.libraryNavigator = libraryNavigator;
    }

    public void setFileItems(List<LocalFileItem> items) {
        this.fileItems = items;
        initialize();
    }

    private void initialize() {
        boolean singleFile = fileItems.size() == 1;
        boolean multiFile = fileItems.size() > 1;

        LocalFileItem firstItem = fileItems.get(0);
        
        boolean playActionEnabled = singleFile;
        boolean launchActionEnabled = singleFile;
        boolean locateActionEnabled = singleFile && !firstItem.isIncomplete();
        boolean viewFileInfoEnabled = singleFile;
        boolean shareActionEnabled = false;
        boolean removeActionEnabled = false;
        boolean deleteActionEnabled = false;
        
        for(LocalFileItem localFileItem : fileItems) {
            if(localFileItem.isShareable()) {
                shareActionEnabled = true;
                break;
            }
        }
        
        for(LocalFileItem localFileItem : fileItems) {
            if(!localFileItem.isIncomplete()) {
                removeActionEnabled = true;
                deleteActionEnabled = true;
                break;
            }
        }

        removeAll();
        switch (category) {
        case AUDIO:
        case VIDEO:
            add(new PlayAction(libraryNavigator, new Catalog(category), firstItem)).setEnabled(playActionEnabled);
            break;
        case IMAGE:
        case DOCUMENT:
            add(new LaunchFileAction(I18n.tr("View"), firstItem)).setEnabled(launchActionEnabled);
            break;
        case PROGRAM:
        case OTHER:
            add(new LocateFileAction(firstItem)).setEnabled(locateActionEnabled);
        }

        if (multiFile) {
            add(new FriendShareAction(friendFileList, createFileItemArray())).setEnabled(shareActionEnabled);
            add(new FriendUnshareAction(friendFileList, createFileItemArray())).setEnabled(shareActionEnabled);
        } else {
            add(new FriendShareCheckBox(friendFileList, firstItem)).setEnabled(shareActionEnabled);
        }

        addSeparator();
        if (category != Category.PROGRAM && category != Category.OTHER) {
            add(new LocateFileAction(firstItem)).setEnabled(locateActionEnabled);
        }

        add(new RemoveAction(createFileItemArray(), libraryManager)).setEnabled(removeActionEnabled);
        add(new DeleteAction(createFileItemArray(), libraryManager)).setEnabled(deleteActionEnabled);

        addSeparator();
        add(new ViewFileInfoAction(firstItem, propertiesFactory)).setEnabled(viewFileInfoEnabled);
    }

    LocalFileItem[] createFileItemArray() {
        return fileItems.toArray(new LocalFileItem[fileItems.size()]);
    };

    private class FriendShareCheckBox extends JCheckBoxMenuItem {
        private final LocalFileItem localFile;

        private final LocalFileList friendFileList;

        public FriendShareCheckBox(LocalFileList friendFileList, LocalFileItem localFileItem) {
            super(I18n.tr("Share"));
            this.friendFileList = friendFileList;
            this.localFile = localFileItem;
            setSelected(friendFileList.contains(localFileItem.getFile()));
            
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isSelected()) {
                        FriendShareCheckBox.this.friendFileList.addFile(localFile.getFile());
                    } else {
                        FriendShareCheckBox.this.friendFileList.removeFile(localFile.getFile());
                    }
                }
            });

        }
    }

    private class FriendShareAction extends AbstractAction {
        private final LocalFileItem[] localFileItems;

        private final LocalFileList friendFileList;

        public FriendShareAction(LocalFileList friendFileList, LocalFileItem[] localFileItems) {
            super(I18n.tr("Share"));
            this.friendFileList = friendFileList;
            this.localFileItems = localFileItems;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (LocalFileItem localFileItem : localFileItems) {
                if(localFileItem.isShareable()) {
                    friendFileList.addFile(localFileItem.getFile());
                }
            }
            GuiUtils.getMainFrame().repaint();
        }
    }

    private class FriendUnshareAction extends AbstractAction {
        private final LocalFileItem[] localFileItems;

        private final LocalFileList friendFileList;

        public FriendUnshareAction(LocalFileList friendFileList, LocalFileItem[] localFileItems) {
            super(I18n.tr("Unshare"));
            this.friendFileList = friendFileList;
            this.localFileItems = localFileItems;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (LocalFileItem localFileItem : localFileItems) {
                friendFileList.removeFile(localFileItem.getFile());
            }
            GuiUtils.getMainFrame().repaint();
        }
    }
}
