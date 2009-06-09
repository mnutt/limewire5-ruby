package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentStatus;
import org.limewire.libtorrent.callback.AlertCallback;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

/**
 * Interface definition for accessing the C LibTorrentWrapper library.
 */
interface LibTorrent extends Library {

    /**
     * Inititalizes the libtorrent session to use the given path as the default
     * download location.
     */
    public WrapperStatus init();

    /**
     * Adds a torrent to the libtorrent session. This can be done with only a
     * sha1 and trackerURI. optionally a path to a fast Resume data file can be
     * included to enable starting the torrent faster.
     */
    public WrapperStatus add_torrent(String sha1, String trackerURI, WString torrentPath,
            WString savePath, WString fastResumeData);

    /**
     * Pauses the torrent with the given sha1
     */
    public WrapperStatus pause_torrent(String id);

    /**
     * Resumes the torrent with the given sha1
     */
    public WrapperStatus resume_torrent(String id);

    /**
     * Used on shutdown to freeze all torrents and wait while saving fast resume
     * data for each.
     */
    public WrapperStatus freeze_and_save_all_fast_resume_data(AlertCallback alertCallback);

    /**
     * Reads any stored alerts in the session, having there data coming in
     * through the callback.
     */
    public WrapperStatus get_alerts(AlertCallback alertCallback);

    /**
     * Fills in the Libtorrent status struct for the torrent with the given sha1
     */
    public WrapperStatus get_torrent_status(String id, TorrentStatus status);

    /**
     * Returns the number of peers for the torrent with the given sha1
     */
    public WrapperStatus get_num_viewable_peers(String id, Pointer num_peers);

    /**
     * Retrieves the peers for the torrent with the given sha1
     */
    public WrapperStatus get_peers(String id, int len, Memory memory);

    /**
     * Tells the session to save the fast resume data for the torrent with the
     * given sha1.
     */
    public WrapperStatus signal_fast_resume_data_request(String id);

    /**
     * Clears the error status on a torrent and attempts to restart it.
     */
    public WrapperStatus clear_error_and_retry(String id);

    /**
     * Removes the torrent with the given sha1 from the session.
     */
    public WrapperStatus remove_torrent(String id);

    /**
     * Moves the torrent with the given sha1 from its current location to the
     * location defined in absolutePath.
     */
    public WrapperStatus move_torrent(String id, WString absolutePath);

    /**
     * Aborts all of the torrents in the session.
     */
    public WrapperStatus abort_torrents();

    /**
     * Frees the given torrentStatus object from memory.
     */
    public WrapperStatus free_torrent_status(Pointer ptr);

    /**
     * Updates the sessions settings using the provided settings structure.
     */
    public WrapperStatus update_settings(LibTorrentSettings libTorrentSettings);

}
