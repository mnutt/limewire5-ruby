package org.limewire.core.impl.download.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.listener.EventListener;
import org.limewire.util.Objects;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;

/**
 * Listens for the completion of downloads, adding completed downloads to the
 * DownloadSettings.RECENT_DOWNLOADS list.
 */
public class RecentDownloadListener implements EventListener<DownloadStatusEvent> {
    private static final int MAX_TRACKED_DOWNLOADS = 10;

    private final Downloader downloader;

    public RecentDownloadListener(Downloader downloader) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        if (downloader.getState() == DownloadStatus.COMPLETE) {
            if (downloader instanceof CoreDownloader) {
                handleEvent(new DownloadStatusEvent((CoreDownloader) downloader,
                        DownloadStatus.COMPLETE));
            }
        }
    }

    @Override
    public void handleEvent(DownloadStatusEvent event) {
        // TODO don't do anything for torrent downloads?
        DownloadStatus downloadStatus = event.getType();
        if (DownloadStatus.COMPLETE == downloadStatus) {
            File saveFile = downloader.getSaveFile();
            if (saveFile != null) {
                if (DownloadSettings.REMEMBER_RECENT_DOWNLOADS.getValue()) {
                    synchronized (RecentDownloadListener.class) {
                        List<File> files;
                        synchronized (DownloadSettings.RECENT_DOWNLOADS) {
                            files = new ArrayList<File>(DownloadSettings.RECENT_DOWNLOADS.getValue());
                        }
                        files.add(saveFile);
                        Collections.sort(files, new FileDateLeastToMostRecentComparator());
                        while(files.size() > MAX_TRACKED_DOWNLOADS) {
                            files.remove(0);
                        }
                        DownloadSettings.RECENT_DOWNLOADS.setValue(new HashSet<File>(files));
                    }
                }
            }
        }
    }

    /**
     * Orders files from least to most recent.
     */
    private class FileDateLeastToMostRecentComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return Long.valueOf(o1.lastModified()).compareTo(Long.valueOf(o2.lastModified()));
        }
    }
}