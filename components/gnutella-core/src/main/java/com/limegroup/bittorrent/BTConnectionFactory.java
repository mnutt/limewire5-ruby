package com.limegroup.bittorrent;

/**
 * Defines the interface to create <code>BTConnection</code>s.
 */
public interface BTConnectionFactory {
    public BTConnection createBTConnection(TorrentContext context, TorrentLocation loc);
}
