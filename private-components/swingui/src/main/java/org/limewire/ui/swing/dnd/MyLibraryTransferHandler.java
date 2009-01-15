package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.DNDUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class MyLibraryTransferHandler extends TransferHandler {

    EventSelectionModel<LocalFileItem> selectionModel;
    private LibraryFileList libraryManagedList;

    public MyLibraryTransferHandler(EventSelectionModel<LocalFileItem> selectionModel, LibraryFileList libraryManagedList) {
        this.selectionModel = selectionModel;
        this.libraryManagedList = libraryManagedList;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return !info.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR) && DNDUtils.containsFileFlavors(info);
    }

    @Override
    public int getSourceActions(JComponent comp) {
        return COPY;
    }

    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
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
            if(file.isDirectory()) {
                libraryManagedList.addFolder(file);
            } else {
                libraryManagedList.addFile(file);
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
