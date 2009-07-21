package org.limewire.core.impl.library;

import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.LibraryChanged;
import org.limewire.friend.api.LibraryChangedEvent;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.util.concurrent.Lock;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

/**
 * Tests the methods and classes contained in PresenceLibraryBrowse with various levels of
 *  internal integration.
 */
public class PresenceLibraryBrowserTest extends BaseTestCase {
    
    public PresenceLibraryBrowserTest(String name) {
        super(name);
    }
    
    /**
     * Goes through the register phase and ensures all listeners are correctly connected.
     */
    @SuppressWarnings("unchecked")
    public void testListeners() {
        Mockery context = new Mockery();
        
        final ListenerSupport<LibraryChangedEvent> listenerSupport = context.mock(ListenerSupport.class);
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        
        final FriendPresence presence = context.mock(FriendPresence.class);
        final EventList<FriendLibrary> friendLibraryList = context.mock(EventList.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, remoteLibraryManager, socketsManager, null);
        
        context.checking(new Expectations() {{
            allowing(remoteLibraryManager).getFriendLibraryList();
            will(returnValue(friendLibraryList));
            
            exactly(1).of(listenerSupport).addListener(presenceLibraryBrowser);
            exactly(1).of(socketsManager).addListener(with(any(EventListener.class)));
            exactly(1).of(friendLibraryList).addListEventListener(with(any(ListEventListener.class)));
            exactly(1).of(remoteLibraryManager).getPresenceLibrary(presence);
            exactly(1).of(remoteLibraryManager).addPresenceLibrary(presence);
        }});
        
        presenceLibraryBrowser.register(listenerSupport);
        presenceLibraryBrowser.registerToRemoteLibraryManager();
        presenceLibraryBrowser.registerToSocksManager();
        presenceLibraryBrowser.handleEvent(new LibraryChangedEvent(presence, LibraryChanged.LIBRARY_CHANGED));
        
        context.assertIsSatisfied();
    }

    /**
     * Force fire a connectivity change event and ensure a browse is attempted on a
     *  a queued PresenceLibrary.  This simulates the retry of an earlier failed browse.
     */
    @SuppressWarnings("unchecked")
    public void testRetryBrowse() {
        Mockery context = new Mockery();
        
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, null, socketsManager, null);
        
        final MatchAndCopy<EventListener> socketsListenerCollector = new MatchAndCopy<EventListener>(EventListener.class);
        
        context.checking(new Expectations() {{
            // Assertions
            exactly(1).of(socketsManager).addListener(with(socketsListenerCollector));
            exactly(1).of(presenceLibrary).setState(LibraryState.LOADING);
            
            // Allow the browse to unfold minimally as it must
            allowing(presenceLibrary);
        }});
        
        // Register the required listener and collect it for probing
        presenceLibraryBrowser.registerToSocksManager();
        
        // Fire a connectivity change with no PresenceLibrary instances queued
        socketsListenerCollector.getLastMatch().handleEvent(new ConnectivityChangeEvent());
        
        // Queue a PresenceLibrary for rebrowse
        presenceLibraryBrowser.librariesToBrowse.add(presenceLibrary);
        
        // Fire a connectivity change again
        socketsListenerCollector.getLastMatch().handleEvent(new ConnectivityChangeEvent());
        
        context.assertIsSatisfied();
    }

    
    /**
     * Fires events for presence library changes but ones that should not spawn new browses.
     */
    @SuppressWarnings("unchecked")
    public void testBrowseNotInitiated() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        
        final EventList<FriendLibrary> friendLibraryList = context.mock(EventList.class);
        
        final MatchAndCopy<ListEventListener> friendListenerCollector
            = new MatchAndCopy<ListEventListener>(ListEventListener.class);
        
        final ListEvent<FriendLibrary> listEventBlank = context.mock(ListEvent.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, remoteLibraryManager, null, null);
        
        context.checking(new Expectations() {{
            allowing(remoteLibraryManager).getFriendLibraryList();
            will(returnValue(friendLibraryList));
          
            one(listEventBlank).next();
            will(returnValue(true));
            one(listEventBlank).getType();
            will(returnValue(ListEvent.UPDATE));
            one(listEventBlank).next();
            will(returnValue(true));
            one(listEventBlank).getType();
            will(returnValue(ListEvent.DELETE));
            one(listEventBlank).next();
            will(returnValue(false));
            
            exactly(1).of(friendLibraryList).addListEventListener(with(friendListenerCollector));
        }});
        
        presenceLibraryBrowser.registerToRemoteLibraryManager();
        
        ListEventListener<FriendLibrary> listener = friendListenerCollector.getLastMatch();
        listener.listChanged(listEventBlank);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Fires events that should spawn a normal friend browse.  When the browse is
     *  complete makes sure the listener handles removing failed browses correctly.
     *  
     * <p>Full internal integration test.
     */
    @SuppressWarnings("unchecked")
    public void testBrowseIntegration() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        final BrowseFactory browseFactory = context.mock(BrowseFactory.class);
        
        final MatchAndCopy<ListEventListener> listenerCollector
            = new MatchAndCopy<ListEventListener>(ListEventListener.class);
        final MatchAndCopy<BrowseListener> browseListenerCollector 
            = new MatchAndCopy<BrowseListener>(BrowseListener.class);
        
        final ListEvent<FriendLibrary> listEvent = context.mock(ListEvent.class);
        final EventList<FriendLibrary> friendLibraryEventList = context.mock(EventList.class);
        final FriendLibrary friendLibrary = context.mock(FriendLibrary.class);
        final EventList<PresenceLibrary> presenceLibraryEventList = context.mock(EventList.class);
        
        final ReadWriteLock dummyRWLock = context.mock(ReadWriteLock.class);
        final Lock dummyLock = context.mock(Lock.class);
        
        final ListEvent<PresenceLibrary> addEvent = context.mock(ListEvent.class);       
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        
        final ListEvent<PresenceLibrary> updateAndRemoveEvent = context.mock(ListEvent.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(browseFactory, remoteLibraryManager, socketsManager, null);
        
        context.checking(new Expectations() {{
            allowing(remoteLibraryManager).getFriendLibraryList();
            will(returnValue(friendLibraryEventList));
          
            one(listEvent).next();
            will(returnValue(true));
            one(listEvent).getType();
            will(returnValue(ListEvent.INSERT));
            one(listEvent).next();
            will(returnValue(false));
            
            allowing(listEvent).getSourceList();
            will(returnValue(friendLibraryEventList));
            allowing(listEvent).getIndex();
            will(returnValue(777));
            allowing(friendLibraryEventList).get(777);
            will(returnValue(friendLibrary));
            
            allowing(friendLibrary).getPresenceLibraryList();
            will(returnValue(presenceLibraryEventList));
            allowing(presenceLibraryEventList).getReadWriteLock();
            will(returnValue(dummyRWLock));
            allowing(presenceLibraryEventList).toArray();
            will(returnValue(new Object[0]));
            
            allowing(dummyRWLock).readLock();
            will(returnValue(dummyLock));
            allowing(dummyLock).lock();
            allowing(dummyLock).unlock();
            
            one(addEvent).next();
            will(returnValue(true));
            one(addEvent).getType();
            will(returnValue(ListEvent.INSERT));
            one(addEvent).next();
            will(returnValue(false));
            one(updateAndRemoveEvent).next();
            will(returnValue(true));
            one(updateAndRemoveEvent).getType();
            will(returnValue(ListEvent.UPDATE));
            one(updateAndRemoveEvent).next();
            will(returnValue(true));
            one(updateAndRemoveEvent).getType();
            will(returnValue(ListEvent.DELETE));
            one(updateAndRemoveEvent).next();
            will(returnValue(false));   
            
            allowing(addEvent).getSourceList();
            will(returnValue(presenceLibraryEventList));
            allowing(presenceLibraryEventList).get(0);
            will(returnValue(presenceLibrary));
            allowing(addEvent).getIndex();
            will(returnValue(0));
            allowing(updateAndRemoveEvent).getSourceList();
            will(returnValue(presenceLibraryEventList));
            allowing(updateAndRemoveEvent).getIndex();
            will(returnValue(0));
            
            // TODO: why two?  
            exactly(2).of(presenceLibrary).setState(LibraryState.LOADING);
            
            FriendPresence friendPresence = context.mock(FriendPresence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(friendPresence));
            
            AddressFeature addressFeature = context.mock(AddressFeature.class);
            allowing(friendPresence).getFeature(AddressFeature.ID);
            will(returnValue(addressFeature));
            
            Address address = context.mock(Address.class);
            allowing(addressFeature).getFeature();
            will(returnValue(address));
            allowing(socketsManager).canResolve(address);
            will(returnValue(false));
            allowing(socketsManager).canConnect(address);
            will(returnValue(true));
            
            allowing(friendPresence).getPresenceId();
            Browse browse = context.mock(Browse.class);
            allowing(browseFactory).createBrowse(friendPresence);
            will(returnValue(browse));
            
            Friend friend = context.mock(Friend.class);
            allowing(friendPresence).getFriend();
            will(returnValue(friend));
            allowing(friend).isAnonymous();
            will(returnValue(true));
            
            // Assertions
            exactly(1).of(friendLibraryEventList).addListEventListener(with(listenerCollector));
            exactly(1).of(presenceLibraryEventList).addListEventListener(with(listenerCollector));
            exactly(1).of(browse).start(with(browseListenerCollector));
            exactly(1).of(presenceLibrary).size();
            exactly(1).of(presenceLibrary).setState(LibraryState.LOADED);
        }});
        
        // Register the initial listener to listen for new remote libraries to manage browsing
        presenceLibraryBrowser.registerToRemoteLibraryManager();
        
        // Force fire a friend being added to the browse list 
        ListEventListener<FriendLibrary> friendListener = listenerCollector.getLastMatch();
        friendListener.listChanged(listEvent);
        
        // Force fire a presence coming available and thus initiate the browse
        assertEquals("A listener was not installed on the presence library",
                2, listenerCollector.getMatches().size());
        ListEventListener<PresenceLibrary> presenceListener = listenerCollector.getLastMatch();
        presenceListener.listChanged(addEvent);
        
        // Complete the browse
        BrowseListener browseListener = browseListenerCollector.getLastMatch();
        browseListener.browseFinished(true);
        
        // PresenceLibrary should not be added to the rebrowse list because the browse was 
        //  successful
        assertNotContains(presenceLibraryBrowser.librariesToBrowse, presenceLibrary);
        
        // Test removal of simulated killed browse
        presenceLibraryBrowser.librariesToBrowse.add(presenceLibrary);
        presenceListener.listChanged(updateAndRemoveEvent);
        assertEmpty(presenceLibraryBrowser.librariesToBrowse);
                
        context.assertIsSatisfied();
    }

    /**
     * Simulates a browse with a resolvable address.
     * 
     * <p>Skips initial integration steps and tests tryToResolveAndBrowse() and browse().
     */
    public void testBrowseWithGoodResolve() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        final BrowseFactory browseFactory = context.mock(BrowseFactory.class);
        
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        
        final Address resolvedAddress = context.mock(Address.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(browseFactory, remoteLibraryManager, socketsManager, null);
        
        final MatchAndCopy<AddressResolutionObserver> observerCollector 
            = new MatchAndCopy<AddressResolutionObserver>(AddressResolutionObserver.class);
        final MatchAndCopy<BrowseListener> browseListenerCollector 
            = new MatchAndCopy<BrowseListener>(BrowseListener.class);
        
        context.checking(new Expectations() {{
            
            allowing(presenceLibrary).setState(with(any(LibraryState.class)));
                        
            FriendPresence presence = context.mock(FriendPresence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(presence));
            AddressFeature addressFeature = context.mock(AddressFeature.class);
            allowing(presence).getFeature(AddressFeature.ID);
            will(returnValue(addressFeature));
            Address address = context.mock(Address.class);
            allowing(addressFeature).getFeature();
            will(returnValue(address));
            allowing(socketsManager).canResolve(address);
            will(returnValue(true));
            
            allowing(socketsManager).canConnect(resolvedAddress);
            will(returnValue(true));
            allowing(presence).getPresenceId();
            will(returnValue("12321321 abajeña 12342323523452"));
            Browse browse = context.mock(Browse.class);
            allowing(browseFactory).createBrowse(presence);
            will(returnValue(browse));
            Friend friend = context.mock(Friend.class);
            allowing(presence).getFriend();
            will(returnValue(friend));
            allowing(friend).isAnonymous();
            will(returnValue(true));
            
            // Assertions
            exactly(1).of(socketsManager).resolve(with(same(address)), with(observerCollector));
            exactly(1).of(browse).start(with(browseListenerCollector));
            exactly(1).of(presenceLibrary).size();
            
        }});
        
        // Kick off the browse
        presenceLibraryBrowser.tryToResolveAndBrowse(presenceLibrary, 0);
        
        // Signal resolution and complete the browse 
        observerCollector.getLastMatch().resolved(resolvedAddress);
        
        // Signal browse failure
        browseListenerCollector.getLastMatch().browseFinished(false);
        
        // Signal browse success
        browseListenerCollector.getLastMatch().browseFinished(true);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Attempt a browse on a presence without an address feature.  Ensure
     *  graceful fail and the presence is added to the retry queue.
     *  
     *  <p> Tests tryToResolveAndBrowse() and handleFailedResolution().
     */
    public void testBrowseWithoutAddressFeature() {
        final Mockery context = new Mockery();
        
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, null, null, null);
        
        context.checking(new Expectations() {{
            FriendPresence presence = context.mock(FriendPresence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(presence));
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(false));
            allowing(presence).getFeature(AddressFeature.ID);
            will(returnValue(null));
            
            allowing(presenceLibrary);
            allowing(presence);
        }});
        
        presenceLibraryBrowser.tryToResolveAndBrowse(presenceLibrary, 0);
        assertContains(presenceLibraryBrowser.librariesToBrowse, presenceLibrary);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Attempt a browse with a currently non connectable or resolvable address.  Ensure
     *  graceful fail and the PresenceLibrary is immediately retried.  On the second failing
     *  check to make sure the PresenceLibrary is added to the retry queue.
     *  
     *  <p> Tests tryToResolveAndBrowse() and handleFailedResolution().
     */  
    public void testBrowseWhenCantResolveAndCantConnect() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, null, socketsManager, null);
        
        context.checking(new Expectations() {{
            FriendPresence presence = context.mock(FriendPresence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(presence));
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(false));
            
            AddressFeature addressFeature = context.mock(AddressFeature.class);
            allowing(presence).getFeature(AddressFeature.ID);
            will(returnValue(addressFeature));
            
            Address address = context.mock(Address.class);
            allowing(addressFeature).getFeature();
            will(returnValue(address));

            // Can't resolve, can't connect, twice because immediate retry
            atLeast(2).of(socketsManager).canConnect(address);
            will(returnValue(false));
            atLeast(2).of(socketsManager).canResolve(address);
            will(returnValue(false));
            
            // General behaviour
            allowing(presenceLibrary);
            allowing(presence);

        }});
        
        // Revision = -1 so an immediate retry will be attempted
        presenceLibraryBrowser.tryToResolveAndBrowse(presenceLibrary, -1);
        assertContains(presenceLibraryBrowser.librariesToBrowse, presenceLibrary);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the edge functionality of the can resolve case.  First resolve but with a 
     *  non connectable address.  Next force fire an IOE event to the then non relevant resolve handler
     *  and make sure things are handled accordingly.
     *  
     *  <p> Tests tryToResolveAndBrowse() and handleFailedResolution().
     */
    public void testBrowseWhenCanResolveButCantConnectPlusIOE() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        final Address resolvedAddress = context.mock(Address.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, null, socketsManager, null);
        
        final MatchAndCopy<AddressResolutionObserver> observerCollector 
            = new MatchAndCopy<AddressResolutionObserver>(AddressResolutionObserver.class);
        
        context.checking(new Expectations() {{
            FriendPresence presence = context.mock(FriendPresence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(presence));
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(false));
            
            AddressFeature addressFeature = context.mock(AddressFeature.class);
            allowing(presence).getFeature(AddressFeature.ID);
            will(returnValue(addressFeature));
            
            Address address = context.mock(Address.class);
            allowing(addressFeature).getFeature();
            will(returnValue(address));

            // Can resolve, can't connect
            allowing(socketsManager).canResolve(address);
            will(returnValue(true));
            allowing(socketsManager).canConnect(resolvedAddress);
            will(returnValue(false));
            
            // General behaviour
            allowing(presenceLibrary);
            allowing(presence);
            
            // Assertions
            exactly(1).of(socketsManager).resolve(with(same(address)), with(observerCollector));
            
        }});
        
        // Kick off browse process
        presenceLibraryBrowser.tryToResolveAndBrowse(presenceLibrary, 0);
        
        // Resolve
        observerCollector.getLastMatch().resolved(resolvedAddress);
        
        // Make sure the failed PresenceLibrary is in the rebrowse list
        assertContains(presenceLibraryBrowser.librariesToBrowse, presenceLibrary);
        
        // Remove the failed presence library so when simulating the IOE we can readd it
        presenceLibraryBrowser.librariesToBrowse.remove(presenceLibrary);
        
        // Force IOE
        observerCollector.getLastMatch().handleIOException(new IOException("forced"));
        
        // Should still have the PresenceLibrary in the rebrowse list
        assertContains(presenceLibraryBrowser.librariesToBrowse, presenceLibrary);
        
        // "Shutdown" the observer
        observerCollector.getLastMatch().shutdown();
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test a browse sequence where the address feature is lost between resolving and browsing.
     *  
     * <p>NOTE: This simulates a browse upon shutdown.
     * 
     * <p>Tests tryToResolveAndBrowse and browse().
     */
    public void testBrowseWhenAfterResolveFeatureIsLost() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        final Address resolvedAddress = context.mock(Address.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, null, socketsManager, null);
        
        final MatchAndCopy<AddressResolutionObserver> observerCollector 
            = new MatchAndCopy<AddressResolutionObserver>(AddressResolutionObserver.class);
        
        context.checking(new Expectations() {{
            FriendPresence presence = context.mock(FriendPresence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(presence));
            
            AddressFeature addressFeature = context.mock(AddressFeature.class);
            
            // Only return the feature the first time since 
            //  after the resolve this should fail
            atMost(1).of(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(true));
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(false));
            atMost(1).of(presence).getFeature(AddressFeature.ID);
            will(returnValue(addressFeature));
            allowing(presence).getFeature(AddressFeature.ID);
            will(returnValue(null));
            
            Address address = context.mock(Address.class);
            allowing(addressFeature).getFeature();
            will(returnValue(address));

            // Can resolve, can connect
            allowing(socketsManager).canResolve(address);
            will(returnValue(true));
            allowing(socketsManager).canConnect(resolvedAddress);
            will(returnValue(true));
            
            // General behaviour
            allowing(presenceLibrary);
            allowing(presence);
            
            // Assertions
            exactly(1).of(socketsManager).resolve(with(same(address)), with(observerCollector));
            
        }});
        
        // Kick off browse process
        presenceLibraryBrowser.tryToResolveAndBrowse(presenceLibrary, 0);
        
        // Resolve
        observerCollector.getLastMatch().resolved(resolvedAddress);
        
        // PresenceLibrary should not be added to the rebrowse list in this case
        //  because this fault indicates the shutdown sequence is in progress.
        assertNotContains(presenceLibraryBrowser.librariesToBrowse, presenceLibrary);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Do a full normal browse on an anonymous friend. 
     * 
     * Only tests browse().
     */
    public void testBrowseWithAnonymousFriend() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        final BrowseFactory browseFactory = context.mock(BrowseFactory.class);
        
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        final RemoteFileDescAdapter searchResult = context.mock(RemoteFileDescAdapter.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(browseFactory, null, socketsManager, null);
        
        final MatchAndCopy<BrowseListener> listenerCollector 
            = new MatchAndCopy<BrowseListener>(BrowseListener.class);
        
        context.checking(new Expectations() {{
            FriendPresence presence = context.mock(FriendPresence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(presence));
            
            AddressFeature addressFeature = context.mock(AddressFeature.class);
            
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(true));
            allowing(presence).getFeature(AddressFeature.ID);
            will(returnValue(addressFeature));
            
            Friend friend = context.mock(Friend.class);
            allowing(presence).getFriend();
            will(returnValue(friend));
            allowing(friend).isAnonymous();
            will(returnValue(true));
            
            Address address = context.mock(Address.class);
            allowing(addressFeature).getFeature();
            will(returnValue(address));
            
            // Browse creation
            Browse browse = context.mock(Browse.class);
            allowing(browseFactory).createBrowse(presence);
            will(returnValue(browse));
            
            // General behaviour
            allowing(presenceLibrary);
            allowing(presence);
            allowing(searchResult);
            
            // Assertions
            exactly(1).of(browse).start(with(listenerCollector));
            
        }});
        
        // Kick off browse process
        presenceLibraryBrowser.browse(presenceLibrary);

        listenerCollector.getLastMatch().handleBrowseResult(searchResult);
        
        // PresenceLibrary should not be added to the rebrowse list because the browse was 
        //  successful
        assertNotContains(presenceLibraryBrowser.librariesToBrowse, presenceLibrary);
        
        context.assertIsSatisfied();
    }
}
