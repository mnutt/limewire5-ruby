package org.limewire.core.impl.friend;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.impl.friend.FriendShareListRefresher;
import org.limewire.core.impl.library.FileViewStub;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendEvent;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.friend.api.feature.LibraryChangedNotifierFeature;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import com.limegroup.gnutella.ClockStub;
import com.limegroup.gnutella.MockFriend;
import com.limegroup.gnutella.MockFriendPresence;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryStatusEvent;

public class FriendShareListRefresherTest extends BaseTestCase {

    public FriendShareListRefresherTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public void testRegister() {
        Mockery context = new Mockery();
        final Library library = context.mock(Library.class);
        final FileViewManager viewManager = context.mock(FileViewManager.class);
        final ListenerSupport friendSupport = context.mock(ListenerSupport.class);
      
        FriendShareListRefresher refresher = new FriendShareListRefresher(null, null, null, null);        
        context.checking(new Expectations() {{
            exactly(1).of(library).addManagedListStatusListener(with(any(EventListener.class)));
            exactly(1).of(viewManager).addListener(with(any(EventListener.class)));
            exactly(1).of(friendSupport).addListener(with(any(EventListener.class)));
        }});        
        refresher.register(library);
        refresher.register(viewManager, friendSupport);        
        context.assertIsSatisfied();
    }

    @SuppressWarnings("unchecked")
    public void testHandleEventBeforeLibraryLoad() {
        Mockery context = new Mockery();
        final Map friendMap = context.mock(Map.class);
        final ScheduledExecutorService executor = context.mock(ScheduledExecutorService.class);
        final FileViewManager viewManager = context.mock(FileViewManager.class);
        final ListenerSupport friendSupport = context.mock(ListenerSupport.class);
       
        final MatchAndCopy<EventListener> viewMatcher = new MatchAndCopy<EventListener>(EventListener.class);
        final MatchAndCopy<EventListener> friendMatcher = new MatchAndCopy<EventListener>(EventListener.class);
        
        FriendShareListRefresher refresher = new FriendShareListRefresher(null, null, executor, friendMap);

        // Events do nothing because library isn't loaded.
        context.checking(new Expectations() {{
            exactly(1).of(viewManager).addListener(with(viewMatcher));
            exactly(1).of(friendSupport).addListener(with(friendMatcher));
        }});        
        refresher.register(viewManager, friendSupport);
        viewMatcher.getLastMatch().handleEvent(new FileViewChangeEvent(new FileViewStub("test id"), FileViewChangeEvent.Type.FILE_ADDED, new FileDescStub()));
        friendMatcher.getLastMatch().handleEvent(new FriendEvent(new MockFriend("test id"), FriendEvent.Type.REMOVED));
        
        context.assertIsSatisfied();

    }
    
    @SuppressWarnings("unchecked")
    public void testHandleEventAfterLibraryLoad() {
        Mockery context = new Mockery();

        final Map friendMap = context.mock(Map.class);
        final ScheduledExecutorService executor = context.mock(ScheduledExecutorService.class);
        final FileViewManager viewManager = context.mock(FileViewManager.class);
        final ListenerSupport friendSupport = context.mock(ListenerSupport.class);
        final Friend friend = new MockFriend("test id");
        
        final MatchAndCopy<EventListener> viewMatcher = new MatchAndCopy<EventListener>(EventListener.class);
        final MatchAndCopy<EventListener> friendMatcher = new MatchAndCopy<EventListener>(EventListener.class);
        
        FriendShareListRefresher refresher = new FriendShareListRefresher(null, null, executor, friendMap);
        refresher.fileManagerLoaded.set(true);
        
        final Sequence resultSequence = context.sequence("resultSequence");        
        
        // Initial setup. 
        context.checking(new Expectations() {{
            exactly(1).of(viewManager).addListener(with(viewMatcher));
            exactly(1).of(friendSupport).addListener(with(friendMatcher));
        }});
        refresher.register(viewManager, friendSupport);       
        context.assertIsSatisfied();
        
        // Event triggers friend retrieval & scheduling.
        context.checking(new Expectations() {{
            exactly(1).of(friendMap).get(friend.getId());
            will(returnValue(friend));
            inSequence(resultSequence);
            
            exactly(1).of(executor).schedule(with(any(Runnable.class)), with(equal(5000000000L)), with(equal(TimeUnit.NANOSECONDS)));
            inSequence(resultSequence);
        }});        
        viewMatcher.getLastMatch().handleEvent(new FileViewChangeEvent(new FileViewStub(friend.getId()), FileViewChangeEvent.Type.FILE_ADDED, new FileDescStub()));
        context.assertIsSatisfied();
        
        // Another modification event will not trigger a new runnable to be scheduled,
        // because one is already scheduled.
        context.checking(new Expectations() {{
            exactly(1).of(friendMap).get(friend.getId());
            will(returnValue(friend));
            inSequence(resultSequence);            
        }});
        viewMatcher.getLastMatch().handleEvent(new FileViewChangeEvent(new FileViewStub(friend.getId()), FileViewChangeEvent.Type.FILE_ADDED, new FileDescStub()));
        context.assertIsSatisfied();
        
        // But now, if we remove the known friend, a later modification will again trigger a new runnable.
        context.checking(new Expectations() {{
            exactly(1).of(friendMap).get(friend.getId());
            will(returnValue(friend));
            inSequence(resultSequence);
            
            exactly(1).of(executor).schedule(with(any(Runnable.class)), with(equal(5000000000L)), with(equal(TimeUnit.NANOSECONDS)));
            inSequence(resultSequence);            
        }});
        friendMatcher.getLastMatch().handleEvent(new FriendEvent(friend, FriendEvent.Type.REMOVED));
        viewMatcher.getLastMatch().handleEvent(new FileViewChangeEvent(new FileViewStub(friend.getId()), FileViewChangeEvent.Type.FILE_ADDED, new FileDescStub()));
        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    public void testFinishedLoadingListenerWithUnrelatedEventsDoesNothing() {
        Mockery context = new Mockery();

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        final Library library = context.mock(Library.class);
        
        FriendShareListRefresher refresher = new FriendShareListRefresher(null, tracker, scheduledExecutorService, null);
        
        final MatchAndCopy<EventListener> libraryMatcher = new MatchAndCopy<EventListener>(EventListener.class);
        
        context.checking(new Expectations() {{
            exactly(1).of(library).addManagedListStatusListener(with(libraryMatcher));
        }});
        refresher.register(library);
        
        libraryMatcher.getLastMatch().handleEvent(new LibraryStatusEvent(library, LibraryStatusEvent.Type.SAVE));
        libraryMatcher.getLastMatch().handleEvent(new LibraryStatusEvent(library, LibraryStatusEvent.Type.LOAD_FINISHING));        
        context.assertIsSatisfied();
    }
    
    
    @SuppressWarnings("unchecked")
    public void testFinishedLoadingListenerWithLoadCompleteSendsNotifications() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        final Library library = context.mock(Library.class);
        final Map<String, Friend> friendMap = new HashMap<String, Friend>();
        
        final MockFriend friend1 = new MockFriend("friend 1");
        final MockFriend friend2 = new MockFriend("friend 2");
        final MockFriendPresence friend1Presence1 = new MockFriendPresence(friend1, "a");
        final MockFriendPresence friend1Presence2 = new MockFriendPresence(friend1, "b");
        final MockFriendPresence friend2Presence1 = new MockFriendPresence(friend2, "c");
        final MockFriendPresence friend2Presence2 = new MockFriendPresence(friend2, "d");
        friend1.addPresence(friend1Presence1);
        friend1.addPresence(friend1Presence2);
        friend2.addPresence(friend2Presence1);
        friend2.addPresence(friend2Presence2);
        friendMap.put(friend1.getId(), friend1);
        friendMap.put(friend2.getId(), friend2);
        
        FriendShareListRefresher refresher = new FriendShareListRefresher(null, tracker, scheduledExecutorService, friendMap);
       
        final LibraryChangedNotifier notifierA = context.mock(LibraryChangedNotifier.class);
        final LibraryChangedNotifier notifierB = context.mock(LibraryChangedNotifier.class);
        final LibraryChangedNotifier notifierC = context.mock(LibraryChangedNotifier.class); 
        
        friend1Presence1.addFeature(new LibraryChangedNotifierFeature(notifierA));
        friend1Presence2.addFeature(new LibraryChangedNotifierFeature(notifierB));
        friend2Presence1.addFeature(new LibraryChangedNotifierFeature(notifierC));
        // Do not add a feature for f2p2.
        
        final MatchAndCopy<EventListener> libraryMatcher = new MatchAndCopy<EventListener>(EventListener.class);
        
        context.checking(new Expectations() {{
            exactly(1).of(library).addManagedListStatusListener(with(libraryMatcher));
        }});
        refresher.register(library);        
        context.assertIsSatisfied();
        
        final FeatureTransport<LibraryChangedNotifier> transport11 = context.mock(FeatureTransport.class);
        friend1Presence1.addTransport(LibraryChangedNotifierFeature.class, transport11);
        final FeatureTransport<LibraryChangedNotifier> transport12 = context.mock(FeatureTransport.class);
        friend1Presence2.addTransport(LibraryChangedNotifierFeature.class, transport12);
        final FeatureTransport<LibraryChangedNotifier> transport21 = context.mock(FeatureTransport.class);
        friend2Presence1.addTransport(LibraryChangedNotifierFeature.class, transport21);
        
        context.checking(new Expectations() {{
            exactly(1).of(tracker).sentRefresh(friend1.getId());
            exactly(1).of(tracker).sentRefresh(friend2.getId());
            one(transport11).sendFeature(friend1Presence1, notifierA);
            one(transport12).sendFeature(friend1Presence2, notifierB);
            one(transport21).sendFeature(friend2Presence1, notifierC);
        }});
        
        assertFalse(refresher.fileManagerLoaded.get());
        libraryMatcher.getLastMatch().handleEvent(new LibraryStatusEvent(library, LibraryStatusEvent.Type.LOAD_COMPLETE));
        assertTrue(refresher.fileManagerLoaded.get());
        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    public void testScheduledLibraryRefreshSenderWithNoRefreshNeeded() {
        Mockery context = new Mockery();
        
        final ClockStub clock = new ClockStub();
        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final ScheduledExecutorService executor = context.mock(ScheduledExecutorService.class);
        final FileViewManager viewManager = context.mock(FileViewManager.class);
        final ListenerSupport friendSupport = context.mock(ListenerSupport.class);
        final Map<String, Friend> friendMap = new HashMap<String, Friend>();
        final Friend friend = new MockFriend("anId");
        friendMap.put(friend.getId(), friend);
        
        final MatchAndCopy<EventListener> viewMatcher = new MatchAndCopy<EventListener>(EventListener.class);
        final MatchAndCopy<EventListener> friendMatcher = new MatchAndCopy<EventListener>(EventListener.class);
       
        FriendShareListRefresher refresher = new FriendShareListRefresher(clock, tracker, executor, friendMap);
        refresher.fileManagerLoaded.set(true);
        
        // Initial setup. 
        context.checking(new Expectations() {{
            exactly(1).of(viewManager).addListener(with(viewMatcher));
            exactly(1).of(friendSupport).addListener(with(friendMatcher));
        }});
        refresher.register(viewManager, friendSupport);
        context.assertIsSatisfied();
        
        final MatchAndCopy<Runnable> runnableMatcher = new MatchAndCopy<Runnable>(Runnable.class);
        
        // Event triggers friend retrieval & scheduling.
        context.checking(new Expectations() {{            
            exactly(1).of(executor).schedule(with(runnableMatcher), with(equal(5000000000L)), with(equal(TimeUnit.NANOSECONDS)));
        }});
        clock.setNanoTime(System.nanoTime());
        viewMatcher.getLastMatch().handleEvent(new FileViewChangeEvent(new FileViewStub(friend.getId()), FileViewChangeEvent.Type.FILE_ADDED, new FileDescStub()));
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            // Browse times should at least be compared
            exactly(1).of(tracker).lastBrowseTime(friend.getId());
            will(returnValue(new Date(50)));
            exactly(1).of(tracker).lastRefreshTime(friend.getId());
            will(returnValue(new Date(100)));            
        }});
        clock.setNanoTime(clock.nanoTime() + TimeUnit.SECONDS.toNanos(6));
        runnableMatcher.getLastMatch().run();
        context.assertIsSatisfied();
    }
    
    /**
     * Force fire a refresh action with the last browse time after the 
     * last refresh time, therefore with notifications necessary.  Also,
     * force one of the feature lookups to return null and make sure the 
     * failure is handled correctly.
     * <p>
     * Ensure that the browse tracker and friend presence notifications are made.  
     */
    @SuppressWarnings("unchecked")
    public void testScheduledLibraryRefreshSender() throws Exception {
        Mockery context = new Mockery();
        
        final ClockStub clock = new ClockStub();
        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final ScheduledExecutorService executor = context.mock(ScheduledExecutorService.class);
        final FileViewManager viewManager = context.mock(FileViewManager.class);
        final ListenerSupport friendSupport = context.mock(ListenerSupport.class);
        final Map<String, Friend> friendMap = new HashMap<String, Friend>();
        final MockFriend friend = new MockFriend("anId");
        final MockFriendPresence presence1 = new MockFriendPresence(friend, "a");
        final MockFriendPresence presence2 = new MockFriendPresence(friend, "b");
        friend.addPresence(presence1);
        friend.addPresence(presence2);
        friendMap.put(friend.getId(), friend);
        
        final MatchAndCopy<EventListener> viewMatcher = new MatchAndCopy<EventListener>(EventListener.class);
        final MatchAndCopy<EventListener> friendMatcher = new MatchAndCopy<EventListener>(EventListener.class);
       
        FriendShareListRefresher refresher = new FriendShareListRefresher(clock, tracker, executor, friendMap);
        refresher.fileManagerLoaded.set(true);
        
        // Initial setup. 
        context.checking(new Expectations() {{
            exactly(1).of(viewManager).addListener(with(viewMatcher));
            exactly(1).of(friendSupport).addListener(with(friendMatcher));
        }});
        refresher.register(viewManager, friendSupport);
        context.assertIsSatisfied();
        
        final MatchAndCopy<Runnable> runnableMatcher = new MatchAndCopy<Runnable>(Runnable.class);
        
        // Event triggers friend retrieval & scheduling.
        context.checking(new Expectations() {{            
            exactly(1).of(executor).schedule(with(runnableMatcher), with(equal(5000000000L)), with(equal(TimeUnit.NANOSECONDS)));
        }});
        clock.setNanoTime(System.nanoTime());
        viewMatcher.getLastMatch().handleEvent(new FileViewChangeEvent(new FileViewStub(friend.getId()), FileViewChangeEvent.Type.FILE_ADDED, new FileDescStub()));
        context.assertIsSatisfied();
                
        final LibraryChangedNotifier notifierA = context.mock(LibraryChangedNotifier.class);
        presence1.addFeature(new LibraryChangedNotifierFeature(notifierA));
        
        final FeatureTransport<LibraryChangedNotifier> transport = context.mock(FeatureTransport.class);
        presence1.addTransport(LibraryChangedNotifierFeature.class, transport);
        
        context.checking(new Expectations() {{
            // Browse times should at least be compared
            exactly(1).of(tracker).lastBrowseTime(friend.getId());
            will(returnValue(new Date(100)));
            exactly(1).of(tracker).lastRefreshTime(friend.getId());
            will(returnValue(new Date(50)));     
            
            exactly(1).of(tracker).sentRefresh(friend.getId());
            one(transport).sendFeature(presence1, notifierA);
        }});
        clock.setNanoTime(clock.nanoTime() + TimeUnit.SECONDS.toNanos(6));
        runnableMatcher.getLastMatch().run();
        context.assertIsSatisfied();
    }
}
