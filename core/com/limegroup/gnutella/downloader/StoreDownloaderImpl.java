package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.net.SocketsManager;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.UrnCache;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.templates.StoreFileNameTemplateProcessor;
import com.limegroup.gnutella.templates.StoreSubDirectoryTemplateProcessor;
import com.limegroup.gnutella.templates.StoreTemplateProcessor;
import com.limegroup.gnutella.templates.StoreTemplateProcessor.IllegalTemplateException;
import com.limegroup.gnutella.tigertree.HashTreeCache;

/**
 *  Allows the rest of LimeWire to treat this as a regular download. Handles downloading
 *  an item purchased off of the LimeWire Store (LWS) website
 */
class StoreDownloaderImpl extends ManagedDownloaderImpl implements StoreDownloader {

    /**
     * To succesfully use the download templates when saving Store Files. The MetaData
     * must be parsed after downloading has been completed.
     */
    private final MetaDataFactory metaDataFactory;

    @Inject
    public StoreDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            FileManager fileManager, IncompleteFileManager incompleteFileManager,
            DownloadCallback downloadCallback, NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory, RequeryManagerFactory requeryManagerFactory,
            QueryRequestFactory queryRequestFactory, OnDemandUnicaster onDemandUnicaster,
            DownloadWorkerFactory downloadWorkerFactory, AltLocManager altLocManager,
            ContentManager contentManager, SourceRankerFactory sourceRankerFactory,
            UrnCache urnCache, 
            VerifyingFileFactory verifyingFileFactory, DiskController diskController,
             IPFilter ipFilter, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter, Provider<HashTreeCache> tigerTreeCache,
            ApplicationServices applicationServices, RemoteFileDescFactory remoteFileDescFactory, 
            Provider<PushList> pushListProvider, SocketsManager socketsManager, MetaDataFactory metaDataFactory) {
        super(saveLocationManager, downloadManager, fileManager, incompleteFileManager,
                downloadCallback, networkManager, alternateLocationFactory, requeryManagerFactory,
                queryRequestFactory, onDemandUnicaster, downloadWorkerFactory, altLocManager,
                contentManager, sourceRankerFactory, urnCache, 
                verifyingFileFactory, diskController, ipFilter, backgroundExecutor, messageRouter,
                tigerTreeCache, applicationServices, remoteFileDescFactory, pushListProvider,
                socketsManager);
        this.metaDataFactory = metaDataFactory;
    }

    // //////////////////////////// Requery Logic ///////////////////////////

    /**
     * Overrides ManagedDownloader to return quickly
     * since we can't requery the store
     */
    @Override
    public QueryRequest newRequery() throws CantResumeException {
        return null;
    }

    /**
     * Overrides ManagedDownloader to never allow new sources to be
     * added
     */
    @Override
    public boolean allowAddition(RemoteFileDesc other) {
        return false;
    }

    /**
     * Never send requires since there is only one location to download from,
     * the LWS.
     */
    @Override
    public boolean canSendRequeryNow() {
        return false;
    }

    /**
     * Overridden to make sure it calls the super method only if 
     * the filesize is known.
     */
    @Override
    protected void initializeIncompleteFile() throws IOException {
        if (getContentLength() != -1) {
            super.initializeIncompleteFile();
        }
    }

    /**
     * Can never chat with LWS, return immediately
     */
    @Override
    public Endpoint getChatEnabledHost() {
        return null;
    }

    /**
     * Can never chat with LWS, always return false
     */
    @Override
    public boolean hasChatEnabledHost() {
        return false;
    }

    /**
     * Can never browse the LWS, return immediately
     */
    @Override
    public RemoteFileDesc getBrowseEnabledHost() {
        return null;
    }

    /**
     * Can never browse the LWS, return immediately
     */
    @Override
    public boolean hasBrowseEnabledHost() {
        return false;
    }

    /**
     * Can only connect to LWS on one socket
     */
    @Override
    public int getNumberOfAlternateLocations() {
        return 0;
    }

    /**
     * Can only connect to LWS
     */
    @Override
    public int getNumberOfInvalidAlternateLocations() {
        return 0;
    }

    /**
     * Use the file ID3 info to perform a lookup with the template to determine the
     * folder substructure for saving the file
     * 
     * WARNING: this reads the id3 tags of the file. This can potentially be a blocking 
     * method and should only be called from within its own thread
     */
    @Override
    protected File getSuggestedSaveLocation(File defaultSaveFile, File newDownloadFile) throws IOException{ 
        // if its not an mp3 its currently not from the store
        if (!newDownloadFile.getName().toLowerCase(Locale.US).endsWith("mp3"))
            return defaultSaveFile;

        // parse the meta data of this file
        AudioMetaData metaData = null;
        try {
            metaData = (AudioMetaData) metaDataFactory.parse(newDownloadFile);
        } catch (IOException e) {
            // don't catch this exception, problem reading the ID3 tags, just
            // use default locations instead
            return defaultSaveFile;
        }
        // if there's no meta data, just return the current name and location
        if (metaData == null)
            return defaultSaveFile;

        // set up a mapping of template variables to real values
        // if the substitutions are null for some reason, return default file
        final Map<String, String> subs = new HashMap<String, String>();
        if (!createSubstitutes(subs, metaData))
            return defaultSaveFile;

        // First attempt to get new directory
        final File realOutputDir = getLWSDirectory(SharingSettings.getSaveLWSDirectory(), subs);

        // make sure it is writable
        if (!FileUtils.setWriteable(realOutputDir)) {
            return defaultSaveFile;
        }
        return new File(realOutputDir, getLWSFileName(defaultSaveFile, subs));
    }

    @Override
    public boolean isRelocatable() {
        return super.isRelocatable() || getState() == DownloadStatus.SAVING;
    }

    /**
     * Attempts to use the meta data associated with this audio file and a template
     * to create sub directories based on the meta data. If the meta data is missing or
     * no template has been selected, reverts to the default LWS save directory
     * 
     * @return directory of where to save songs purchased from LimeWire Store
     */
    private File getLWSDirectory(File directory, Map<String, String> subs) {

        final String template = SharingSettings.getSubDirectoryLWSTemplate();

        // fail quickly if no template has been chosen for subdirectories
        if (template == null || template.length() == 0)
            return directory;

        File outDir = null;
        try {
            outDir = new StoreSubDirectoryTemplateProcessor().getOutputDirectory(template, subs, directory);
        } catch (IllegalTemplateException e) {
            return directory;
        }

        // if directory couldn't be made, return default location
        if (outDir == null)
            return directory;

        outDir.mkdirs();
        FileUtils.setWriteable(outDir);

        if (!outDir.isDirectory() || !outDir.canRead() || !FileUtils.canWrite(outDir))
            return directory;

        return outDir;
    }

    /**
     * Using a template and meta data, it creates a new file name by replacing the
     * template using the meta data associated with the file
     * 
     * @param defaultSaveFile current file name
     * @param subs mapping of template variables to meta data values
     * @return new file name based on template, if something went wrong, return
     *         current file name
     */
    private String getLWSFileName(File defaultSaveFile, Map<String, String> subs) {
        String currentFileName = defaultSaveFile.getName();

        final String template = SharingSettings.getFileNameLWSTemplate();

        try {
            currentFileName = new StoreFileNameTemplateProcessor().getFileName(template, subs);
            if (currentFileName == null || currentFileName.length() == 0)
                return defaultSaveFile.getName();
        } catch (IllegalTemplateException e) {
            return defaultSaveFile.getName();
        }

        String ext = FileUtils.getFileExtension(defaultSaveFile);
        if (!ext.isEmpty())
            return currentFileName + "." + ext;
        else  // no extension, shouldn't happen
            return defaultSaveFile.getName();
    }

    /**
     * Fills a map, mapping template variables to values retrieved from the meta data.
     * If values in the meta data don't exist, this returns false, otherwise returns
     * true and sanitizes all meta data to remove illegal characters that may exist
     * and filling the map
     * 
     * @param subs map to hold the substitutions
     * @param metaData id3 information about the file
     * @return false if meta data is missing, otherwise return true
     */
    private boolean createSubstitutes(Map<String, String> subs, AudioMetaData metaData) {
        String artist = metaData.getArtist();
        String album = metaData.getAlbum();
        String track = metaData.getTrack();
        String title = metaData.getTitle();

        // fail if we can't read artist or album or track or title data is corrupt or not available
        // this should never happen since the store writes all this data themselves.
        // if something is missing chances are something went wrong so just revert to 
        // defaults
        if (artist == null || album == null || title == null || track == null)
            return false;

        // sanitize data to remove any illegal chars for file names/directories
        artist = CommonUtils.santizeString(artist);
        album = CommonUtils.santizeString(album);
        track = sanitizeTrack(track);
        title = CommonUtils.santizeString(title);

        subs.put(StoreTemplateProcessor.ARTIST_LABEL, artist);
        subs.put(StoreTemplateProcessor.ALBUM_LABEL, album);
        subs.put(StoreTemplateProcessor.TITLE_LABEL, title);
        subs.put(StoreTemplateProcessor.TRACK_LABEL, track);

        return true;
    }

    /**
     * Removes the number of tracks on the album and returns just the track number
     * of this song. If the song is a single digit, it adds a zero before it
     * 
     * TODO: should modify tag reader to return track and total tracks seperately to
     * avoid needing to do this
     */
    private static String sanitizeTrack(String track) {
        String[] subStrings = track.split("/");

        String trackNumber = subStrings[0].trim();
        if (trackNumber.length() == 1)
            trackNumber = 0 + trackNumber;

        return trackNumber;
    }

    /**
     * Store files aren't shared so don't bother saving the tree
     * hash of them
     */
    @Override
    protected URN saveTreeHash(URN fileHash) {
        return null;
    }

    /**
     * Not sharing so do nothing
     */
    @Override
    protected void shareSavedFile(File saveFile) {
        // Always load the resulting file in the FileManager
        fileManager.getManagedFileList().add(saveFile);
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.STORE;
    }

    @Override
    protected boolean shouldPublishIFD() {
        return false;
    }
}
