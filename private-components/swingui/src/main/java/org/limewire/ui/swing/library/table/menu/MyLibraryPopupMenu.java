package org.limewire.ui.swing.library.table.menu;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.Catalog;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.menu.actions.DeleteAction;
import org.limewire.ui.swing.library.table.menu.actions.LaunchFileAction;
import org.limewire.ui.swing.library.table.menu.actions.LocateFileAction;
import org.limewire.ui.swing.library.table.menu.actions.PlayAction;
import org.limewire.ui.swing.library.table.menu.actions.RemoveAction;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactory;
import org.limewire.ui.swing.library.table.menu.actions.ViewFileInfoAction;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Popup menu implementation for MyLibrary
 */
public class MyLibraryPopupMenu extends JPopupMenu {

    private final LibraryManager libraryManager;
    private final Category category;
    private final FileInfoDialogFactory fileInfoFactory;
    private final XMPPService xmppService;
    private final Provider<SharingActionFactory> sharingActionFactoryProvider;   
    private final LibraryNavigator libraryNavigator;
    private final PlaylistManager playlistManager;

    private SelectAllable<LocalFileItem> librarySelectable;

    @Resource
    private Color disabledColor;

    @Inject
    public MyLibraryPopupMenu(@Assisted Category category, LibraryManager libraryManager,
            Provider<SharingActionFactory> sharingActionFactory, XMPPService xmppService, LibraryNavigator libraryNavigator, 
            PlaylistManager playlistManager, FileInfoDialogFactory fileInfoFactory) {
        this.libraryManager = libraryManager;
        this.sharingActionFactoryProvider = sharingActionFactory;
        this.category = category;
        this.xmppService = xmppService;
        this.libraryNavigator = libraryNavigator;
        this.playlistManager = playlistManager;
        this.fileInfoFactory = fileInfoFactory;
        
        GuiUtils.assignResources(this);
    }

    public void setSelectable(SelectAllable<LocalFileItem> librarySelectable) {
        this.librarySelectable = librarySelectable;
        initialize();
    }

    private JMenuItem decorateDisabledfItem(AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setForeground(disabledColor);
        return item;
    }

    private void initialize() {
        List<LocalFileItem> fileItems = librarySelectable.getSelectedItems();
        boolean singleFile = fileItems.size() == 1;

        LocalFileItem firstItem = librarySelectable.getSelectedItems().get(0);

        boolean playActionEnabled = singleFile;
        boolean launchActionEnabled = singleFile;
        boolean locateActionEnabled = singleFile && !firstItem.isIncomplete();
        boolean viewFileInfoEnabled = singleFile;
        boolean shareActionEnabled = false;
        boolean removeActionEnabled = false;
        boolean deleteActionEnabled = false;
        boolean playlistActionEnabled = true;

        for (LocalFileItem localFileItem : fileItems) {
            if (localFileItem.isShareable()) {
                shareActionEnabled = true;
                break;
            }
        }

        // Disable playlist action if any selected files are incomplete.
        for (LocalFileItem localFileItem : fileItems) {
            if (localFileItem.isIncomplete()) {
                playlistActionEnabled = false;
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

        // Create playlist sub-menu for audio files.
        if (category == Category.AUDIO) {
            // Get list of playlists.
            List<Playlist> playlistList = playlistManager.getPlaylists();

            // Add action for default playlist only; this is assumed to be the
            // first item in the list.  When multiple playlists are supported,
            // we need to upgrade this to create a sub-menu of playlists. 
            if (playlistList.size() > 0) {
                Playlist playlist = playlistList.get(0);
                String name = I18n.tr("Add to {0}", playlist.getName());
                add(new PlaylistAction(name, playlist, fileItems)).setEnabled(playlistActionEnabled);
            }
        }

        addSeparator();

        SharingActionFactory sharingActionFactory = sharingActionFactoryProvider.get();
        
        boolean isDocumentSharingAllowed = isGnutellaShareAllowed(category) & shareActionEnabled;
        add(sharingActionFactory.createShareGnutellaAction(false, librarySelectable)).setEnabled(isDocumentSharingAllowed);
        add(sharingActionFactory.createUnshareGnutellaAction(false, librarySelectable)).setEnabled(isDocumentSharingAllowed);
        
        addSeparator();
        
        if(xmppService.isLoggedIn()) {
            add(sharingActionFactory.createShareFriendAction(false, librarySelectable)).setEnabled(shareActionEnabled);
            add(sharingActionFactory.createUnshareFriendAction(false, librarySelectable)).setEnabled(shareActionEnabled);
        } else {
            add(decorateDisabledfItem(sharingActionFactory.createDisabledFriendAction(I18n.tr("Share with Friend"))));
            add(decorateDisabledfItem(sharingActionFactory.createDisabledFriendAction(I18n.tr("Unshare with Friend"))));
        }
        
        addSeparator();
        if (category != Category.PROGRAM && category != Category.OTHER) {
            add(new LocateFileAction(firstItem)).setEnabled(locateActionEnabled);
        }

        add(new RemoveAction(fileItems.toArray(new LocalFileItem[fileItems.size()]), libraryManager)).setEnabled(removeActionEnabled);
        
        add(new DeleteAction(fileItems.toArray(new LocalFileItem[fileItems.size()]), libraryManager)).setEnabled(deleteActionEnabled);

        addSeparator();
        add(new ViewFileInfoAction(firstItem, fileInfoFactory)).setEnabled(viewFileInfoEnabled);
    }

    private boolean isGnutellaShareAllowed(Category category) {
        if(category != Category.DOCUMENT)
            return true;
        return LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue();
    }
    
    /**
     * Menu action to add audio files to a playlist.  Only file types that are
     * playable by the LimeWire player are added.
     */
    private class PlaylistAction extends AbstractAction {
        private final Playlist playlist;
        private final List<LocalFileItem> fileItems;
        
        public PlaylistAction(String name, Playlist playlist, List<LocalFileItem> fileItems) {
            super(name);
            this.playlist = playlist;
            this.fileItems = fileItems;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (LocalFileItem fileItem : fileItems) {
                File file = fileItem.getFile();
                if (playlist.canAdd(file) && PlayerUtils.isPlayableFile(file)) {
                    playlist.addFile(fileItem.getFile());
                }
            }
        }
    }
}
