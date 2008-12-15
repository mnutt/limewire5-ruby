package com.limegroup.bittorrent;

import java.util.EventObject;

/**
 * Encapsulates the torrent object, and its state and description. Torrent 
 * listeners receive this event and must act appropriate.
 */
public class TorrentEvent extends EventObject {

	private static final long serialVersionUID = 5166816249517367147L;

	/**
	 * Defines the various states for a torrent.
	 */
	public enum Type {STARTING,STARTED,DOWNLOADING,STOPPED,COMPLETE,STOP_REQUESTED, STOP_APPROVED, FIRST_CHUNK_VERIFIED, TRACKER_FAILED}
	
	private final Type type;
	private final ManagedTorrent torrent;
    private final String description;
    
    public TorrentEvent(Object source, Type type, ManagedTorrent torrent) {
        this(source,type,torrent, null);
    }
    
	public TorrentEvent(Object source, Type type, ManagedTorrent torrent, String description) {
		super(source);
		this.type = type;
		this.torrent = torrent;
        this.description = description;
	}
	
	public Type getType() {
		return type;
	}
	
	public ManagedTorrent getTorrent() {
		return torrent;
	}
    
    public String getDescription() {
        return description;
    }
}
