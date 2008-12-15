package com.limegroup.bittorrent;

import java.util.EventListener;

/**
 * Defines an interface to listen and respond to torrent events. 
 */
public interface TorrentEventListener extends EventListener{
	public void handleTorrentEvent(TorrentEvent evt);
}
