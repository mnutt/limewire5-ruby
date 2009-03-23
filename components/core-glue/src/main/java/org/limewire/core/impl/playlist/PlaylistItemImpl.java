package org.limewire.core.impl.playlist;

import java.io.File;
import java.net.URI;

import org.limewire.core.api.playlist.PlaylistItem;

/**
 * Live implementation of PlaylistItem.
 */
class PlaylistItemImpl implements PlaylistItem {

    /** Location of the media source. */
    private final URI uri;
    
    /** Location of the media source. */
    private final String name;
    
    /** Location of the media source. */
    private final boolean local;

    /**
     * Constructs a PlaylistItem for the specified file.
     */
    public PlaylistItemImpl(File file) {
        this.uri = file.toURI();
        this.name = file.getName();
        this.local = true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public boolean isLocal() {
        return local;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlaylistItem) {
            return getURI().equals(((PlaylistItem) obj).getURI());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + uri.hashCode();
        return result;
    }
}
