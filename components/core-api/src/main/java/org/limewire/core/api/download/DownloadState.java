package org.limewire.core.api.download;

/**
 * An enum to describe the various general download states and their capabilities. 
 */
public enum DownloadState {
	DONE( false, false), 
	CONNECTING( true, false), 
	DOWNLOADING( true, false), 
	PAUSED( false, true), 
	FINISHING( false, false), 
    LOCAL_QUEUED( true, false), 
    REMOTE_QUEUED( true, false), 
	CANCELLED( false, false), 
	STALLED( false, false),
	TRYING_AGAIN( true, false),
	ERROR( false, false),
	RESUMING(false, false);

	private final boolean pausable;
	private final boolean resumable;

	DownloadState(boolean pausable, boolean resumable) {
		this.pausable = pausable;
		this.resumable = resumable;
	}

	public boolean isPausable() {
		return pausable;
	}

	public boolean isResumable() {
		return resumable;
	}
}