package com.limegroup.gnutella.library;

import java.io.File;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;
import org.limewire.util.Objects;


/**
 * A collection of FileDescs containing files shared with an individual friend.
 */
class FriendFileListImpl extends AbstractFileList implements FriendFileList {
    
    private final String id;
    
    private boolean addNewImagesAlways = false;
    private boolean addNewAudioAlways = false;
    private boolean addNewVideoAlways = false;
    
    protected final LibraryFileData data;

    public FriendFileListImpl(LibraryFileData data, ManagedFileListImpl managedList,  String id) {
        super(managedList);
        this.id = Objects.nonNull(id, "id");
        this.data = data;
        
        addNewAudioAlways = LibrarySettings.containsFriendShareNewAudio(id);
        addNewImagesAlways = LibrarySettings.containsFriendShareNewImages(id);
        addNewVideoAlways = LibrarySettings.containsFriendShareNewVideo(id);
        initialize();
    }
    
    @Override
    public boolean add(FileDesc fileDesc) {
        return super.add(fileDesc);
    }

    /**
     * This method initializes the friend file list.  It adds the files
     * that are shared with the friend represented by this list.  This
     * is necessary because friend file lists are populated/unpopulated when
     * needed, not upon startup.
     */
    void initialize() {

        // add files from the MASTER list which are for the current friend
        managedList.getReadLock().lock();
        try {
            for (FileDesc fd : managedList) {
                if(isPending(fd.getFile(), fd)) {
                    add(fd);
                }
            }
        } finally {
            managedList.getReadLock().unlock();
        }
    }

    /**
     * Unloading the list makes the sharing
     * characteristics of the files in the list invisible externally (files are still in list,
     * but do not have the appearance of being shared)
     */
    public void unload() {
        // for each file in the friend list, decrement its' file share count
        getReadLock().lock();
        try {
            for (FileDesc fd : this) {
                fd.decrementShareListCount();
            }
        } finally {
            getReadLock().unlock();
        }
        clear();
        dispose();
    }
    
    /**
     * Returns false if it's an {@link IncompleteFileDesc} or it's a store
     * file.
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        if (fileDesc instanceof IncompleteFileDesc) {
            return false;
        } else if( fileDesc.getLimeXMLDocuments().size() != 0 && 
                isStoreXML(fileDesc.getLimeXMLDocuments().get(0))) {
            return false;
        } 
        return true;
    }
    
    /**
     * Changes the smart sharing value for images. If true, all new images added to
     * the library will be shared with this list, if false, new images added to 
     * the library will not be automatically shared with this list but current images
     * will not be removed.
     */
    @Override
    public void setAddNewImageAlways(boolean value) {
        if(value != addNewImagesAlways) {
            if(value == false) {
                LibrarySettings.removeFiendShareNewFiles(LibrarySettings.SHARE_NEW_IMAGES_ALWAYS, id);
            } else {
                LibrarySettings.addFriendShareNewFiles(LibrarySettings.SHARE_NEW_IMAGES_ALWAYS, id);
            }
            fireCollectionEvent(FileListChangedEvent.Type.IMAGE_COLLECTION, value);
            addNewImagesAlways = value;
        }
    }
    
    /**
     * Returns true if image files are being smartly shraed with this friend, false otherwise.
     */
    @Override
    public boolean isAddNewImageAlways() {
        return addNewImagesAlways;
    }
    
    /**
     * Changes the smart sharing value for audio files. If true, all new audio files added to
     * the library will be shared with this list, if false, new audio files added to 
     * the library will not be automatically shared with this list but current audio files
     * will not be removed.
     */
    @Override
    public void setAddNewAudioAlways(boolean value) {
        if(value != addNewAudioAlways) {
            if(value == false) {
                LibrarySettings.removeFiendShareNewFiles(LibrarySettings.SHARE_NEW_AUDIO_ALWAYS, id);
            } else {
                LibrarySettings.addFriendShareNewFiles(LibrarySettings.SHARE_NEW_AUDIO_ALWAYS, id);
            }
            fireCollectionEvent(FileListChangedEvent.Type.AUDIO_COLLECTION, value);
            addNewAudioAlways = value;
        }
    }
    
    /**
     * Returns true if audio files are being smartly shared with this friend, false otherwise.
     */
    @Override
    public boolean isAddNewAudioAlways() {
        return addNewAudioAlways;
    }
    
    /**
     * Changes the smart sharing value for videos. If true, all new videos added to
     * the library will be shared with this list, if false, new videos added to 
     * the library will not be automatically shared with this list but current videos
     * will not be removed.
     */
    @Override
    public void setAddNewVideoAlways(boolean value) {
        if(value != addNewVideoAlways) {
            if(value == false) {
                LibrarySettings.removeFiendShareNewFiles(LibrarySettings.SHARE_NEW_VIDEO_ALWAYS, id);
            } else {
                LibrarySettings.addFriendShareNewFiles(LibrarySettings.SHARE_NEW_VIDEO_ALWAYS, id);
            }
            fireCollectionEvent(FileListChangedEvent.Type.VIDEO_COLLECTION, value);
            addNewVideoAlways = value;
        }
    }
    
    /**
     * Returns true if videos are being smartly shared with this friend, false otherwise.
     */
    @Override
    public boolean isAddNewVideoAlways() {
        return addNewVideoAlways;
    }
    
    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return isSmartlySharedType(file) || data.isSharedWithFriend(file, id);
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        // Just synchronize the data.
        data.setSharedWithFriend(file, id, added);      
    }
    
    @Override
    public boolean remove(FileDesc fileDesc) {
        boolean contains = super.remove(fileDesc);
        // to stop auto sharing on remove, must be contained in this list, not an incomplete file
        // be of the type that is smartly shared and must still exist in the managed List
        // (not existing in the managed list means it was removed from LW)
        if(contains && !(fileDesc instanceof IncompleteFileDesc) && isSmartlySharedType(fileDesc.getFile()) && managedList.contains(fileDesc)) {
            stopSmartSharingType(fileDesc.getFile());
        }
        return contains;
    }
    
    /**
     * Returns true if this file type is being smartly shared. Smartly shared file
     * types are always added to this list.
     */
    protected boolean isSmartlySharedType(File file) {
        if(addNewAudioAlways == false && addNewImagesAlways == false && addNewVideoAlways == false)
            return false;
        
        String ext = FileUtils.getFileExtension(file);
        MediaType type = MediaType.getMediaTypeForExtension(ext);
        if (type == MediaType.getAudioMediaType() && addNewAudioAlways)
            return true;
        else if (type == MediaType.getVideoMediaType() && addNewVideoAlways)
            return true;
        else if (type == MediaType.getImageMediaType() && addNewImagesAlways)
            return true;
        return false;
    }
    
    /**
     * Stops smartly sharing this file type if it was being smartly shared prior.
     */
    private void stopSmartSharingType(File file) {
        if(addNewAudioAlways == false && addNewImagesAlways == false && addNewVideoAlways == false)
            return;
        
        String ext = FileUtils.getFileExtension(file);
        MediaType type = MediaType.getMediaTypeForExtension(ext);
        if (type == MediaType.getAudioMediaType() && addNewAudioAlways)
            setAddNewAudioAlways(false);
        else if (type == MediaType.getVideoMediaType() && addNewVideoAlways)
            setAddNewVideoAlways(false);
        else if (type == MediaType.getImageMediaType() && addNewImagesAlways)
            setAddNewImageAlways(false);
    }
    
    @Override
    protected void fireAddEvent(FileDesc fileDesc) {
        fileDesc.incrementShareListCount();
        super.fireAddEvent(fileDesc);
    }

    @Override
    protected void fireRemoveEvent(FileDesc fileDesc) {
        fileDesc.decrementShareListCount();
        super.fireRemoveEvent(fileDesc);
    }

    @Override
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        oldFileDesc.decrementShareListCount();
        newFileDesc.incrementShareListCount();
        super.fireChangeEvent(oldFileDesc, newFileDesc);
    }
}
