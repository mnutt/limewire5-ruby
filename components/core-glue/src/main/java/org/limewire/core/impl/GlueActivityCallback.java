package org.limewire.core.impl;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.impl.download.DownloadListener;
import org.limewire.core.impl.download.DownloadListenerList;
import org.limewire.core.impl.monitor.IncomingSearchListener;
import org.limewire.core.impl.monitor.IncomingSearchListenerList;
import org.limewire.core.impl.search.QueryReplyListener;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.core.impl.upload.UploadListener;
import org.limewire.core.impl.upload.UploadListenerList;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentEvent;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * An implementation of the UI callback to handle notifications about 
 * asynchronous backend events.
 */
@Singleton
class GlueActivityCallback implements ActivityCallback, QueryReplyListenerList,
        DownloadListenerList, UploadListenerList, 
        IncomingSearchListenerList, GuiCallbackService {

    private final SortedMap<byte[], List<QueryReplyListener>> queryReplyListeners;

    private final List<DownloadListener> downloadListeners = new CopyOnWriteArrayList<DownloadListener>();
    
    private final List<UploadListener> uploadListeners = new CopyOnWriteArrayList<UploadListener>();

    private final List<IncomingSearchListener> monitorListeners = new CopyOnWriteArrayList<IncomingSearchListener>();

    private final DownloadManager downloadManager;
    
    private final TorrentManager torrentManager;
    
    private GuiCallback guiCallback = null;
    
    
    
    @Inject
    public GlueActivityCallback(DownloadManager downloadManager, TorrentManager torrentManager) {
        this.downloadManager = downloadManager;
        this.torrentManager = torrentManager;
        queryReplyListeners = new ConcurrentSkipListMap<byte[], List<QueryReplyListener>>(
                GUID.GUID_BYTE_COMPARATOR);
    }

    @Override
    public void addQueryReplyListener(byte[] guid, QueryReplyListener listener) {
        synchronized (queryReplyListeners) {
            List<QueryReplyListener> listeners = queryReplyListeners.get(guid);
            if (listeners == null) {
                listeners = new CopyOnWriteArrayList<QueryReplyListener>();
                queryReplyListeners.put(guid, listeners);
            }
            listeners.add(listener);
        }
    }

    @Override
    public void removeQueryReplyListener(byte[] guid, QueryReplyListener listener) {
        synchronized (queryReplyListeners) {
            List<QueryReplyListener> listeners = queryReplyListeners.get(guid);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    queryReplyListeners.remove(guid);
                }
            }
        }
    }

    @Override
    public void addDownloadListener(DownloadListener listener) {
        downloadListeners.add(listener);
    }

    @Override
    public void removeDownloadListener(DownloadListener listener) {
        downloadListeners.remove(listener);
    }

    @Override
    public void acceptChat(InstantMessenger ctr) {
        // TODO Auto-generated method stub

    }


    @Override
    public void chatErrorMessage(InstantMessenger chatter, String str) {
        // TODO Auto-generated method stub

    }

    @Override
    public void chatUnavailable(InstantMessenger chatter) {
        // TODO Auto-generated method stub
    }

    public void handleAddressStateChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        // Does nothing.
    }

    @Override
    public boolean handleDAAPConnectionError(Throwable t) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMagnets(MagnetOptions[] magnets) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        List<QueryReplyListener> listeners = queryReplyListeners.get(queryReply.getGUID());
        if (listeners != null) {
            for (QueryReplyListener listener : listeners) {
                listener.handleQueryReply(rfd, queryReply, locs);
            }
        }
    }

    @Override
    public void handleQueryString(String query) {
        for (IncomingSearchListener listener : monitorListeners) {
            listener.handleQueryString(query);
        }
    }

    @Override
    public void handleSharedFileUpdate(File file) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleTorrent(File torrentFile) {
        try {
            downloadManager.downloadTorrent(torrentFile, false);
        } catch (SaveLocationException e) {
            handleSaveLocationException(new DownloadAction() {
              @Override
                public void download(File saveFile, boolean overwrite) throws SaveLocationException {
                      downloadManager.downloadTorrent(saveFile, overwrite);
                }  
            },e,false);
        }
    }

    @Override
    public void installationCorrupted() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isQueryAlive(GUID guid) {
        return queryReplyListeners.containsKey(guid.bytes());
    }

    @Override
    public void receiveMessage(InstantMessenger chr, String messsage) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addUpload(Uploader u) {
        for (UploadListener listener : uploadListeners) {
            listener.uploadAdded(u);
        }
    }
    
    @Override
    public void removeUpload(Uploader u) {
        for (UploadListener listener : uploadListeners) {
            listener.uploadRemoved(u);
        }
    }

    @Override
    public void restoreApplication() {
        // TODO Auto-generated method stub

    }

    @Override
    public String translate(String s) {
        return s; // TODO: Plug this to the UI somehow.
    }

    @Override
    public void uploadsComplete() {
        for (UploadListener listener : uploadListeners) {
            listener.uploadsCompleted();
        }
    }

    @Override
    public boolean warnAboutSharingSensitiveDirectory(File dir) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addDownload(Downloader d) {
        for (DownloadListener listener : downloadListeners) {
            listener.downloadAdded(d);
        }
    }
    
    @Override
    public void downloadsComplete() {
        for (DownloadListener listener : downloadListeners) {
            listener.downloadsCompleted();
        }
    }
    
    @Override
    public void promptAboutCorruptDownload(Downloader dloader) {
        //just kill the download if it is corrupt
        dloader.discardCorruptDownload(true);

    }
    
    @Override
    public void downloadCompleted(Downloader d) {
        for (DownloadListener listener : downloadListeners) {
            listener.downloadRemoved(d);
        }
    }
    
    @Override
    public void showDownloads() {
        // TODO Auto-generated method stub
    }
    
    public void setGuiCallback(GuiCallback guiCallback) {
        this.guiCallback = guiCallback;
    }

    @Override
    public void handleSaveLocationException(DownloadAction downLoadAction,
            SaveLocationException sle, boolean supportsNewSaveDir) {
        if(guiCallback != null) {
            guiCallback.handleSaveLocationException(downLoadAction, sle, supportsNewSaveDir);
        } else {
            throw new UnsupportedOperationException("TODO notify user");
        }
    }

    @Override
    public void addUploadListener(UploadListener listener) {
        uploadListeners.add(listener);
    }

    @Override
    public void removeUploadListener(UploadListener listener) {
        uploadListeners.remove(listener);
    }

    @Override
    public void addIncomingSearchListener(IncomingSearchListener listener) {
        monitorListeners.add(listener);
    }

    @Override
    public void removeIncomingSearchListener(IncomingSearchListener listener) {
        monitorListeners.remove(listener);
    }

    @Override
    public void promptTorrentUploadCancel(ManagedTorrent torrent) {
        if(guiCallback != null) {
            if (!torrent.isActive())
                return;
            boolean approve = true;
            if (!torrent.isComplete()) {
                approve = guiCallback.promptTorrentDownloading();
            } else if (torrent.getRatio() < 1.0f) {
                approve = guiCallback.promptTorrentSeedRatioLow();
            }
    
            if (approve && torrent.isActive()) {
                torrentManager
                        .dispatchEvent(new TorrentEvent(this, TorrentEvent.Type.STOP_APPROVED, torrent));
            }
        } else {
            throw new UnsupportedOperationException("TODO notify user");
        }
    }
    
}
