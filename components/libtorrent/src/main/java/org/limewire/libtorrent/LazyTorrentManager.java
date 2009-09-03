package org.limewire.libtorrent;

import java.io.File;
import java.util.List;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.inject.EagerSingleton;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Lazy TorrentManager wraps the TorrentManagerImpl and allows holding off
 * initializing against the native libraries until the first time and methods
 * are called on the torrent manager.
 * 
 * It registers itself as a service to still enabled proper shutdown of the
 * Torrent code, but cleanup only needs to be done is the underlying
 * implementation was initialized.
 * 
 */
@EagerSingleton
public class LazyTorrentManager implements TorrentManager, Service {
    private final Provider<TorrentManagerImpl> torrentManager;

    private volatile boolean initialized = false;
    
    @SuppressWarnings("unused")
    @InspectionPoint("torrent manager status")
    private final Inspectable torrentManagerStatus = new TorrentManagerStatus();

    @Inject
    public LazyTorrentManager(Provider<TorrentManagerImpl> torrentManager) {
        this.torrentManager = torrentManager;
    }

    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }

    private void setupTorrentManager() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    try {
                        this.torrentManager.get().initialize();
                        if (torrentManager.get().isValid()) {
                            this.torrentManager.get().start();
                        }
                    } finally {
                        initialized = true;
                    }
                }
            }
        }
    }

    @Override
    public String getServiceName() {
        return "TorrentManager";
    }

    @Override
    public TorrentManagerSettings getTorrentManagerSettings() {
        // not calling setup because we don't want to initialize the library
        // here.
        // settings can be gotten without initialization.
        return torrentManager.get().getTorrentManagerSettings();
    }

    @Override
    public void initialize() {
        // handled in setup method.
    }

    @Override
    public boolean isDownloadingTorrent(File torrentFile) {
        if(!initialized) {
            return false;
        }
        
        setupTorrentManager();
        return torrentManager.get().isDownloadingTorrent(torrentFile);
    }

    @Override
    public boolean isManagedTorrent(File torrentFile) {
        if (!initialized) {
            return false;
        }

        setupTorrentManager();
        return torrentManager.get().isManagedTorrent(torrentFile);
    }

    @Override
    public boolean isManagedTorrent(String sha1) {
        if (!initialized) {
            return false;
        }

        setupTorrentManager();
        return torrentManager.get().isManagedTorrent(sha1);
    }

    @Override
    public boolean isValid() {
        setupTorrentManager();
        return torrentManager.get().isValid();
    }

    @Override
    public void moveTorrent(Torrent torrent, File directory) {
        setupTorrentManager();
        torrentManager.get().moveTorrent(torrent, directory);
    }

    @Override
    public void pauseTorrent(Torrent torrent) {
        setupTorrentManager();
        torrentManager.get().pauseTorrent(torrent);
    }

    @Override
    public void recoverTorrent(Torrent torrent) {
        setupTorrentManager();
        torrentManager.get().recoverTorrent(torrent);
    }

    @Override
    public void registerTorrent(Torrent torrent) {
        setupTorrentManager();
        torrentManager.get().registerTorrent(torrent);
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        setupTorrentManager();
        torrentManager.get().removeTorrent(torrent);
    }

    @Override
    public void resumeTorrent(Torrent torrent) {
        setupTorrentManager();
        torrentManager.get().resumeTorrent(torrent);
    }

    @Override
    public void start() {
        // handled in setup method.
    }

    @Override
    public void stop() {
        synchronized (this) {
            try {
                if (initialized && torrentManager.get().isValid()) {
                    torrentManager.get().stop();
                }
            } finally {
                initialized = true;
            }
        }
    }

    @Override
    public void setTorrentManagerSettings(TorrentManagerSettings settings) {
        setupTorrentManager();
        torrentManager.get().setTorrentManagerSettings(settings);
    }

    @Override
    public float getTotalDownloadRate() {
        if (!initialized) {
            return 0;
        }
        
        setupTorrentManager();
        return torrentManager.get().getTotalDownloadRate();
    }

    @Override
    public float getTotalUploadRate() {
        if (!initialized) {
            return 0;
        }
        
        setupTorrentManager();
        return torrentManager.get().getTotalUploadRate();
    }

    @Override
    public List<TorrentFileEntry> getTorrentFileEntries(Torrent torrent) {
        setupTorrentManager();
        return torrentManager.get().getTorrentFileEntries(torrent);
    }

    @Override
    public List<TorrentPeer> getTorrentPeers(Torrent torrent) {
        setupTorrentManager();
        return torrentManager.get().getTorrentPeers(torrent);
    }

    @Override
    public void setAutoManaged(Torrent torrent, boolean autoManaged) {
        setupTorrentManager();
        torrentManager.get().setAutoManaged(torrent, autoManaged);
    }
    
    private enum Status{NOT_INITIALIZED, LOADED, FAILED}

    private class TorrentManagerStatus implements Inspectable {
        
        @Override
        public Object inspect() {
            synchronized (LazyTorrentManager.this) {
                return initialized ? (torrentManager.get().isValid() ? Status.LOADED.toString() : Status.FAILED.toString()) : Status.NOT_INITIALIZED.toString();
            }
        }
    }

    @Override
    public void setTorrenFileEntryPriority(Torrent torrent, TorrentFileEntry torrentFileEntry,
            int priority) {
        setupTorrentManager();
        torrentManager.get().setTorrenFileEntryPriority(torrent, torrentFileEntry, priority);
    }

    @Override
    public boolean isInitialized() {
       return initialized;
    }
}
