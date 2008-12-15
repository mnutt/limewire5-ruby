package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;


public class LocalFileTransferable implements Transferable {
    
    public static final DataFlavor LOCAL_FILE_DATA_FLAVOR = new DataFlavor(File[].class, "Local File array");
    
    private File[] files;
    
    public LocalFileTransferable(File[] files){
        this.files = files;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if(!flavor.equals(LOCAL_FILE_DATA_FLAVOR)){
            throw new UnsupportedFlavorException(flavor);
        }
        return files;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{LOCAL_FILE_DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(LOCAL_FILE_DATA_FLAVOR);
    }

}
