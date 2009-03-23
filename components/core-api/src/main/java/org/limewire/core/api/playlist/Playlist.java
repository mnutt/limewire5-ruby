package org.limewire.core.api.playlist;

import java.io.File;

import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Defines the API for a playlist.  A playlist contains an ordered list of
 * files.
 */
public interface Playlist {

    /**
     * Adds the specified listener to the list that is notified when the 
     * playlist is changed. 
     */
    void addPlaylistListener(PlaylistListener listener);
    
    /**
     * Removes the specified listener from the list that is notified when the 
     * playlist is changed. 
     */
    void removePlaylistListener(PlaylistListener listener);
    
    /**
     * Returns the name of the playlist.
     */
    String getName();
    
    /**
     * Sets the name of the playlist.
     */
    void setName(String name);

    /**
     * Adds the specified file to the end of the playlist.
     */
    void addFile(File file);

    /**
     * Adds the specified file at the specified index in the playlist.
     */
    void addFile(int index, File file);
    
    /**
     * Removes the specified file to the playlist.
     */
    void removeFile(File file);
    
    /**
     * Reorders the specified files in the playlist.
     */
    void reorderFiles(File[] files);
    
    /**
     * Clears the playlist.
     */
    void clear();
    
    /**
     * Returns true if the specified file can be added to the playlist.
     */
    boolean canAdd(File file);
    
    /**
     * Returns true if the playlist contains the specified file.
     */
    boolean contains(File file);
    
    /**
     * Returns the position of the specified file in the playlist.  The method
     * returns -1 if the file is not contained in the playlist.
     */
    int getIndex(File file);

    /**
     * Returns a filter that can be applied to a Glazed List of local file 
     * items to display the playlist.
     */
    MatcherEditor<LocalFileItem> getFilter();
    
}
