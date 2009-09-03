package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.bittorrent.TorrentState;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.bittorrent.util.TorrentUtil;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPortImpl;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.serial.BTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.LibTorrentBTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.LibTorrentBTDownloadMementoImpl;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.malware.DangerousFileChecker;

/**
 * Wraps the Torrent class in the Downloader interface to enable the gui to
 * treat the torrent downloader as a normal downloader.
 */
public class BTDownloaderImpl extends AbstractCoreDownloader implements BTDownloader,
        EventListener<TorrentEvent> {

    private static final Log LOG = LogFactory.getLog(BTDownloaderImpl.class);
    private static final String DANGEROUS_TORRENT_WARNING = I18nMarker.marktr(
        "This torrent may have been designed to damage your computer.\n" +
        "LimeWire has cancelled the download for your protection.");
    
    @InspectablePrimitive(value = "number of torrents started", category = DataCategory.USAGE)
    private static final AtomicInteger torrentsStarted = new AtomicInteger();
    @InspectablePrimitive(value = "number of torrents finished", category = DataCategory.USAGE)
    private static final AtomicInteger torrentsFinished = new AtomicInteger();
    
    private final DownloadManager downloadManager;
    private final Torrent torrent;
    private final BTUploaderFactory btUploaderFactory;
    private final AtomicBoolean finishing = new AtomicBoolean(false);
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Library library;
    private final EventMulticaster<DownloadStateEvent> listeners;
    private final AtomicReference<DownloadState> lastState = new AtomicReference<DownloadState>(DownloadState.QUEUED);
    private final FileCollection gnutellaFileCollection;
    private final Provider<TorrentUploadManager> torrentUploadManager;
    private final Provider<DangerousFileChecker> dangerousFileChecker;
    private final Provider<DownloadCallback> downloadCallback;
    
    /**
     * Torrent info hash based URN used as a cache for getSha1Urn().
     */
    private volatile URN urn = null;

    @Inject
    BTDownloaderImpl(SaveLocationManager saveLocationManager,
            DownloadManager downloadManager,
            BTUploaderFactory btUploaderFactory,
            Provider<Torrent> torrentProvider,
            Library library,
            @Named("fastExecutor") ScheduledExecutorService fastExecutor,
            @GnutellaFiles FileCollection gnutellaFileCollection,
            Provider<TorrentUploadManager> torrentUploadManager,
            Provider<DangerousFileChecker> dangerousFileChecker,
            Provider<DownloadCallback> downloadCallback) {
        super(saveLocationManager);
        this.downloadManager = downloadManager;
        this.btUploaderFactory = btUploaderFactory;
        this.torrent = torrentProvider.get();
        this.library = library;
        this.gnutellaFileCollection = gnutellaFileCollection;
        this.listeners = new AsynchronousMulticasterImpl<DownloadStateEvent>(fastExecutor);
        this.torrentUploadManager = torrentUploadManager;
        this.dangerousFileChecker = dangerousFileChecker;
        this.downloadCallback = downloadCallback;
    }

    /**
     * Registers the a listener on the torrent to update internal state of the
     * downloader, based on updates to the torrent.
     */
    @Inject
    public void registerTorrentListener() {
        torrent.addListener(this);
    }

    @Override
    public void handleEvent(TorrentEvent event) {
        if (TorrentEvent.COMPLETED == event && !complete.get()) {
            finishing.set(true);
            torrentsFinished.incrementAndGet();
            if (checkForDangerousFiles()) {
                return;
            }
            FileUtils.forceDeleteRecursive(getSaveFile());
            File completeDir = getSaveFile().getParentFile();
            torrent.moveTorrent(completeDir);
            createUploadMemento();
            cleanupPriorityZeroFiles();
            File completeFile = getSaveFile();
            addFileToCollections(completeFile);
            complete.set(true);
            deleteIncompleteFiles();
            lastState.set(DownloadState.COMPLETE);
            listeners.broadcast(new DownloadStateEvent(this, DownloadState.COMPLETE));
            BTDownloaderImpl.this.downloadManager.remove(BTDownloaderImpl.this, true);
        } else if (TorrentEvent.STOPPED == event) {
            torrent.removeListener(this);
            lastState.set(DownloadState.ABORTED);
            listeners.broadcast(new DownloadStateEvent(this, DownloadState.ABORTED));
            BTDownloaderImpl.this.downloadManager.remove(BTDownloaderImpl.this, true);
        } else if (TorrentEvent.FAST_RESUME_FILE_SAVED == event) {
            // nothing to do now.
        } else if(TorrentEvent.STARTED == event) {
            torrentsStarted.incrementAndGet();    
        } else if (TorrentEvent.META_DATA_UPDATED == event) {
            if(!finishing.get() && !complete.get()) {
                //TODO should be able to remove once the libtorrent alert bug is fixed.
                torrent.initFiles();
            }
        } else {
            DownloadState currentState = getState();
            if (lastState.getAndSet(currentState) != currentState) {
                listeners.broadcast(new DownloadStateEvent(this, currentState));
            }
        }
    }

    private void createUploadMemento() {
        try {
            torrentUploadManager.get().writeMemento(torrent);
            torrent.setAutoManaged(true);
        } catch (IOException e) {
            LOG.error("Error saving torrent upload menento for torrent: " + torrent.getName(), e);
            // non-fatal, upload will just not be loaded on application
            // restart
        }
    }

    /**
     * Returns true if there are any Dangerous Files in this torrent after
     * warning the user about them.
     */
    private boolean checkForDangerousFiles() {
            // If the torrent contains any dangerous files, delete everything
            // and inform the user that the download has been cancelled.
            for(File f : getIncompleteFiles()) {
                if(dangerousFileChecker.get().isDangerous(f)) {
                    torrent.stop();
                    listeners.broadcast(new DownloadStateEvent(this, DownloadState.DANGEROUS));
                    downloadCallback.get().warnUser(getSaveFile().getName(),
                            DANGEROUS_TORRENT_WARNING);
                    return true;
                }
            }
        return false;
    }

    /**
     * Checks to see if this torrent has any priority zero files and removes
     * them.
     */
    private void cleanupPriorityZeroFiles() {
        boolean hasAnyPriorityZero = false;

        List<TorrentFileEntry> fileEntries = torrent.getTorrentFileEntries();
        for (TorrentFileEntry fileEntry : fileEntries) {
            if (fileEntry.getPriority() == 0) {
                hasAnyPriorityZero = true;
                break;
            }
        }

        if (hasAnyPriorityZero) {

            torrent.stop();// TODO for now not seeding paritally downloaded
            // torrents, we can make the last pieces of the files
            // priority zero potentially so it will not
            // download/seed those pieces, need to look into
            // this.

            // TODO should maybe not use the same TorrentFileEntry for items in
            // the TorrentInfo object, right now making a copy of the last known
            // contents, to display the correct value in the ui, in general
            // these fields are not kept up to date however.
            TorrentInfo torrentInfo = new TorrentInfo(fileEntries);
            torrent.setTorrentInfo(torrentInfo);

            for (TorrentFileEntry fileEntry : fileEntries) {
                if (fileEntry.getPriority() == 0) {
                    File torrentDataFile = torrent.getTorrentDataFile(fileEntry);
                    FileUtils.forceDelete(torrentDataFile);
                }
            }
        }
    }

    /**
     * Adds the torrents files to the gnutella share list if the torrent is not
     * private and sharing is enabled, otehrwise the files are added to the
     * library.
     */
    private void addFileToCollections(File completeFile) {

        if (completeFile.isDirectory()) {
            FileFilter torrentFileFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    // library addFile method will filter out any truly
                    // unaddable files.
                    return true;
                }
            };
            library.addFolder(completeFile, torrentFileFilter);
            if (!torrent.isPrivate()
                    && SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()) {
                gnutellaFileCollection.addFolder(completeFile, torrentFileFilter);
            }
        } else {
            library.add(completeFile);
            if (!torrent.isPrivate()
                    && SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()) {
                gnutellaFileCollection.add(completeFile);
            }
        }
    };

    /**
     * initializes this downloader from the given torrent file.
     */
    @Override
    public void init(File torrentFile, File saveDirectory) throws IOException {
        torrent.init(new TorrentParams(torrentFile));
        setDefaultFileName(torrent.getName());
    }

    @Override
    public boolean registerTorrentWithTorrentManager() {
        return torrent.registerWithTorrentManager();
    }

    /**
     * Stops a torrent download. If the torrent is in seeding state, it does
     * nothing. (To stop a seeding torrent it must be stopped from the uploads
     * pane)
     */
    @Override
    public void stop() {
        if (!torrent.isFinished()) {
            torrent.stop();
            downloadManager.remove(this, true);
        } else {
            downloadManager.remove(this, true);
        }
    }

    @Override
    public void pause() {
        torrent.pause();
    }

    @Override
    public boolean isPaused() {
        return torrent.isPaused();
    }

    @Override
    public boolean isPausable() {
        return !isPaused();
    }

    @Override
    public boolean isInactive() {
        return isResumable() || getState() == DownloadState.QUEUED;
    }

    @Override
    public boolean isLaunchable() {
        return torrent.isSingleFileTorrent();
        // TODO old logic would check last verified offest, but logic seems
        // wrong, wince the pieces download randomly, there is no guarentee that
        // the begginning of the file is ok to preview.
    }

    @Override
    public boolean isResumable() {
        return isPaused();
    }

    @Override
    public boolean resume() {
        torrent.resume();
        return true;
    }

    @Override
    public File getFile() {
        if (torrent.isFinished()) {
            return getSaveFile();
        } else {
            return getIncompleteFile();
        }
    }

    @Override
    public File getIncompleteFile() {
        return new File(SharingSettings.INCOMPLETE_DIRECTORY.get(), torrent.getName());
    }

    @Override
    public File getDownloadFragment() {
        if (isCompleted()) {
            return getSaveFile();
        }

        if (!isLaunchable()) {
            return null;
        }

        if (torrent.isMultiFileTorrent()) {
            return null;
        }

        File file = new File(getIncompleteFile().getParent(), IncompleteFileManager.PREVIEW_PREFIX
                + getIncompleteFile().getName());

        // TODO come up with correct size for preview, look at old code checking
        // last verified offsets etc. old code looks wrong though, since the
        // file downloads randomly, the last verified offset does not tell us
        // much.
        long size = Math.min(getIncompleteFile().length(), 2 * 1024 * 1024);
        if (FileUtils.copy(getIncompleteFile(), size, file) <= 0) {
            return null;
        }
        return file;

    }

    @Override
    public DownloadState getState() {
        TorrentStatus status = torrent.getStatus();
        if (!torrent.isStarted() || status == null) {
            return DownloadState.QUEUED;
        }

        TorrentState state = status.getState();

        // complete must be before aborted in order to not remove the download
        // from the list prematurely when teh seed ratio is reached and the
        // torrent is marked as cancelled.
        if (torrent.isFinished()) {
            return DownloadState.COMPLETE;
        }

        if (torrent.isCancelled()) {
            return DownloadState.ABORTED;
        }

        if (status.isError()) {
            // gave up maps to stalled in the core api, which is a recoverable
            // error. All torrent downlaods are recoverable.
            return DownloadState.GAVE_UP;
        }

        if (finishing.get()) {
            return DownloadState.SAVING;
        }

        return convertState(state);
    }

    private DownloadState convertState(TorrentState state) {
        switch (state) {
        case DOWNLOADING:
            if (isPaused()) {
                return DownloadState.PAUSED;
            } else {
                return DownloadState.DOWNLOADING;
            }
        case QUEUED_FOR_CHECKING:
            return DownloadState.RESUMING;
        case CHECKING_FILES:
            return DownloadState.RESUMING;
        case SEEDING:
            return DownloadState.COMPLETE;
        case FINISHED:
            return DownloadState.COMPLETE;
        case ALLOCATING:
            return DownloadState.CONNECTING;
        case DOWNLOADING_METADATA:
            return DownloadState.INITIALIZING;
        default:
            throw new IllegalStateException("Unknown libtorrent state: " + state);
        }
    }

    @Override
    public int getRemainingStateTime() {
        // Unused
        return 0;
    }

    @Override
    public long getContentLength() {
        TorrentStatus status = torrent.getStatus();
        long contentLength = status != null ? status.getTotalWanted() : -1;
        return contentLength;
    }

    @Override
    public long getAmountRead() {
        TorrentStatus status = torrent.getStatus();
        if (status == null) {
            return -1;
        } else {
            return status.getTotalWantedDone();
        }
    }

    @Override
    public String getVendor() {
        return BITTORRENT_DOWNLOAD;
    }

    @Override
    public void discardCorruptDownload(boolean delete) {
        // we never give up because of corruption (because this can never be
        // called)
    }

    @Override
    public List<RemoteFileDesc> getRemoteFileDescs() {
        return Collections.emptyList();
    }

    @Override
    public int getQueuePosition() {
        return 1;
    }

    @Override
    public int getNumberOfAlternateLocations() {
        return getPossibleHostCount();
    }

    @Override
    public int getNumberOfInvalidAlternateLocations() {
        return 0; // not applicable to torrents
    }

    @Override
    public int getPossibleHostCount() {
        return torrent.getNumPeers();
    }

    @Override
    public int getBusyHostCount() {
        return 0;
    }

    @Override
    public int getQueuedHostCount() {
        return 0;
    }

    @Override
    public GUID getQueryGUID() {
        // Unused for torrents
        return null;
    }

    @Override
    public boolean isCompleted() {
        return complete.get();
    }

    @Override
    public boolean shouldBeRemoved() {
        switch (getState()) {
        case ABORTED:
        case COMPLETE:
            return true;
        }
        return false;
    }

    @Override
    public long getAmountVerified() {
        TorrentStatus status = torrent.getStatus();
        if (status == null) {
            return -1;
        } else {
            return status.getTotalWantedDone();
        }
    }

    @Override
    public void measureBandwidth() {
        // Unused, we are using the bandwidth reported by libtorrent
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        return (torrent.getDownloadRate() / 1024);
    }

    @Override
    public float getAverageBandwidth() {
        // Unused by anything
        return (torrent.getDownloadRate() / 1024);
    }

    @Override
    public boolean isRelocatable() {
        return !isCompleted();
    }

    @Override
    protected File getDefaultSaveFile() {
        return new File(SharingSettings.getSaveDirectory(), torrent.getName());
    }

    @Override
    public URN getSha1Urn() {
        if (urn == null) {
            synchronized (this) {
                if (urn == null) {
                    try {
                        urn = URN.createSha1UrnFromHex(torrent.getSha1());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return urn;
    }

    @Override
    public int getNumHosts() {
        return torrent.getNumPeers();
    }

    @Override
    public List<Address> getSourcesAsAddresses() {

        List<Address> list = new LinkedList<Address>();

        List<TorrentPeer> peers = torrent.getTorrentPeers();
        for (TorrentPeer peer : peers) {
            String ip = peer.getIPAddress();
            try {
                list.add(new ConnectableImpl(new IpPortImpl(ip), false));
            } catch (UnknownHostException e) {
                // Discard invalid host
            }
        }

        return list;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void startDownload() {
        btUploaderFactory.createBTUploader(torrent);
        torrent.start();
    }

    @Override
    public void handleInactivity() {
        // nothing happens when we're inactive
    }

    @Override
    public boolean shouldBeRestarted() {
        return true;
    }

    @Override
    public boolean isAlive() {
        return false; // doesn't apply to torrents
    }

    @Override
    public boolean isQueuable() {
        return !isPaused();
    }

    @Override
    public synchronized void finish() {
        deleteIncompleteFiles();
    }

    @Override
    public String getCustomIconDescriptor() {
        return BITTORRENT_DOWNLOAD;
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.BTDOWNLOADER;
    }

    @Override
    protected DownloadMemento createMemento() {
        return new LibTorrentBTDownloadMementoImpl();
    }

    @Override
    protected void fillInMemento(DownloadMemento memento) {
        super.fillInMemento(memento);

        LibTorrentBTDownloadMemento btMemento = (LibTorrentBTDownloadMemento) memento;

        btMemento.setName(torrent.getName());
        btMemento.setSha1Urn(getSha1Urn());
        btMemento.setIncompleteFile(getIncompleteFile());
        btMemento.setTrackerURL(torrent.getTrackerURL());
        File fastResumeFile = torrent.getFastResumeFile();
        String fastResumePath = fastResumeFile != null ? fastResumeFile.getAbsolutePath() : null;
        btMemento.setFastResumePath(fastResumePath);
        File torrentFile = torrent.getTorrentFile();
        String torrentPath = torrentFile != null ? torrentFile.getAbsolutePath() : null;
        btMemento.setTorrentPath(torrentPath);

        btMemento.setPrivate(torrent.isPrivate());

    }

    public void initFromCurrentMemento(LibTorrentBTDownloadMemento memento)
            throws InvalidDataException {
        urn = memento.getSha1Urn();

        if (urn == null) {
            throw new InvalidDataException(
                    "Null SHA1 URN retrieved from LibTorrent torrent momento.");
        }

        if (!urn.isSHA1()) {
            throw new InvalidDataException(
                    "Non SHA1 URN retrieved from LibTorrent torrent momento.");
        }

        String fastResumePath = memento.getFastResumePath();
        File fastResumeFile = fastResumePath != null ? new File(fastResumePath) : null;

        String torrentPath = memento.getTorrentPath();
        File torrentFile = torrentPath != null ? new File(torrentPath) : null;

        try {
            TorrentParams params = new TorrentParams(memento.getName(), StringUtils.toHexString(urn
                    .getBytes()));
            params.trackerURL(memento.getTrackerURL()).fastResumeFile(fastResumeFile).torrentFile(
                    torrentFile).torrentDataFile(memento.getIncompleteFile()).isPrivate(
                    memento.isPrivate());
            torrent.init(params);
        } catch (IOException e) {
            // the .torrent file could be invalid, try to initialize just with
            // the memento contents.
            try {
                TorrentParams params = new TorrentParams(memento.getName(), StringUtils
                        .toHexString(urn.getBytes()));
                params.trackerURL(memento.getTrackerURL()).fastResumeFile(fastResumeFile)
                        .torrentDataFile(memento.getIncompleteFile())
                        .isPrivate(memento.isPrivate());
                torrent.init(params);
            } catch (IOException e1) {
                throw new InvalidDataException("Could not initialize the BTDownloader", e1);
            }
        }
    }

    public void initFromOldMemento(BTDownloadMemento memento) throws InvalidDataException {
        BTMetaInfoMemento btmetainfo = memento.getBtMetaInfoMemento();

        URI[] trackers = btmetainfo.getTrackers();
        URI tracker1 = trackers[0];

        String name = btmetainfo.getFileSystem().getName();

        byte[] infoHash = btmetainfo.getInfoHash();

        String sha1 = StringUtils.toHexString(infoHash);

        boolean isPrivate = btmetainfo.isPrivate();

        File saveFile = memento.getSaveFile();
        File saveDir = saveFile == null ? SharingSettings.getSaveDirectory() : saveFile
                .getParentFile();
        saveDir = saveDir == null ? SharingSettings.getSaveDirectory() : saveDir;
        File oldIncompleteFile = btmetainfo.getFileSystem().getIncompleteFile();
        File newIncompleteFile = new File(SharingSettings.INCOMPLETE_DIRECTORY.get(), name);
        if (newIncompleteFile.exists()) {
            throw new InvalidDataException(
                    "Cannot init memento for BTDownloader, incomplete file already exists: "
                            + newIncompleteFile);
        }

        FileUtils.forceRename(oldIncompleteFile, newIncompleteFile);
        File torrentDir = oldIncompleteFile.getParentFile();
        if (torrentDir.getName().length() == 32) {
            // looks like the old torrent dir
            FileUtils.forceDeleteRecursive(torrentDir);
        }

        try {
            TorrentParams params = new TorrentParams(name, sha1);
            params.trackerURL(tracker1.toString()).torrentDataFile(newIncompleteFile).isPrivate(
                    isPrivate);
            torrent.init(params);
        } catch (IOException e) {
            throw new InvalidDataException("Could not initialize the BTDownloader", e);
        }
    }

    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        super.initFromMemento(memento);
        if (BTDownloadMemento.class.isInstance(memento)) {
            initFromOldMemento((BTDownloadMemento) memento);
        } else if (LibTorrentBTDownloadMemento.class.isInstance(memento)) {
            initFromCurrentMemento((LibTorrentBTDownloadMemento) memento);
        }
        if (!registerTorrentWithTorrentManager())
            throw new InvalidDataException("Error registering torrent");
    }

    /**
     * Adds basic DownloadStateEvent listener support. Currently only
     * broadcasts, COMPLETED and ABORTED states.
     */
    @Override
    public void addListener(EventListener<DownloadStateEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<DownloadStateEvent> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public void deleteIncompleteFiles() {
        if (!complete.get()) {
            File incompleteFile = getIncompleteFile();
            if (incompleteFile != null) {
                FileUtils.forceDeleteRecursive(incompleteFile);
            }
        }
    }

    @Override
    public List<File> getCompleteFiles() {
        return TorrentUtil.buildTorrentFiles(torrent, getSaveFile().getParentFile());
    }

    public List<File> getIncompleteFiles() {
        return TorrentUtil.buildTorrentFiles(torrent, getIncompleteFile().getParentFile());
    }

    @Override
    public boolean conflicts(URN urn, long fileSize, File... file) {
        if (getSha1Urn().equals(urn)) {
            return true;
        }

        for (File f : file) {
            if (conflictsSaveFile(f)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean conflictsSaveFile(File complete) {
        return complete.equals(getSaveFile());
    }

    @Override
    public boolean conflictsWithIncompleteFile(File incomplete) {
        return incomplete.equals(getIncompleteFile());
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public int getChunkSize() {
        throw new UnsupportedOperationException("BTDownloaderImpl.getChunkSize() not implemented");
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public long getAmountLost() {
        throw new UnsupportedOperationException("BTDownloaderImpl.getAmountLost() not implemented");
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public int getAmountPending() {
        throw new UnsupportedOperationException(
                "BTDownloaderImpl.getAmountPending() not implemented");
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public int getTriedHostCount() {
        throw new UnsupportedOperationException(
                "BTDownloaderImpl.getTriedHostCount() not implemented");
    }

    @Override
    public File getTorrentFile() {
        return torrent.getTorrentFile();
    }

    public Torrent getTorrent() {
        return torrent;
    }
}
