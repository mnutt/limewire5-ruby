package org.limewire.bittorrent;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.listener.EventListener;

/**
 * Class representing the torrent being downloaded.
 */
public interface Torrent {

    /**
     * Initializes the torrent from the given torrent parameters Object.
     */
    public void init(TorrentParams params) throws IOException;

    /**
     * Returns the name of this torrent.
     */
    public String getName();

    /**
     * Starts the torrent.
     */
    public void start();

    /**
     * Returns the torrent file backing this torrent if any exist. Null or a
     * non-existent file can be returned.
     */
    public File getTorrentFile();

    /**
     * Returns the fastResume file backing this torrent if any. Null or a
     * non-existent file can be returned.
     */
    public File getFastResumeFile();

    /**
     * Moves the torrent to the specified directory.
     */
    public void moveTorrent(File directory);

    /**
     * Pauses the torrent.
     */
    public void pause();

    /**
     * Resumes the torrent from a paused state.
     */
    public void resume();

    /**
     * Returns the download rate in bytes/second.
     */
    public float getDownloadRate();

    /**
     * Returns a hexString representation of this torrents sha1.
     */
    public String getSha1();

    /**
     * Returns true if this torrent is paused, false otherwise.
     */
    public boolean isPaused();

    /**
     * Returns true if this torrent is finished, false otherwise.
     */
    public boolean isFinished();

    /**
     * Returns true if the torrent has been started, false otherwise.
     */
    public boolean isStarted();

    /**
     * Returns the first tracker url to this torrent. Can be null.
     */
    public String getTrackerURL();

    /**
     * Returns true if this is a multi file torrent, false otherwise.
     */
    public boolean isMultiFileTorrent();

    /**
     * Returns the number of peers in this torrents swarm.
     */
    public int getNumPeers();

    /**
     * Returns the root data file for this torrent.
     */
    public File getTorrentDataFile();

    /**
     * Returns true if this is a single file torrent, false otherwise.
     */
    public boolean isSingleFileTorrent();

    /**
     * Stops the torrent by removing it from the torrent manager.
     */
    public void stop();

    /**
     * Returns the total number of byte uploaded for this torrent.
     */
    public long getTotalUploaded();

    /**
     * Returns current number of upload connections.
     */
    public int getNumUploads();

    /**
     * Returns the current upload rate in bytes/second.
     */
    public float getUploadRate();

    /**
     * Returns the current seed ratio, with 1.0 being at 100%.
     */
    public float getSeedRatio();

    /**
     * Returns true if this torrent has been canceled, false otherwise.
     */
    public boolean isCancelled();

    /**
     * Returns a status object representing this torrents internal state.
     */
    public TorrentStatus getStatus();

    /**
     * Updates this torrents internal state using the given LibTorrentStatus
     * object.
     */
    public void updateStatus(TorrentStatus torrentStatus);

    /**
     * Updates this torrents internal state using the given LibTorrentAlerts.
     */
    public void alert(TorrentAlert alert);

    /**
     * Registers the torrent with the torrent manager.
     * 
     * @returns true if the torrent was registered, or false if an error
     *          occurred.
     */
    public boolean registerWithTorrentManager();

    /**
     * Removes the listener from the torrent. Returning true if the listener
     * attached and removed.
     */
    boolean removeListener(EventListener<TorrentEvent> listener);

    /**
     * Adds a listener to this torrent.
     */
    void addListener(EventListener<TorrentEvent> listener);

    /**
     * Returns the number of connections this torrent has.
     */
    public int getNumConnections();

    /**
     * Returns true if this is a private torrent.
     */
    public boolean isPrivate();

    /**
     * Returns a list of TorrentFileEntry containing an entry for each file in
     * this torrent.
     */
    public List<TorrentFileEntry> getTorrentFileEntries();

    /**
     * Returns a list of currently connected peers for this torrent.
     */
    public List<TorrentPeer> getTorrentPeers();

    /**
     * Returns true if the torrent is automanaged.
     */
    public boolean isAutoManaged();

    /**
     * Sets whether or not this torrent is automanaged. For an explanation of
     * automanagement see
     * http://www.rasterbar.com/products/libtorrent/manual.html#queuing
     * 
     * Basically it means that the torrent will be managed by libtorrent. Every
     * polling period queued and active torrents are checked to see if they
     * should be given some active time to allow for seeding/downloading.
     * Automanaged torrents adhere to limits for total torrents allowed active,
     * total seeds, etc.
     */
    public void setAutoManaged(boolean autoManaged);

    /**
     * Sets the priority for the specified TorrentFileEntry.
     */
    public void setTorrenFileEntryPriority(TorrentFileEntry torrentFileEntry, int priority);

    /**
     * Returns the filesystem path for the specified torrentFileEntry.
     */
    public File getTorrentDataFile(TorrentFileEntry torrentFileEntry);

    /**
     * Sets the snapshot TorrentInfo for this torrent.
     */
    public void setTorrentInfo(TorrentInfo torrentInfo);

    /**
     * Returns true if this torrent has metadata yet or not.
     */
    public boolean hasMetaData();

    /**
     * Initializes files on the file system for this torrent.
     */
    public void initFiles();
}
