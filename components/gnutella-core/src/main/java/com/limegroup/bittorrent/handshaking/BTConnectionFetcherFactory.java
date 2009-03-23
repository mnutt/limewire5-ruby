package com.limegroup.bittorrent.handshaking;

import com.limegroup.bittorrent.ManagedTorrent;

public interface BTConnectionFetcherFactory {

    public BTConnectionFetcher getBTConnectionFetcher(ManagedTorrent torrent);

}