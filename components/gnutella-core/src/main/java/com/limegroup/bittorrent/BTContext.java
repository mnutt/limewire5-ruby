package com.limegroup.bittorrent;

import org.limewire.collection.BitField;
import org.limewire.collection.BitFieldSet;
import org.limewire.collection.BitSet;

import com.limegroup.bittorrent.disk.DiskManagerFactory;
import com.limegroup.bittorrent.disk.TorrentDiskManager;

public class BTContext implements TorrentContext {

	private final BTMetaInfo info;
	private final BitSet fullSet = new FullBitSet();
	private final BitField fullBitField;
	
	private TorrentDiskManager diskManager;
	private final DiskManagerFactory diskManagerFactory;
	
	public BTContext(BTMetaInfo info, DiskManagerFactory diskManagerFactory) {
	    this.diskManagerFactory = diskManagerFactory;
		this.info = info;
		info.setContext(this);
		fullBitField = new BitFieldSet(fullSet, info.getNumBlocks());
		initializeDiskManager(false);
	}
	
	public TorrentDiskManager getDiskManager() {
		return diskManager;
	}

	public TorrentFileSystem getFileSystem() {
		return info.getFileSystem();
	}

	public BitField getFullBitField() {
		return fullBitField;
	}

	public BitSet getFullBitSet() {
		return fullSet;
	}

	public BTMetaInfo getMetaInfo() {
		return info;
	}

	public void initializeDiskManager(boolean complete) {
		diskManager = diskManagerFactory.getManager(this, info.getDiskManagerData(), complete);
	}

	/**
	 * A bitset that has fixed size and every bit in it is set.
	 */
	private class FullBitSet extends BitSet {
		private static final long serialVersionUID = -2621319856548383315L;
		@Override
        public void set(int i) {}
		@Override
        public void clear(int i){}
		@Override
        public boolean get(int i) {
			return true;
		}
		@Override
        public int cardinality() {
			return info.getNumBlocks();
		}
		@Override
        public int length() {
			return info.getNumBlocks();
		}
		@Override
        public int nextSetBit(int i) {
			if (i >= cardinality())
				return -1;
			return i;
		}
	}
}
