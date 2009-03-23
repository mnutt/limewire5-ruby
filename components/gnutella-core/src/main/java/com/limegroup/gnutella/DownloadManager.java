package com.limegroup.gnutella;

import java.io.File;
import java.net.Socket;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.listener.ListenerSupport;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.LWSIntegrationServicesDelegate;
import com.limegroup.gnutella.downloader.PushedSocketHandler;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.version.DownloadInformation;
import com.limegroup.mozilla.MozillaDownload;


/** 
 * The list of all downloads in progress.  <code>DownloadManager</code> has a 
 * fixed number of download slots given by the MAX_SIM_DOWNLOADS property.  It 
 * is responsible for starting downloads and scheduling and queueing them as 
 * needed.  This class is thread safe.<p>
 *
 * As with other classes in this package, a <code>DownloadManager</code> instance 
 * may not be used until initialize(..) is called.  The arguments to this are 
 * not passed in to the constructor in case there are circular dependencies.<p>
 *
 * <code>DownloadManager</code> provides ways to serialize download state to 
 * disk.  Reads are initiated by RouterService, since we have to wait until the 
 * GUI is initiated.  Writes are initiated by this, since we need to be notified 
 * of completed downloads.  Downloads in the COULDNT_DOWNLOAD state are not 
 * serialized.  
 */
public interface DownloadManager extends BandwidthTracker, SaveLocationManager, 
LWSIntegrationServicesDelegate, PushedSocketHandler, ListenerSupport<DownloadManagerEvent> {
    
    /**
     * Adds a new downloader that this will manager.
     */
    public void addNewDownloader(CoreDownloader downloader);

    /** 
     * Initializes this manager. <b>This method must be called before any other
     * methods are used.</b> 
     */
    public void start();

    /**
     * Performs the slow, low-priority initialization tasks: reading in
     * snapshots and scheduling snapshot checkpointing.
     */
    public void loadSavedDownloadsAndScheduleWriting();

    /** True if saved downloads have been loaded from disk. */
    public boolean isSavedDownloadsLoaded();

    /**
     * Determines if an 'In Network' download exists in either active or waiting.
     */
    public boolean hasInNetworkDownload();

    /**
     * Kills all in-network downloaders that are not present in the list of 
     * DownloadInformations
     * @param updates a current set of DownloadInformation that we are 
     * downloading in-network.
     */
    public void killDownloadersNotListed(Collection<? extends DownloadInformation> updates);

    public boolean acceptPushedSocket(String file, int index, byte[] clientGUID, Socket socket);

    /**
     * Determines if the given URN has an incomplete file.
     */
    public boolean isIncomplete(URN urn);

    /**
     * Returns whether or not we are actively downloading this file.
     */
    public boolean isActivelyDownloading(URN urn);

    /**
     * Returns the IncompleteFileManager used by this DownloadManager
     * and all ManagedDownloaders.
     */
    public IncompleteFileManager getIncompleteFileManager();

    public int downloadsInProgress();

    public int getNumIndividualDownloaders();

    /**
     * Inner network traffic and downloads from the LWS don't count towards overall
     * download activity.
     */
    public int getNumActiveDownloads();

    public int getNumWaitingDownloads();

    public Downloader getDownloaderForURN(URN sha1);

    /**
     * Returns the active or waiting downloader that uses or will use 
     * <code>file</code> as incomplete file.
     * @param file the incomplete file candidate
     * @return <code>null</code> if no downloader for the file is found
     */
    public Downloader getDownloaderForIncompleteFile(File file);

    public boolean isGuidForQueryDownloading(GUID guid);

    /** 
     * Tries to "smart download" any of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * AlreadyDownloadingException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, FileExistsException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The DownloadCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.
     * 
     * @param files a group of "similar" files to smart download
     * @param alts a List of secondary RFDs to use for other sources
     * @param queryGUID the guid of the query that resulted in the RFDs being
     * downloaded.
     * @param overwrite whether or not to overwrite the file
     * @param saveDir can be null, then the default save directory is used
     * @param fileName can be null, then the first filename of one of element of
     * <code>files</code> is taken.
     * @throws SaveLocationException when there was an error setting the
     * location of the final download destination.
     *
     *     @modifies this, disk 
     */
    public Downloader download(RemoteFileDesc[] files, List<? extends RemoteFileDesc> alts,
            GUID queryGUID, boolean overwrite, File saveDir, String fileName)
            throws SaveLocationException;

    /**
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURL</tt>, if specified.  If that fails, or if defaultURL does
     * not provide alternate locations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (At least one must be non-null.)  If
     * <tt>filename</tt> is specified, it will be used as the name of the
     * complete file; otherwise it will be taken from any search results or
     * guessed from <tt>defaultURLs</tt>.
     *
     * @param magnet information fields extracted from a magnet link
     * @param overwrite whether or not to overwrite the file
     * @param saveDir can be null, then the default save directory is used
     * @param filename the final file name, or <code>null</code> if unknown
     * @exception IllegalArgumentException if the magnet is not downloadable
     * @throws SaveLocationException if the file can't save because of an 
     * existing file in the location
     */
    public Downloader download(MagnetOptions magnet, boolean overwrite, File saveDir,
            String fileName) throws IllegalArgumentException, SaveLocationException;

    /**
     * Creates a new LimeWire Store (LWS) download. Store downloads are handled 
     * in a similar fashion as MAGNET links except there are no alternative 
     * locations.  <tt>filename</tt> should always be specified since we have 
     * complete control over META-DATA for these downloads, it will be used as 
     * the name of the complete file. Unlike all other downloads performed here, 
     * saveDir is a unique directory specified in the options menu under Store 
     * Downloads.
     * 
     * @param rfd Descriptor describing the download from the store including URN
     * @param overwrite true if same file names should be overwritten; false if
     * the user should be notified of a duplicate file name
     * @param saveDir directory to save the completed file into
     * @param fileName name of the completed file
     * @return a download object
     * @throws IllegalArgumentException if there is a bad argument
     * @throws SaveLocationException if the file is already downloading
     */
    public Downloader downloadFromStore(RemoteFileDesc rfd, boolean overwrite, File saveDir,
            String fileName) throws IllegalArgumentException, SaveLocationException;

    /**
     * Starts a resume download for the given incomplete file.
     * @exception CantResumeException incompleteFile is not a valid 
     * incomplete file
     * @throws SaveLocationException if the file can't save because of an 
     * existing file in the location 
     */
    public Downloader download(File incompleteFile) throws CantResumeException,
            SaveLocationException;

    /**
     * Downloads an InNetwork update, using the info from the DownloadInformation.
     */
    public Downloader download(DownloadInformation info, long now) throws SaveLocationException;

    /**
     * Downloads the torrent file from the specified URI then begins the torrent download as a seperate item.
     */
    public Downloader downloadTorrent(URI torrentURI, boolean overwrite) throws SaveLocationException;
    
    /**
     * Opens the torrent for the specified file, and begins the torrent download.
     */
    public Downloader downloadTorrent(File torrentFile, boolean overwrite) throws SaveLocationException;
    
    /**
     * Downloads the given torrent specified by the meta info object.
     */
    public Downloader downloadTorrent(BTMetaInfo info, boolean overwrite)
            throws SaveLocationException;

    /**
     * Returns <code>true</code> if there already is a download with the same urn. 
     * @param urn may be <code>null</code>, then a check based on the fileName
     * and the fileSize is performed
     * @return true if there is a conflict
     */
    public boolean conflicts(URN urn, long fileSize, File... fileName);

    /**
     * Returns <code>true</code> if there already is a download that is or
     * will be saving to this file location.
     * @param candidateFile the final file location.
     * @return true if the file's save location is already used
     */
    public boolean isSaveLocationTaken(File candidateFile);

    /** 
     * Adds all responses (and alternates) in <code>qr</code> to any downloaders, if
     * appropriate.
     * @param address can be null, otherwise overrides the address information in <code>qr</code>
     */
    public void handleQueryReply(QueryReply qr, Address address);

    /**
     * Removes downloader entirely from the list of current downloads.
     * Notifies callback of the change in status.
     * If completed is true, finishes the download completely.  Otherwise,
     * puts the download back in the waiting list to be finished later.
     *     @modifies this, callback
     */
    public void remove(CoreDownloader downloader, boolean completed);

    /**
     * Bumps the priority of an inactive download either up or down
     * by an amount (if amt==0, bump to start/end of list).
     */
    public void bumpPriority(Downloader downl, boolean up, int amt);

    /** 
     * Attempts to send the given requery to provide the given downloader with 
     * more sources to download.  May not actually send the requery if it doing
     * so would exceed the maximum requery rate.
     * @param query the requery to send, which should have a marked GUID.
     *  Queries are subjected to global rate limiting if and only if they have marked 
     *  requery GUIDs.
     */
    public void sendQuery(QueryRequest query);

    /** Calls measureBandwidth on each uploader. */
    public void measureBandwidth();

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
    public float getMeasuredBandwidth();

    /**
     * returns the summed average of the downloads
     */
    public float getAverageBandwidth();

    /**
     * Returns the measured bandwidth as calculated from the last
     * getMeasuredBandwidth() call.
     */
    public float getLastMeasuredBandwidth();

    public Iterable<CoreDownloader> getAllDownloaders();
    
    /** Writes a snapshot of all downloaders in this and all incomplete files to
     *  the file named DOWNLOAD_SNAPSHOT_FILE.  It is safe to call this method
     *  at any time for checkpointing purposes.  Returns true if and only if the 
     *  file was successfully written. */
    public void writeSnapshot();

    /**
     * Creates a Downloader wrapping the MozillaDownloadListener. 
     * Adds capability to track status of mozilla download.
     */
    public Downloader downloadFromMozilla(MozillaDownload listener);

    /**
     * Returns true if the given downloader is in either the waiting or active lists. 
     */
    public boolean contains(Downloader downloader);

}
