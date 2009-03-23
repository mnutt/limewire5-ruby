package com.limegroup.gnutella.library;

import org.limewire.core.api.Category;

public interface FriendFileList extends SharedFileList {

    /** Adds the FileDesc to this list. */
    boolean add(FileDesc fileDesc);
    
    /**
     * Changes the smart sharing value for images. If true, all new images added to
     * the library will be shared with this list, if false, new images added to 
     * the library will not be automatically shared with this list but current images
     * will not be removed.
     */
    void setAddNewImageAlways(boolean value);
    
    /**
     * Returns true if image files are being smartly shraed with this friend, false otherwise.
     */
    boolean isAddNewImageAlways();
    
    /**
     * Changes the smart sharing value for audio files. If true, all new audio files added to
     * the library will be shared with this list, if false, new audio files added to 
     * the library will not be automatically shared with this list but current audio files
     * will not be removed.
     */
    void setAddNewAudioAlways(boolean value);
    
    /**
     * Returns true if audio files are being smartly shared with this friend, false otherwise.
     */
    boolean isAddNewAudioAlways();
    
    /**
     * Changes the smart sharing value for videos. If true, all new videos added to
     * the library will be shared with this list, if false, new videos added to 
     * the library will not be automatically shared with this list but current videos
     * will not be removed.
     */
    void setAddNewVideoAlways(boolean value);
    
    /**
     * Returns true if videos are being smartly shared with this friend, false otherwise.
     */
    boolean isAddNewVideoAlways();

    /** Clears all files in the given category from this list. */
    void clearCategory(Category category);
    
    /** 
     *  Adds all the files of type category in Managed FileList to this FileList.
     *  Future files of type category added to Managed FileList will not be added
     *  to this list.
     */
    void addSnapshotCategory(Category category);

}
