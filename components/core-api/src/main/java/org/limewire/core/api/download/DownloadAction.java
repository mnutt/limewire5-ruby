package org.limewire.core.api.download;

import java.io.File;

/**
 * General interface for a download action.
 */
public interface DownloadAction {
    
    /**
     * Called to start the download.
     * @param saveFile   Location to save the downloaded file.
     * @param overwrite  Whether to overwrite or not if the file exists already. 
     * @throws SaveLocationException  If the save fails an exception is returned describing the problem.
     */
    void download(File saveFile, boolean overwrite) throws SaveLocationException;
}