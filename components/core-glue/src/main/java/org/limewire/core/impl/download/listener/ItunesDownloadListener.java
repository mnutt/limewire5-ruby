package org.limewire.core.impl.download.listener;

import java.io.File;

import org.limewire.core.impl.itunes.ItunesMediator;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.listener.EventListener;
import org.limewire.util.Objects;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;

/**
 * Listens for the completion of downloads, adding completed ituens supported downloads to the itunes library.
 */
public class ItunesDownloadListener implements EventListener<DownloadStatusEvent> {
    private final Downloader downloader;
    private final ItunesMediator itunesMediator;
    @AssistedInject
    public ItunesDownloadListener(@Assisted Downloader downloader, ItunesMediator itunesMediator) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        this.itunesMediator = itunesMediator;
        if(downloader.getState() == DownloadStatus.COMPLETE) {
            if(downloader instanceof CoreDownloader) {
                handleEvent(new DownloadStatusEvent((CoreDownloader)downloader, DownloadStatus.COMPLETE));
            }
        }
    }
    @Override
    public void handleEvent(DownloadStatusEvent event) {
        DownloadStatus downloadStatus = event.getType();
        if(DownloadStatus.COMPLETE == downloadStatus) {
            File saveFile = downloader.getSaveFile();
            if(saveFile != null) {
                if (iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue()) {
                    itunesMediator.addSong(saveFile);
                }
            }
        }
    }
}