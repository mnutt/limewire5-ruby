package com.limegroup.bittorrent;

import org.limewire.collection.BitField;
import org.limewire.collection.BitSet;

import com.limegroup.bittorrent.disk.TorrentDiskManager;

/**
 * Defines an interface to get information about the torrent including file 
 * system, meta and bit information. 
 */
public interface TorrentContext {
	TorrentFileSystem getFileSystem();
	TorrentDiskManager getDiskManager();
	BTMetaInfo getMetaInfo();
	
	BitField getFullBitField();
	BitSet getFullBitSet();
	
	void initializeDiskManager(boolean complete);
}
