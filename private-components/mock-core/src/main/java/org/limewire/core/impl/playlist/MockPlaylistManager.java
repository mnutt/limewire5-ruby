package org.limewire.core.impl.playlist;

import java.util.Arrays;
import java.util.List;

import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Mock implementation of PlaylistManager. 
 */
@Singleton
class MockPlaylistManager implements PlaylistManager {

    private final Playlist defaultPlaylist;
    private final List<Playlist> playlistList;
    
    @Inject
    public MockPlaylistManager() {
        // Create the only playlist currently supported.
        this.defaultPlaylist = new MockPlaylist("Quicklist");
        this.playlistList = Arrays.asList(new Playlist[] {
                defaultPlaylist
        });
    }

    @Override
    public void renameDefaultPlaylist(String name) {
        defaultPlaylist.setName(name);
    }
    
    @Override
    public List<Playlist> getPlaylists() {
        return playlistList;
    }

}
