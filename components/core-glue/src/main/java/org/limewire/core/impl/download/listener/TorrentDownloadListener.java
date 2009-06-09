package org.limewire.core.impl.download.listener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.settings.SharingSettings;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.library.FileManager;

/**
 * Listens for downloads of .torrent files to complete. When the download
 * finishes then the torrent download will be started.
 */
public class TorrentDownloadListener implements EventListener<DownloadStateEvent> {

    private final Log LOG = LogFactory.getLog(TorrentDownloadListener.class);

    private final Downloader downloader;

    private final DownloadManager downloadManager;

    private final ActivityCallback activityCallback;

    private final List<DownloadItem> downloadItems;

    private final FileManager fileManager;

    private final TorrentManager torrentManager;

    @Inject
    public TorrentDownloadListener(DownloadManager downloadManager,
            ActivityCallback activityCallback, FileManager fileManager,
            TorrentManager torrentManager, @Assisted List<DownloadItem> downloadItems,
            @Assisted Downloader downloader) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.torrentManager = Objects.nonNull(torrentManager, "torrentManager");
        this.fileManager = Objects.nonNull(fileManager, "fileManager");
        this.activityCallback = Objects.nonNull(activityCallback, "activityCallback");
        this.downloadItems = Objects.nonNull(downloadItems, "downloadItems");

        if (downloader.getState() == DownloadState.COMPLETE) {
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
                handleBTTorrentFileDownloader();
            } else {
                handleCoreDownloader();
            }
        }
    }

    private void handleCoreDownloader() {
        File possibleTorrentFile = null;

        possibleTorrentFile = downloader.getSaveFile();
        String fileExtension = FileUtils.getFileExtension(possibleTorrentFile);
        if ("torrent".equalsIgnoreCase(fileExtension)) {
            try {
                shareTorrentFile(possibleTorrentFile);
                downloadManager.downloadTorrent(possibleTorrentFile, false);
                downloadItems.remove(getDownloadItem(downloader));
            } catch (SaveLocationException sle) {
                final File torrentFile = possibleTorrentFile;
                activityCallback.handleSaveLocationException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws SaveLocationException {
                        downloadManager.downloadTorrent(torrentFile, overwrite);
                        downloadItems.remove(getDownloadItem(downloader));
                    }

                    @Override
                    public void downloadCanceled(SaveLocationException sle) {
                        // nothing to do
                    }

                }, sle, false);
            }
        }
    }

    private void handleBTTorrentFileDownloader() {
        File torrentFile = null;
        final BTTorrentFileDownloader btTorrentFileDownloader = (BTTorrentFileDownloader) downloader;
        try {
            torrentFile = btTorrentFileDownloader.getTorrentFile();
            shareTorrentFile(torrentFile);
            downloadManager.downloadTorrent(torrentFile, false);
            downloadItems.remove(getDownloadItem(downloader));
        } catch (SaveLocationException sle) {
            final File torrentFileCopy = torrentFile;
            activityCallback.handleSaveLocationException(new DownloadAction() {
                @Override
                public void download(File saveFile, boolean overwrite) throws SaveLocationException {
                    downloadManager.downloadTorrent(torrentFileCopy, overwrite);
                    downloadItems.remove(getDownloadItem(downloader));
                }

                @Override
                public void downloadCanceled(SaveLocationException sle) {
                    if (!torrentManager.isDownloadingTorrent(torrentFileCopy)) {
                        // need to delete to clean up the torrent file in the
                        // incomplete directory
                        FileUtils.delete(torrentFileCopy, false);
                    }
                }

            }, sle, false);
        }
    }

    DownloadItem getDownloadItem(Downloader downloader) {
        DownloadItem item = (DownloadItem) downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
        return item;
    }

    private File getSharedTorrentMetaDataFile(BTData btData) {
        String fileName = btData.getName().concat(".torrent");
        File f = new File(SharingSettings.getSaveDirectory(), fileName);
        return f;
    }

    /**
     * Returns true if the code was executed correctly. False if there was an
     * error trying to share the file. If the file was not supposed to be
     * shared, and was not shared, true would still be returned.
     */
    private boolean shareTorrentFile(File torrentFile) {
        if (torrentManager.isDownloadingTorrent(torrentFile)) {
            return true;
        }
        
        if (!SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()) {
            return true;
        }

        BTData btData = null;
        FileInputStream torrentInputStream = null;
        try {
            torrentInputStream = new FileInputStream(torrentFile);
            Map<?, ?> torrentFileMap = (Map<?, ?>) Token.parse(torrentInputStream.getChannel());
            btData = new BTDataImpl(torrentFileMap);
        } catch (IOException e) {
            LOG.error("Error reading torrent file: " + torrentFile, e);
            return false;
        } finally {
            FileUtils.close(torrentInputStream);
        }

        if (btData.isPrivate()) {
            fileManager.getGnutellaFileList().remove(torrentFile);
            return true;
        }

        File saveDir = SharingSettings.getSaveDirectory();
        File torrentParent = torrentFile.getParentFile(); 
        if (torrentParent.equals(saveDir)) {
            // already in saveDir
            fileManager.getGnutellaFileList().add(torrentFile);
            return true;
        }

        final File tFile = getSharedTorrentMetaDataFile(btData);
        if (tFile.equals(torrentFile)) {
            fileManager.getGnutellaFileList().add(tFile);
            return true;
        }

        fileManager.getGnutellaFileList().remove(tFile);
        File backup = null;
        if (tFile.exists()) {
            backup = new File(tFile.getParent(), tFile.getName().concat(".bak"));
            FileUtils.forceRename(tFile, backup);
        }

        if (FileUtils.copy(torrentFile, tFile)) {
            fileManager.getGnutellaFileList().add(tFile);
        } else {
            if (backup != null) {
                // restore backup
                if (FileUtils.forceRename(backup, tFile)) {
                    fileManager.getGnutellaFileList().add(tFile);
                }
            }
        }
        return true;
    }
}