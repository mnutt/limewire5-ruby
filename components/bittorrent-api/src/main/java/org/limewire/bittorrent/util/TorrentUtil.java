package org.limewire.bittorrent.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.bittorrent.Torrent;

public class TorrentUtil {
    /**
     * Builds a list of files from the torrents list of paths, starting at the
     * rootFolder.
     */
    public static List<File> buildTorrentFiles(Torrent torrent, File rootFolder) {
        List<File> files = new ArrayList<File>();
        if (torrent.getPaths().size() > 0) {
            for (String path : torrent.getPaths()) {
                File file = new File(rootFolder, path);
                files.add(file);
            }
        } else {
            files.add(rootFolder);
        }
        return files;
    }
}
