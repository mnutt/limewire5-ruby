package org.limewire.core.api.playlist;

/**
 * Defines a listener that is notified when a playlist is changed.
 */
public interface PlaylistListener {

    /**
     * Handles a change notification for the specified playlist.
     */
    void listChanged(Playlist playlist);
    
}
