package com.limegroup.bittorrent;

import java.util.Collections;
import java.util.List;

import org.limewire.io.Address;


public class FinishedTorrentDownload implements Torrent {

	private final long totalDownloaded, lost;
	private final TorrentState state;
	public FinishedTorrentDownload(Torrent active) {
		this.totalDownloaded = active.getTotalDownloaded();
		this.lost = active.getAmountLost();
		this.state = active.getState();
	}
	
	public List<BTLink> getConnections() {
		return Collections.emptyList();
	}

	public long getNextTrackerRequestTime() {
		return -1;
	}

	public int getNumNonInterestingPeers() {
		return 0;
	}

	public int getNumConnections() {
		return 0;
	}

	public int getNumPeers() {
		return 0;
	}
	
	@Override
	public List<Address> getSourceAddresses() {
	    return Collections.emptyList();
	}

	public int getNumChockingPeers() {
		return 0;
	}

	public long getTotalDownloaded() {
		return totalDownloaded;
	}

	public boolean isPausable() {
		return false;
	}

	public boolean isPaused() {
		return false;
	}

	public void pause() {
	}

	public boolean resume() {
		return false;
	}

	public float getMeasuredBandwidth(boolean downstream) {
		return 0;
	}

	public TorrentState getState() {
		return state;
	}

	public boolean isActive() {
		return false;
	}

	public boolean isComplete() {
		return state == TorrentState.SEEDING;
	}

	public void measureBandwidth() {}

	public void start() {}

	public void stop() {}
	
	public long getAmountLost() {
		return lost;
	}

	public int getTriedHostCount() {
		return -1;
	}

    @Override
    public boolean isDownloading() {
        return false;
    }
}
