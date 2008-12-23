package org.limewire.core.api.callback;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;

/**
 * This class acts as a means of getting user input on issues happening in the
 * core.
 */
public interface GuiCallback {

    /**
     * Attempts to handle the supplied SaveLocationException with the supplied
     * download action.
     */
    void handleSaveLocationException(DownloadAction downLoadAction, SaveLocationException sle,
            boolean supportsNewSaveDir);

    /**
     * Prompts the user about canceling the torrent upload, because they have a
     * low seed ratio.
     */
    boolean promptTorrentSeedRatioLow();

    /**
     * Prompts the user about canceling the torrent upload, because it will
     * cancel a current download of that torrent as well.
     */
    boolean promptTorrentDownloading();
}
