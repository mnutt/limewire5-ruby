package com.limegroup.bittorrent;

import java.io.File;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.util.FileLocker;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * Defines an interface which manages active torrents and dispatching of 
 * incoming BitTorrent connections.
 * <p>
 * Also stores information common to all the torrents.
 */
public interface TorrentManager extends FileLocker, ConnectionAcceptor, TorrentEventListener,
        EventDispatcher<TorrentEvent, TorrentEventListener> {

    /**
     * Initializes this. Always call this method before starting any torrents.
     */
	public void initialize(ConnectionDispatcher dispatcher);

    /**
     * @return active torrent for the given infoHash, null if no such.
     */
    ManagedTorrent getTorrentForHash(byte[] infoHash);

    /**
     * @return active torrent for the given <code>URN</code>, null if no
     *         such.
     */
    ManagedTorrent getTorrentForURN(URN urn);

    boolean allowNewTorrent();

    int getNumActiveTorrents();

    boolean hasNonSeeding();

    boolean killTorrentForFile(File f);

    /**
     * Returns the expected shared .torrent meta data file. Can be null.
     */
    File getSharedTorrentMetaDataFile(BTMetaInfo info);

    /**
     * @return the number of connections per torrent we'll try to maintain. This
     *         is somewhat arbitrary
     */
    int getMaxTorrentConnections();

    /**
     * Checks to see if the given torrent file can be shared with the gnutella network. 
     * If it can, the file is saved to a special location on disk and shared.
     */
    public void shareTorrentFile(BTMetaInfo m, byte[] body);
}