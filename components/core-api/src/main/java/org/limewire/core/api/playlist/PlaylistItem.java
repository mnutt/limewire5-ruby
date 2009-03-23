package org.limewire.core.api.playlist;

import java.net.URI;

/**
 * Defines the API for an item in a playlist.
 */
public interface PlaylistItem {

    /**
     * Returns the unique URI.
     */
    URI getURI();
    
    /**
     * Returns the file name.
     */
    String getName();
    
    /**
     * Returns true if the item is a local file.
     */
    boolean isLocal();
    
}
