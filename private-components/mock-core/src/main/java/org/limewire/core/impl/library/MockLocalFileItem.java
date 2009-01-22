package org.limewire.core.impl.library;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.xmpp.api.client.FileMetaData;

//import com.limegroup.gnutella.FileDetails;

public class MockLocalFileItem implements LocalFileItem {

    private final String name;
    private final long size;
    private final long creationTime;
    private final long lastModifiedTime;
    private final int numHits;
    private final int numUploads;
    private final Category category;
    
    public MockLocalFileItem(String name, long size, long creationTime, long lastModified,
            int numHits, int numUploads, Category category) {
        this.name = name;
        this.size = size;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModified;
        this.numHits = numHits;
        this.numUploads = numUploads;
        this.category = category;
    }
    
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getNumHits() {
        return numHits;
    }

    @Override
    public int getNumUploads() {
        return numUploads;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return null;
    }
    @Override
    public String getPropertyString(FilePropertyKey key) {
        return null;
    }

    public FileMetaData toMetadata() {
        return null;
    }

    @Override
    public String getFileName() {
        return "FileName.doc";
    }
    
    @Override
    public int getFriendShareCount() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public boolean isSharedWithGnutella() {
        // TODO Auto-generated method stub
        if(name.indexOf(".bmp") > -1 || name.indexOf(".jpg") > -1)
            return true;
        return false;
    }
    
    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public URN getUrn() {
        return null;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public void setProperty(FilePropertyKey key, Object value) {
        
    }

    @Override
    public int getNumUploadAttempts() {
        // TODO Auto-generated method stub
        return 0;
    }
}
