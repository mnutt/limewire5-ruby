package org.limewire.ui.swing.library.playlist;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

/**
 * An implementation of Transferable for playlist data. 
 */
public class TransferablePlaylistData implements Transferable {
    /** Data flavor for playlist files. */
    public static final DataFlavor PLAYLIST_DATA_FLAVOR = new DataFlavor(
            File[].class, "Playlist files");
    
    private File[] files;

    /**
     * Constructs a TransferablePlaylistData object containing the specified
     * file array.
     */
    public TransferablePlaylistData(File[] files) {
        this.files = files;
    }
    
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (PLAYLIST_DATA_FLAVOR.equals(flavor)) {
            return files;
        }
        
        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {PLAYLIST_DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return PLAYLIST_DATA_FLAVOR.equals(flavor);
    }

}
