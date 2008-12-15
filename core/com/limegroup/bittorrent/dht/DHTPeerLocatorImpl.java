package com.limegroup.bittorrent.dht;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.InvalidDataException;
import org.limewire.lifecycle.Service;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.FindValueResult;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentEvent;
import com.limegroup.bittorrent.TorrentEventListener;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * Given an instance of torrent, locates peers in the Mojito DHT seeding that 
 * file. Also re-attempts to locate peers if DHT was not available.
 */
@Singleton
public class DHTPeerLocatorImpl implements DHTPeerLocator, Service {

    private static final Log LOG = LogFactory.getLog(DHTPeerLocator.class);

    private final Provider<DHTManager> dhtManager;

    private final Provider<TorrentManager> torrentManager;

    /**
     * List of torrents for which a peer is trying to locate a seeder in DHT but
     * was unsuccessful since DHT is unavailable.
     */
    private final List<URN> torrentsWaitingForDHTList = new ArrayList<URN>();

    @Inject
    DHTPeerLocatorImpl(Provider<DHTManager> dhtManager, Provider<TorrentManager> torrentManager) {
        this.dhtManager = dhtManager;
        this.torrentManager = torrentManager;
    }

    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    /**
     * Adds the required listeners to TorrentManager and DHTManager. This method
     * should only be called once.
     */
    public void initialize() {
        // listens for the TorrentEvent TRACKER_FAILED to start locating a
        // peer
        torrentManager.get().addEventListener(new LocatorTorrentEventListener());
        // listens for the DHTEvent CONNECTED to re-attempt locating a peer
        dhtManager.get().addEventListener(new DHTEventListenerForLocator());
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("DHT Peer Locator");
    }
     
    public void start() {
    }  
     public void stop() {
    }

    /**
     * Searches in the MojitoDHT for peers sharing the given torrent. Torrent's
     * infoHash is used as the key.
     * 
     * @param urn SHA1 hash of the torrent file.
     */
    public void locatePeer(URN urn) {
        LOG.debug("In locatePeer");
        // checking if the torrent is active, getTorrentForURN returns null if
        // there is no active managedTorrent for the given urn.
        if (torrentManager.get().getTorrentForURN(urn) != null) {
            // holding a lock on torrentsWaitingForDHTList here so that we do
            // not perform search for a same torrent twice.
            synchronized (torrentsWaitingForDHTList) {
                if (!torrentsWaitingForDHTList.contains(urn)) {
                    LOG.debug("Passed Initial checks");
                    // creates a KUID from torrent's metadata
                    KUID key = KUIDUtils.toKUID(urn);
                    // creates an entity key from the KUID and DHTValueType
                    EntityKey eKey = EntityKey.createEntityKey(key,
                            DHTPeerLocatorUtils.BT_PEER_TRIPLE);
                    // retrieves the future
                    final DHTFuture<FindValueResult> future = dhtManager.get().get(eKey);
                    if (future == null) {
                        torrentsWaitingForDHTList.add(urn);
                    } else {
                        // handle the result retrieved in the future
                        future.addDHTFutureListener(new LocatePeerResultHandler(urn));
                        LOG.debug("Future Listener added");
                    }
                }
            }
        }
    }

    /**
     * Gets the torrent's location and adds it as an endPoint.
     * 
     * @param managedTorrent the routed torrent
     * @param entity <code>DHTValueEntity</code> containing the 
     * <code>DHTValue</code> that is sought.
     */
    private void dispatch(ManagedTorrent managedTorrent, DHTValueEntity entity) {
        DHTValue value = entity.getValue();
        LOG.debug("dispatch entered");
        try {
            final TorrentLocation torLoc = DHTPeerLocatorUtils.decode(value.getValue());
            managedTorrent.addEndpoint(torLoc);
            if (LOG.isDebugEnabled())
                LOG.debug("IP:PORT of found: " + torLoc.getAddress() + ":" + torLoc.getPort());
        } catch (IllegalArgumentException iae) {
            // if the payload passed in to decode was incorrect
            LOG.error("Invalid payload");
        } catch (InvalidDataException ide) {
            // if the network information stored was incorrect
            LOG.error("Invalid network information");
        }
    }

    /**
     * Listens for a <code>TRACKER_FAILED</code> event and launches search for 
     * alternate location.
     */
    private class LocatorTorrentEventListener implements TorrentEventListener {
        public void handleTorrentEvent(TorrentEvent evt) {
            if (evt.getType() == TorrentEvent.Type.TRACKER_FAILED) {
                LOG.debug("TRACKER_FAILED_EVENT");
                locatePeer(evt.getTorrent().getMetaInfo().getURN());
            }
        }
    }

    /**
     * Performs future attempts at locating a peer based on <code>DHTEvent</code>.
     */
    private class DHTEventListenerForLocator implements DHTEventListener {
        /**
         * This method is invoked when a DHTEvent occurs and if DHT is connected
         * then it tries to search for a peer.
         */
        public void handleDHTEvent(DHTEvent evt) {
            List<URN> torrentsWaitingForDHTListCopy;
            if (evt.getType() == DHTEvent.Type.CONNECTED) {
                LOG.debug("DHT available");
                // copying the array then iterating since reading is cheaper
                // than removing.
                synchronized (torrentsWaitingForDHTList) {
                    torrentsWaitingForDHTListCopy = torrentsWaitingForDHTList;
                    torrentsWaitingForDHTList.clear();
                }
                for (int i = 0; i < torrentsWaitingForDHTListCopy.size(); i++) {
                    locatePeer(torrentsWaitingForDHTListCopy.get(i));
                }
            }
        }
    }

    /**
     * Listens to see a result was successfully retrieved.
     */
    private class LocatePeerResultHandler extends DHTFutureAdapter<FindValueResult> {
        private final URN urn;

        public LocatePeerResultHandler(URN urn) {
            this.urn = urn;
        }

        @Override
        public void handleFutureSuccess(FindValueResult result) {
            LOG.debug("handle result");
            ManagedTorrent managedTorrent = torrentManager.get().getTorrentForURN(urn);
            if (result.isSuccess() && managedTorrent != null) {
                LOG.debug("successful result");
                for (DHTValueEntity entity : result.getEntities()) {
                    dispatch(managedTorrent, entity);
                }

                for (EntityKey entityKey : result.getEntityKeys()) {
                    if (!entityKey.getDHTValueType().equals(DHTPeerLocatorUtils.BT_PEER_TRIPLE)) {
                        continue;
                    }
                    DHTFuture<FindValueResult> future = dhtManager.get().get(entityKey);
                    if (future != null) {
                        try {
                            FindValueResult resultFromKey = future.get();
                            if (resultFromKey.isSuccess()) {
                                for (DHTValueEntity entity : resultFromKey.getEntities()) {
                                    dispatch(managedTorrent, entity);
                                }
                            }
                        } catch (ExecutionException e) {
                            LOG.error("ExecutionException", e);
                        } catch (InterruptedException e) {
                            LOG.error("InterruptedException", e);
                        }
                    }
                }
            }
        }
    }
}