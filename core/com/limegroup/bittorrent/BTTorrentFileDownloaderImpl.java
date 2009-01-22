package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

public class BTTorrentFileDownloaderImpl extends AbstractCoreDownloader implements
        BTTorrentFileDownloader, EventListener<DownloadStatusEvent> {
    
    private static Log LOG = LogFactory.getLog(BTTorrentFileDownloaderImpl.class);

    private static final int TIMEOUT = 5000;

    private final DownloadManager downloadManager;

    private final HttpExecutor httpExecutor;

    private final TorrentManager torrentManager;

    private final BTMetaInfoFactory btMetaInfoFactory;

    private final EventListenerList<DownloadStatusEvent> eventListenerList;

    private DownloadStatus downloadStatus = DownloadStatus.QUEUED;

    private BTMetaInfo btMetaInfo = null;

    /**
     * Something to shutdown if the user cancels the fetching
     */
    private volatile Shutdownable aborter;

    private URI torrentURI;

    @Inject
    public BTTorrentFileDownloaderImpl(DownloadManager downloadManager,
            @Assisted SaveLocationManager saveLocationManager, HttpExecutor httpExecutor,
            TorrentManager torrentManager, BTMetaInfoFactory btMetaInfoFactory,
            ActivityCallback activityCallback) {
        super(saveLocationManager);
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.httpExecutor = Objects.nonNull(httpExecutor, "httpExecutor");
        this.torrentManager = Objects.nonNull(torrentManager, "torrentMaanger");
        this.btMetaInfoFactory = Objects.nonNull(btMetaInfoFactory, "btMetaInfoFactory");
        this.eventListenerList = new EventListenerList<DownloadStatusEvent>();
        addListener(this);
    }

    public void fetch() {
        final HttpGet get = new HttpGet(torrentURI);
        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpClientParams.setRedirecting(params, true);
        aborter = httpExecutor.execute(get, params, this);
    }

    public boolean requestComplete(HttpUriRequest method, HttpResponse response) {
        aborter = null;
        if (downloadStatus == DownloadStatus.ABORTED)
            return false;
        BTMetaInfo m = null;
        byte[] body = null;
        try {
            if (response.getEntity() != null) {
                body = IOUtils.readFully(response.getEntity().getContent());
            }
            if (response.getStatusLine().getStatusCode() < 200
                    || response.getStatusLine().getStatusCode() >= 300)
                throw new IOException("bad status code, downloading .torrent file "
                        + response.getStatusLine().getStatusCode());
            if (body == null || body.length == 0)
                throw new IOException("invalid response");

            m = btMetaInfoFactory.createBTMetaInfoFromBytes(body);           
        } catch (IOException iox) {
            downloadStatus = DownloadStatus.INVALID;
            if(LOG.isErrorEnabled()) {
                LOG.error("Error downloading torrent: " + torrentURI, iox);
            }
        } finally {
            httpExecutor.releaseResources(response);
        }

        if (m == null) {
            downloadStatus = DownloadStatus.INVALID;
            return false;
        }

        torrentManager.shareTorrentFile(m, body);
        downloadStatus = DownloadStatus.COMPLETE;
        this.btMetaInfo = m;
        eventListenerList.broadcast(new DownloadStatusEvent(this, DownloadStatus.COMPLETE));
        return false;
    }

    public boolean requestFailed(HttpUriRequest method, HttpResponse response, IOException exc) {
        downloadStatus = DownloadStatus.INVALID;
        downloadManager.remove(this, true);
        return false;
    }

    public void discardCorruptDownload(boolean delete) {
    }

    public long getAmountLost() {
        return 0;
    }

    public int getAmountPending() {
        return 0;
    }

    public long getAmountRead() {
        return 0;
    }

    public long getAmountVerified() {
        return 0;
    }

    public RemoteFileDesc getBrowseEnabledHost() {
        return null;
    }

    public int getBusyHostCount() {
        return 0;
    }

    public Endpoint getChatEnabledHost() {
        return null;
    }

    public int getChunkSize() {
        return 1;
    }

    public long getContentLength() {
        return 0;
    }

    public File getDownloadFragment() {
        return null;
    }

    public File getFile() {
        return getSaveFile();
    }

    public int getNumHosts() {
        return 0;
    }
    
    @Override
    public List<Address> getSourcesAsAddresses() {
        return Collections.emptyList();
    }

    public int getNumberOfAlternateLocations() {
        return 0;
    }

    public int getNumberOfInvalidAlternateLocations() {
        return 0;
    }

    public int getPossibleHostCount() {
        return 0;
    }

    public int getQueuePosition() {
        return 0;
    }

    public int getQueuedHostCount() {
        return 0;
    }

    public int getRemainingStateTime() {
        return 0;
    }

    public URN getSha1Urn() {
        return null;
    }

    public File getSaveFile() {
        // TODO revamp

        // try to get a meaningful name out of the URI
        String uri = torrentURI.toString();
        String name = null;
        if (uri.endsWith(".torrent")) {
            int slash = uri.lastIndexOf("/");
            if (slash != -1)
                name = uri.substring(slash);
        }

        // can't figure it out? show the uri
        if (name == null)
            name = uri;

        return new File(uri);
    }

    @Override
    public void setSaveFile(File saveDirectory, String fileName, boolean overwrite)
            throws SaveLocationException {
    }

    public DownloadStatus getState() {
        return downloadStatus;
    }

    public String getVendor() {
        return null;
    }

    public boolean hasBrowseEnabledHost() {
        return false;
    }

    public boolean hasChatEnabledHost() {
        return false;
    }

    public boolean isCompleted() {
        return downloadStatus == DownloadStatus.COMPLETE;
    }

    public boolean isInactive() {
        return false;
    }

    public boolean isLaunchable() {
        return false;
    }

    public boolean isPausable() {
        return false;
    }

    public boolean isPaused() {
        return false;
    }

    public boolean isRelocatable() {
        return false;
    }

    public boolean isResumable() {
        return false;
    }

    public void pause() {

    }

    public boolean resume() {
        return false;
    }

    public void stop(boolean deleteFile) {
        finish();
        downloadManager.remove(this, true);
    }

    public float getAverageBandwidth() {
        return 0;
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        return 0;
    }

    public void measureBandwidth() {
    }

    public int getTriedHostCount() {
        return -1;
    }

    public String getCustomIconDescriptor() {
        return null;
    }

    public void finish() {
        downloadStatus = DownloadStatus.ABORTED;
        if (aborter != null) {
            aborter.shutdown();
            aborter = null;
        }    
    }

    public GUID getQueryGUID() {
        return null;
    }

    public void handleInactivity() {

    }

    public void initialize() {

    }

    public boolean isAlive() {
        return false;
    }

    public boolean isQueuable() {
        return false;
    }

    public void setInactivePriority(int priority) {

    }

    public boolean shouldBeRemoved() {
        return isCompleted() || downloadStatus == DownloadStatus.ABORTED || downloadStatus == DownloadStatus.INVALID;
    }

    public boolean shouldBeRestarted() {
        return downloadStatus == DownloadStatus.QUEUED;
    }

    public void startDownload() {
        fetch();
    }

    public DownloaderType getDownloadType() {
        return DownloaderType.TORRENTFETCHER;
    }

    public void addListener(EventListener<DownloadStatusEvent> listener) {
        eventListenerList.addListener(listener);
    }

    public boolean removeListener(EventListener<DownloadStatusEvent> listener) {
        return eventListenerList.removeListener(listener);
    }

    @Override
    public boolean allowRequest(HttpUriRequest request) {
        return true;
    }

    @Override
    public boolean isMementoSupported() {
        return false;
    }

    @Override
    public void initDownloadInformation(URI torrentURI, boolean overwrite) {
        this.torrentURI = torrentURI;
    }

    @Override
    public BTMetaInfo getBtMetaInfo() {
        return btMetaInfo;
    }

    @Override
    protected DownloadMemento createMemento() {
        return null;
    }

    @Override
    protected File getDefaultSaveFile() {
        return getSaveFile();
    }

    @Override
    public boolean conflicts(URN urn, long fileSize, File... files) {
        return false;
    }

    @Override
    public boolean conflictsWithIncompleteFile(File incomplete) {
        return false;
    }

    @Override
    public void handleEvent(DownloadStatusEvent event) {
        if (DownloadStatus.COMPLETE == event.getType()) {
            downloadManager.remove(this, true);
        }
    }
}
