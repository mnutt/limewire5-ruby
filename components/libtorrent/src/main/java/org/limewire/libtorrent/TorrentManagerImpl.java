package org.limewire.libtorrent;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentException;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.inject.LazySingleton;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@LazySingleton
public class TorrentManagerImpl implements TorrentManager {

    private static final boolean PERIODICALLY_SAVE_FAST_RESUME_DATA = true;

    private static final Log LOG = LogFactory.getLog(TorrentManagerImpl.class);

    private final ScheduledExecutorService fastExecutor;

    private final LibTorrentWrapper libTorrent;

    private final Map<String, Torrent> torrents;

    private final AtomicReference<TorrentSettings> torrentSettings = new AtomicReference<TorrentSettings>(
            null);

    /**
     * Used to protect from calling libtorrent code with invalid torrent data.
     * Locks access around libtorrent and removing/updating the torrents map to
     * make sure the torrents torrent manger knows about are the same as what
     * libtorrent knows about.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Future for the job updating the torrent status.
     */
    private ScheduledFuture<?> torrentFuture;

    /**
     * Future for the job listening to torrent alerts.
     */
    private ScheduledFuture<?> alertFuture;

    /**
     * Future for the job creating resume files. The alert job must be running
     * for the resume files to be created properly.
     */
    private ScheduledFuture<?> resumeFileFuture;

    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint("torrent manager")
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                int active = 0;
                int seeding = 0;
                int starting = 0;

                lock.readLock().lock();
                try {
                    for (Torrent torrent : torrents.values()) {
                        if (!torrent.isStarted()) {
                            starting++;
                        } else if (torrent.isFinished()) {
                            seeding++;
                        } else {
                            active++;
                        }
                    }
                } finally {
                    lock.readLock().unlock();
                }
                data.put("active", active);
                data.put("seeding", seeding);
                data.put("starting", starting);
                return data;
            }
        };
    }

    @Inject
    public TorrentManagerImpl(LibTorrentWrapper torrentWrapper,
            @Named("fastExecutor") ScheduledExecutorService fastExecutor,
            @TorrentSettingsAnnotation TorrentSettings torrentSettings) {
        this.fastExecutor = fastExecutor;
        this.libTorrent = torrentWrapper;
        this.torrents = new ConcurrentHashMap<String, Torrent>();
        this.torrentSettings.set(torrentSettings);
    }

    private void validateLibrary() {
        if (!torrentSettings.get().isTorrentsEnabled()) {
            throw new TorrentException("LibTorrent is disabled (through settings)",
                    TorrentException.DISABLED_EXCEPTION);
        }
        if (!isValid()) {
            throw new TorrentException("There was a problem loading LibTorrent",
                    TorrentException.LOAD_EXCEPTION);
        }
    }

    @Override
    public void registerTorrent(Torrent torrent) {
        validateLibrary();
        addTorrent(torrent);
    }

    @Override
    public boolean isValid() {
        return libTorrent.isLoaded();
    }

    private void addTorrent(Torrent torrent) {
        File torrentFile = torrent.getTorrentFile();
        File fastResumefile = torrent.getFastResumeFile();

        String trackerURI = torrent.getTrackerURL();
        String fastResumePath = fastResumefile != null ? fastResumefile.getAbsolutePath() : null;
        String torrentPath = torrentFile != null ? torrentFile.getAbsolutePath() : null;

        lock.writeLock().lock();
        try {
            libTorrent.add_torrent(torrent.getSha1(), trackerURI, torrentPath, torrent
                    .getIncompleteDownloadPath(), fastResumePath);
            updateStatus(torrent);
            torrents.put(torrent.getSha1(), torrent);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<String> getPeers(Torrent torrent) {
        validateLibrary();
        return libTorrent.get_peers(torrent.getSha1());
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        validateLibrary();
        lock.writeLock().lock();
        try {
            torrents.remove(torrent.getSha1());
            libTorrent.remove_torrent(torrent.getSha1());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void pauseTorrent(Torrent torrent) {
        validateLibrary();
        lock.readLock().lock();
        try {
            String sha1 = torrent.getSha1();
            libTorrent.pause_torrent(sha1);
            updateStatus(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void resumeTorrent(Torrent torrent) {
        validateLibrary();
        lock.readLock().lock();
        try {
            String sha1 = torrent.getSha1();
            libTorrent.resume_torrent(sha1);
            updateStatus(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void recoverTorrent(Torrent torrent) {
        validateLibrary();
        lock.readLock().lock();
        try {
            String sha1 = torrent.getSha1();
            libTorrent.clear_error_and_retry(sha1);
            updateStatus(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    private LibTorrentStatus getStatus(Torrent torrent) {
        validateLibrary();
        LibTorrentStatus status = new LibTorrentStatus();

        String sha1 = torrent.getSha1();
        libTorrent.get_torrent_status(sha1, status);
        libTorrent.free_torrent_status(status);
        return status;
    }

    private void updateStatus(Torrent torrent) {
        lock.readLock().lock();
        try {
            LibTorrentStatus torrentStatus = getStatus(torrent);
            torrent.updateStatus(torrentStatus);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void moveTorrent(Torrent torrent, File directory) {
        validateLibrary();
        lock.writeLock().lock();
        try {
            String sha1 = torrent.getSha1();
            libTorrent.move_torrent(sha1, directory.getAbsolutePath());
            updateStatus(torrent);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getServiceName() {
        return "TorrentManager";
    }

    @Override
    public void initialize() {
        if (torrentSettings.get().isTorrentsEnabled()) {
            lock.writeLock().lock();
            try {
                libTorrent.initialize(torrentSettings.get());
                if (libTorrent.isLoaded()) {
                    updateSettings(torrentSettings.get());
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void start() {
        if (isValid()) {
            lock.writeLock().lock();
            try {
                torrentFuture = fastExecutor.scheduleAtFixedRate(new EventPoller(), 1000, 500,
                        TimeUnit.MILLISECONDS);

                if (!OSUtils.isMacOSX()) {
                    // TODO disabling for now on the mac, on osx there is an
                    // error
                    // calling the alert callback, need toi investigate.
                    // but disabling for now so that it does not crash the jvm.

                    if (PERIODICALLY_SAVE_FAST_RESUME_DATA) {
                        alertFuture = fastExecutor.scheduleAtFixedRate(new AlertPoller(), 1000,
                                500, TimeUnit.MILLISECONDS);
                        resumeFileFuture = fastExecutor.scheduleAtFixedRate(
                                new ResumeDataScheduler(), 10000, 10000, TimeUnit.MILLISECONDS);
                    }
                }

            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void stop() {
        lock.writeLock().lock();
        try {
            if (resumeFileFuture != null) {
                resumeFileFuture.cancel(true);
            }

            if (torrentFuture != null) {
                torrentFuture.cancel(true);
            }
            if (alertFuture != null) {
                alertFuture.cancel(true);
            }

            if (isValid()) {
                if (!OSUtils.isMacOSX()) {
                    // TODO disabling for now on the mac, on osx there is an
                    // error
                    // calling the alert callback, need to investigate.
                    // but disabling for now so that it does not crash the jvm.
                    libTorrent.freeze_and_save_all_fast_resume_data(new BasicAlertCallback());
                }
                libTorrent.abort_torrents();
            }
            torrents.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isManagedTorrent(File torrentFile) {
        if (torrentFile != null) {
            synchronized (torrents) {
                for (Torrent torrent : torrents.values()) {
                    if (torrentFile.equals(torrent.getTorrentFile())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isManagedTorrent(String sha1) {
        return torrents.containsKey(sha1);
    }

    /**
     * Basic implemenation of the AlertCallback interface used to delegate
     * alerts back to the appropriate torrent.
     */
    private class BasicAlertCallback implements AlertCallback {

        @Override
        public void callback(LibTorrentAlert alert) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(alert.toString());
            }

            String sha1 = alert.getSha1();
            if (sha1 != null) {
                Torrent torrent = torrents.get(sha1);
                if (torrent != null) {
                    torrent.alert(alert);
                }
            }
        }
    }

    /**
     * Used to clear the alert queue in the native code passing event back to
     * the java code through the alertCallback interface.
     */
    private class AlertPoller implements Runnable {
        @Override
        public void run() {
            libTorrent.get_alerts(new BasicAlertCallback());
        }
    }

    /**
     * Iterates through the torrents updating the status of each one to the
     * correct current state.
     */
    private class EventPoller implements Runnable {

        @Override
        public void run() {
            pumpStatus();
        }

        private void pumpStatus() {
            for (Torrent torrent : torrents.values()) {
                updateStatus(torrent);
            }
        }
    }

    /**
     * Iterates through the torrents periodically saving a fastresume file for
     * each file.
     */
    private class ResumeDataScheduler implements Runnable {

        private Iterator<String> torrentIterator = torrents.keySet().iterator();

        @Override
        public void run() {
            lock.readLock().lock();
            try {
                if (!torrentIterator.hasNext()) {
                    torrentIterator = torrents.keySet().iterator();
                    if (!torrentIterator.hasNext()) {
                        return;
                    }
                }

                String sha1 = torrentIterator.next();
                Torrent torrent = torrents.get(sha1);

                if (torrent != null && !torrent.isFinished()) {
                    libTorrent.signal_fast_resume_data_request(sha1);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    @Override
    public boolean isDownloadingTorrent(File torrentFile) {
        if (torrentFile != null) {
            synchronized (torrents) {
                for (Torrent torrent : torrents.values()) {
                    if (torrentFile.equals(torrent.getTorrentFile()) && !torrent.isFinished()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void updateSettings(TorrentSettings settings) {
        validateLibrary();
        torrentSettings.set(settings);
        libTorrent.update_settings(settings);
    }

    @Override
    public TorrentSettings getTorrentSettings() {
        return torrentSettings.get();
    }
}
