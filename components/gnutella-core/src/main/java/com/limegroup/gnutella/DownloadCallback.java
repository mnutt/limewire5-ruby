package com.limegroup.gnutella;

import org.limewire.bittorrent.Torrent;

/**
 * A callback for download information.
 */
public interface DownloadCallback {
    

    /** Add a file to the download window */
    public void addDownload(Downloader d);

    /** Remove a downloader from the download window. */
    public void removeDownload(Downloader d);

     /** 
      * Notifies the GUI that all active downloads have been completed.
      */   
    public void downloadsComplete();

    /**
     * Shows the user a message informing her that a file being downloaded 
     * is corrupt.
     * <p>
     * This method MUST call dloader.discardCorruptDownload(boolean b) 
     * otherwise there will be threads piling up waiting for a notification
     */
    public void promptAboutCorruptDownload(Downloader dloader);
    
    /**
     * Shows the user a warning about a file.
     */
    public void warnUser(String filename, String message);

    /**
     * Prompts the user about what priorities to assign the files in this
     * torrent. Returns true if ok was selected in the end false if cancel.
     */
    public boolean promptTorrentFilePriorities(Torrent torrent);
}