package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import org.limewire.core.api.library.RemoteFileItem;

public class RemoteFileTransferable implements Transferable {
    
    public static final DataFlavor REMOTE_FILE_DATA_FLAVOR = new DataFlavor(List.class, "RemoteFileItem List");
    
    private List<RemoteFileItem> remoteFileList;
    
    public RemoteFileTransferable(List<RemoteFileItem> remoteFileList){
        this.remoteFileList = remoteFileList;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if(!flavor.equals(REMOTE_FILE_DATA_FLAVOR)){
            throw new UnsupportedFlavorException(flavor);
        }
        return remoteFileList;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{REMOTE_FILE_DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(REMOTE_FILE_DATA_FLAVOR);
    }

}
