package com.limegroup.bittorrent.dht;

import com.limegroup.gnutella.URN;

/**
 * Defines an interface for storing network information in the Mojito DHT
 * for a torrent file.
 */
public interface DHTPeerPublisher {

    /**
     * Register the listeners used.
     */
    public void initialize();

    /**
     * Stores the network information of the local host as a seeder in DHT for
     * the given torrent.
     * 
     * @param urn SHA1 hash of the torrent file.
     */
    public void publishYourself(URN urn);

}
