package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.limewire.core.api.download.DownloadException;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.version.DownloadInformation;

@Singleton
public class CoreDownloaderFactoryImpl implements CoreDownloaderFactory {

    private final Provider<ManagedDownloader> managedDownloaderFactory;

    private final Provider<MagnetDownloader> magnetDownloaderFactory;

    private final Provider<InNetworkDownloader> inNetworkDownloaderFactory;

    private final Provider<ResumeDownloader> resumeDownloaderFactory;

    private final Provider<StoreDownloader> storeDownloaderFactory;

    private final Provider<BTDownloader> btDownloaderFactory;
    
    private final Provider<BTTorrentFileDownloader> torrentFileDownloaderFactory;

    @Inject
    public CoreDownloaderFactoryImpl(Provider<ManagedDownloader> managedDownloaderFactory,
            Provider<MagnetDownloader> magnetDownloaderFactory,
            Provider<InNetworkDownloader> inNetworkDownloaderFactory,
            Provider<ResumeDownloader> resumeDownloaderFactory,
            Provider<StoreDownloader> storeDownloaderFactory,
            Provider<BTDownloader> btDownloaderFactory,
            Provider<BTTorrentFileDownloader> torrentFileDownloaderFactory) {
        this.managedDownloaderFactory = managedDownloaderFactory;
        this.magnetDownloaderFactory = magnetDownloaderFactory;
        this.inNetworkDownloaderFactory = inNetworkDownloaderFactory;
        this.resumeDownloaderFactory = resumeDownloaderFactory;
        this.storeDownloaderFactory = storeDownloaderFactory;
        this.btDownloaderFactory = btDownloaderFactory;
        this.torrentFileDownloaderFactory = torrentFileDownloaderFactory;
    }

    public ManagedDownloader createManagedDownloader(RemoteFileDesc[] files,
            GUID originalQueryGUID, File saveDirectory, String fileName, boolean overwrite)
            throws DownloadException {
        ManagedDownloader md = managedDownloaderFactory.get();
        md.addInitialSources(Arrays.asList(files), fileName);
        md.setQueryGuid(originalQueryGUID);
        md.setSaveFile(saveDirectory, fileName, overwrite);
        return md;
    }

    public MagnetDownloader createMagnetDownloader(MagnetOptions magnet, boolean overwrite,
            File saveDirectory, String fileName) throws DownloadException {
        if (!magnet.isDownloadable())
            throw new IllegalArgumentException("magnet not downloadable");
        if (fileName == null)
            fileName = magnet.getFileNameForSaving();

        MagnetDownloader md = magnetDownloaderFactory.get();
        md.addInitialSources(null, fileName);
        md.setSaveFile(saveDirectory, fileName, overwrite);
        md.setMagnet(magnet);
        return md;
    }

    public InNetworkDownloader createInNetworkDownloader(DownloadInformation info, File dir,
            long startTime) throws DownloadException {
        InNetworkDownloader id = inNetworkDownloaderFactory.get();
        id.addInitialSources(null, info.getUpdateFileName());
        id.setSaveFile(dir, info.getUpdateFileName(), true);
        id.initDownloadInformation(info, startTime);
        return id;
    }

    public ResumeDownloader createResumeDownloader(File incompleteFile, String name, long size)
            throws DownloadException {
        ResumeDownloader rd = resumeDownloaderFactory.get();
        rd.addInitialSources(null, name);
        rd.setSaveFile(null, name, false);
        rd.initIncompleteFile(incompleteFile, size);
        return rd;
    }

    public StoreDownloader createStoreDownloader(RemoteFileDesc rfd, File saveDirectory,
            String fileName, boolean overwrite) throws DownloadException {
        StoreDownloader sd = storeDownloaderFactory.get();
        sd.addInitialSources(Collections.singletonList(rfd), fileName);
        sd.setSaveFile(saveDirectory, fileName, overwrite);
        return sd;
    }

    @Override
    public BTDownloader createBTDownloader(File torrent, File saveDirectory) throws IOException {
        BTDownloader bd = btDownloaderFactory.get();
        bd.init(torrent, saveDirectory);
        return bd;
    }
    
    @Override
    public BTTorrentFileDownloader createTorrentFileDownloader(URI torrentURI, boolean overwrite) {
        BTTorrentFileDownloader torrentFileDownloader = torrentFileDownloaderFactory.get();
        torrentFileDownloader.initDownloadInformation(torrentURI, overwrite);
        return torrentFileDownloader;
    }

    public CoreDownloader createFromMemento(DownloadMemento memento) throws InvalidDataException {
        try {
            Provider<? extends CoreDownloader> coreFactory = providerForMemento(memento);
            CoreDownloader downloader = coreFactory.get();
            downloader.initFromMemento(memento);
            return downloader;
        } catch (Throwable t) {
            throw new InvalidDataException("invalid memento!", t);
        }
    }

    private Provider<? extends CoreDownloader> providerForMemento(DownloadMemento memento)
            throws InvalidDataException {
        switch (memento.getDownloadType()) {
        case BTDOWNLOADER:
            return btDownloaderFactory;
        case INNETWORK:
            return inNetworkDownloaderFactory;
        case MAGNET:
            return magnetDownloaderFactory;
        case MANAGED:
            return managedDownloaderFactory;
        case STORE:
            return storeDownloaderFactory;
        case TORRENTFETCHER:
        case MOZILLA:
        default:
            throw new InvalidDataException("invalid memento type: " + memento.getDownloadType());
        }

    }

}
