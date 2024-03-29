package org.limewire.core.api.callback;

import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.magnet.MagnetLink;

/**
 * This class acts as a means of getting user input on issues happening in the
 * core.
 */
public interface GuiCallback {

    /**
     * Attempts to handle the supplied DownloadException with the supplied
     * download action.
     */
    void handleDownloadException(DownloadAction downLoadAction, DownloadException e,
            boolean supportsNewSaveDir);

    /**
     * Restores the application from a minimized state.
     */
    void restoreApplication();

    /**
     * Properly handles the magnet by either spawning a download or a search.
     */
    void handleMagnet(MagnetLink magnetLink);

    /**
     * Returns the locale translated version of the given string.
     */
    String translate(String s);

    /**
     * Prompts the user with a yes/no question and returns true if the user
     * responded yes, false otherwise.
     */
    boolean promptUserQuestion(String marktr);

    /**
     * Shows the user a warning about a file.
     */
    void warnUser(String filename, String message);

    /**
     * Prompts the user about what priorities to assign the files in this
     * torrent. Returns true if ok was selected in the end false if cancel.
     */
    boolean promptTorrentFilePriorities(Torrent torrent);
}
