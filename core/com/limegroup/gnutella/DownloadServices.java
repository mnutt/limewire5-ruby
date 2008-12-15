package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.io.GUID;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.CantResumeException;

public interface DownloadServices {

    /**
     * Starts a resume download for the given incomplete file.
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     * @throws SaveLocationException 
     */
    public Downloader download(File incompleteFile) throws CantResumeException,
            SaveLocationException;

    /**
     * Creates a downloader for a magnet using the given additional options.
     *
     * @param magnet provides the information of the  file to download, must be
     *  valid
     * @param overwrite whether an existing file a the final file location 
     * should be overwritten
     * @param saveDir can be null, then the save directory from the settings
     * is used
     * @param fileName the final filename of the download, can be
     * <code>null</code>
     * @return
     * @throws SaveLocationException
     * @throws IllegalArgumentException if the magnet is not
     * {@link MagnetOptions#isDownloadable() downloadable}.
     */
    public Downloader download(MagnetOptions magnet, boolean overwrite,
            File saveDir, String fileName) throws SaveLocationException;

    /**
     * Creates a downloader for a magnet.
     * @param magnetprovides the information of the  file to download, must be
     *  valid
     * @param overwrite whether an existing file a the final file location 
     * should be overwritten
     * @return
     * @throws SaveLocationException
     * @throws IllegalArgumentException if the magnet is not 
     * {@link MagnetOptions#isDownloadable() valid}.
     */
    public Downloader download(MagnetOptions magnet, boolean overwrite)
            throws SaveLocationException;

    /**
     * Creates a downloader for songs purchased from the LimeWire Store (LWS)
     * @param store - provides information of the file to download including a URN
     * @param overwrite - whether an existing file with the same name should be overwritten
     * @param saveDir - location to store the completed download to, this may be different than
     *              downloads from gnutella
     * @param fileName - name of the file once completed
     * @return
     * @throws SaveLocationException
     */
    public Downloader downloadFromStore(RemoteFileDesc rfd, boolean overwrite,
            File saveDir, String fileName) throws SaveLocationException;

    public Downloader download(RemoteFileDesc[] files, boolean overwrite,
            GUID queryGUID) throws SaveLocationException;

    /**
     * Stub for calling download(RemoteFileDesc[], DataUtils.EMPTY_LIST, boolean)
     * @throws SaveLocationException 
     */
    public Downloader download(RemoteFileDesc[] files, GUID queryGUID,
            boolean overwrite, File saveDir, String fileName)
            throws SaveLocationException;

    public Downloader download(RemoteFileDesc[] files,
            List<? extends RemoteFileDesc> alts, GUID queryGUID,
            boolean overwrite) throws SaveLocationException;

    /** 
     * Tries to "smart download" <b>any</b> [sic] of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * SaveLocationException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, SaveLocationException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The ActivityCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.  
     *
     * @param files a group of "similar" files to smart download
     * @param alts a List of secondary RFDs to use for other sources
     * @param queryGUID guid of the query that returned the results (i.e. files)
     * @param overwrite true iff the download should proceedewithout
     *  checking if it's on disk
     * @param saveDir can be null, then the save directory from the settings
     * is used
     * @param fileName can be null, then one of the filenames of the 
     * <code>files</code> array is used
     * array is used
     * @return the download object you can use to start and resume the download
     * @throws SaveLocationException if there is an error when setting the final
     * file location of the download 
     * @see DownloadManager#getFiles(RemoteFileDesc[], boolean)
     */
    public Downloader download(RemoteFileDesc[] files,
            List<? extends RemoteFileDesc> alts, GUID queryGUID,
            boolean overwrite, File saveDir, String fileName)
            throws SaveLocationException;

    /**
     * Starts a torrent download for a given Inputstream to the .torrent file
     * 
     * @param is
     *            the InputStream belonging to the .torrent file
     * @throws IOException
     *             in case there was a problem reading the file 
     */
    public Downloader downloadTorrent(BTMetaInfo info, boolean overwrite)
            throws SaveLocationException;

    /**
     * Returns whether there are any active internet (non-multicast) transfers
     * going at speed greater than 0.
     */
    public boolean hasActiveDownloads();
    
    /**
     * Returns the number of active downloads.
     */
    public int getNumActiveDownloads();

    /**
     * Returns the number of downloads in progress.
     */
    public int getNumDownloads();

}