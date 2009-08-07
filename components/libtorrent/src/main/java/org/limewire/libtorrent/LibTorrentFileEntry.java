package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentFileEntry;

import com.sun.jna.Structure;
import com.sun.jna.WString;

/**
 * Represents a file in the torrent.
 */
public class LibTorrentFileEntry extends Structure implements Structure.ByReference,
        TorrentFileEntry {

    public int index;

    public WString path;

    public long size;

    public long total_done;

    public int priority;

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getPath() {
        return path.toString();
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getTotalDone() {
        return total_done;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public float getProgress() {
        if (getSize() == 0) {
            return 1;
        }

        return getTotalDone() / (float) getSize();
    }

    @Override
    public String toString() {
        return index + " - " + path + " - " + total_done + "/" + size + "priority: " + priority;
    }
}
