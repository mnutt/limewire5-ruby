package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.util.DNDUtils;

public class LocalFileListTransferHandler extends TransferHandler {

    private final LocalFileList fileList;

    public LocalFileListTransferHandler(LocalFileList fileList) {
        this.fileList = fileList;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return DNDUtils.containsFileFlavors(info);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }

        List<File> files = Collections.emptyList();        
        if (DNDUtils.containsFileFlavors(info)) {
            Transferable t = info.getTransferable();
            try {
                files = Arrays.asList(DNDUtils.getFiles(t));
            } catch(Throwable failed) {
                return false;
            }
        } 
        
        handleFiles(files);
        return true;
    }

    private void handleFiles(final List<File> files) {
        for (File file : files) {
            if(file.isDirectory()) {
                fileList.addFolder(file);
            } else {
                fileList.addFile(file);
            }
        }
    }

}
