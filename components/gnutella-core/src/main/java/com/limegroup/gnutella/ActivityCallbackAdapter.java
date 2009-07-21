package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.google.inject.Singleton;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * Provides a default implementation of <code>ActivityCallback</code> where
 * all the methods are either empty or return <code>false</code>. You can extend 
 * this class when you are only need specific methods.
 */
@Singleton
public class ActivityCallbackAdapter implements ActivityCallback {

    public void addUpload(Uploader u) {
        
    }

    public void browseHostFailed(GUID guid) {
        
    }

    public void componentLoading(String state, String component) {
        
    }

    public void handleAddressStateChanged() {
        
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        
    }

    public void handleMagnets(MagnetOptions[] magnets) {

    }

    public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        
    }

    public void handleQuery(QueryRequest query, String address, int port) {
        
    }

    public void handleSharedFileUpdate(File file) {
        
    }

    public void handleTorrent(File torrentFile) {
        
    }

    public void installationCorrupted() {
        
    }

    public boolean isQueryAlive(GUID guid) {
        return false;
    }

    public void removeUpload(Uploader u) {
        
    }

    public void restoreApplication() {
        
    }

    public void updateAvailable(UpdateInformation info) {
        
    }

    public void uploadsComplete() {
        
    }

    public boolean warnAboutSharingSensitiveDirectory(File dir) {
        return false;
    }

    public void addDownload(Downloader d) {
        
    }

    public void downloadsComplete() {
        
    }

    public String getHostValue(String key) {
        return null;
    }

    public void promptAboutCorruptDownload(Downloader dloader) {
       
    }
    
    public void dangerousDownloadDeleted(String filename) {
        
    }

    public void removeDownload(Downloader d) {
        
    }

    public void showDownloads() {
        
    }
    
    public void handleDAAPConnectionError(Throwable t) {  }
    public String translate(String s) { return s;}

    @Override
    public void handleDownloadException(DownloadAction downLoadAction,
            DownloadException e, boolean supportsNewSaveDir) {
    }

    @Override
    public boolean promptTorrentUploadCancel(Torrent torrent) {
        return true;
    }

}
