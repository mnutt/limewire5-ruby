package org.limewire.core.impl.bittorrent;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;

import com.google.inject.Inject;

public class MockTorrentManager implements TorrentManager {

    private final TorrentSettings torrentSettings;
    
    @Inject
    public MockTorrentManager(@TorrentSettingsAnnotation TorrentSettings torrentSettings) {
        this.torrentSettings = torrentSettings;
    }
    
    @Override
    public List<String> getPeers(Torrent torrent) {
        return Collections.emptyList();
    }

    @Override
    public TorrentSettings getTorrentSettings() {
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
    public void updateSettings(TorrentSettings settings) {
        
    }

    @Override
    public String getServiceName() {
        return getClass().getName();
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
}
