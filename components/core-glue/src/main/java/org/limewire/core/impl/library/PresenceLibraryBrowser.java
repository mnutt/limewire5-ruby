package org.limewire.core.impl.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.GuardedBy;

import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.core.impl.xmpp.XMPPRemoteFileDescDeserializer;
import org.limewire.io.Address;
import org.limewire.io.IpPortSet;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.XMPPAddress;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class PresenceLibraryBrowser implements EventListener<LibraryChangedEvent> {
   
    private static final Log LOG = LogFactory.getLog(PresenceLibraryBrowser.class);

    private final BrowseFactory browseFactory;
    private final RemoteLibraryManagerImpl remoteLibraryManager;

    private final SocketsManager socketsManager;
    
    /**
     * Keeps track of libraries that could not be browsed yet, because the local peer didn't have
     * enough connection capabilities.
     */
    private final Set<PresenceLibrary> librariesToBrowse = Collections.synchronizedSet(new HashSet<PresenceLibrary>());

    private final XMPPRemoteFileDescDeserializer remoteFileDescDeserializer;
    
    /**
     * Is incremented when a new connectivity change event is received, should
     * only be modified holding the lock to {@link #librariesToBrowse}.
     * 
     * When address resolution fails, the revision that was used when resolution is started
     * can be compared to the latest revision to see if the client's connectivity
     * has changed in the meantime and resolution should be retried.
     * 
     * Still volatile, so it can be read without a lock.
     */
    @GuardedBy("librariesToBrowse")
    private volatile int latestConnectivityEventRevision = 0;

    @Inject
    public PresenceLibraryBrowser(BrowseFactory browseFactory, RemoteLibraryManagerImpl remoteLibraryManager,
            SocketsManager socketsManager, XMPPRemoteFileDescDeserializer remoteFileDescDeserializer) {
        this.browseFactory = browseFactory;
        this.remoteLibraryManager = remoteLibraryManager;
        this.socketsManager = socketsManager;
        this.remoteFileDescDeserializer = remoteFileDescDeserializer;
        socketsManager.addListener(new ConnectivityChangeListener());
    }

    @Inject void register(ListenerSupport<LibraryChangedEvent> listenerSupport) {
        listenerSupport.addListener(this);
    }

    public void handleEvent(LibraryChangedEvent event) {
        remoteLibraryManager.removePresenceLibrary(event.getSource());
        remoteLibraryManager.addPresenceLibrary(event.getSource());
    }

    @Inject
    public void addListener(RemoteLibraryManager remoteLibraryManager) {
        remoteLibraryManager.getFriendLibraryList().addListEventListener(new ListEventListener<FriendLibrary>() {
            @Override
            public void listChanged(ListEvent<FriendLibrary> listChanges) {
                while(listChanges.next()) {
                    if(listChanges.getType() == ListEvent.INSERT) {
                        final FriendLibrary friendLibrary = listChanges.getSourceList().get(listChanges.getIndex());
                        
                        new AbstractListEventListener<PresenceLibrary>() {
                            @Override
                            protected void itemAdded(PresenceLibrary presenceLibrary, int idx, EventList<PresenceLibrary> source) {
                                tryToResolveAndBrowse(presenceLibrary, latestConnectivityEventRevision);
                            }

                            @Override
                            protected void itemRemoved(PresenceLibrary item, int idx, EventList<PresenceLibrary> source) {
                                librariesToBrowse.remove(item);
                            }

                            @Override
                            protected void itemUpdated(PresenceLibrary item, PresenceLibrary prior, int idx, EventList<PresenceLibrary> source) {
                            }
                        }.install(friendLibrary.getPresenceLibraryList());
                    }
                }
            }
        });
    }

    private void browse(final PresenceLibrary presenceLibrary) {
        presenceLibrary.setState(LibraryState.LOADING);
        final FriendPresence friendPresence = presenceLibrary.getPresence();
        LOG.debugf("browsing {0} ...", friendPresence.getPresenceId());
        final Browse browse = browseFactory.createBrowse(friendPresence);
        AddressFeature addressFeature = ((AddressFeature)friendPresence.getFeature(AddressFeature.ID));
        if(addressFeature == null) {
            // happens during sign-off
            return;    
        }
        final XMPPAddress address;
        if(!friendPresence.getFriend().isAnonymous()) {
            address = (XMPPAddress) addressFeature.getFeature();    
        } else {
            address = null;
        }
        browse.start(new BrowseListener() {
            public void handleBrowseResult(SearchResult searchResult) {
                LOG.debugf("browse result: {0}, {1}", searchResult.getUrn(), searchResult.getSize());
                RemoteFileDescAdapter remoteFileDescAdapter = (RemoteFileDescAdapter)searchResult;
                if(!friendPresence.getFriend().isAnonymous()) {
                    // TODO: This should be removed & the address passed here should be corrected.
                    // copy construct to add injectables and change address to xmpp address                    
                    remoteFileDescAdapter = new RemoteFileDescAdapter(remoteFileDescDeserializer.createClone(remoteFileDescAdapter.getRfd(), address), new IpPortSet(remoteFileDescAdapter.getAlts()), friendPresence);
                }
                RemoteFileItem file = new CoreRemoteFileItem(remoteFileDescAdapter);
                presenceLibrary.addFile(file);
            }
            @Override
            public void browseFinished(boolean success) {
                if(success) {
                    presenceLibrary.setState(LibraryState.LOADED);
                } else {
                    presenceLibrary.setState(LibraryState.FAILED_TO_LOAD);
                    LOG.debugf("browse failed: {0}", presenceLibrary);
                }
            }
        });
    }
    
    /**
     * Tries to resolve the address of <code>presenceLibrary<code> and browse it
     * after successful resolution and/or if it can connect to the address. Otherwise,
     * it handle the failure by calling {@link #handleFailedResolution(PresenceLibrary, int)}.
     * 
     * @param presenceLibrary the presence library whose address should be resolved
     * and browsed
     * @param startConnectivityRevision the revisions of {@link #latestConnectivityEventRevision}
     * when this method is called
     */
    private void tryToResolveAndBrowse(final PresenceLibrary presenceLibrary, final int startConnectivityRevision) {
        presenceLibrary.setState(LibraryState.LOADING);
        final FriendPresence friendPresence = presenceLibrary.getPresence();
        AddressFeature addressFeature = (AddressFeature)friendPresence.getFeature(AddressFeature.ID);
        if (addressFeature == null) {
            LOG.debug("no address feature");
            handleFailedResolution(presenceLibrary, startConnectivityRevision);
            return;
        }
        Address address = addressFeature.getFeature();
        if (socketsManager.canResolve(address)) {
            socketsManager.resolve(address, new AddressResolutionObserver() {
                @Override
                public void resolved(Address address) {
                    if (socketsManager.canConnect(address)) {
                        LOG.debugf("resolved {0} for {1} and can connect", address, friendPresence);
                        browse(presenceLibrary);
                    } else {
                        LOG.debugf("resolved {0} for {1} and cannot connect", address, friendPresence);
                        handleFailedResolution(presenceLibrary, startConnectivityRevision);
                    }
                }
                @Override
                public void handleIOException(IOException iox) {
                    LOG.debug("resolve error", iox);
                    handleFailedResolution(presenceLibrary, startConnectivityRevision);
                }
                @Override
                public void shutdown() {
                }
            });
        } else if (socketsManager.canConnect(address)) {
            browse(presenceLibrary);
        } else {
            handleFailedResolution(presenceLibrary, startConnectivityRevision);
        }
    }
 
    /**
     * Called when resolution failed.
     * 
     * If {@link #latestConnectivityEventRevision} is greater then <code>startRevision</code>,
     * a new attempt at resolving the presence address is started, otherwise <code>presenceLibrary</code>
     * is queued up in libraries to browse.
     * 
     * @param presenceLibrary the library that could not be browsed
     * @param startRevision the revision under which the address resolution attempt
     * was started
     */
    private void handleFailedResolution(PresenceLibrary presenceLibrary, int startRevision) { 
        LOG.debugf("failed resolution for:{0} revision:{1}", presenceLibrary.getPresence().getPresenceId(), startRevision);
        presenceLibrary.setState(LibraryState.FAILED_TO_LOAD);
        
        boolean retry;
        synchronized (librariesToBrowse) {
            retry = latestConnectivityEventRevision > startRevision;
            if (!retry) {
                assert(librariesToBrowse.add(presenceLibrary));
            } else {
                // copy value under lock
                startRevision = latestConnectivityEventRevision;
            }
        }
        if (retry) {
            tryToResolveAndBrowse(presenceLibrary, startRevision);
        }
    }
    
    /**
     * Is notified of better connection capabilities and iterates over the list of unbrowsable
     * presence libraries to see if they can be browsed now.
     */
    private class ConnectivityChangeListener implements EventListener<ConnectivityChangeEvent> {

        /**
         * Increments the {@link PresenceLibraryBrowser#latestConnectivityEventRevision}
         * copies and empties {@link PresenceLibraryBrowser#librariesToBrowse} and
         * tries calls {@link PresenceLibraryBrowser#tryToResolveAndBrowse(PresenceLibrary, int)}
         * for each with the new revision.
         */
        @Override
        public void handleEvent(ConnectivityChangeEvent event) {
            LOG.debug("connectivity change");
            List<PresenceLibrary> copy;
            int currentRevision;
            synchronized (librariesToBrowse) {
                currentRevision = ++latestConnectivityEventRevision;
                copy = new ArrayList<PresenceLibrary>(librariesToBrowse);
                librariesToBrowse.clear();
            }
            for (PresenceLibrary library : copy) {
                tryToResolveAndBrowse(library, currentRevision);
            }
        }
    }
}