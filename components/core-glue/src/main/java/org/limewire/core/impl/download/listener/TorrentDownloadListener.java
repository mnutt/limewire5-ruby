package org.limewire.core.impl.download.listener;

import java.io.File;
import java.util.List;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;

/**
 * Listens for downloads of .torrent files to complete. When the download
 * finishes then the torrent download will be started.
 */
public class TorrentDownloadListener implements EventListener<DownloadStateEvent> {
    
    private final Downloader downloader;

    private final DownloadManager downloadManager;

    private final ActivityCallback activityCallback;

    private final List<DownloadItem> downloadItems;

    public TorrentDownloadListener(DownloadManager downloadManager, ActivityCallback activityCallback,
            List<DownloadItem> downloadItems, Downloader downloader) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.activityCallback = Objects.nonNull(activityCallback, "activityCallback");
        this.downloadItems = Objects.nonNull(downloadItems, "downloadItems");

        if (downloader.getState() == DownloadState.COMPLETE) {
            // TODO not sure why Downloader and CoreDownloader are not merged
            // into one class.
            // No classes implement Downloader, CoreDownloader extends
            // Downloader, and all downloaders implement CoreDownloader
            if (downloader instanceof CoreDownloader) {
                handleEvent(new DownloadStateEvent((CoreDownloader) downloader,
                        DownloadState.COMPLETE));
            }
        }
    }

    @Override
    public void handleEvent(DownloadStateEvent event) {
        DownloadState downloadStatus = event.getType();
        if (DownloadState.COMPLETE == downloadStatus) {
            if (downloader instanceof BTTorrentFileDownloader) {
                BTMetaInfo btMetaInfo = null;
                try {
                    BTTorrentFileDownloader btTorrentFileDownloader = (BTTorrentFileDownloader) downloader;
                    btMetaInfo = btTorrentFileDownloader.getBtMetaInfo();
                    downloadManager.downloadTorrent(btMetaInfo, false);
                    downloadItems.remove(getDownloadItem(downloader));
                } catch (SaveLocationException sle) {
                    final BTMetaInfo btMetaInfoCopy = btMetaInfo;
                    activityCallback.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            downloadManager.downloadTorrent(btMetaInfoCopy, overwrite);
                            downloadItems.remove(getDownloadItem(downloader));
                        }
                    }, sle, false);
                }
            } else {
                File possibleTorrentFile = null;
                try {
                    possibleTorrentFile = downloader.getSaveFile();
                    String fileExtension = FileUtils.getFileExtension(possibleTorrentFile);
                    if ("torrent".equalsIgnoreCase(fileExtension)) {
                        downloadManager.downloadTorrent(possibleTorrentFile, false);
                        downloadItems.remove(getDownloadItem(downloader));
                    }
                } catch (SaveLocationException sle) {
                    final File torrentFile = possibleTorrentFile;
                    activityCallback.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            downloadManager.downloadTorrent(torrentFile, overwrite);
                            downloadItems.remove(getDownloadItem(downloader));
                        }
                    }, sle, false);
                }
            }
        }
    }

    DownloadItem getDownloadItem(Downloader downloader) {
        DownloadItem item = (DownloadItem) downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
        return item;
    }
}