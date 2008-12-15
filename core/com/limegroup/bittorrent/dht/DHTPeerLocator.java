package com.limegroup.bittorrent.dht;

import com.limegroup.gnutella.URN;

/**
 * Defines an interface for looking up a peer in the Mojito
 * DHT given the <code>ManagedTorrent</code> instance of the torrent.
 */
public interface DHTPeerLocator {

    /**
     * Registers the listeners used.
     */
    public void initialize();

    /**
     * Searches for peers sharing the given torrent. If no peers are found, then
     * the search stops.
     * 
     * @param urn SHA1 hash of the torrent file.
     */
    public void locatePeer(URN urn);

}
