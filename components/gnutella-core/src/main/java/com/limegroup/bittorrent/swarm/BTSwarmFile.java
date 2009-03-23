package com.limegroup.bittorrent.swarm;

import java.io.File;

import org.limewire.swarm.SwarmFile;

import com.limegroup.bittorrent.TorrentFile;

public class BTSwarmFile implements SwarmFile {

    private final TorrentFile torrentFile;

    public BTSwarmFile(TorrentFile torrentFile) {
        this.torrentFile = torrentFile;
    }

    public long getEndBytePosition() {
        return torrentFile.getEndByte();
    }

    public File getFile() {
        return torrentFile;
    }

    public long getFileSize() {
        return torrentFile.length();
    }

    public String getPath() {
        return torrentFile.getTorrentPath();
    }

    public long getStartBytePosition() {
        return torrentFile.getStartByte();
    }

}
