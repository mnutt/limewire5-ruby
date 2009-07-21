package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.util.List;

import com.limegroup.gnutella.URN;

public interface LibTorrentBTDownloadMemento extends DownloadMemento {

    public String getName();

    public long getContentLength();

    public File getIncompleteFile();
    
    public URN getSha1Urn();

    public String getTrackerURL();

    public List<String> getPaths();

    public String getFastResumePath();

    public String getTorrentPath();
    
    public Boolean isPrivate();
    
    public void setName(String name);

    public void setContentLength(long contentLength);

    public void setIncompleteFile(File incompleteFile);
    
    public void setSha1Urn(URN sha1Urn);

    public void setPaths(List<String> paths);

    public void setTrackerURL(String url);

    public void setFastResumePath(String data);

    public void setTorrentPath(String torrentPath);

    public void setPrivate(Boolean isPrivate);

}