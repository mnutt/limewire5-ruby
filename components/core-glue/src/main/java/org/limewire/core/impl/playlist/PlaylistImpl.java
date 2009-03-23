package org.limewire.core.impl.playlist;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistItem;
import org.limewire.core.api.playlist.PlaylistListener;
import org.limewire.util.MediaType;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Live implementation of Playlist.
 */
class PlaylistImpl implements Playlist {

    private final MediaType mediaType;
    private final List<PlaylistItem> itemList;
    private final PlaylistMatcherEditor matcherEditor;
    private final List<PlaylistListener> playlistListeners;

    private String name;

    /**
     * Constructs a Playlist with the specified name.
     */
    public PlaylistImpl(String name) {
        this.mediaType = MediaType.getAudioMediaType();
        this.itemList = Collections.synchronizedList(new ArrayList<PlaylistItem>());
        this.matcherEditor = new PlaylistMatcherEditor();
        this.playlistListeners = new CopyOnWriteArrayList<PlaylistListener>(); 
        this.name = name;
    }

    @Override
    public void addPlaylistListener(PlaylistListener listener) {
        playlistListeners.add(listener);
    }

    @Override
    public void removePlaylistListener(PlaylistListener listener) {
        playlistListeners.remove(listener);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds the specified file to the end of the playlist.
     * At present, the same file cannot be added multiple times.
     */
    @Override
    public void addFile(File file) {
        if (!contains(file)) {
            // Add playlist item to playlist.
            itemList.add(new PlaylistItemImpl(file));
            // Update filter on local file items, and notify listeners.  
            matcherEditor.update();
            fireListChanged();
        }
    }

    /**
     * Adds the specified file at the specified index in the playlist.  
     * At present, the same file cannot be added multiple times.
     */
    @Override
    public void addFile(int index, File file) {
        // Find file in current playlist.  If it already exists, remove the
        // old entry and insert a new one at the specified index.
        int oldIndex = getIndex(file);
        if (oldIndex < 0) {
            itemList.add(index, new PlaylistItemImpl(file));
        } else {
            PlaylistItem item = itemList.remove(oldIndex);
            itemList.add(index, item);
        }
        
        // Update filter on local file items, and notify listeners.  
        matcherEditor.update();
        fireListChanged();
    }
    
    @Override
    public void removeFile(File file) {
        int oldIndex = getIndex(file);
        if (oldIndex >= 0) {
            // Remove playlist item from playlist.
            itemList.remove(oldIndex);
            // Update filter on local file items, and notify listeners.  
            matcherEditor.update();
            fireListChanged();
        }
    }
    
    @Override
    public void reorderFiles(File[] files) {
        // This is a slow implementation because it physically moves the list
        // elements.  A possible performance upgrade could use a row mapping.
        for (int i = 0; i < files.length; i++) {
            int oldIndex = getIndex(files[i]);
            if (oldIndex >= 0) {
                PlaylistItem item = itemList.remove(oldIndex);
                itemList.add(i, item);
            }
        }
    }

    @Override
    public void clear() {
        // Remove all items from playlist.
        itemList.clear();
        // Update filter on local file items, and notify listeners.  
        matcherEditor.update();
        fireListChanged();
    }
    
    @Override
    public boolean canAdd(File file) {
        return mediaType.matches(file.getName());
    }
    
    @Override
    public boolean contains(File file) {
        return itemList.contains(new PlaylistItemImpl(file));
    }
    
    @Override
    public int getIndex(File file) {
        return itemList.indexOf(new PlaylistItemImpl(file));
    }

    @Override
    public MatcherEditor<LocalFileItem> getFilter() {
        return matcherEditor;
    }
    
    /**
     * Notifies all registered listeners that the playlist has changed.
     */
    private void fireListChanged() {
        for (PlaylistListener listener : playlistListeners) {
            listener.listChanged(this);
        }
    }

    /**
     * Defines a MatcherEditor that notifies a Glazed List when the items in 
     * the playlist have changed. 
     */
    private class PlaylistMatcherEditor extends AbstractMatcherEditor<LocalFileItem> {
        
        private final Matcher<LocalFileItem> matcher = new PlaylistMatcher();

        @Override
        public Matcher<LocalFileItem> getMatcher() {
            return matcher;
        }
        
        /**
         * Updates the playlist by firing an event to notify listeners that the
         * matcher has changed. 
         */
        public void update() {
            fireChanged(matcher);
        }
    }

    /**
     * Defines a Matcher that can be applied to a Glazed List to display the
     * items in the playlist. 
     */
    private class PlaylistMatcher implements Matcher<LocalFileItem> {

        @Override
        public boolean matches(LocalFileItem item) {
            return contains(item.getFile());
        }
    }
}
