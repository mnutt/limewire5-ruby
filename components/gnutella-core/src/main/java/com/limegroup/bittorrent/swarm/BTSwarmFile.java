package com.limegroup.bittorrent.swarm;

import java.io.File;

import org.limewire.swarm.SwarmFile;

import com.limegroup.bittorrent.TorrentFile;

/**
 * Adapts the TorrentFile interface to the SwarmFile interface.
 * 
 * This is used to allow the swarmer code to not need to know about the
 * internals of the bit torrent code base.
 */
public class BTSwarmFile implements SwarmFile {

    private final TorrentFile torrentFile;

    public BTSwarmFile(TorrentFile torrentFile) {
        this.torrentFile = torrentFile;
    }

    @Override
    public long getEndBytePosition() {
        return torrentFile.getEndByte();
    }

    @Override
    public File getFile() {
        return torrentFile;
    }

    @Override
    public long getFileSize() {
        return torrentFile.length();
    }

    @Override
    public String getPath() {
        return torrentFile.getTorrentPath();
    }

    @Override
    public long getStartBytePosition() {
        return torrentFile.getStartByte();
    }

}
