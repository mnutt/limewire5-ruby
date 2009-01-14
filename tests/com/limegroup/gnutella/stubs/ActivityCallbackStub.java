package com.limegroup.gnutella.stubs;

import java.io.File;
import java.util.Set;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.io.GUID;

import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * A stub for ActivityCallback.  Does nothing.
 */
@Singleton
public class ActivityCallbackStub implements ActivityCallback {
    
    //don't delete corrupt file on detection
    public boolean delCorrupt = false;
    //if corruptness was queried
    public boolean corruptChecked = false;

    public void componentLoading(String state, String component) {}
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {}
    public void knownHost(Endpoint e) { }
    public void handleNoInternetConnection() { }
    public void installationCorrupted() {}
    //public void handleQueryReply( QueryReply qr ) { }

	public void handleQueryResult(RemoteFileDesc rfd, 
	                              QueryReply queryReply,
	                              Set alts) {}
    public void handleQueryString( String query ) { }    
    public void addDownload(Downloader d) { }    
    public void downloadCompleted(Downloader d) { }    
    public void addUpload(Uploader u) { }
    public void removeUpload(Uploader u) { }    	
    public void addSharedDirectory(final File directory, final File parent) { }
    public void addSharedFile(final FileDesc file, final File parent) { }
    public boolean warnAboutSharingSensitiveDirectory(final File dir) { return false; }
	public void clearSharedFiles() { }           
    public void downloadsComplete() { }
    public void uploadsComplete() { }
    public void error(int errorCode) { }
    public void error(int errorCode, Throwable t) { }
    public void error(Throwable t) { }
    public void promptAboutCorruptDownload(Downloader dloader) {
        corruptChecked = true;
        dloader.discardCorruptDownload(delCorrupt);
    }    
    public void browseHostFailed(GUID guid) {}
	public void restoreApplication() {}
	public void showDownloads() {}
    public String getHostValue(String key) { return null;}
    public void handleSharedFileUpdate(File file) { }
    public void updateAvailable(UpdateInformation uc) {}
    public void showError(String message, String messageKey) {}
    public boolean isQueryAlive(GUID guid) {
        return false;
    }
    public void handleAddressStateChanged() {}
	public void handleMagnets(final MagnetOptions[] magnets) {
	}

    public void handleTorrent(File torrentFile) {
	}
    public void handleDAAPConnectionError(Throwable t) {  }
    public String translate(String s) { return s;}

    @Override
    public void handleSaveLocationException(DownloadAction downLoadAction,
            SaveLocationException sle, boolean supportsNewSaveDir) {
    }

    @Override
    public void promptTorrentUploadCancel(ManagedTorrent torrent) {
        
    }
}
