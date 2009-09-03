package org.limewire.bittorrent;

import java.io.File;
import java.util.List;

public interface TorrentManager {

    /**
     * Removes torrent from control of the torrent manager. Also delegates a
     * remove torrent call to the libtorrent wrapper lbirary.
     */
    public void removeTorrent(Torrent torrent);

    /**
     * Delegates a pause torrent call to the libtorrent wrapper library.
     */
    public void pauseTorrent(Torrent torrent);

    /**
     * Delegates a resume torrent call to the libtorrent wrapper library.
     */
    public void resumeTorrent(Torrent torrent);

    /**
     * Delegates to clear any error state and restarts the torrent.
     */
    public void recoverTorrent(Torrent torrent);

    /**
     * Moves the underlying storage of the torrent to the specified directory.
     * Currently this is used to move an a completed torrent from the incomplete
     * directory to the complete directory, without interrupting any seeding
     * which maybe be happening.
     */
    public void moveTorrent(Torrent torrent, File directory);

    /**
     * Registers the specified torrent with the TorrentManager. Delegates an add
     * torrent call to the underlying libtorrent wrapper library.
     */
    void registerTorrent(Torrent torrent);

    /**
     * Returns true if the torrent manager currently has a torrent registered
     * from the specified torrent file.
     */
    public boolean isManagedTorrent(File torrentFile);

    /**
     * Returns true if the torrent manager currently has a torrent with the
     * specified sha1.
     */
    public boolean isManagedTorrent(String sha1);

    /**
     * Returns true if the TorrentManager loaded properly and is in a valid
     * state.
     */
    public boolean isValid();

    /**
     * Returns true if the torrent manager currently had a torrent registered
     * with the specified torrent file, and the torrent is not finished
     * downloading yet.
     */
    public boolean isDownloadingTorrent(File torrentFile);

    /**
     * Updates the torrent manager with any new setting values.
     */
    void setTorrentManagerSettings(TorrentManagerSettings settings);

    /**
     * Returns the current TorrentSettings object set on the torrent session.
     */
    public TorrentManagerSettings getTorrentManagerSettings();

    /**
     * Returns the total upload rate used by libtorrent. This includes payload
     * and protocol overhead.
     */
    public float getTotalUploadRate();

    /**
     * Returns the total download rate used by libtorrent. This includes payload
     * and protocol overhead.
     */
    public float getTotalDownloadRate();

    /**
     * Returns a list of torrent file entries for the given torrent.
     */
    public List<TorrentFileEntry> getTorrentFileEntries(Torrent torrent);

    /**
     * Returns a list of connected peers for the given torrent.
     */
    public List<TorrentPeer> getTorrentPeers(Torrent torrent);

    /**
     * Sets the automanaged flag to the given value for the torrent.
     */
    public void setAutoManaged(Torrent torrent, boolean autoManaged);

    /**
     * Starts the torrent manager, and its necessary worker threads.
     */
    public void start();

    /**
     * Shuts down the torrent manager, and any needed threads.
     */
    public void stop();

    /**
     * Initializes this torrent manager.
     */
    public void initialize();

    /**
     * Sets the given priority for the given TorrentFileEntry. 
     */
    public void setTorrenFileEntryPriority(Torrent torrent, TorrentFileEntry torrentFileEntry,
            int priority);

    /**
     * Returns true if the torrent manager is initialized.
     */
    public boolean isInitialized();
}
