package com.limegroup.gnutella.gui.library;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * An item from the library tree that is being transferred.
 * @author Sam
 *
 */
public class LibraryTreeTransferable implements Transferable {
    
    public static final DataFlavor libraryTreeFlavor = new DataFlavor(LibraryTreeTransferable.class, "LimeWire LibraryTreeItem");

    private final DirectoryHolder holder;
    
    public LibraryTreeTransferable(DirectoryHolder holder) {
        this.holder = holder;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { libraryTreeFlavor };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(libraryTreeFlavor);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if(isDataFlavorSupported(flavor))
            return holder;
        else
            throw new UnsupportedFlavorException(flavor);
    }

}
