package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.io.InvalidDataException;
import org.limewire.net.SocketsManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.InNetworkDownloadMemento;
import com.limegroup.gnutella.downloader.serial.InNetworkDownloadMementoImpl;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryUtils;
import com.limegroup.gnutella.library.UrnCache;
import com.limegroup.gnutella.malware.DangerousFileChecker;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.version.DownloadInformation;

/**
 * A downloader that works in the background, using the network to continue itself.
 */
class InNetworkDownloaderImpl extends ManagedDownloaderImpl implements InNetworkDownloader {
    
    private String tigerTreeRoot;
    private long startTime;
    private int downloadAttempts;
    
    /** 
     * Constructs a new downloader that's gonna work off the network.
     */
    @Inject
    InNetworkDownloaderImpl(SaveLocationManager saveLocationManager,
            DownloadManager downloadManager,
            @GnutellaFiles FileCollection gnutellaFileCollection,
            IncompleteFileManager incompleteFileManager,
            DownloadCallback downloadCallback,
            NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory,
            RequeryManagerFactory requeryManagerFactory,
            QueryRequestFactory queryRequestFactory,
            OnDemandUnicaster onDemandUnicaster,
            DownloadWorkerFactory downloadWorkerFactory,
            AltLocManager altLocManager,
            ContentManager contentManager,
            SourceRankerFactory sourceRankerFactory,
            UrnCache urnCache,
            VerifyingFileFactory verifyingFileFactory,
            DiskController diskController,
            IPFilter ipFilter,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter,
            Provider<HashTreeCache> tigerTreeCache,
            ApplicationServices applicationServices,
            RemoteFileDescFactory remoteFileDescFactory,
            Provider<PushList> pushListProvider,
            SocketsManager socketsManager, 
            @Named("downloadStateProcessingQueue") ListeningExecutorService downloadStateProcessingQueue,
            DangerousFileChecker dangerousFileChecker,
            SpamManager spamManager,
            Library library) throws DownloadException {
        super(saveLocationManager, downloadManager, gnutellaFileCollection,
                incompleteFileManager, downloadCallback, networkManager,
                alternateLocationFactory, requeryManagerFactory,
                queryRequestFactory, onDemandUnicaster, downloadWorkerFactory,
                altLocManager, contentManager, sourceRankerFactory, urnCache, 
                verifyingFileFactory, diskController, ipFilter,
                backgroundExecutor, messageRouter, tigerTreeCache,
                applicationServices, remoteFileDescFactory, pushListProvider,
                socketsManager, downloadStateProcessingQueue,
                dangerousFileChecker, spamManager, library);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.InNetworkDownloader#initDownloadInformation(com.limegroup.gnutella.version.DownloadInformation, long)
     */
    public void initDownloadInformation(DownloadInformation downloadInformation, long startTime) {
        // note: even though we support bigger files, this is a good sanity check
        if (downloadInformation.getSize() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("size too big for now.");
        setContentLength(downloadInformation.getSize());
        if(downloadInformation.getUpdateURN() != null)
            setSha1Urn(downloadInformation.getUpdateURN());
        setTigerTreeRoot(downloadInformation.getTTRoot());
        setStartTime(startTime);
        setDownloadAttempts(0);
    }    
    
    protected synchronized void setDownloadAttempts(int i) {
        this.downloadAttempts = i;
    }

    protected synchronized void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    protected synchronized void setTigerTreeRoot(String tigerTreeRoot) {
        this.tigerTreeRoot = tigerTreeRoot;
    }

    /**
     * Overriden to use a different incomplete directory.
     */
    @Override
    protected File getIncompleteFile(String name, URN urn,
                                     long length) throws IOException {
        return incompleteFileManager.getFile(name, urn, length, new File(LibraryUtils.PREFERENCE_SHARE, "Incomplete"));
    }
    
    /**
     * Gets a new SourceRanker, using only LegacyRanker (not PingRanker).
     */
    @Override
    protected SourceRanker getSourceRanker(SourceRanker oldRanker) {
        if(oldRanker != null)
            return oldRanker;
        else
            return new LegacyRanker();
    }
        
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.InNetworkDownloader#startDownload()
     */
    @Override
    public synchronized void startDownload() {
        incrementDownloadAttempts();
        super.startDownload();
    }
    
    private synchronized void incrementDownloadAttempts() {
        downloadAttempts++;
    }

    @Override
    protected boolean shouldValidate() {
        return false;
    }
    
    /**
     * Ensures that the VerifyingFile knows what TTRoot we're expecting.
     */
    @Override
    protected void initializeVerifyingFile() throws IOException {
        super.initializeVerifyingFile();
        if(commonOutFile != null) {
            commonOutFile.setExpectedHashTreeRoot(getTigerTreeRoot());
        }
    }
    
    protected synchronized String getTigerTreeRoot() {
        return tigerTreeRoot;
    }

    /** Sends a targeted query for this. */
    @Override
    public synchronized QueryRequest newRequery() throws CantResumeException {
        QueryRequest qr = super.newRequery();
        qr.setTTL((byte) 2);
        return qr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.downloader.InNetworkDownloader#getNumAttempts()
     */
    public synchronized int getDownloadAttempts() {
        return downloadAttempts;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.InNetworkDownloader#getStartTime()
     */
    public synchronized long getStartTime() {
        return startTime;
    }
    
    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.INNETWORK;
    }
    
    @Override
    protected void fillInMemento(DownloadMemento memento) {
        super.fillInMemento(memento);
        InNetworkDownloadMemento imem = (InNetworkDownloadMemento)memento;
        imem.setTigerTreeRoot(getTigerTreeRoot());
        imem.setStartTime(getStartTime());
        imem.setDownloadAttempts(getDownloadAttempts());
    }
    
    @Override
    protected DownloadMemento createMemento() {
        return new InNetworkDownloadMementoImpl();
    }
    
    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        super.initFromMemento(memento);
        InNetworkDownloadMemento imem = (InNetworkDownloadMemento)memento;
        setTigerTreeRoot(imem.getTigerTreeRoot());
        setStartTime(imem.getStartTime());
        setDownloadAttempts(imem.getDownloadAttempts());
    }
    
    @Override
    public void setSaveFile(File saveDirectory, String fileName, boolean overwrite)
            throws DownloadException {
        //overriding to track down cause of https://www.limewire.org/jira/browse/LWC-3697 remove when fixed
        super.setSaveFile(saveDirectory, fileName, overwrite);
    }
}
