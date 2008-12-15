package com.limegroup.bittorrent.disk;

import org.limewire.core.settings.BittorrentSettings;

import com.google.inject.Singleton;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.TorrentFile;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;

/**
 * Returns either a memory map or a random access file disk manager.
 */
@Singleton
public class DiskManagerFactory {
	
	public TorrentDiskManager getManager(TorrentContext context,
			BTDiskManagerMemento memento,
			boolean complete) {
		return new VerifyingFolder(context, 
				complete,
				memento, 
				BittorrentSettings.TORRENT_USE_MMAP.getValue() ?
						new MMDiskController<TorrentFile>() :
							new RAFDiskController<TorrentFile>());
	}
}
