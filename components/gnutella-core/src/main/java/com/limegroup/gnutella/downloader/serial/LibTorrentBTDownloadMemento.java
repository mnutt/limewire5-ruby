package com.limegroup.gnutella.downloader.serial;

import java.io.File;

import com.limegroup.gnutella.URN;

public interface LibTorrentBTDownloadMemento extends DownloadMemento {

    public String getName();

    public File getIncompleteFile();
    
    public URN getSha1Urn();

    public String getTrackerURL();

    public String getFastResumePath();

    public String getTorrentPath();
    
    public Boolean isPrivate();
    
    public void setName(String name);

    public void setIncompleteFile(File incompleteFile);
    
    public void setSha1Urn(URN sha1Urn);

    public void setTrackerURL(String url);

    public void setFastResumePath(String data);

    public void setTorrentPath(String torrentPath);

    public void setPrivate(Boolean isPrivate);

}