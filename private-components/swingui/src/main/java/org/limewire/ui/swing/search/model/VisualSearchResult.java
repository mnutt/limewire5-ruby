package org.limewire.ui.swing.search.model;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.nav.NavSelectable;

public interface VisualSearchResult extends NavSelectable, PropertiableFile {

    List<SearchResult> getCoreSearchResults();

    BasicDownloadState getDownloadState();

    String getFileExtension();
    
    Map<FilePropertyKey, Object> getProperties();

    String getPropertyString(FilePropertyKey key);

    Collection<RemoteHost> getSources();
    
    List<VisualSearchResult> getSimilarResults();
    
    VisualSearchResult getSimilarityParent();

    long getSize();
    
    void setDownloadState(BasicDownloadState downloadState);
    
    boolean isVisible();

    void setVisible(boolean visible);
        
    void addPropertyChangeListener(PropertyChangeListener listener);
    
    void removePropertyChangeListener(PropertyChangeListener listener);

    boolean isChildrenVisible();
    
    void setChildrenVisible(boolean childrenVisible);
    
    void toggleChildrenVisibility();
    
    boolean isSpam();
    
    void setSpam(boolean spam);

    public void addSimilarSearchResult(VisualSearchResult similarResult);

    public void removeSimilarSearchResult(VisualSearchResult result);

    public void setSimilarityParent(VisualSearchResult parent);
    
    public String getMagnetLink();

    String getHeading();

    String getSubHeading();
    
    double getRelevance();
    
    void setShowingContextOptions(boolean showing);
    
    boolean isShowingContextOptions();
    
    boolean isPreExistingDownload();
    void setPreExistingDownload(boolean preExistingDownload);

    /**
     * @return true if the associated file contains a license
     */
    boolean isLicensed();

}