package com.limegroup.bittorrent;

public interface ManagedTorrentFactory {

    /**
     * Creates an instance of ManagedTorrent for a .torrent file based on the
     * torrent context
     * 
     * @param context information about the torrent including file 
     * system, meta and bit information
     * @return a new instance of ManagedTorrent
     */
    public ManagedTorrent createFromContext(TorrentContext context);

}