package org.limewire.bittorrent;

/**
 * Enum representing various events that a Torrent can send out to listeners.
 */
public enum TorrentEvent {
    STATUS_CHANGED, STOPPED, COMPLETED, FAST_RESUME_FILE_SAVED;
}
