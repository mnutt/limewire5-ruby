package org.limewire.libtorrent;

import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.bittorrent.TorrentException;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;

/**
 * Wrapper class for the LibTorrent c interface. Provides library loading logic,
 * and handles rethrowing c++ exceptions as java exceptions.
 */
class LibTorrentWrapper {

    private static final Log LOG = LogFactory.getLog(LibTorrentWrapper.class);

    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private LibTorrent libTorrent;

    /**
     * Initializes the LibTorrent library. Finding necessary dependencies first,
     * then loading the libtorrent library as a jna lib.
     */
    void initialize(TorrentManagerSettings torrentSettings) {
        try {

            this.libTorrent = (LibTorrent) Native.loadLibrary("torrent-wrapper", LibTorrent.class);

            init(torrentSettings);
            loaded.set(true);
        } catch (Throwable e) {
            LOG.error("Failure loading the libtorrent libraries.", e);
            if (torrentSettings.isReportingLibraryLoadFailture()) {
                ExceptionUtils.reportOrReturn(e);
            }
        }
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    private void init(TorrentManagerSettings torrentSettings) {
        LOG.debugf("before init");
        catchWrapperException(libTorrent.init(new LibTorrentSettings(torrentSettings)));
        LOG.debugf("after init");
    }

    public void add_torrent(String sha1, String trackerURI, String torrentPath, String savePath,
            String fastResumePath) {
        LOG.debugf("before add_torrent: {0}", sha1);
        catchWrapperException(libTorrent.add_torrent(sha1, trackerURI, new WString(torrentPath),
                new WString(savePath), new WString(fastResumePath)));
        LOG.debugf("after add_torrent: {0}", sha1);
    }

    public void freeze_and_save_all_fast_resume_data(AlertCallback alertCallback) {
        LOG.debug("before get_alerts");
        catchWrapperException(libTorrent.freeze_and_save_all_fast_resume_data(alertCallback));
        LOG.debug("after get_alerts");
    }

    public void get_alerts(AlertCallback alertCallback) {
        LOG.debug("before get_alerts");
        catchWrapperException(libTorrent.get_alerts(alertCallback));
        LOG.debug("after get_alerts");
    }

    public void pause_torrent(String id) {
        LOG.debugf("before pause_torrent: {0}", id);
        catchWrapperException(libTorrent.pause_torrent(id));
        LOG.debugf("after pause_torrent: {0}", id);

    }

    public void resume_torrent(String id) {
        LOG.debugf("before resume_torrent: {0}", id);
        catchWrapperException(libTorrent.resume_torrent(id));
        LOG.debugf("after resume_torrent: {0}", id);

    }

    public void get_torrent_status(String id, TorrentStatus status) {
        LOG.debugf("before get_torrent_status: {0}", id);
        catchWrapperException(libTorrent.get_torrent_status(id, status));
        LOG.debugf("after get_torrent_status: {0}", id);
    }

    public void remove_torrent(String id) {
        LOG.debugf("before remove_torrent: {0}", id);
        catchWrapperException(libTorrent.remove_torrent(id));
        LOG.debugf("after remove_torrent: {0}", id);
    }

    public LibTorrentPeer[] get_peers(String id) {

        LOG.debugf("before get_num_peers: {0}", id);
        IntByReference numPeersReference = new IntByReference();
        catchWrapperException(libTorrent.get_num_peers(id, numPeersReference));
        LOG.debugf("after get_num_peers: {0} - {1}", id, numPeersReference);

        int numPeers = numPeersReference.getValue();

        if (numPeers == 0) {
            return new LibTorrentPeer[0];
        }

        LibTorrentPeer[] torrentPeers = new LibTorrentPeer[numPeers];
        Pointer[] torrentPeersPointers = new Pointer[numPeers];
        for (int i = 0; i < torrentPeersPointers.length; i++) {
            LibTorrentPeer torrentPeer = new LibTorrentPeer();
            torrentPeers[i] = torrentPeer;
            torrentPeersPointers[i] = torrentPeer.getPointer();
        }

        LOG.debugf("before get_peers: {0}", id);
        catchWrapperException(libTorrent.get_peers(id, torrentPeersPointers,
                torrentPeersPointers.length));

        for (int i = 0; i < torrentPeers.length; i++) {
            torrentPeers[i].read();
        }

        free_peers(id, torrentPeersPointers, torrentPeersPointers.length);
        LOG.debugf("after get_peers: {0}", id);
        return torrentPeers;
    }

    private void free_peers(String id, Pointer[] torrentPeersPointers, int length) {
        LOG.debugf("before free_peers: {0}", id);
        catchWrapperException(libTorrent.free_peers(torrentPeersPointers,
                torrentPeersPointers.length));
        LOG.debugf("after free_peers: {0}", id);
    }

    public void signal_fast_resume_data_request(String id) {
        LOG.debugf("before print signal_fast_resume_data_request: {0}", id);
        catchWrapperException(libTorrent.signal_fast_resume_data_request(id));
        LOG.debugf("after print signal_fast_resume_data_request: {0}", id);
    }

    public void clear_error_and_retry(String id) {
        LOG.debugf("before print clear_error_and_retry: {0}", id);
        catchWrapperException(libTorrent.clear_error_and_retry(id));
        LOG.debugf("after print clear_error_and_retry: {0}", id);
    }

    public void move_torrent(String id, String absolutePath) {
        LOG.debugf("before move_torrent: {0} - {1}", id, absolutePath);
        catchWrapperException(libTorrent.move_torrent(id, new WString(absolutePath)));
        LOG.debugf("after move_torrent: {0} - {1}", id, absolutePath);
    }

    public void abort_torrents() {
        LOG.debug("before abort");
        catchWrapperException(libTorrent.abort_torrents());
        LOG.debug("after abort");
    }

    public void free_torrent_status(LibTorrentStatus status) {
        LOG.debugf("before free_torrent_status: {0}", status);
        catchWrapperException(libTorrent.free_torrent_status(status.getPointer()));
        LOG.debugf("after free_torrent_status: {0}", status);
    }

    private void catchWrapperException(WrapperStatus status) {
        if (status != null) {
            throw new TorrentException(status.message, status.type);
        }
    }

    public void update_settings(TorrentManagerSettings torrentSettings) {
        LOG.debugf("before update_settings: {0}", torrentSettings);
        catchWrapperException(libTorrent.update_settings(new LibTorrentSettings(torrentSettings)));
        LOG.debugf("after update_settings: {0}", torrentSettings);
    }

    public void start_dht() {
        LOG.debugf("before start_dht");
        catchWrapperException(libTorrent.start_dht());
        LOG.debugf("after start_dht");
    }

    public void stop_dht() {
        LOG.debugf("before stop_dht");
        catchWrapperException(libTorrent.stop_dht());
        LOG.debugf("after stop_dht");
    }

    public void start_upnp() {
        LOG.debugf("before start_upnp");
        catchWrapperException(libTorrent.start_upnp());
        LOG.debugf("after start_upnp");
    }

    public void stop_upnp() {
        LOG.debugf("before stop_upnp");
        catchWrapperException(libTorrent.stop_upnp());
        LOG.debugf("after stop_upnp");
    }

    public void start_lsd() {
        LOG.debugf("before start_lsd");
        catchWrapperException(libTorrent.start_lsd());
        LOG.debugf("after start_lsd");
    }

    public void stop_lsd() {
        LOG.debugf("before stop_lsd");
        catchWrapperException(libTorrent.stop_lsd());
        LOG.debugf("after stop_lsd");
    }

    public void start_natpmp() {
        LOG.debugf("before start_natpmp");
        catchWrapperException(libTorrent.start_natpmp());
        LOG.debugf("after start_natpmp");
    }

    public void stop_natpmp() {
        LOG.debugf("before stop_natpmp");
        catchWrapperException(libTorrent.stop_natpmp());
        LOG.debugf("after stop_natpmp");
    }

    /**
     * Set the target seed ratio for this torrent.
     */
    public void set_seed_ratio(String id, float seed_ratio) {
        LOG.debugf("before set_seed_ratio");
        catchWrapperException(libTorrent.set_seed_ratio(id, seed_ratio));
        LOG.debugf("after set_seed_ratio");
    }

    /**
     * Returns the file priority for the given index.
     */
    public int get_file_priority(String id, int fileIndex) {
        LOG.debugf("before get_file_priority");
        IntByReference priority = new IntByReference();
        catchWrapperException(libTorrent.get_file_priority(id, fileIndex, priority));
        LOG.debugf("after get_file_priority");
        return priority.getValue();
    }

    /**
     * Sets the file priority for the given index.
     */
    public void set_file_priorities(String id, int[] priorities) {
        LOG.debugf("before set_file_priorities");
        catchWrapperException(libTorrent.set_file_priorities(id, priorities, priorities.length));
        LOG.debugf("after set_file_priorities");
    }

    /**
     * Returns the number of files for the given torrent.
     */
    public int get_num_files(String id) {
        LOG.debugf("before get_num_files");
        IntByReference numFiles = new IntByReference();
        catchWrapperException(libTorrent.get_num_files(id, numFiles));
        LOG.debugf("after get_num_files");
        return numFiles.getValue();
    }

    /**
     * Returns the files for the given torrent.
     */
    public LibTorrentFileEntry[] get_files(String id) {
        LOG.debugf("before get_files");
        int numFiles = get_num_files(id);
        LibTorrentFileEntry[] fileEntries = new LibTorrentFileEntry[numFiles];
        Pointer[] filePointers = new Pointer[numFiles];
        for (int i = 0; i < fileEntries.length; i++) {
            LibTorrentFileEntry fileEntry = new LibTorrentFileEntry();
            fileEntries[i] = fileEntry;
            filePointers[i] = fileEntry.getPointer();
        }

        catchWrapperException(libTorrent.get_files(id, filePointers));

        for (int i = 0; i < fileEntries.length; i++) {
            fileEntries[i].read();
        }

        LOG.debugf("after get_files");
        return fileEntries;
    }

    public void set_auto_managed_torrent(String sha1, boolean auto_managed) {
        LOG.debugf("before set_auto_managed_torrent: {0} - {1}", sha1, auto_managed);
        catchWrapperException(libTorrent.set_auto_managed_torrent(sha1, auto_managed));
        LOG.debugf("after set_auto_managed_torrent: {0} - {1}", sha1, auto_managed);
    }

    public void set_file_priority(String sha1, int index, int priority) {
        LOG.debugf("before set_file_priority: {0} - index: {1} - priority: {2}", sha1, index,
                priority);
        catchWrapperException(libTorrent.set_file_priority(sha1, index, priority));
        LOG.debugf("after set_file_priority: {0} - index: {1} - priority: {2}", sha1, index,
                priority);
    }
}
