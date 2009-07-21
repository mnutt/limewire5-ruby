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
import org.limewire.core.api.download.DownloadException;
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
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.GnutellaFiles;

/**
 * Listens for downloads of .torrent files to complete. When the download
 * finishes then the torrent download will be started.
 */
public class TorrentDownloadListener implements EventListener<DownloadStateEvent> {

    private static final Log LOG = LogFactory.getLog(TorrentDownloadListener.class);

    private final Downloader downloader;

    private final DownloadManager downloadManager;

    private final ActivityCallback activityCallback;

    private final List<DownloadItem> downloadItems;

    private final FileCollection gnutellaFileList;

    private final TorrentManager torrentManager;

    @Inject
    public TorrentDownloadListener(DownloadManager downloadManager,
            ActivityCallback activityCallback, @GnutellaFiles FileCollection gnutellaFileList,
            TorrentManager torrentManager, @Assisted List<DownloadItem> downloadItems,
            @Assisted Downloader downloader) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.torrentManager = Objects.nonNull(torrentManager, "torrentManager");
        this.gnutellaFileList = Objects.nonNull(gnutellaFileList, "gnutellaFileList");
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
                downloadManager.downloadTorrent(possibleTorrentFile, null, false);
                downloadItems.remove(getDownloadItem(downloader));
            } catch (DownloadException e) {
                final File torrentFile = possibleTorrentFile;
                activityCallback.handleDownloadException(new DownloadAction() {
                    @Override
                    public void download(File saveDirectory, boolean overwrite)
                            throws DownloadException {
                        downloadManager.downloadTorrent(torrentFile, saveDirectory, overwrite);
                        downloadItems.remove(getDownloadItem(downloader));
                    }

                    @Override
                    public void downloadCanceled(DownloadException ignored) {
                        // nothing to do
                    }

                }, e, false);
            }
        }
    }

    private void handleBTTorrentFileDownloader() {
        File torrentFile = null;
        final BTTorrentFileDownloader btTorrentFileDownloader = (BTTorrentFileDownloader) downloader;
        try {
            torrentFile = btTorrentFileDownloader.getTorrentFile();
            shareTorrentFile(torrentFile);
            downloadManager.downloadTorrent(torrentFile, null, false);
            downloadItems.remove(getDownloadItem(downloader));
        } catch (DownloadException e) {
            final File torrentFileFinal = torrentFile;
            activityCallback.handleDownloadException(new DownloadAction() {
                @Override
                public void download(File saveDirectory, boolean overwrite) throws DownloadException {
                    downloadManager.downloadTorrent(torrentFileFinal, saveDirectory, overwrite);
                    downloadItems.remove(getDownloadItem(downloader));
                }

                @Override
                public void downloadCanceled(DownloadException ex) {
                    if (!torrentManager.isDownloadingTorrent(torrentFileFinal)) {
                        // need to delete to clean up the torrent file in the
                        // incomplete directory
                        FileUtils.forceDelete(torrentFileFinal);
                    }
                }

            }, e, false);
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
            gnutellaFileList.remove(torrentFile);
            return true;
        }

        File saveDir = SharingSettings.getSaveDirectory();
        File torrentParent = torrentFile.getParentFile(); 
        if (torrentParent.equals(saveDir)) {
            // already in saveDir
            gnutellaFileList.add(torrentFile);
            return true;
        }

        final File tFile = getSharedTorrentMetaDataFile(btData);
        if (tFile.equals(torrentFile)) {
            gnutellaFileList.add(tFile);
            return true;
        }

        gnutellaFileList.remove(tFile);
        File backup = null;
        if (tFile.exists()) {
            backup = new File(tFile.getParent(), tFile.getName().concat(".bak"));
            FileUtils.forceRename(tFile, backup);
        }

        if (FileUtils.copy(torrentFile, tFile)) {
            gnutellaFileList.add(tFile);
        } else {
            if (backup != null) {
                // restore backup
                if (FileUtils.forceRename(backup, tFile)) {
                    gnutellaFileList.add(tFile);
                }
            }
        }
        return true;
    }
}