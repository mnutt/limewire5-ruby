package org.limewire.core.api.playlist;

import java.util.List;

/**
 * Defines the API for the playlist manager.
 */
public interface PlaylistManager {

    /**
     * Renames the default playlist.
     */
    void renameDefaultPlaylist(String name);
    
    /**
     * Returns a list of available playlists.
     */
    List<Playlist> getPlaylists();
    
}
