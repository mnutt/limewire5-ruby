package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.util.DNDUtils;

/**
 * Drops with this handler will add the file to the ManagedLibrary and share
 * with the owner of this LocalFileList.
 * 
 * Drops from a LocalFileTransferable are not allowed into this transfer handler. This
 * is to prevent dragging and dropping from the same share list and inadvertantly 
 * sharing the files.
 * 
 * If the Library table parameter is not null drags from the component this transfer
 * handler is set on will create a transferable that can be imported by other transfer 
 * handlers.
 */
public class SharingLibraryTransferHandler extends TransferHandler {

    private LibraryTable table;

    private LocalFileList friendFileList;

    public SharingLibraryTransferHandler(LibraryTable table, LocalFileList friendFileList) {
        this.table = table;
        this.friendFileList = friendFileList;
    }

    @Override
    public int getSourceActions(JComponent comp) {
        return COPY;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return !info.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR) && DNDUtils.containsFileFlavors(info);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }

        Transferable t = info.getTransferable();

        final List<File> fileList;
        try {
            fileList = Arrays.asList(DNDUtils.getFiles(t));
        } catch (Exception e) {
            return false;
        }
        for (File file : fileList) {
            if (file.isDirectory()) {
                friendFileList.addFolder(file);
            } else {
                friendFileList.addFile(file);
            }
        }
        return true;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
        if(table != null) {
            int indices[] = table.getSelectedRows();
            List<File> files = new ArrayList<File>();
            for (int i = 0; i < indices.length; i++) {
                LocalFileItem localFileItem = (LocalFileItem) ((LibraryTableModel) table.getModel()).getFileItem(indices[i]); 
                files.add(localFileItem.getFile());
            }
            return new LocalFileTransferable(files.toArray(new File[0]));
            }
        return null;
    }
}