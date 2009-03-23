package org.limewire.ui.swing.library.playlist;

import static org.limewire.ui.swing.library.playlist.TransferablePlaylistData.PLAYLIST_DATA_FLAVOR;

import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 * An implementation of TransferHandler for playlists.  This handles user
 * actions to reorder the files in a playlist. 
 */
public class PlaylistTransferHandler extends TransferHandler {
    private static Log LOG = LogFactory.getLog(PlaylistTransferHandler.class);
    
    private final Playlist playlist;
    
    /** Indicator that determines whether files were reordered. */
    private boolean reordered;

    /**
     * Constructs a PlaylistTransferHandler for the specified playlist.
     */
    public PlaylistTransferHandler(Playlist playlist) {
        this.playlist = playlist;
    }

    /**
     * Creates a Transferable object containing the files for transfer.
     */
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof PlaylistLibraryTable) {
            PlaylistLibraryTable<?> libTable = (PlaylistLibraryTable<?>) c;
            
            // Get list of selected items.
            EventSelectionModel<?> selectionModel = (EventSelectionModel<?>) libTable.getSelectionModel();
            EventList<?> selectedList = selectionModel.getSelected();
            
            // Get files for transfer.
            File[] files = new File[selectedList.size()];
            for (int i = 0; i < selectedList.size(); i++) {
                LocalFileItem fileItem = (LocalFileItem) selectedList.get(i);
                files[i] = fileItem.getFile();
            }
            
            // Create transferable data.
            return new TransferablePlaylistData(files);
        }
        
        return null;
    }

    /**
     * Returns the type of transfer operations supported by the component.
     */
    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    /**
     * Initiates drag operation to reorder or move files.
     */
    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        // Reset indicator.
        reordered = false;
        
        // Start drag operation.
        super.exportAsDrag(comp, e, action);
    }

    /**
     * Invoked after data is exported.  This method will remove files from
     * the playlist if they were not reordered within the list.
     */
    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if ((action == MOVE) && !reordered) {
            try {
                // Get files to remove.
                File[] files = (File[]) data.getTransferData(PLAYLIST_DATA_FLAVOR);

                // Remove files from playlist.
                for (File file : files) {
                    playlist.removeFile(file);
                }
                
            } catch (UnsupportedFlavorException ufx) {
                LOG.error("Error removing playlist files", ufx);
            } catch (IOException iox) {
                LOG.error("Error removing playlist files", iox);
            }
        }
    }

    /**
     * Returns true if the component can accept the drop operation.
     */
    @Override
    public boolean canImport(TransferSupport support) {
        // Verify data flavors.  Only files dragged within the playlist table
        // can be accepted at this time.
        boolean canImport = support.isDataFlavorSupported(PLAYLIST_DATA_FLAVOR);
        
        if (canImport && support.isDrop()) {
            support.setShowDropLocation(false);
        }
        
        return canImport;
    }

    /**
     * Imports the transfer data into the component.
     */
    @Override
    public boolean importData(TransferSupport support) {
        // Verify drop operation.
        if (!support.isDrop()) {
            return false;
        }
        
        // Verify target component.
        Component component = support.getComponent();
        if (!(component instanceof PlaylistLibraryTable)) {
            return false;
        }
        
        if (support.isDataFlavorSupported(PLAYLIST_DATA_FLAVOR)) {
            // Get target row for drop, and verify.
            PlaylistLibraryTable<?> libTable = (PlaylistLibraryTable<?>) component;
            int dropRow = libTable.rowAtPoint(support.getDropLocation().getDropPoint());
            if (dropRow < 0) {
                return false;
            }
            
            try {
                // Set reorder indicator so files are not removed when done.
                reordered = true;

                // Get files to move.
                Transferable data = support.getTransferable();
                File[] files = (File[]) data.getTransferData(PLAYLIST_DATA_FLAVOR);

                // Move files in the playlist.  This assumes that a file can 
                // appear only once in the playlist.
                for (File file : files) {
                    playlist.addFile(dropRow, file);
                }

                // Return success.
                return true;
                
            } catch (UnsupportedFlavorException ufx) {
                LOG.error("Error moving playlist files", ufx);
                return false;
            } catch (IOException iox) {
                LOG.error("Error moving playlist files", iox);
                return false;
            }
        }

        // Other data flavors not supported so return failure.
        return false;
    }
}
