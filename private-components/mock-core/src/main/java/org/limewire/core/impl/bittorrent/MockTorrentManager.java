package org.limewire.core.impl.bittorrent;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;

import com.google.inject.Inject;

public class MockTorrentManager implements TorrentManager {

    private final TorrentManagerSettings torrentSettings;
    
    @Inject
    public MockTorrentManager(@TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentSettings = torrentSettings;
    }
    
    @Override
    public TorrentManagerSettings getTorrentManagerSettings() {
        return torrentSettings;
    }

    @Override
    public boolean isDownloadingTorrent(File torrentFile) {
        return false;
    }

    @Override
    public boolean isManagedTorrent(File torrentFile) {
        return false;
    }

    @Override
    public boolean isManagedTorrent(String sha1) {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void moveTorrent(Torrent torrent, File directory) {
        
    }

    @Override
    public void pauseTorrent(Torrent torrent) {
        
    }

    @Override
    public void recoverTorrent(Torrent torrent) {
        
    }

    @Override
    public void registerTorrent(Torrent torrent) {
        
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        
    }

    @Override
    public void resumeTorrent(Torrent torrent) {
        
    }

    @Override
    public void setTorrentManagerSettings(TorrentManagerSettings settings) {
        
    }

    @Override
    public void initialize() {
        
    }

    @Override
    public void start() {
        
    }

    @Override
    public void stop() {
        
    }

    @Override
    public float getTotalDownloadRate() {
        return 0;
    }

    @Override
    public float getTotalUploadRate() {
        return 0;
    }

    @Override
    public List<TorrentFileEntry> getTorrentFileEntries(Torrent torrent) {
        return Collections.emptyList();
    }

    @Override
    public List<TorrentPeer> getTorrentPeers(Torrent torrent) {
        return Collections.emptyList();
    }

    @Override
    public void setAutoManaged(Torrent torrent, boolean autoManaged) {
        
    }

    @Override
    public void setTorrenFileEntryPriority(Torrent torrent, TorrentFileEntry torrentFileEntry,
            int priority) {
        
    }

    @Override
    public boolean isInitialized() {
        return false;
    }
}
