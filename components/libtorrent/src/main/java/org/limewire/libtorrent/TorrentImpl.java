package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentAlert;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.bittorrent.BTData.BTFileData;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Class representing the torrent being downloaded. It is updated periodically
 * by the TorrentManager. It has all necessary helper methods to provide
 * functionality to the BTDownloaderImpl. It delegates calls to native methods
 * back to the TorrentManager.
 */
public class TorrentImpl implements Torrent {
    private final EventMulticaster<TorrentEvent> listeners;

    private final TorrentManager torrentManager;

    private final AtomicReference<TorrentStatus> status;

    private final List<String> paths;

    private File incompleteFile = null;

    private AtomicReference<File> completeFile = new AtomicReference<File>(null);

    private File torrentFile = null;

    // TODO can't think of a way of keeping the incomplete folder
    // clean before the download starts other than keeping a reference
    // to the file that we want to start the download with.
    //probably should copy to our own temporary directory that gets cleaned up at program start
    private File initialTorrentFile = null;

    private volatile File fastResumeFile = null;

    private String sha1 = null;

    private String name = null;

    private String trackerURL = null;

    private long totalSize = -1;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final AtomicBoolean complete = new AtomicBoolean(false);
    
    private Boolean isPrivate = null;
    
    @Inject
    public TorrentImpl(TorrentManager torrentManager,
            @Named("fastExecutor") ScheduledExecutorService fastExecutor) {
        this.torrentManager = torrentManager;
        listeners = new AsynchronousMulticasterImpl<TorrentEvent>(fastExecutor);
        status = new AtomicReference<TorrentStatus>();
        paths = new ArrayList<String>();
    }

    @Override
    public void addListener(EventListener<TorrentEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<TorrentEvent> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public synchronized void init(String name, String sha1, long totalSize, String trackerURL,
            List<String> paths, File fastResumeFile, File torrentFile, File saveDir,
            File incompleteFile, Boolean isPrivate) throws IOException {
        
        Objects.nonNull(saveDir, "saveDir");
        
        this.sha1 = sha1;
        this.trackerURL = trackerURL;
        if (paths != null) {
            this.paths.addAll(paths);
        }
        this.totalSize = totalSize;
        File torrentDownloadFolder = torrentManager.getTorrentSettings().getTorrentDownloadFolder();

        if (name != null) {
            this.name = name;
        }
        
        if(isPrivate != null) {
            this.isPrivate = isPrivate.booleanValue();
        }

        if (torrentFile != null && torrentFile.exists()) {
            FileInputStream fis = null;
            FileChannel fileChannel = null;
            try {
                fis = new FileInputStream(torrentFile);
                fileChannel = fis.getChannel();
                Map metaInfo = (Map) Token.parse(fileChannel);
                BTData btData = new BTDataImpl(metaInfo);
                if (this.name == null) {
                    this.name = btData.getName();
                }
                this.totalSize = getTotalSize(btData);

                buildPaths(btData);

                if (this.trackerURL == null) {
                    this.trackerURL = btData.getAnnounce();
                }

                if (this.sha1 == null) {
                    this.sha1 = StringUtils.toHexString(btData.getInfoHash());
                }
                
                if(this.isPrivate == null) {
                    this.isPrivate = btData.isPrivate();
                }

            } finally {
                IOUtils.close(fileChannel);
                IOUtils.close(fis);
            }
        }
        
        if(this.isPrivate == null) {
            //private by default if unknown
            this.isPrivate = Boolean.TRUE;
        }
        
        if(this.name == null || torrentDownloadFolder== null || this.totalSize <= 0 || this.sha1 == null) {
            throw new IOException("There was an error initializing the torrent.");
        }

        this.incompleteFile = incompleteFile == null ? new File(torrentDownloadFolder, this.name)
                : incompleteFile;
        this.fastResumeFile = fastResumeFile == null ? new File(torrentDownloadFolder, this.name
                + ".fastresume") : fastResumeFile;
        this.completeFile.set(new File(saveDir, this.name));
        this.torrentFile = new File(torrentDownloadFolder, this.name + ".torrent");
        this.initialTorrentFile = torrentFile;
    }

    private void buildPaths(BTData btData) {
        if (this.paths.size() == 0) {
            if (btData.getFiles() != null) {
                for (BTFileData fileData : btData.getFiles()) {
                    this.paths.add(fileData.getPath());
                }
            }
        }
    }

    private long getTotalSize(BTData btData) {
        long totalSize = 0;
        if (btData.getLength() == null) {
            if (btData.getFiles() != null) {
                for (BTFileData file : btData.getFiles()) {
                    totalSize += file.getLength();
                }
            }
        } else {
            totalSize = btData.getLength();
        }
        return totalSize;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void start() {
        if (!started.getAndSet(true)) {
            resume();
        }
    }

    @Override
    public File getTorrentFile() {
        return torrentFile;
    }

    @Override
    public File getFastResumeFile() {
        return fastResumeFile;
    }

    @Override
    public List<String> getPeers() {
        return torrentManager.getPeers(this);
    }

    @Override
    public void moveTorrent(File directory) {
        torrentManager.moveTorrent(this, directory);
    }

    @Override
    public void pause() {
        torrentManager.pauseTorrent(this);
    }

    @Override
    public void resume() {
        if (getStatus().isError()) {
            torrentManager.recoverTorrent(this);
        } else {
            torrentManager.resumeTorrent(this);
        }
    }

    @Override
    public float getDownloadRate() {
        TorrentStatus status = this.status.get();
        return status == null ? 0 : status.getDownloadRate();
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public boolean isPaused() {
        TorrentStatus status = this.status.get();
        return status == null ? false : status.isPaused();
    }

    @Override
    public boolean isFinished() {
        return complete.get();
    }

    @Override
    public long getTotalSize() {
        return totalSize;
    }

    @Override
    public boolean isStarted() {
        return started.get();
    }

    @Override
    public String getTrackerURL() {
        return trackerURL;
    }

    @Override
    public boolean isMultiFileTorrent() {
        return paths.size() > 0;
    }

    @Override
    public long getTotalDownloaded() {
        TorrentStatus status = this.status.get();
        if (status == null) {
            return -1;
        } else {
            return status.getTotalDone();
        }
    }

    @Override
    public int getNumPeers() {
        TorrentStatus status = this.status.get();
        return status == null ? 0 : status.getNumPeers();
    }

    @Override
    public List<String> getPaths() {
        return paths;
    }

    private List<File> collectFiles(File rootFile) {
        List<File> files = new ArrayList<File>();
        if (paths.size() > 0) {
            for (String path : paths) {
                File file = new File(rootFile, path);
                files.add(file);
            }
        } else {
            files.add(rootFile);
        }
        return files;
    }

    @Override
    public List<File> getCompleteFiles() {
        return collectFiles(getCompleteFile());
    }

    @Override
    public List<File> getIncompleteFiles() {
        return collectFiles(getIncompleteFile());
    }

    @Override
    public File getIncompleteFile() {
        return incompleteFile;
    }

    @Override
    public File getCompleteFile() {
        return completeFile.get();
    }

    @Override
    public boolean isSingleFileTorrent() {
        return !isMultiFileTorrent();
    }

    @Override
    public void stop() {
        if (!cancelled.getAndSet(true)) {
            torrentManager.removeTorrent(this);
        }
        listeners.broadcast(TorrentEvent.STOPPED);
    }

    @Override
    public long getTotalUploaded() {
        TorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.getTotalUpload();
        }
    }

    @Override
    public int getNumUploads() {
        TorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.getNumUploads();
        }
    }

    @Override
    public float getUploadRate() {
        TorrentStatus status = this.status.get();
        return status == null ? 0 : status.getUploadRate();
    }

    @Override
    public float getSeedRatio() {
        TorrentStatus status = this.status.get();
        if (status != null && status.getTotalDownload() != 0) {
            return (status.getTotalUpload() / getTotalSize());
        }
        return 0;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public TorrentStatus getStatus() {
        return status.get();
    }

    @Override
    public void updateStatus(TorrentStatus torrentStatus) {
        if (!cancelled.get()) {
            synchronized (TorrentImpl.this) {
                TorrentImpl.this.status.set(torrentStatus);
                boolean newlyfinished = complete.get() != torrentStatus.isFinished()
                        && torrentStatus.isFinished();
                complete.set(torrentStatus.isFinished());

                if (newlyfinished) {
                    listeners.broadcast(TorrentEvent.COMPLETED);
                } else {
                    listeners.broadcast(TorrentEvent.STATUS_CHANGED);
                }
            }
        }
    }

    @Override
    public void alert(TorrentAlert alert) {
        synchronized (TorrentImpl.this) {
            if (alert.getCategory() == TorrentAlert.SAVE_RESUME_DATA_ALERT) {
                listeners.broadcast(TorrentEvent.FAST_RESUME_FILE_SAVED);
            }
        }
    }

    @Override
    public String getIncompleteDownloadPath() {
        return incompleteFile.getParentFile().getAbsolutePath();
    }

    @Override
    public boolean registerWithTorrentManager() {
        if(!torrentManager.isValid())
            return false;
        for (File file : getIncompleteFiles()) {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                } catch (Throwable e) {
                    // non-fatal libtorrent will create them
                }
            }
        }

        if (initialTorrentFile != null && !initialTorrentFile.equals(torrentFile)) {
            FileUtils.copy(initialTorrentFile, torrentFile);
        }
        torrentManager.registerTorrent(this);
        return true;
    }

    @Override
    public int getNumConnections() {
        TorrentStatus status = getStatus();
        if(status != null) {
            return status.getNumConnections();
        }
        return 0; 
    }

    @Override
    public void updateSaveDirectory(File saveDirectory) {
        this.completeFile.set(new File(saveDirectory, name));
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }
}
