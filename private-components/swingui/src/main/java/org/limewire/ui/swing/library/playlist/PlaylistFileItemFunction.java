package org.limewire.ui.swing.library.playlist;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;

import ca.odell.glazedlists.FunctionList.Function;

/**
 * An implementation of Function that converts a LocalFileItem into a
 * PlaylistFileItem for display. 
 */
public class PlaylistFileItemFunction implements Function<LocalFileItem, PlaylistFileItem> {

    private final Playlist playlist;
    
    /**
     * Constructs a PlaylistFileItemFunction for the specified playlist.
     */
    public PlaylistFileItemFunction(Playlist playlist) {
        this.playlist = playlist;
    }
    
    @Override
    public PlaylistFileItem evaluate(LocalFileItem sourceValue) {
        // Create playlist file item.
        return new PlaylistFileItemImpl(playlist, sourceValue);
    }

}
