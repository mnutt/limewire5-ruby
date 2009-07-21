package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentState;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPortImpl;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
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

/**
 * Wraps the Torrent class in the Downloader interface to enable the gui to
 * treat the torrent downloader as a normal downloader.
 */
public class BTDownloaderImpl extends AbstractCoreDownloader implements BTDownloader,
        EventListener<TorrentEvent> {

    private final DownloadManager downloadManager;

    private final Torrent torrent;

    private final BTUploaderFactory btUploaderFactory;

    private final AtomicBoolean finishing = new AtomicBoolean(false);

    private final AtomicBoolean complete = new AtomicBoolean(false);

    private final Library library;

    private final EventMulticaster<DownloadStateEvent> listeners;
    
    private final AtomicReference<DownloadState> lastState = new AtomicReference<DownloadState>(DownloadState.QUEUED);

    private final FileCollection gnutellaFileCollection;
    
    /**
     * Torrent info hash based URN used as a cache for getSha1Urn().
     */
    private volatile URN urn = null;

    @Inject
    BTDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            BTUploaderFactory btUploaderFactory, Provider<Torrent> torrentProvider,
            Library library, @Named("fastExecutor") ScheduledExecutorService fastExecutor, @GnutellaFiles FileCollection gnutellaFileCollection) {
        super(saveLocationManager);
        this.downloadManager = downloadManager;
        this.btUploaderFactory = btUploaderFactory;
        this.torrent = torrentProvider.get();
        this.library = library;
        this.gnutellaFileCollection = gnutellaFileCollection;
        this.listeners = new AsynchronousMulticasterImpl<DownloadStateEvent>(fastExecutor);
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
            FileUtils.forceDeleteRecursive(torrent.getCompleteFile());
            File completeDir = getSaveFile().getParentFile();
            torrent.moveTorrent(completeDir);
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
            // TODO kind of an ugly way to clean this up
            if (finishing.get() || complete.get() || torrent.isCancelled()) {
                deleteIncompleteFiles();
            }
        } else {
            DownloadState currentState = getState();
            if(lastState.getAndSet(currentState) != currentState) {
                listeners.broadcast(new DownloadStateEvent(this, currentState));
            }
        }
    }

    /**
     * Adds the torrents files to the gnutella share list if the torrent is not
     * private and sharing is enabled, otehrwise the files are added to the library. 
     */
    private void addFileToCollections(File completeFile) {
        
        if (completeFile.isDirectory()) {
            FileFilter torrentFileFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    //library addFile method will filter out any truly unaddable files.
                    return true;
                }
            }; 
            library.addFolder(completeFile, torrentFileFilter);
            if(!torrent.isPrivate() && SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()) {
                gnutellaFileCollection.addFolder(completeFile, torrentFileFilter);
            }
        } else {
            library.add(completeFile);
            if(!torrent.isPrivate() && SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()) {
                gnutellaFileCollection.add(completeFile);
            }
        }
    };

    /**
     * initializes this downloader from the given torrent file.
     */
    @Override
    public void init(File torrentFile, File saveDirectory) throws IOException {
        torrent.init(null, null, -1, null, null, null, torrentFile, saveDirectory, null, null);
        File completeFile = torrent.getCompleteFile();
        setDefaultFileName(completeFile.getName());
    }

    
    @Override
    public void setSaveFile(File saveDirectory, String fileName, boolean overwrite)
            throws DownloadException {
        super.setSaveFile(saveDirectory, fileName, overwrite);
        torrent.updateSaveDirectory(saveDirectory);
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
        return torrent.getIncompleteFile();
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

        if (torrent.isCancelled()) {
            return DownloadState.ABORTED;
        }

        if (torrent.isFinished()) {
            return DownloadState.COMPLETE;
        }
        
        // TODO: This currently shows stalled which will probably
        // be inaccurate.
        if (status.isError()) {
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
        return torrent.getTotalSize();
    }

    @Override
    public long getAmountRead() {
        return torrent.getTotalDownloaded();
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
        return torrent.getTotalDownloaded();
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
        return torrent.getCompleteFile();
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

        for (String ip : torrent.getPeers()) {
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
        btMemento.setContentLength(getContentLength());
        btMemento.setIncompleteFile(getIncompleteFile());
        btMemento.setTrackerURL(torrent.getTrackerURL());
        btMemento.setPaths(torrent.getPaths());
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
        File saveDir = memento.getSaveFile().getParentFile();

        // TODO: needs a bit of cleanup
        try {
            torrent.init(memento.getName(), StringUtils.toHexString(urn.getBytes()), memento
                    .getContentLength(), memento.getTrackerURL(), memento.getPaths(),
                    fastResumeFile, torrentFile, saveDir, memento.getIncompleteFile(), memento.isPrivate());
        } catch (IOException e) {
            try {
                torrent.init(memento.getName(), StringUtils.toHexString(urn.getBytes()), memento
                        .getContentLength(), memento.getTrackerURL(), memento.getPaths(),
                        fastResumeFile, null, saveDir, memento.getIncompleteFile(), memento.isPrivate());
            } catch (IOException e1) {
                throw new InvalidDataException("Could not initialize the BTDownloader", e1);
            }
        }
    }

    public void initFromOldMemento(BTDownloadMemento memento) throws InvalidDataException {
        BTMetaInfoMemento btmetainfo = memento.getBtMetaInfoMemento();

        List<String> paths = new ArrayList<String>();

        for (TorrentFile torrentFile : btmetainfo.getFileSystem().getFiles()) {
            paths.add(torrentFile.getTorrentPath());
        }

        URI[] trackers = btmetainfo.getTrackers();
        URI tracker1 = trackers[0];

        String name = btmetainfo.getFileSystem().getName();

        long totalSize = btmetainfo.getFileSystem().getTotalSize();

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
            torrent.init(name, sha1, totalSize, tracker1.toString(), paths, null, null, saveDir,
                    null, isPrivate);
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
        if(!registerTorrentWithTorrentManager())
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
        if(!complete.get()) {
            File incompleteFile = getIncompleteFile();
            if(incompleteFile != null) {
                FileUtils.forceDeleteRecursive(incompleteFile);
            }
        }
        
        File torrentFile = torrent.getTorrentFile();
        if (torrentFile != null) {
            FileUtils.forceDelete(torrentFile);
        }

        File fastResumeFile = torrent.getFastResumeFile();
        if (fastResumeFile != null) {
            FileUtils.forceDelete(fastResumeFile);
        }
    }

    @Override
    public List<File> getCompleteFiles() {
        return torrent.getCompleteFiles();
    }

    public List<File> getIncompleteFiles() {
        return torrent.getIncompleteFiles();
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
