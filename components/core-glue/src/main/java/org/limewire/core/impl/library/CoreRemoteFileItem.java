package org.limewire.core.impl.library;

import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.RemoteFileDesc;

public class CoreRemoteFileItem implements RemoteFileItem, Comparable {
    private final RemoteFileDescAdapter searchResult;

    public CoreRemoteFileItem(RemoteFileDescAdapter rfd) {
        this.searchResult = rfd;
    }
    
    public RemoteFileDesc getRfd() {
        return searchResult.getRfd();
    }
    
    public SearchResult getSearchResult() {
        return searchResult;
    }
    
    @Override
    public String getName() {
        return (String)searchResult.getProperty(FilePropertyKey.NAME);
    }
    
    @Override
    public String getFileName(){
        return getRfd().getFileName();
    }
    
    @Override
    public long getSize() {
        return searchResult.getSize();
    }
    
    @Override
    public long getCreationTime() {
        Long time = (Long)searchResult.getProperty(FilePropertyKey.DATE_CREATED);
        if(time == null)
            return -1;
        else
            return time;
    }

    @Override
    public Category getCategory() {
        return searchResult.getCategory();   
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return searchResult.getProperty(key);
    }
    
    @Override
    public List<RemoteHost> getSources() {
        return searchResult.getSources();
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();
            return stringValue;
        } else {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    @Override
    public URN getUrn() {
        return searchResult.getUrn();
    }

    @Override
    public int compareTo(Object obj) {
        if (getClass() != obj.getClass()) {
            return -1;
        }
        return getFileName().compareToIgnoreCase(((CoreRemoteFileItem) obj).getFileName());
    }

}
