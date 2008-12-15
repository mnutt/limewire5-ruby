package com.limegroup.gnutella.gui.library;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class LibraryTableTransferable implements Transferable {
    
    public static final DataFlavor libraryTableTransferable =
        new DataFlavor(LibraryTableTransferable.class, "LimeWire LibraryTableTransfer");
    
    private final LibraryTableDataLine[] lines;

    public LibraryTableTransferable(LibraryTableDataLine[] lines) {
        this.lines = lines;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { libraryTableTransferable };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(libraryTableTransferable);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if(isDataFlavorSupported(flavor))
            return lines;
        else
            throw new UnsupportedFlavorException(flavor);
    }

}
