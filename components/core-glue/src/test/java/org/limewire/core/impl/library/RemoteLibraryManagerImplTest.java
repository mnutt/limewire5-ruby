package org.limewire.core.impl.library;

import java.util.Iterator;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.SearchResultList;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.util.BaseTestCase;

public class RemoteLibraryManagerImplTest extends BaseTestCase {

    public RemoteLibraryManagerImplTest(String name) {
        super(name);
    }

    public void testPresenceLibraryMethods() {
        Mockery context = new Mockery();
        final FriendPresence friendPresence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "presence1";
        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "friend1";
        context.checking(new Expectations() {
            {
                allowing(friend1).isAnonymous();
                will(returnValue(false));
                allowing(friendPresence1).getFriend();
                will(returnValue(friend1));
                allowing(friendPresence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
            }
        });

        RemoteLibraryManagerImpl remoteLibraryManagerImpl = new RemoteLibraryManagerImpl();

        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        boolean added = remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(added);
        PresenceLibrary presenceLibrary1 = remoteLibraryManagerImpl.getPresenceLibrary(friendPresence1);

        assertNotNull(presenceLibrary1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        added = remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertFalse(added);

        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        remoteLibraryManagerImpl.removePresenceLibrary(friendPresence1);
        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        added = remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(added);
        presenceLibrary1 = remoteLibraryManagerImpl.getPresenceLibrary(friendPresence1);
        
        assertNotNull(presenceLibrary1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        context.assertIsSatisfied();
    }

    public void testFriendLibraryMethods() {
        Mockery context = new Mockery();
        final FriendPresence friendPresence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "presence1";
        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "friend1";

        final FriendPresence friendPresence2 = context.mock(FriendPresence.class);
        final String presenceId2 = "presence2";
        final Friend friend2 = context.mock(Friend.class);
        final String friendId2 = "friend2";
        context.checking(new Expectations() {
            {
                allowing(friend1).isAnonymous();
                will(returnValue(false));
                allowing(friend2).isAnonymous();
                will(returnValue(false));
                
                allowing(friendPresence1).getFriend();
                will(returnValue(friend1));
                allowing(friendPresence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(friend1).getId();
                will(returnValue(friendId1));

                allowing(friendPresence2).getFriend();
                will(returnValue(friend2));
                allowing(friendPresence2).getPresenceId();
                will(returnValue(presenceId2));
                allowing(friend2).getId();
                will(returnValue(friendId2));
            }
        });

        RemoteLibraryManagerImpl remoteLibraryManagerImpl = new RemoteLibraryManagerImpl();

        List<FriendLibrary> friendLibraries = remoteLibraryManagerImpl.getFriendLibraryList();
        assertEmpty(friendLibraries);
        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(1, friendLibraries.size());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(1, friendLibraries.size());

        remoteLibraryManagerImpl.removePresenceLibrary(friendPresence1);
        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEmpty(friendLibraries);

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(1, friendLibraries.size());

        remoteLibraryManagerImpl.removeFriendLibrary(friend1);
        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEmpty(friendLibraries);

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(1, friendLibraries.size());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence2);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend2));
        assertEquals(2, friendLibraries.size());

        Iterator<FriendLibrary> friendLibraryIterator = friendLibraries.iterator();
        assertEquals(friend1, friendLibraryIterator.next().getFriend());
        assertEquals(friend2, friendLibraryIterator.next().getFriend());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(2, friendLibraries.size());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence2);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend2));
        assertEquals(2, friendLibraries.size());

        friendLibraryIterator = friendLibraries.iterator();
        assertEquals(friend1, friendLibraryIterator.next().getFriend());
        assertEquals(friend2, friendLibraryIterator.next().getFriend());

        context.assertIsSatisfied();
    }

    public void testAllFriendsFileList() {
        Mockery context = new Mockery();

        final FriendPresence friendPresence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "presence1";
        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "friend1";

        final FriendPresence friendPresence2 = context.mock(FriendPresence.class);
        final String presenceId2 = "presence2";
        final Friend friend2 = context.mock(Friend.class);
        final String friendId2 = "friend2";

        final SearchResult remoteFileItem1 = context.mock(SearchResult.class);
        final SearchResult remoteFileItem2 = context.mock(SearchResult.class);
        final SearchResult remoteFileItem3 = context.mock(SearchResult.class);

        final URN urn1 = new TestURN("1");
        final URN urn2 = new TestURN("2");
        final URN urn3 = new TestURN("3");

        context.checking(new Expectations() {
            {
                allowing(friend1).isAnonymous();
                will(returnValue(false));
                allowing(friend2).isAnonymous();
                will(returnValue(false));
                
                allowing(friendPresence1).getFriend();
                will(returnValue(friend1));
                allowing(friendPresence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(friend1).getId();
                will(returnValue(friendId1));

                allowing(friendPresence2).getFriend();
                will(returnValue(friend2));
                allowing(friendPresence2).getPresenceId();
                will(returnValue(presenceId2));
                allowing(friend2).getId();
                will(returnValue(friendId2));

                allowing(remoteFileItem1).getUrn();
                will(returnValue(urn1));
                allowing(remoteFileItem2).getUrn();
                will(returnValue(urn2));
                allowing(remoteFileItem3).getUrn();
                will(returnValue(urn3));
            }
        });

        RemoteLibraryManagerImpl remoteLibraryManagerImpl = new RemoteLibraryManagerImpl();

        SearchResultList allFriendFiles = remoteLibraryManagerImpl.getAllFriendsFileList();
        assertEmpty(allFriendFiles.getModel());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        PresenceLibrary presenceLibrary1 = remoteLibraryManagerImpl.getPresenceLibrary(friendPresence1);

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence2);
        PresenceLibrary presenceLibrary2 = remoteLibraryManagerImpl.getPresenceLibrary(friendPresence2);

        assertEmpty(allFriendFiles.getModel());

        presenceLibrary1.addNewResult(remoteFileItem1);

        assertEquals(1, allFriendFiles.getModel().size());
        assertContains(allFriendFiles.getModel(), remoteFileItem1);

        presenceLibrary2.addNewResult(remoteFileItem2);

        assertEquals(2, allFriendFiles.getModel().size());
        assertContains(allFriendFiles.getModel(), remoteFileItem1);
        assertContains(allFriendFiles.getModel(), remoteFileItem2);

        presenceLibrary2.addNewResult(remoteFileItem3);
        assertEquals(3, allFriendFiles.getModel().size());
        assertContains(allFriendFiles.getModel(), remoteFileItem1);
        assertContains(allFriendFiles.getModel(), remoteFileItem2);
        assertContains(allFriendFiles.getModel(), remoteFileItem2);
        
        context.assertIsSatisfied();
    }

    private class TestURN implements URN {
        private String urn;

        public TestURN(String urn) {
            this.urn = urn;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TestURN) {
                TestURN oUrn = (TestURN) obj;
                return urn.equals(oUrn);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return urn.hashCode();
        }

        @Override
        public int compareTo(URN o) {
            if (o instanceof TestURN) {
                TestURN testURN = (TestURN) o;
                return urn.compareTo(testURN.urn);
            }
            return -1;
        }

        @Override
        public String toString() {
            return urn;
        }
    }
}
