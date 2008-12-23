package org.limewire.ui.swing.search.model;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;

public class MockVisualSearchResult implements VisualSearchResult {
    private List<VisualSearchResult> similarResults = new ArrayList<VisualSearchResult>();
    private String name;
    private String subheading = "";
    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;
    private VisualSearchResult similarityParent;
    private boolean spam;
    private HashMap<FilePropertyKey, Object> properties = new HashMap<FilePropertyKey, Object>();
    
    public MockVisualSearchResult(String name) {
        this.name = name;
    }

    public MockVisualSearchResult(String name, VisualSearchResult parent) {
        this(name);
        this.similarityParent = parent;
        parent.getSimilarResults().add(this);
    }

    @Override
    public Category getCategory() {
        return Category.AUDIO;
    }

    @Override
    public List<SearchResult> getCoreSearchResults() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BasicDownloadState getDownloadState() {
        return downloadState;
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<FilePropertyKey, Object> getProperties() {
        return properties;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        if(key == FilePropertyKey.NAME) {
            return name; 
        } else {
            return null;
        }
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object val = getProperties().get(key);
        return val == null ? null : val.toString();
    }
    
    public void setSimilarResults(List<VisualSearchResult> similarResults) {
        this.similarResults = similarResults;
    }

    @Override
    public List<VisualSearchResult> getSimilarResults() {
        return similarResults;
    }
    
    @Override
    public VisualSearchResult getSimilarityParent() {
        return similarityParent;
    }
    
    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Collection<RemoteHost> getSources() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setDownloadState(BasicDownloadState downloadState) {
        this.downloadState = downloadState;
    }

    @Override
    public void setVisible(boolean visible) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isChildrenVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSpam() {
        return spam;
    }

    @Override
    public void setSpam(boolean spam) {
        this.spam = spam;
    }

    @Override
    public void setChildrenVisible(boolean childrenVisible) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String toString() {
        return name;
    }

    public void removeSimilarSearchResult(VisualSearchResult result) {
    }

    public void addSimilarSearchResult(VisualSearchResult similarResult) {
    }

    public void setSimilarityParent(VisualSearchResult parent) {
    }

    @Override
    public URN getUrn() {
        return null;
    }

    @Override
    public String getNavSelectionId() {
        return null;
    }

    @Override
    public String getMagnetLink() {
        return null;
    }
    
    public void setHeading(String heading) {
        this.name = heading;
    }

    @Override
    public String getHeading() {
        return name;
    }
    
    public void setSubHeading(String subheading) {
        this.subheading = subheading;
    }

    @Override
    public String getSubHeading() {
        return subheading;
    }

    @Override
    public double getRelevance() {
        return 0;
    }

    @Override
    public String getFileName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPreExistingDownload() {
        return false;
    }

    @Override
    public void setPreExistingDownload(boolean preExistingDownload) {
        
    }

    public boolean isLicensed() {
        return false;
    }

    @Override
    public void toggleChildrenVisibility() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isShowingContextOptions() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setShowingContextOptions(boolean showing) {
        // TODO Auto-generated method stub
        
    }
}
