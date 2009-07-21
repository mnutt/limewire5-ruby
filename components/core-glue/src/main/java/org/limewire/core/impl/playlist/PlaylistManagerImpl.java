package org.limewire.core.impl.playlist;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistManager;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.Library;

/**
 * Live implementation of PlaylistManager. 
 */
@Singleton
class PlaylistManagerImpl implements PlaylistManager {
    
    private final Library managedList;
    private final Playlist defaultPlaylist;
    private final List<Playlist> playlistList;

    /**
     * Constructs a PlaylistManager using the specified managed file list.
     */
    @Inject
    public PlaylistManagerImpl(Library managedList) {
        this.managedList = managedList;
        
        // Create the only playlist currently supported.
        this.defaultPlaylist = new PlaylistImpl("Quicklist");
        this.playlistList = Arrays.asList(new Playlist[] {
                defaultPlaylist
        });
    }

    /**
     * Registers internal listeners on core objects.
     */
    @Inject
    void register() {
        // Install listener on managed file list.
        managedList.addListener(new EventListener<FileViewChangeEvent>() {
            @Override
            public void handleEvent(FileViewChangeEvent event) {
                switch (event.getType()) {
                case FILE_REMOVED:
                    removeFile(event.getFile());
                    break;
                    
                default:
                    break;
                }
            }
        });
    }
    
    /**
     * Renames the default playlist.
     */
    @Override
    public void renameDefaultPlaylist(String name) {
        defaultPlaylist.setName(name);
    }
    
    /**
     * Returns a list of available playlists.
     */
    @Override
    public List<Playlist> getPlaylists() {
        return playlistList;
    }

    /**
     * Removes the specified file from all playlists.
     */
    private void removeFile(File file) {
        for (Playlist playlist : playlistList) {
            playlist.removeFile(file);
        }
    }
}
