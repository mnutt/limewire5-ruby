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
     * Initializes the torrent from the given fields. Either the torrentFile and
     * saveDir fields cannot be null. Or the name, sha1, long totalSize,
     * paths, and saveDir fields must be set.
     * <p>
     * Otherwise if torrentFile is set and other fields are as well, the field
     * passed in will be used, and any missing field will be pulled from the
     * torrent file.
     */
    public void init(String name, String sha1, long totalSize, String trackerURL,
            List<String> paths, File fastResumeFile, File torrentFile, File saveDir,
            File incompleteFile, Boolean isPrivate) throws IOException;

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
     * Returns a list of peers connected to this torrent.
     */
    public List<String> getPeers();

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
     * Returns the total size of this torrent if all files were to be
     * downloaded.
     */
    public long getTotalSize();

    /**
     * Returns true if the torrent has been started, false otherwise.
     */
    public boolean isStarted();

    /**
     * Returns the first tracker url to this torrent.
     * Can be null.
     */
    public String getTrackerURL();

    /**
     * Returns true if this is a multi file torrent, false otherwise.
     */
    public boolean isMultiFileTorrent();

    /**
     * Returns the total amount of the torren that has fnished downloading.
     */
    public long getTotalDownloaded();

    /**
     * Returns the number of peers in this torrents swarm.
     */
    public int getNumPeers();

    /**
     * Returns the non absolute paths to all files in the torrent.
     */
    public List<String> getPaths();

    /**
     * Returns a list of where all files in the torrent where be when completed.
     */
    public List<File> getCompleteFiles();

    /**
     * Returns a list of where all files in the torrent where be when
     * incomplete.
     */
    public List<File> getIncompleteFiles();

    /**
     * Returns the root incompelteFile for this torrent.
     */
    public File getIncompleteFile();

    /**
     * Returns the root compelete file for this torrent.
     */
    public File getCompleteFile();

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
     * Returns the path where incomplete torrents are downloaded to.
     */
    public String getIncompleteDownloadPath();

    /**
     * Registers the torrent with the torrent manager.
     * @returns true if the torrent was registered, or false if an error
     * occurred.
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
     * Changes the saveDirectory for the torrent. 
     */
    public void updateSaveDirectory(File saveDirectory);

    /**
     * Returns true if this is a private torrent. 
     */
    public boolean isPrivate();

}
