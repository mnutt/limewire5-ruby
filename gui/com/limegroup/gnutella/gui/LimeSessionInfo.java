package com.limegroup.gnutella.gui;

import org.limewire.net.SocketsManager;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.bugs.SessionInfo;
import com.limegroup.gnutella.downloader.DiskController;
import com.limegroup.gnutella.library.CreationTimeCache;

/** An implementation of SessionInfo that gets it's statistics from various LimeWire components. */
@Singleton
public class LimeSessionInfo implements SessionInfo {
    
    private final NIODispatcher dispatcher;
    private final DownloadManager downloadManager;
    private final Statistics statistics;
    private final ConnectionManager connectionManager;
    private final ContentManager contentManager;
    private final CreationTimeCache creationTimeCache;
    private final DiskController diskController;
    private final SocketsManager socketsManager;
    private final ByteBufferCache byteBufferCache;
    private final UDPService udpService;
    private final Acceptor acceptor;

    @Inject
    public LimeSessionInfo(NIODispatcher dispatcher,
                        DownloadManager downloadManager,
                        Statistics statistics,
                        ConnectionManager connectionManager,
                        ContentManager contentManager,
                        CreationTimeCache creationTimeCache,
                        DiskController diskController,
                        SocketsManager socketsManager,
                        ByteBufferCache byteBufferCache,
                        UDPService udpService,
                        Acceptor acceptor) {
        this.dispatcher = dispatcher;
        this.downloadManager = downloadManager;
        this.statistics = statistics;
        this.connectionManager = connectionManager;
        this.contentManager = contentManager;
        this.creationTimeCache = creationTimeCache;
        this.diskController = diskController;
        this.socketsManager = socketsManager;
        this.byteBufferCache = byteBufferCache;
        this.udpService = udpService;
        this.acceptor = acceptor;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumberOfPendingTimeouts()
     */
    public int getNumberOfPendingTimeouts() {
        return dispatcher.getNumPendingTimeouts();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumWaitingDownloads()
     */
    public int getNumWaitingDownloads() {
        return downloadManager.getNumWaitingDownloads();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumIndividualDownloaders()
     */
    public int getNumIndividualDownloaders() {
        return downloadManager.getNumIndividualDownloaders();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getCurrentUptime()
     */
    public long getCurrentUptime() {
        return statistics.getUptime();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumUltrapeerToLeafConnections()
     */
    public int getNumUltrapeerToLeafConnections() {
        return connectionManager.getNumInitializedClientConnections();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumLeafToUltrapeerConnections()
     */
    public int getNumLeafToUltrapeerConnections() {
        return connectionManager.getNumClientSupernodeConnections();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumUltrapeerToUltrapeerConnections()
     */
    public int getNumUltrapeerToUltrapeerConnections() {
        return connectionManager.getNumUltrapeerConnections();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumOldConnections()
     */
    public int getNumOldConnections() {
        return connectionManager.getNumOldConnections();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getContentResponsesSize()
     */
    public long getContentResponsesSize() {
        return contentManager.getCacheSize();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getCreationCacheSize()
     */
    public long getCreationCacheSize() {
        return creationTimeCache.getSize();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getVerifyingFileByteCacheSize()
     */
    public long getDiskControllerByteCacheSize() {
        return diskController.getSizeOfByteCache();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getVerifyingFileVerifyingCacheSize()
     */
    public long getDiskControllerVerifyingCacheSize() {
        return diskController.getSizeOfVerifyingCache();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getVerifyingFileQueueSize()
     */
    public int getDiskControllerQueueSize() {
        return diskController.getNumPendingItems();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getByteBufferCacheSize()
     */
    public long getByteBufferCacheSize() {
        return byteBufferCache.getHeapCacheSize();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumberOfWaitingSockets()
     */
    public int getNumberOfWaitingSockets() {
        return socketsManager.getNumWaitingSockets();
    }

    public boolean isGUESSCapable() {
        return udpService.isGUESSCapable();
    }

    public boolean canReceiveSolicited() {
        return udpService.canReceiveSolicited();
    }

    public boolean acceptedIncomingConnection() {
        return acceptor.acceptedIncoming();
    }
    
    public int getPort() {
        return acceptor.getPort(true);
    }

}
