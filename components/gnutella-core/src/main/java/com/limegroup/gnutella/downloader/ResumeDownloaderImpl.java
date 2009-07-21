package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.net.SocketsManager;
import org.limewire.util.Objects;

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
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.UrnCache;
import com.limegroup.gnutella.malware.DangerousFileChecker;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.tigertree.HashTreeCache;

/**
 * A ManagedDownloader that tries to resume a specific incomplete file.  The
 * ResumeDownloader initially has no locations to download from.  Instead it
 * immediately requeries--by hash if possible--and only accepts results that
 * would result in resumes from the specified incomplete file.  Do not be
 * confused by the name; ManagedDownloader CAN resume from incomplete files,
 * but it is less strict about its choice of download.
 */
class ResumeDownloaderImpl extends ManagedDownloaderImpl implements ResumeDownloader {

    /** 
     * Creates a RequeryDownloader to finish downloading incompleteFile.  This
     * constructor has preconditions on several parameters; putting the burden
     * on the caller makes the method easier to implement, since the superclass
     * constructor immediately starts a download thread.
     * @param incompleteFile the incomplete file to resume to, which
     *  MUST be the result of IncompleteFileManager.getFile.
     * @param name the name of the completed file, which MUST be the result of
     *  IncompleteFileManager.getCompletedName(incompleteFile)
     * @param size the size of the completed file, which MUST be the result of
     *  IncompleteFileManager.getCompletedSize(incompleteFile) */
    @Inject
    ResumeDownloaderImpl(SaveLocationManager saveLocationManager,
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
            Library library) {
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
     * @see com.limegroup.gnutella.downloader.ResumeDownloader#initIncompleteFile(java.io.File, java.lang.String, long)
     */
    public void initIncompleteFile(File incompleteFile, long size) {
        setIncompleteFile(Objects.nonNull(incompleteFile, "incompleteFile"));
        setContentLength(size);
        URN sha1 = incompleteFileManager.getCompletedHash(incompleteFile);
        if(sha1 != null)
            setSha1Urn(sha1);
    }

    /**
     * Overrides ManagedDownloader to ensure that progress is initially non-zero
     * and file previewing works.
     */
    @Override
    public void initialize() {
        super.initialize();
        
        // NOTE: We do not auto-activate anymore because
        //       we automatically create downloaders for
        //       incomplete files.
//        // Auto-activate the requeryManager if this was created
//        // from clicking 'Resume' in the library (as opposed to
//        // from being deserialized from disk).
//        requeryManager.activate();
    }
    
    @Override
    public void setSaveFile(File saveDirectory, String fileName, boolean overwrite)
            throws DownloadException {
        //overriding to track down cause of https://www.limewire.org/jira/browse/LWC-3697 remove when fixed
        super.setSaveFile(saveDirectory, fileName, overwrite);
    }
}
