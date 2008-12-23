package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.settings.LWSSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.lifecycle.Service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.lws.server.LWSManagerCommandResponseHandlerWithCallback;
import com.limegroup.gnutella.lws.server.LWSUtil;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.Tagged;


@Singleton
public final class LWSIntegrationServicesImpl implements LWSIntegrationServices, Service {
    
    private static final Log LOG = LogFactory.getLog(LWSIntegrationServicesImpl.class);
    
    /** 
     * The period in milliseconds for checking that {@link #getDownloadProgress()} is called.
     */
    private final static int CALL_GET_DOWNLOAD_PROGRESS_PERIOD_MILLIS =  5 * 60 * 1000;
    
    /** The bytes on a 1x1 png image for respond to ping requests. */
    
    /**
     * The period in milliseconds for checking that
     * {@link #everActiveDownloaderIDs2Downloaders()} only contains active
     * downloaders.
     */
    private final static int CHECK_EVER_ACTIVE_DOWNLOADER_IDS_2_DOWNLOADERS_PERIOD_MILLIS =  10 * 1000;
    
    /** Maintain the last time we called {@link #getDownloadProgress()}, intialized to <code>-1</code>. */
    private long lastTimeWeCalledGetDownloadProgress = -1;
    
    private final LWSManager lwsManager;
    private final DownloadServices downloadServices;
    private final LWSIntegrationServicesDelegate lwsIntegrationServicesDelegate;
    private final RemoteFileDescFactory remoteFileDescFactory;
    private final ScheduledExecutorService scheduler;
    
    /**
     * Maintain a map from downloader IDs to progress bar IDs, because the client sometimes
     * cannot keep this state.  Clear them whenever the downloader finishes.
     */
    private final Map<String,String> downloaderIDs2progressBarIDs = new HashMap<String,String>();
    
    /**
     * Instances of this interface should appear like a downloader, but not
     * actually contain one. There are two implementations:
     * <ol>
     * <li>{@link #HeavyWeightDownloaderInterface} contains a {@link CoreDownloader}</li>
     * <li>{@link #LightWeightDownloaderInterface} contains only the state of a completed {@link CoreDownloader}</li>
     * </ol>
     */
    private interface DownloaderInterface {

        /**
         * @see Downloader#getAmountRead()
         */
        long getAmountRead();

        /**
         * @see Downloader#getContentLength()
         */        
        long getContentLength();

        /**
         * @see Downloader#getState()
         */        
        DownloadStatus getState();

        /**
         * @see Downloader#isCompleted()
         */        
        boolean isCompleted();

        /**
         * Returns <code>true</code> if this instance actually contains a downloader.
         * 
         * @return <code>true</code> if this instance actually contains a downloader.
         */        
        boolean isHeavyWeight();
        
        /**
         * Releases any resources.  This may not be totally necessary.
         */
        void release();
    }
    
    /**
     * Implementation of {@link DownloaderInterface} with an actual downloader.
     * Package protected for testing.
     */
    final static class HeavyWeightDownloaderInterface implements DownloaderInterface {
        
        private Downloader d;
        HeavyWeightDownloaderInterface(Downloader d) {
            this.d = d;
        }

        public boolean isHeavyWeight() {
            return true;
        }

        public long getAmountRead() {
            return d.getAmountRead();
        }

        public long getContentLength() {
            return d.getContentLength();
        }

        public DownloadStatus getState() {
            return d.getState();
        }

        public boolean isCompleted() {
            return d.isCompleted();
        }

        public void release() {
            d = null;
        }  
    }
    
    /**
     * Implementation of {@link DownloaderInterface} with just the values from
     * another. Package protected for testing.
     */
    final static class LightWeightDownloaderInterface implements DownloaderInterface {
        
        private final long amountRead;
        private final long contentLength;
        private final DownloadStatus state;
        
        LightWeightDownloaderInterface(DownloaderInterface d) {
            amountRead = d.getAmountRead();
            contentLength = d.getContentLength();
            state = d.getState();
        }

        public boolean isHeavyWeight() {
            return false;
        }

        public long getAmountRead() {
            return amountRead;
        }

        public long getContentLength() {
            return contentLength;
        }

        public DownloadStatus getState() {
            return state;
        }

        public boolean isCompleted() {
            return true;
        }

        public void release() {
            // Nothing
        }  
    }    
    
    
    
    /**
     * We maintain a collection of ever-active downloaders, so that when
     * we iterate over all the current downloaders, we know that if one
     * appears in the ever-active collection, but not the current we
     * need to inspect further.
     * 
     * More specifically, that downloader could have completed normally
     * or it could have been completed by being cancelled. If the
     * downloader completed: 
     * 
     * - Normally: Remove it from the ever-active
     *   collection and pass back a progress of 1.0
     *    
     * - By being cancelled: Pass back the String 'X' denoting it 
     *   was cancelled.
     * 
     * The reason we need to do this is because, when the Store is on
     * the download page and connected to the Client, it will poll the
     * Client for the progress of all active and waiting downloads. In
     * the case that a downloader is either (1) finished normally or (2)
     * cancelled, and the Client polls after this occurs, we don't know
     * which occured. Basically this allows the Store to sync with the
     * Client, so we know when a download actually completes.
     * 
     * Furthermore, if the download is at 99% and then completes, and
     * the Store polls after this occurs the progress bars on the Store
     * web site will remain at 99%, because we never passed back
     * notification that the download completed.
     */
    private final Map<String, DownloaderInterface> everActiveDownloaderIDs2Downloaders = new HashMap<String, DownloaderInterface>();
    
    private String downloadPrefix;
    
    @Inject
    public LWSIntegrationServicesImpl(LWSManager lwsManager, 
            DownloadServices downloadServices,
            LWSIntegrationServicesDelegate lwsIntegrationServicesDelegate,
            RemoteFileDescFactory remoteFileDescFactory,
            @Named("backgroundExecutor") ScheduledExecutorService scheduler) {
        this(lwsManager,downloadServices,lwsIntegrationServicesDelegate,remoteFileDescFactory,scheduler,LWSSettings.LWS_DOWNLOAD_PREFIX.getValue());
    }    
    
    /** For testing. */
    LWSIntegrationServicesImpl(LWSManager lwsManager, 
                                      DownloadServices downloadServices,
                                      LWSIntegrationServicesDelegate lwsIntegrationServicesDelegate,
                                      RemoteFileDescFactory remoteFileDescFactory,
                                      ScheduledExecutorService scheduler,
                                      String downloadPrefix) {
        this.lwsManager = lwsManager;
        this.downloadServices = downloadServices;
        this.lwsIntegrationServicesDelegate = lwsIntegrationServicesDelegate;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.scheduler = scheduler;
        this.downloadPrefix = downloadPrefix;
        this.scheduler.schedule(new Runnable() {
            public void run() {
                //
                // Call getDownloadProgress() if the difference between the last time we called
                // it and now is not less than CALL_GET_DOWNLOAD_PROGRESS_PERIOD_MILLIS
                //
                long now = System.currentTimeMillis();
                if (lastTimeWeCalledGetDownloadProgress != -1 && (now-lastTimeWeCalledGetDownloadProgress) < CALL_GET_DOWNLOAD_PROGRESS_PERIOD_MILLIS) {
                    return;
                }
                getDownloadProgress();
            }
        }, CALL_GET_DOWNLOAD_PROGRESS_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
        this.scheduler.schedule(new Runnable() {
            public void run() {
                   checkEverActiveDownloaderIDs2Downloaders();
            }
        }, CHECK_EVER_ACTIVE_DOWNLOADER_IDS_2_DOWNLOADERS_PERIOD_MILLIS, TimeUnit.MILLISECONDS);        
    }
    
    public void setDownloadPrefix(String downloadPrefix) {
        this.downloadPrefix = downloadPrefix;
    }
    
   /**
     * Returns a new {@link RemoveFileDesc} for the file name, relative path,
     * and file length given.
     * 
     * @param fileName simple file name to which we save
     * @param urlString relative path of the URL we use to perform the download
     * @param length length of the file or <code>-1</code> to look up the
     *        length remotely
     * @return a new {@link RemoveFileDesc} for the file name, relative path,
     *         and file length given.
     */
    public RemoteFileDesc createRemoteFileDescriptor(String fileName, String urlString, long length) throws IOException, URISyntaxException, HttpException, InterruptedException {
        //
        // We don't want to pass in a full URL and download it, so have
        // the remote setting LWSSettings.LWS_DOWNLOAD_PREFIX specifying
        // the entire prefix of where we're getting the file from and
        // construct
        // the full URL from that
        //
        String baseDir = "http://" + downloadPrefix;
        if (fileName == null) {
            fileName = fileNameFromURL(urlString);
        }
        String urlStr = baseDir + urlString;
        //
        // This will need NO url encoding, and will contain ?'s and
        // &'s
        // which we want to keep. So for testing, we can only pass
        // in
        // URLs that don't contain spaces
        //
        URL url = new URL(urlStr);
        // this make the size looked up
        RemoteFileDesc rfd = remoteFileDescFactory.createUrlRemoteFileDesc(url, fileName, null, length);
        rfd.setHTTP11(false);
        return rfd;
    }
    
   /**
     * Returns a new Store {@link Downloader} for the given arguments.
     * 
     * @param rfd file descriptor used for the download. This should be created
     *        from {@link #createRemoteFileDescriptor(String, String, long)}.
     * @param saveDir directory to which we save the downloaded file
     * @return a new Store {@link Downloader} for the given arguments.
     * @throws SaveLocationException
     */
    public Downloader createDownloader(RemoteFileDesc rfd, File saveDir)
            throws SaveLocationException {
        //
        // We'll associate the identity hash code of the downloader
        // with this file so that the web page can keep track
        // of this downloader w.r.t this file
        //
        //
        // Make sure we aren't already downloading this
        //
        String fileName = rfd.getFileName();
        final AtomicReference<Downloader> downloader = new AtomicReference<Downloader>();
        synchronized (lwsIntegrationServicesDelegate) {
            final File saveFile = new File(saveDir, fileName);
            lwsIntegrationServicesDelegate.visitDownloads(new Visitor<CoreDownloader>() {
                public boolean visit(CoreDownloader d) {
                    if (d.conflictsSaveFile(saveFile)) {
                        downloader.set(d);
                        return false; // don't continue
                    }
                    return true; // continue
                }
            });
        }
        if (downloader.get() == null) {
            downloader.set(downloadServices.downloadFromStore(rfd, true, saveDir, fileName));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Have downloader " + downloader.toString());
        }
        return downloader.get();
    }
   
    public void initialize() {
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that
        // we can keep track of downloads that were made on the
        // DownloadMediator
        // 
        // INPUT
        // OUTPUT
        // ID of download - a string of form
        // <ID> ' ' <percentage-downloaded> ':' <download-status> [ '|' <ID> ' '
        // <percentage-downloaded> ':' <download-status> ]
        // This ID is the identity hash code
        // ====================================================================================================================================
        lwsManager.registerHandler("GetDownloadProgress", new LWSManagerCommandResponseHandlerWithCallback("GetDownloadProgress") {          
            @Override
            protected String handleRest(Map<String, String> args) {
                return getDownloadProgress();
            }  
        }); 
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        // url - to download
        // file - name of file (optional)
        // id - id of progress bar to update on the way back
        // length - length of the track (optional)
        // OUTPUT
        // URN - of downloader for keeping track of progress
        // -or-
        // timeout - if we timeout
        // ====================================================================================================================================
        lwsManager.registerHandler("Download", new LWSManagerCommandResponseHandlerWithCallback("Download") {

            @Override
            protected String handleRest(Map<String, String> args) {
                //
                // The relative URL
                //
                Tagged<String> urlString = LWSUtil.getArg(args, "url", "downloading");
                if (!urlString.isValid()) return urlString.getValue();
                //
                // The file name. If this isn't given (mainly for testing),
                // we'll let it
                // go, but note that it was missing
                //
                Tagged<String> fileString = LWSUtil.getArg(args, "file", "downloading");
                if (!fileString.isValid()) LOG.info("no file name given to downloader...");
                //
                // The id of the tag we want to associate with the URN we return
                // 
                Tagged<String> idOfTheProgressBarString = LWSUtil.getArg(args, "id", "downloading");
                if (!idOfTheProgressBarString.isValid()) return idOfTheProgressBarString.getValue(); 
                //
                // The length of the URL (optional)
                // 
                Tagged<String> lengthString = LWSUtil.getArg(args, "length", "downloading");
                long length = -1;
                if (lengthString.isValid()) {
                    try {
                        length = Long.parseLong(lengthString.getValue());
                    } catch (NumberFormatException e) { /* ignore */ }
                } 
                try {
                    File saveDir = SharingSettings.getSaveLWSDirectory();
                    RemoteFileDesc rfd = createRemoteFileDescriptor(fileString.getValue(), urlString.getValue(), length); 
                    Downloader theDownloader = createDownloader(rfd, saveDir);
                    long idOfTheDownloader = System.identityHashCode(theDownloader);
                    downloaderIDs2progressBarIDs.put(String.valueOf(idOfTheDownloader), idOfTheProgressBarString.getValue());
                    return idOfTheDownloader + " " + idOfTheProgressBarString.getValue();
                } catch (IOException e) {
                    // invalid url or other causes, fail silently
                } catch (HttpException e) {
                    // invalid url or other causes, fail silently
                } catch (InterruptedException e) {
                    // invalid url or other causes, fail silently
                } catch (URISyntaxException e) {
                    // invalid url or other causes, fail silently
                }

                return "invalid.download";
            }          
        });
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        // id - to pause
        // OUTPUT
        // OK | ID of downloader paused
        // ====================================================================================================================================
        lwsManager.registerHandler("PauseDownload", new LWSManagerCommandResponseForDownloading("PauseDownload", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.pause();
            }          
        });
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        // id - to stop
        // OUTPUT
        // OK | ID of downloader stopped
        // ====================================================================================================================================
        lwsManager.registerHandler("StopDownload", new LWSManagerCommandResponseForDownloading("StopDownload", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.stop(false);
            }              
        }); 
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        // id - to resume
        // OUTPUT
        // OK | ID of downloader resumed
        // ====================================================================================================================================
        lwsManager.registerHandler("ResumeDownload", new LWSManagerCommandResponseForDownloading("ResumeDownload", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.resume();
            }           
        });
        
        
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        // OUTPUT
        // OK | IDs of downloader paused
        // ====================================================================================================================================
        lwsManager.registerHandler("PauseAllDownloads", new LWSManagerCommandResponseForDownloadingAll("PauseAllDownloads", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.pause();
            }          
        });
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        // OUTPUT
        // OK | IDs of downloader stopped
        // ====================================================================================================================================
        lwsManager.registerHandler("StopAllDownloads", new LWSManagerCommandResponseForDownloadingAll("StopAllDownloads", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.stop(false);
            }              
        }); 
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        // OUTPUT
        // OK | IDs of downloader resumed
        // ====================================================================================================================================
        lwsManager.registerHandler("ResumeAllDownloads", new LWSManagerCommandResponseForDownloadingAll("ResumeAllDownloads", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.resume();
            }           
        });        
        // ====================================================================================================================================
        // Add a handler for the LimeWire Store Server so that we can find the
        // info of the client running
        // INPUT
        // --
        // OUTPUT
        // ( <name> '=' <value> '\t' )*
        // ====================================================================================================================================
        lwsManager.registerHandler("GetInfo", new LWSManagerCommandResponseHandlerWithCallback("GetInfo") {
            
            private void add(StringBuffer b, String name, Object value) {
                b.append(name);
                b.append('=');
                b.append(value);
                b.append('\t');
            }

            @Override
            protected String handleRest(Map<String, String> args) {
                StringBuffer res = new StringBuffer();
                add(res, "version"                  ,LimeWireUtils.getLimeWireVersion());
                add(res, "major.version.number"     ,LimeWireUtils.getMajorVersionNumber());
                add(res, "minor.version.number"     ,LimeWireUtils.getMinorVersionNumber());
                add(res, "vendor"                   ,LimeWireUtils.getVendor());                
                add(res, "service.version.number"   ,LimeWireUtils.getServiceVersionNumber());
                add(res, "is.alpha.release"         ,LimeWireUtils.isAlphaRelease());
                add(res, "is.beta.release"          ,LimeWireUtils.isBetaRelease());
                add(res, "is.pro"                   ,LimeWireUtils.isPro());
                add(res, "is.testing.version"       ,LimeWireUtils.isTestingVersion());
                return res.toString();
            }
           
        });        
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("LimeWire Store Integration");
    }  
    
    public void start() {
    }
    
    public void stop() {
    }
    
    /**
     * A class to find a downloader, given an identity hashcode and take an
     * action. Returns the ID of the downloader upon which was taken action.
     */
    private abstract class LWSManagerCommandResponseForDownloading extends LWSManagerCommandResponseHandlerWithCallback {
        
        private final LWSIntegrationServicesDelegate del;
        
        LWSManagerCommandResponseForDownloading(String name, LWSIntegrationServicesDelegate del) {
            super(name);
            this.del = del;
        }
        
        protected abstract void takeAction(Downloader d);
        
        @Override
        protected final String handleRest(Map<String, String> args) {
            //
            // The id of the downloader we want to pause
            // 
            Tagged<String> idOfTheDownloader = LWSUtil.getArg(args, "id", "downloading");
            if (!idOfTheDownloader.isValid()) return idOfTheDownloader.getValue();
            final String id = idOfTheDownloader.getValue();
            //
            // Find the downloader, by System.identityHashCode()
            //
            final AtomicReference<String> res = new AtomicReference<String>("OK");
            del.visitDownloads(new Visitor<CoreDownloader>() {
                public boolean visit(CoreDownloader d) {  
                    String hash = String.valueOf(System.identityHashCode(d));
                    if (hash.equals(id)) {
                        takeAction(d);
                        res.set(hash);
                        return false; // we're done
                    }
                    return true; // continue
                }
            });
            //
            // The response don't matter
            //
            return res.get();
        }         
    }
    
    /**
     * A class to find all the downloaders, given an identity hashcode and take an
     * action.  Returns the list of ids took action upon.
     */
    private abstract class LWSManagerCommandResponseForDownloadingAll extends LWSManagerCommandResponseHandlerWithCallback {
        
        private final LWSIntegrationServicesDelegate del;
        
        LWSManagerCommandResponseForDownloadingAll(String name, LWSIntegrationServicesDelegate del) {
            super(name);
            this.del = del;
        }
        
        protected abstract void takeAction(Downloader d);
        
        @Override
        protected final String handleRest(Map<String, String> args) {
            //
            // Find the downloaders and compare by System.identityHashCode()
            //
            final StringBuffer res = new StringBuffer();
            //
            // Use another list to avoid concurrent modification errors
            //
            final List<Downloader> downloadersToAffect = new ArrayList<Downloader>();
            del.visitDownloads(new Visitor<CoreDownloader>() {
                public boolean visit(CoreDownloader d) {
                    String hash = String.valueOf(System.identityHashCode(d));
                    for (String id : downloaderIDs2progressBarIDs.keySet()) {
                        if (hash.equals(id)) {
                            downloadersToAffect.add(d);
                            if (res.length() > 0) {
                                res.append(" ");
                            }
                            res.append(id);
                            break;
                        }
                    }
                    return true; // continue
                }          
            });
            for (int i=0; i<downloadersToAffect.size(); i++) {
                takeAction(downloadersToAffect.get(i));
            }
            return res.toString();
        }
    }     
    
    /**
     * Returns a printable version of a {@link DownloadStatus}.
     * 
     * @param s status in question
     * @return a printable version of a {@link DownloadStatus}.
     */
    private static String downloadStatusToString(DownloadStatus s) {
        if (s == DownloadStatus.INITIALIZING) {
            return "Initializing";
        }
        if (s == DownloadStatus.QUEUED) {
            return "Queued";
        }
        if (s == DownloadStatus.CONNECTING) {
            return "Connecting";
        }
        if (s == DownloadStatus.DOWNLOADING) {
            return "Downloading";
        }
        if (s == DownloadStatus.BUSY) {
            return "Busy";
        }
        if (s == DownloadStatus.COMPLETE) {
            return "Complete";
        }
        if (s == DownloadStatus.ABORTED) {
            return "Aborted";
        }
        if (s == DownloadStatus.GAVE_UP) {
            return "Gave up";
        }
        if (s == DownloadStatus.DISK_PROBLEM) {
            return "Disk problem";
        }
        if (s == DownloadStatus.WAITING_FOR_GNET_RESULTS) {
            return "Waiting for gnet results";
        }
        if (s == DownloadStatus.CORRUPT_FILE) {
            return "Corrupt_FILE";
        }
        if (s == DownloadStatus.REMOTE_QUEUED) {
            return "Remote_QUEUED";
        }
        if (s == DownloadStatus.HASHING) {
            return "Hashing";
        }
        if (s == DownloadStatus.SAVING) {
            return "Saving";
        }
        if (s == DownloadStatus.WAITING_FOR_USER) {
            return "Waiting for user";
        }
        if (s == DownloadStatus.WAITING_FOR_CONNECTIONS) {
            return "Waiting for connections";
        }
        if (s == DownloadStatus.ITERATIVE_GUESSING) {
            return "Iterative guessing";
        }
        if (s == DownloadStatus.QUERYING_DHT) {
            return "Querying DHT";
        }
        if (s == DownloadStatus.IDENTIFY_CORRUPTION) {
            return "Identify corruption";
        }
        if (s == DownloadStatus.RECOVERY_FAILED) {
            return "Recovery failed";
        }
        if (s == DownloadStatus.PAUSED) {
            return "Paused";
        }
        if (s == DownloadStatus.INVALID) {
            return "Invalid";
        }
        if (s == DownloadStatus.RESUMING) {
            return "Resuming";
        }
        if (s == DownloadStatus.FETCHING) {
            return "Fetching";
        }
        return null;
    }
    
    private String fileNameFromURL(String urlString) {
        int ilast = urlString.lastIndexOf("/");
        if (ilast == -1) {
            ilast = urlString.lastIndexOf("\\");                    
        }
        return urlString.substring(ilast+1);
    }      
    
    private synchronized String getDownloadProgress() {
        //
        // Record the last time this was called, so that we know when to not call this from the periodic checker
        //
        lastTimeWeCalledGetDownloadProgress = System.currentTimeMillis();
        //
        // Return a string mapping urns to download percentages
        //
        final StringBuffer res = new StringBuffer();
        //
        // Remember the downloader that are actually active, so we see
        // if there are any that once existed
        // but are completed and take the correct action. See the
        // comment for 'everActiveDownloaderIDs'
        // for why we have to do this
        //
        final Set<String> activeDownloaderIDS = new HashSet<String>();
        lwsIntegrationServicesDelegate.visitDownloads(new Visitor<CoreDownloader>() {
            public boolean visit(CoreDownloader d) {
                if (d == null) return true;
                if (!(d instanceof StoreDownloader)) return true;
                String id = String.valueOf(System.identityHashCode(d));
                if (!everActiveDownloaderIDs2Downloaders.containsKey(id)) {
                    everActiveDownloaderIDs2Downloaders.put(id, new HeavyWeightDownloaderInterface(d));
                }
                activeDownloaderIDS.add(id);
                recordProgress(d, res, id);
                return true;
            }
        });
        //
        // Now check whether the list of ever-active downloaders
        // contains a downloader
        // not in the current list
        //
        final Collection<String> idsToRemove = new ArrayList<String>();
        for (String downloaderID : everActiveDownloaderIDs2Downloaders.keySet()) {
            DownloaderInterface d = everActiveDownloaderIDs2Downloaders.get(downloaderID);
            if (!activeDownloaderIDS.contains(downloaderID)) {
                //
                // We must have removed one of the previous from the
                // list, so record it
                // once then remove it from this list so we don't put it
                // on again -- because
                // that isn't neeed
                //
                recordProgress(d, res, downloaderID);
                idsToRemove.add(downloaderID);
            }
        }
        for (String idToRemove: idsToRemove) {
            everActiveDownloaderIDs2Downloaders.remove(idToRemove);
            downloaderIDs2progressBarIDs.remove(idToRemove);
        }
        return res.toString();         
    }
    
    private void recordProgress(DownloaderInterface d, StringBuffer res, String id) {
        recordProgress(res, d.getAmountRead(), d.getContentLength(), d.getState(), id);
    }
    
    private void recordProgress(CoreDownloader d, StringBuffer res, String id) {
        recordProgress(res, d.getAmountRead(), d.getContentLength(), d.getState(), id);
    }
    
    private void recordProgress(StringBuffer res, long read, long total, DownloadStatus state, String id) {
        String ratio = String.valueOf((float)read / (float)total);
        String status = downloadStatusToString(state);
        String progressBarID = downloaderIDs2progressBarIDs.get(id);
        res.append(id);
        res.append(" ");
        res.append(progressBarID);
        res.append(" ");
        res.append(ratio);
        res.append(":");
        res.append(status);
        res.append("|");                
    }    
    
    /**
     * Checks that {@link #everActiveDownloaderIDs2Downloaders only contains downloaders
     * that are still active.  If we encounter a downloader that has completed replace
     * its instance with a stub.  Package protected for testing.
     */
    synchronized void checkEverActiveDownloaderIDs2Downloaders() {
        final Collection<String> idsToChange = new ArrayList<String>();
        for (String downloaderID : everActiveDownloaderIDs2Downloaders.keySet()) {
            DownloaderInterface d = everActiveDownloaderIDs2Downloaders.get(downloaderID);
            if (d.isCompleted() && d.isHeavyWeight()) {
                idsToChange.add(downloaderID);
            }
        }
        for (String id : idsToChange) {
            DownloaderInterface d = everActiveDownloaderIDs2Downloaders.get(id);
            everActiveDownloaderIDs2Downloaders.remove(id);
            everActiveDownloaderIDs2Downloaders.put(id, new LightWeightDownloaderInterface(d));
        }
    }


}
