package org.limewire.core.impl.playlist;

import java.net.URI;

import org.limewire.core.api.playlist.PlaylistItem;

/**
 * Mock implementation of PlaylistItem.
 */
public class MockPlaylistItem implements PlaylistItem {

    @Override
    public String getName() {
        return null;
    }

    @Override
    public URI getURI() {
        return null;
    }

    @Override
    public boolean isLocal() {
        return false;
    }
    
}
