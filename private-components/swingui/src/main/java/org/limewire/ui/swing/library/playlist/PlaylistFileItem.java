package org.limewire.ui.swing.library.playlist;

import org.limewire.core.api.library.LocalFileItem;

/**
 * Defines a LocalFileItem that describes an item in a playlist.  This is a   
 * decorator that provides the position index of the item in a playlist.
 */
public interface PlaylistFileItem extends LocalFileItem {

    /**
     * Returns the position index in the playlist.
     */
    int getIndex();
    
}
