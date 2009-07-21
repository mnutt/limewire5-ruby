package org.limewire.libtorrent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.bittorrent.TorrentException;
import org.limewire.bittorrent.TorrentSettings;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;
import org.limewire.util.OSUtils;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.WString;

/**
 * Wrapper class for the LibTorrent c interface. Provides library loading logic,
 * and handles rethrowing c++ exceptions as java exceptions.
 */
class LibTorrentWrapper {

    private static final char MAX_LENGTH_DIGITS = 4;

    private static final int IP_SIZE = 16;

    private static final Log LOG = LogFactory.getLog(LibTorrentWrapper.class);

    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private LibTorrent libTorrent;

    /**
     * Initializes the LibTorrent library. Finding necessary dependencies first,
     * then loading the libtorrent library as a jna lib.
     */
    void initialize(TorrentSettings torrentSettings) {
        try {
            if (OSUtils.isWindows()) {
                System.loadLibrary("libeay32");
                System.loadLibrary("ssleay32");
                System.loadLibrary("boost_date_time-vc90-mt-1_39");
                System.loadLibrary("boost_system-vc90-mt-1_39");
                System.loadLibrary("boost_filesystem-vc90-mt-1_39");
                System.loadLibrary("boost_thread-vc90-mt-1_39");
                System.loadLibrary("torrent");
            } else if (OSUtils.isLinux()) {
                // everything compiled into libtorrent-wrapper.so
            } else if (OSUtils.isMacOSX()) {
                // everything compiled into libtorrent-wrapper.dylib
            }

            this.libTorrent = (LibTorrent) Native.loadLibrary("torrent-wrapper", LibTorrent.class);

            init();
            loaded.set(true);
        } catch (Throwable e) {
            if(torrentSettings.isReportingLibraryLoadFailture()) {
                ExceptionUtils.reportOrReturn(e);
            }
            LOG.error("Failure loading the libtorrent libraries.", e);
        }
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    private void init() {
        LOG.debugf("before init");
        catchWrapperException(libTorrent.init());
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

    public List<String> get_peers(String id) {
        Memory numPeersMemory = new Memory(MAX_LENGTH_DIGITS);
        int numUnfilteredPeers = numPeersMemory.getInt(0);

        LOG.debugf("before get_num_peers: {0}", id);
        catchWrapperException(libTorrent.get_num_viewable_peers(id, numPeersMemory));
        try {
            numUnfilteredPeers = Integer.parseInt(numPeersMemory.getString(0));
        } catch (NumberFormatException e) {
            numUnfilteredPeers = 0;
        }
        LOG.debugf("after get_num_peers: {0} - {1}", id, numUnfilteredPeers);

        if (numUnfilteredPeers == 0) {
            return Collections.emptyList();
        }

        Memory memory = new Memory(numUnfilteredPeers * IP_SIZE);

        LOG.debugf("before get_peers: {0}", id);
        catchWrapperException(libTorrent.get_peers(id, (int) memory.getSize(), memory));
        LOG.debugf("after get_peers: {0}", id);

        List<String> peers = Arrays.asList(memory.getString(0).split(";"));

        return peers;
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

    public void update_settings(TorrentSettings torrentSettings) {
        LOG.debugf("before update_settings: {0}", torrentSettings);
        catchWrapperException(libTorrent.update_settings(new LibTorrentSettings(torrentSettings)));
        LOG.debugf("after update_settings: {0}", torrentSettings);

    }
}
