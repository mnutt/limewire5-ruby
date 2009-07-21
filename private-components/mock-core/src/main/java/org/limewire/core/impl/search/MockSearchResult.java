package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.MockSearch.MockRemoteHost;

public class MockSearchResult implements Cloneable, SearchResult {

    public MockSearchResult(){}
    
    private MockURN urn = null;

    private List<RemoteHost> sources = new ArrayList<RemoteHost>();

    private Map<FilePropertyKey, Object> properties = new HashMap<FilePropertyKey, Object>();

    private String extension;

    private Category resultType;

    private long size;

    private boolean spam = false;
    
    private String magnetURL;

    public void addSource(String host) {
        sources.add(new MockRemoteHost(host));
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        MockSearchResult copy = (MockSearchResult) super.clone();

        // Copy contents of all the collection fields so they aren't shared.

        copy.sources = new ArrayList<RemoteHost>();
        for (RemoteHost rh : sources)
            copy.sources.add(rh);

        copy.properties = new HashMap<FilePropertyKey, Object>();
        for (FilePropertyKey key : properties.keySet()) {
            copy.properties.put(key, properties.get(key));
        }

        return copy;
    }

    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return properties.get(key);
    }

    @Override
    public Category getCategory() {
        return resultType;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public List<RemoteHost> getSources() {
        return sources;
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setProperty(FilePropertyKey key, Object value) {
        properties.put(key, value);
    }

    public void setResultType(Category resultType) {
        this.resultType = resultType;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getProperty(FilePropertyKey.NAME) + "spam: " + spam;
    }

    @Override
    public boolean isSpam() {
        return spam;
    }

    public void setSpam(boolean spam) {
        this.spam = spam;
    }

    @Override
    public String getFileName() {
        String fileName = "";
        if (getProperty(FilePropertyKey.NAME) != null) {
            fileName = getProperty(FilePropertyKey.NAME).toString();
        }

        if (getFileExtension() != null) {
            fileName += "." + getFileExtension();
        }
        return fileName;
    }
    
    @Override
    public String getFileNameWithoutExtension() {
        return (String)getProperty(FilePropertyKey.NAME);
    }

    public void setUrn(String string) {
        this.urn = new MockURN(string);
    }
    
    private final class MockURN implements URN {
        private String urn;

        public MockURN(String urn) {
            this.urn = urn;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MockURN) {
                MockURN mockURN = (MockURN) obj;

                return urn.equals(mockURN.urn);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return urn.hashCode();
        }

        @Override
        public int compareTo(URN o) {
            if (o instanceof MockURN) {
                MockURN urnObj = (MockURN) o;
                return urn.compareTo(urnObj.urn);
            }
            return -1;
        }

        @Override
        public String toString() {
            return urn;
        }
    }

    public void setMagnetURL(String magnet) {
        this.magnetURL = magnet;
    }
    
    @Override
    public String getMagnetURL() {
        return magnetURL;
    }

    public int getRelevance() {
        return 0;
    }

    public boolean isLicensed() {
        return false;
    }
}