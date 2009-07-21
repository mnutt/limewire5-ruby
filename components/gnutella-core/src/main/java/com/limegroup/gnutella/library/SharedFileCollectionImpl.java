package com.limegroup.gnutella.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.SharedFileCollectionChangeEvent.Type;
import com.limegroup.gnutella.tigertree.HashTreeCache;


/**
 * A collection of FileDescs containing files that may be shared with one or more people.
 */
class SharedFileCollectionImpl extends AbstractFileCollection implements SharedFileCollection {
    
    private final int collectionId;    
    private final Provider<LibraryFileData> data;
    private final HashTreeCache treeCache;    
    private final EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster;
    private final List<String> defaultFriendIds;
    private final boolean publicCollection;

    @Inject
    public SharedFileCollectionImpl(Provider<LibraryFileData> data, LibraryImpl managedList, 
                                    SourcedEventMulticaster<FileViewChangeEvent, FileView> multicaster,
                                    EventBroadcaster<SharedFileCollectionChangeEvent> sharedCollectionBroadcaster,
                                    @Assisted int id, HashTreeCache treeCache,
                                    @Assisted boolean publicCollection,
                                    @Assisted String... defaultFriendIds) {
        super(managedList, multicaster);
        this.collectionId = id;
        this.data = data;
        this.treeCache = treeCache;
        this.sharedBroadcaster = sharedCollectionBroadcaster;
        this.publicCollection = publicCollection;
        if(defaultFriendIds.length == 0) {
            this.defaultFriendIds = Collections.emptyList();
        } else {
            this.defaultFriendIds = Collections.unmodifiableList(Arrays.asList(defaultFriendIds));
        }
    }
    
    @Override
    public int getId() {
        return collectionId;
    }
    
    public String getName() {
        return data.get().getNameForCollection(collectionId);
    }
    
    public void setName(String name) {
        if(data.get().setNameForCollection(collectionId, name)) {
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.NAME_CHANGED, this, name));
        }
    }
    
    @Override
    public void addFriend(String id) {
        if(data.get().addFriendToCollection(collectionId, id)) {
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_ADDED, this, id));
        }
    }
    
    @Override
    public boolean removeFriend(String id) {
        if(data.get().removeFriendFromCollection(collectionId, id)) {
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_REMOVED, this, id));
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public List<String> getFriendList() {
        List<String> cached = data.get().getFriendsForCollection(collectionId);
        if(defaultFriendIds.isEmpty()) {
            return cached;
        } else if(cached.isEmpty()) {
            return defaultFriendIds;
        } else {
            List<String> friends = new ArrayList<String>(cached.size() + defaultFriendIds.size());
            friends.addAll(defaultFriendIds);
            friends.addAll(cached);
            return friends;
        }
    }
    
    @Override
    public void setFriendList(List<String> ids) {
        List<String> oldIds = data.get().setFriendsForCollection(collectionId, ids);
        if(oldIds != null) { // if it changed, broadcast the change.
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_IDS_CHANGED, this, oldIds, ids));
        }
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this) + ", name: " + getName();
    }
    
    @Override
    protected boolean addFileDescImpl(FileDesc fileDesc) {
        if(super.addFileDescImpl(fileDesc)) {
            // if no root, calculate one and propagate it.
            if(fileDesc.getTTROOTUrn() == null) {
                // Schedule all additions for having a hash tree root.
                URN root = treeCache.getOrScheduleHashTreeRoot(fileDesc);
                if(root != null) {
                	for(FileDesc fd : library.getFileDescsMatching(fileDesc.getSHA1Urn())) {
                	    fd.addUrn(root);
                	}
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void initialize() {
        super.initialize();
        addPendingManagedFiles();
    }
    
    /**
     * This method initializes the friend file list.  It adds the files
     * that are shared with the friend represented by this list.  This
     * is necessary because friend file lists are populated/unpopulated when
     * needed, not upon startup.
     */
    protected void addPendingManagedFiles() {
        // add files from the MASTER list which are for the current friend
        // normally we would not want to lock the master list while adding
        // items internally... but it's OK here because we're guaranteed
        // that nothing is listening to this list, since this will happen
        // immediately after construction.
        library.getReadLock().lock();
        try {
            for (FileDesc fd : library) {
                if(isPending(fd.getFile(), fd)) {
                    add(fd);
                }
            }
        } finally {
            library.getReadLock().unlock();
        }
    }
    
    /**
     * Returns false if it's an {@link IncompleteFileDesc} or it's a store
     * file.
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        if (fileDesc instanceof IncompleteFileDesc) {
            return false;
        } else {
	        return isFileAddable(fileDesc.getFile());
	    }
    }
    
    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return data.get().isFileInCollection(file, collectionId);
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        data.get().setFileInCollection(file, collectionId, added);      
    }
    
    @Override
    protected boolean clearImpl() {
        data.get().setFilesInCollection(this, collectionId, false);
        return super.clearImpl();
    }
    
    @Override
    void dispose() {
        super.dispose();
        data.get().removeCollection(collectionId);
    }
    
    @Override
    protected void fireAddEvent(FileDesc fileDesc) {
        super.fireAddEvent(fileDesc);
    }

    @Override
    protected void fireRemoveEvent(FileDesc fileDesc) {
        super.fireRemoveEvent(fileDesc);
    }

    @Override
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        super.fireChangeEvent(oldFileDesc, newFileDesc);
    }

    @Override
    public boolean isFileAddable(File file) {
        if(!library.isFileAddable(file)) {
            return false;
        }
        
        if(isPublic()) {
            MediaType mediaType = MediaType.getMediaTypeForExtension(FileUtils.getFileExtension(file));
            if(MediaType.getDocumentMediaType().equals(mediaType) && !LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue()) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean isPublic() {
        return publicCollection;
    }
}
