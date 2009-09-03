package org.limewire.bittorrent;

/**
 * Represents a peer connected to a torrent.
 */
public interface TorrentPeer {
    /**
     * Returns a hex string representation for this torrents peer id.
     */
    public String getPeerId();

    /**
     * Returns this peers ip address.
     */
    public String getIPAddress();

    /**
     * Returns the source for this peer.
     */
    public short getSource();

    /**
     * Returns the current total upload speed to this peer in bytes/sec.
     */
    public float getUploadSpeed();

    /**
     * Returns the current total download speed from this peer in bytes/sec.
     */
    public float getDownloadSpeed();

    /**
     * Returns the current payload upload speed to this peer in bytes/sec.
     */
    public float getPayloadUploadSpeed();

    /**
     * Returns the current payload download speed from this peer in bytes/sec.
     */
    public float getPayloadDownloadSpeed();

    /**
     * Returns the peers progress downloading the torrent in a number from 0 to
     * 1
     */
    public float getProgress();

    /**
     * Returns a 2 character code representing the peers country.
     */
    public String getCountry();
}
