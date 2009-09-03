package org.limewire.bittorrent;

import java.util.List;

public class TorrentInfo {
    private final List<TorrentFileEntry> fileEntries;

    public TorrentInfo(List<TorrentFileEntry> fileEntries) {
        this.fileEntries = fileEntries;
    }
    public List<TorrentFileEntry> getTorrentFileEntries() {
        return fileEntries;
    }
}
