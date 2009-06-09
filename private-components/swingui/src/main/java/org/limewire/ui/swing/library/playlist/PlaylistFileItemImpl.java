package org.limewire.ui.swing.library.playlist;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;

/**
 * An implementation of PlaylistFileItem.  This decorates a LocalFileItem to
 * provide the position index of the item in a playlist.  All LocalFileItem
 * method calls are forwarded to the underlying file item.
 */
public class PlaylistFileItemImpl implements PlaylistFileItem, Comparable {

    /** Playlist associated with this item. */
    private final Playlist playlist;
    
    /** LocalFileItem that underlies this item. */
    private final LocalFileItem localFileItem;
    
    /**
     * Constructs a PlaylistFileItemImpl for the specified playlist and local
     * file item.
     */
    public PlaylistFileItemImpl(Playlist playlist, LocalFileItem localFileItem) {
        this.playlist = playlist;
        this.localFileItem = localFileItem;
    }
    
    /**
     * Returns the underlying file item that contains the file details.
     */
    public LocalFileItem getLocalFileItem() {
        return localFileItem;
    }
    
    @Override
    public int getIndex() {
        return playlist.getIndex(getFile());
    }

    @Override
    public File getFile() {
        return localFileItem.getFile();
    }

    @Override
    public int getFriendShareCount() {
        return localFileItem.getFriendShareCount();
    }

    @Override
    public long getLastModifiedTime() {
        return localFileItem.getLastModifiedTime();
    }

    @Override
    public int getNumHits() {
        return localFileItem.getNumHits();
    }

    @Override
    public int getNumUploadAttempts() {
        return localFileItem.getNumUploadAttempts();
    }

    @Override
    public int getNumUploads() {
        return localFileItem.getNumUploads();
    }

    @Override
    public boolean isIncomplete() {
        return localFileItem.isIncomplete();
    }

    @Override
    public boolean isShareable() {
        return localFileItem.isShareable();
    }

    @Override
    public boolean isSharedWithGnutella() {
        return localFileItem.isSharedWithGnutella();
    }

    @Override
    public void setProperty(FilePropertyKey key, Object value) {
        localFileItem.setProperty(key, value);
    }

    @Override
    public FileMetaData toMetadata() {
        return localFileItem.toMetadata();
    }

    @Override
    public long getCreationTime() {
        return localFileItem.getCreationTime();
    }

    @Override
    public String getName() {
        return localFileItem.getName();
    }

    @Override
    public long getSize() {
        return localFileItem.getSize();
    }

    @Override
    public Category getCategory() {
        return localFileItem.getCategory();
    }

    @Override
    public String getFileName() {
        return localFileItem.getFileName();
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return localFileItem.getProperty(key);
    }

    @Override
    public String getPropertyString(FilePropertyKey filePropertyKey) {
        return localFileItem.getPropertyString(filePropertyKey);
    }

    @Override
    public URN getUrn() {
        return localFileItem.getUrn();
    }

    @Override
    public int compareTo(Object obj) {
        if (obj instanceof PlaylistFileItem) {
            return getFileName().compareToIgnoreCase(((PlaylistFileItem) obj).getFileName());
            // TODO: This is making things insanely slow -- commenting out for now.
//            return StringUtils.compareFullPrimary(getFileName(),
//                    ((PlaylistFileItem) obj).getFileName());
        } else {
            return 1;
        }
    }
}
