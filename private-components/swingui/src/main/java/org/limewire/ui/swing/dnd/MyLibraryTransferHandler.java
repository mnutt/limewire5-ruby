package org.limewire.ui.swing.dnd;

import static org.limewire.ui.swing.library.playlist.TransferablePlaylistData.PLAYLIST_DATA_FLAVOR;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.LibraryListSourceChanger;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.DNDUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class MyLibraryTransferHandler extends TransferHandler {

    private final EventSelectionModel<LocalFileItem> selectionModel;
    private final LibraryFileList libraryManagedList;
    private final ShareListManager shareListManager;
    private final LibraryListSourceChanger listChanger;
    
    public MyLibraryTransferHandler(EventSelectionModel<LocalFileItem> selectionModel,
            LibraryFileList libraryManagedList, ShareListManager shareListManager,
            LibraryListSourceChanger listChanger) {
        this.selectionModel = selectionModel;
        this.libraryManagedList = libraryManagedList;
        this.shareListManager = shareListManager;
        this.listChanger = listChanger;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return (!info.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR) && DNDUtils.containsFileFlavors(info))
            || info.isDataFlavorSupported(PLAYLIST_DATA_FLAVOR);
    }

    @Override
    public int getSourceActions(JComponent comp) {
        return COPY;
    }

    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }
        
        // Accept playlist data without importing - files will be removed from
        // the playlist.
        if (info.isDataFlavorSupported(PLAYLIST_DATA_FLAVOR)) {
            return true;
        }

        // Get the string that is being dropped.
        Transferable t = info.getTransferable();
        final List<File> fileList;
        try {
            fileList = Arrays.asList(DNDUtils.getFiles(t));
        } catch (Exception e) {
            return false;
        }

        for (File file : fileList) {
            // if is a folder
            if(file.isDirectory()) {
                //if not in filtered mode
                if(listChanger.getCurrentFriend() == null)
                libraryManagedList.addFolder(file);
                else if(listChanger.getCurrentFriend().getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                    shareListManager.getGnutellaShareList().addFolder(file);
                else
                    shareListManager.getFriendShareList(listChanger.getCurrentFriend()).addFolder(file);
            } else {
                //if not in filtered mode
                if(listChanger.getCurrentFriend() == null)
                libraryManagedList.addFile(file);
                else if(listChanger.getCurrentFriend().getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                    shareListManager.getGnutellaShareList().addFile(file);
                else
                    shareListManager.getFriendShareList(listChanger.getCurrentFriend()).addFile(file);
            }
        }
        return true;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
        if(selectionModel != null) {
            EventList<LocalFileItem> fileList = selectionModel.getSelected();
            File[] files = new File[fileList.size()];
            for (int i = 0; i < files.length; i++) {
                files[i] = fileList.get(i).getFile();
            }
            return new LocalFileTransferable(files);
        }
        
        return null;
    }
}
