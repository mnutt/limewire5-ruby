package org.limewire.libtorrent;

import java.math.BigInteger;

import org.limewire.bittorrent.TorrentState;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.util.StringUtils;

import com.sun.jna.Structure;

/**
 * Structure used to pass data from the native code regarding a torrents state
 * back to java.
 */
public class LibTorrentStatus extends Structure implements TorrentStatus {

    /**
     * String containing the total amount of the torrent downloaded and
     * verified.
     */
    public String total_done;

    /**
     * String containing the total amount of the torrent downloaded.
     */
    public String total_download;

    /**
     * String containing the total amount of the torrent uploaded.
     */
    public String total_upload;

    /**
     * The current rate of the torrent download in bytes/second
     */
    public float download_rate;

    /**
     * The current rate of the torrent upload in bytes/second
     */
    public float upload_rate;

    /**
     * The number of peers this torrent has.
     */
    public int num_peers;

    /**
     * The number of uplaod peers this torrent has.
     */
    public int num_uploads;

    /**
     * The number of seeds this torrent has.
     */
    public int num_seeds;

    /**
     * The number of connections this torrent has.
     */
    public int num_connections;

    /**
     * The state of this torrent, aligning with LibTorrentState.
     */
    public int state;

    /**
     * The progress of this torretns download, from 0 to 1.0.
     */
    public float progress;

    /**
     * boolean of whether this torrent is paused.
     */
    public int paused;

    /**
     * boolean of whether this torrent is finished.
     */
    public int finished;

    /**
     * boolean of whether this torrent is valid.
     */
    public int valid;

    /**
     * String containing the error message for the torrent. Null/empty if there
     * is no error.
     */
    public String error;

    @Override
    public float getDownloadRate() {
        return download_rate;
    }

    @Override
    public float getUploadRate() {
        return upload_rate;
    }

    @Override
    public int getNumPeers() {
        return num_peers;
    }

    @Override
    public int getNumUploads() {
        return num_uploads;
    }

    @Override
    public int getNumSeeds() {
        return num_seeds;
    }

    @Override
    public int getNumConnections() {
        return num_connections;
    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public long getTotalDone() {
        if (total_done == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_done);
            return total.longValue();
        }
    }

    @Override
    public long getTotalDownload() {
        if (total_download == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_download);
            return total.longValue();
        }
    }

    @Override
    public long getTotalUpload() {
        if (total_upload == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_upload);
            return total.longValue();
        }
    }

    @Override
    public boolean isPaused() {
        return paused != 0;
    }

    @Override
    public boolean isFinished() {
        return finished != 0;
    }

    @Override
    public boolean isError() {
        return !StringUtils.isEmpty(error);
    }

    @Override
    public TorrentState getState() {
        LibTorrentState libTorrentState = LibTorrentState.forId(state);
        switch (libTorrentState) {
        case ALLOCATING:
            return TorrentState.ALLOCATING;
        case CHECKING_FILES:
            return TorrentState.CHECKING_FILES;
        case DOWNLOADING:
            return TorrentState.DOWNLOADING;
        case DOWNLOADING_METADATA:
            return TorrentState.DOWNLOADING_METADATA;
        case FINISHED:
            return TorrentState.FINISHED;
        case QUEUED_FOR_CHECKING:
            return TorrentState.QUEUED_FOR_CHECKING;
        case SEEDING:
            return TorrentState.SEEDING;
        default:
            throw new UnsupportedOperationException("No known state for id: " + state
                    + " and libtorrent state: " + libTorrentState);
        }
    }
}
