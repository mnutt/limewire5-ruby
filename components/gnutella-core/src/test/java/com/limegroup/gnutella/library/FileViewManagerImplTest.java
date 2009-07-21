package com.limegroup.gnutella.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.listener.EventListener;
import org.limewire.util.CommonUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.library.FileViewChangeEvent.Type;

public class FileViewManagerImplTest extends LimeTestCase {
    
    private File f1, f2;
    private FileDesc fd1, fd2;
    @Inject private Library library;
    @Inject private FileViewManagerImpl viewManager;
    @Inject private FileCollectionManager collectionManager;
    private SharedFileCollection c1;
    private SharedFileCollection c2;
    private String id1 = "id1";
    private String id2 = "id2";
    
    @Override
    protected void setUp() throws Exception {
        LimeTestUtils.createInjector(LimeTestUtils.createModule(this));
        
        f1 = TestUtils.getResourceInPackage("one.txt", getClass());
        f2 = TestUtils.getResourceInPackage("two.txt", getClass());  
        FileManagerTestUtils.waitForLoad(library, 2000);
        FileManagerTestUtils.assertAdds(library, f1, f2);
        fd1 = library.getFileDesc(f1);
        fd2 = library.getFileDesc(f2);
        
        c1 = collectionManager.createNewCollection("1");
        c2 = collectionManager.createNewCollection("2");
    }
    
    public void testCreateAfterExists() {
        c1.add(f1);
        c1.addFriend(id1);
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
        Listener l = new Listener();
        v.addListener(l);
        l.assertNoChanges();
    }
    
    public void testFriendAdded() {
        c1.add(f1);
        FileView v = viewManager.getFileViewForId(id1);
        Listener l = new Listener();
        v.addListener(l);
        c1.addFriend(id1);        
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(fd1, e.getFileDesc());
        assertEquals(f1, e.getFile());
        assertEquals(Type.FILE_ADDED, e.getType());
    }
    
    public void testFriendRemoved() {
        c1.add(f1);
        c1.addFriend(id1);
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
        Listener l = new Listener();
        v.addListener(l);
        c1.removeFriend(id1);
        assertEquals(0, v.size());
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(fd1, e.getFileDesc());
        assertEquals(f1, e.getFile());
        assertEquals(Type.FILE_REMOVED, e.getType());
    }

    public void testFileAdded() {
        c1.addFriend(id1);
        FileView v = viewManager.getFileViewForId(id1);
        Listener l = new Listener();
        v.addListener(l);
        c1.add(f1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(fd1, e.getFileDesc());
        assertEquals(f1, e.getFile());
        assertEquals(Type.FILE_ADDED, e.getType());
    }
    
    public void testFileRemoved() {
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(0, v.size());
        c1.add(f1);
        c1.addFriend(id1);
        assertEquals(1, v.size());
        
        Listener l = new Listener();
        v.addListener(l);
        c1.remove(f1);
        assertEquals(0, v.size());
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(fd1, e.getFileDesc());
        assertEquals(f1, e.getFile());
        assertEquals(Type.FILE_REMOVED, e.getType());
    }    
    
    public void testDuplicateFileAdded() {        
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(0, v.size());
        Listener l = new Listener();
        v.addListener(l);        
        c1.add(f1);
        c2.add(f1);
        c1.addFriend(id1);
        assertEquals(Type.FILE_ADDED, l.getEventAndClear().getType());
        c2.addFriend(id1);
        l.assertNoChanges();
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next()); 
    }
    
    public void testDuplicateFileRemoved() {        
        FileView v = viewManager.getFileViewForId(id1);
        Listener l = new Listener();
        v.addListener(l);
        assertEquals(0, v.size());
        c1.add(f1);
        c2.add(f1);
        c1.addFriend(id1);
        c2.addFriend(id1);
        assertEquals(1, v.size());
        assertEquals(Type.FILE_ADDED, l.getEventAndClear().getType());
        
        c2.remove(f1);
        l.assertNoChanges();
        assertEquals(1, v.size());
        c1.remove(f1);
        assertEquals(0, v.size());
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(fd1, e.getFileDesc());
        assertEquals(f1, e.getFile());
        assertEquals(Type.FILE_REMOVED, e.getType());
    }
    
    public void testMultipleFriends() {
        FileView v1 = viewManager.getFileViewForId(id1);
        FileView v2 = viewManager.getFileViewForId(id2);
        Listener l1 = new Listener();
        Listener l2 = new Listener();
        v1.addListener(l1);
        v2.addListener(l2);
        
        c1.add(f1);
        c1.addFriend(id1);
        assertEquals(1, v1.size());
        assertEquals(0, v2.size());
        assertEquals(fd1, v1.iterator().next());
        assertEquals(Type.FILE_ADDED, l1.getEventAndClear().getType());
        l2.assertNoChanges();
        
        c2.add(f2);
        c2.addFriend(id2);
        assertEquals(1, v1.size());
        assertEquals(1, v2.size());
        assertEquals(fd1, v1.iterator().next());
        assertEquals(fd2, v2.iterator().next());
        l1.assertNoChanges();
        assertEquals(Type.FILE_ADDED, l2.getEventAndClear().getType());
        
        c1.addFriend(id2);
        assertEquals(1, v1.size());
        assertEquals(2, v2.size());
        assertEquals(fd1, v1.iterator().next());
        Iterator<FileDesc> v2Iter = v2.iterator();
        assertEquals(fd1, v2Iter.next());
        assertEquals(fd2, v2Iter.next());
        l1.assertNoChanges();
        assertEquals(Type.FILE_ADDED, l2.getEventAndClear().getType());        
        
        c2.removeFriend(id2);
        assertEquals(1, v1.size());
        assertEquals(1, v2.size());
        assertEquals(fd1, v1.iterator().next());
        assertEquals(fd1, v2.iterator().next());      
        l1.assertNoChanges();
        assertEquals(Type.FILE_REMOVED, l2.getEventAndClear().getType());        
    }
    
    public void testClearLibrary() {
        c1.add(f1);
        c1.addFriend(id1);
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
        
        Listener l = new Listener();
        v.addListener(l);
        library.clear();
        assertEquals(0, v.size());
        assertEquals(Type.FILES_CLEARED, l.getEventAndClear().getType());
    }
    
    public void testClearCollection() {
        c1.add(f1);
        c1.addFriend(id1);
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
    
        Listener l = new Listener();
        v.addListener(l);
        c1.clear();
        assertEquals(0, v.size());
        assertEquals(Type.FILE_REMOVED, l.getEventAndClear().getType());
    }
    
    public void testClearCollectionIfDuplicated() {
        c1.add(f1);
        c1.add(f2);
        c1.addFriend(id1);
        
        c2.add(f1);
        c2.addFriend(id1);
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(2, v.size());
        Iterator<FileDesc> vIter = v.iterator();
        assertEquals(fd1, vIter.next());
        assertEquals(fd2, vIter.next());
        
        Listener l = new Listener();
        v.addListener(l);
        c1.clear();
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(fd2, e.getFileDesc());
        assertEquals(f2, e.getFile());
        assertEquals(Type.FILE_REMOVED, e.getType());
    }    
    
    public void testCollectionDeleted() {
        c1.add(f1);
        c1.addFriend(id1);
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
        
        Listener l = new Listener();
        v.addListener(l);
        collectionManager.removeCollectionById(c1.getId());
        assertEquals(0, v.size());
        assertEquals(Type.FILE_REMOVED, l.getEventAndClear().getType());
    }
    
    public void testCollectionDeletedIfDuplicated() {
        c1.add(f1);
        c1.add(f2);
        c1.addFriend(id1);
        
        c2.add(f1);
        c2.addFriend(id1);
        FileView v = viewManager.getFileViewForId(id1);
        assertEquals(2, v.size());
        Iterator<FileDesc> vIter = v.iterator();
        assertEquals(fd1, vIter.next());
        assertEquals(fd2, vIter.next());
        
        Listener l = new Listener();
        v.addListener(l);
        collectionManager.removeCollectionById(c1.getId());
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(fd2, e.getFileDesc());
        assertEquals(f2, e.getFile());
        assertEquals(Type.FILE_REMOVED, e.getType());
    }
    
    public void testFriendsSet() {
        c1.add(f1);
        
        FileView v1 = viewManager.getFileViewForId(id1);
        FileView v2 = viewManager.getFileViewForId(id2);
        assertEquals(0, v1.size());
        assertEquals(0, v2.size());
        
        Listener l1 = new Listener();
        v1.addListener(l1);
        Listener l2 = new Listener();
        v2.addListener(l2);
        
        c1.setFriendList(Arrays.asList(id1));
        assertEquals(1, v1.size());
        assertEquals(0, v2.size());
        assertEquals(fd1, v1.iterator().next());
        assertEquals(Type.FILE_ADDED, l1.getEventAndClear().getType());
        l2.assertNoChanges();
        
        c1.setFriendList(Arrays.asList(id1, id2));
        assertEquals(1, v1.size());
        assertEquals(1, v2.size());
        assertEquals(fd1, v1.iterator().next());
        assertEquals(fd1, v2.iterator().next());
        l1.assertNoChanges();
        assertEquals(Type.FILE_ADDED, l2.getEventAndClear().getType());

        c1.setFriendList(Arrays.asList(id2));
        assertEquals(0, v1.size());
        assertEquals(1, v2.size());
        assertEquals(fd1, v2.iterator().next());
        assertEquals(Type.FILE_REMOVED, l1.getEventAndClear().getType());
        l2.assertNoChanges();
        
        c1.setFriendList(Collections.<String>emptyList());
        assertEquals(0, v1.size());
        assertEquals(0, v2.size());
        l1.assertNoChanges();
        assertEquals(Type.FILE_REMOVED, l2.getEventAndClear().getType());
    }
    
    public void testFileChanged() throws Exception {
        FileView v = viewManager.getFileViewForId(id1);
        c1.add(f1);
        c1.addFriend(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());

        Listener l = new Listener();
        v.addListener(l);
        l.assertNoChanges();
        FileManagerTestUtils.assertFileChanges(library, f1);
        FileDesc newFd1 = library.getFileDesc(f1);
        assertEquals(1, v.size());
        assertNotSame(fd1, v.iterator().next());        
        assertEquals(newFd1, v.iterator().next());
        // Note: Because when a FileDesc is changed it has to recalculate the URNs,
        //       that means the the library sends two events: FILE_CHANGED & FILE_META_CHANGED,
        //       the meta changing after the new URN is calculated.  The initial FILE_CHANGED
        //       is for a FileDesc that has no URN or metadata!!
        //       This has the effect of causing a view to temporarily remove the file,
        //       waiting for the URN to calculate.
        //       The end result is that the view sends two events: REMOVE, ADD.
        //       It removes when the change happens (because it can't share a file w/o a URN),
        //       and it re-adds when the meta changes, because it can share it now.
        List<FileViewChangeEvent> events = l.getEventsAndClear(2);
        FileViewChangeEvent remove = events.get(0);
        assertSame(fd1, remove.getFileDesc());
        assertEquals(f1, remove.getFile());
        assertEquals(Type.FILE_REMOVED, remove.getType());
        
        FileViewChangeEvent add = events.get(1);
        assertSame(newFd1, add.getFileDesc());
        assertEquals(f1, add.getFile());
        assertEquals(Type.FILE_ADDED, add.getType());
        
    }
    
    public void testFileChangedWithDuplicates() throws Exception {
        FileView v = viewManager.getFileViewForId(id1);
        c1.add(f1);
        c1.addFriend(id1);
        c2.add(f1);
        c2.addFriend(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());

        Listener l = new Listener();
        v.addListener(l);
        FileManagerTestUtils.assertFileChanges(library, f1);
        FileDesc newFd1 = library.getFileDesc(f1);
        assertEquals(1, v.size());
        assertNotSame(fd1, v.iterator().next());
        assertEquals(newFd1, v.iterator().next());
        // Note: Because when a FileDesc is changed it has to recalculate the URNs,
        //       that means the the library sends two events: FILE_CHANGED & FILE_META_CHANGED,
        //       the meta changing after the new URN is calculated.  The initial FILE_CHANGED
        //       is for a FileDesc that has no URN or metadata!!
        //       This has the effect of causing a view to temporarily remove the file,
        //       waiting for the URN to calculate.
        //       The end result is that the view sends two events: REMOVE, ADD.
        //       It removes when the change happens (because it can't share a file w/o a URN),
        //       and it re-adds when the meta changes, because it can share it now.
        // Further complication:
        //       Because each collection resends a META_CHANGE when it hears it,
        //       we learn about multiple META_CHANGES.  Each view also needs to resend
        //       the META_CHANGE, so listeners can be kept up to date with changes to the
        //       metadata.  It's hard to distinguish between a valid META_CHANGE and one
        //       that existed because two different collections shared with a single person.
        //       So, for now, we just accept that an additional META_CHANGE will be sent.
        List<FileViewChangeEvent> events = l.getEventsAndClear(3);
        FileViewChangeEvent remove = events.get(0);
        assertSame(fd1, remove.getFileDesc());
        assertEquals(f1, remove.getFile());
        assertEquals(Type.FILE_REMOVED, remove.getType());
        
        FileViewChangeEvent add = events.get(1);
        assertSame(newFd1, add.getFileDesc());
        assertEquals(f1, add.getFile());
        assertEquals(Type.FILE_ADDED, add.getType());
        
        FileViewChangeEvent meta = events.get(2);
        assertSame(newFd1, meta.getFileDesc());
        assertEquals(f1, meta.getFile());
        assertEquals(Type.FILE_META_CHANGED, meta.getType());
    }
    
    public void testFileChangeFails() throws Exception {
        FileView v = viewManager.getFileViewForId(id1);
        
        File copy1 = new File("f1 copy.txt").getCanonicalFile();
        CommonUtils.copyFile(f1, copy1);
        FileDesc copyFd1;
        try {
            FileManagerTestUtils.assertAdds(c1, copy1);
            copyFd1 = library.getFileDesc(copy1);
            c1.addFriend(id1);
            assertEquals(1, v.size());
            assertEquals(copyFd1, v.iterator().next());
        } finally {
            copy1.delete();
        }

        Listener l = new Listener();
        v.addListener(l);
        FileManagerTestUtils.assertFileChangedFails(FileViewChangeFailedException.Reason.NOT_MANAGEABLE, library, copy1);
        assertEquals(0, v.size());
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(copyFd1, e.getFileDesc());
        assertEquals(copy1, e.getFile());
        assertEquals(Type.FILE_REMOVED, e.getType());
    }
    
    public void testFileRenamed() throws Exception {
        FileView v = viewManager.getFileViewForId(id1);
        c1.add(f1);
        c1.addFriend(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());

        Listener l = new Listener();
        v.addListener(l);
        File copy1 = new File("f1 copy.txt").getCanonicalFile();
        CommonUtils.copyFile(f1, copy1);
        try {
            FileManagerTestUtils.assertFileRenames(library, f1, copy1);
            assertEquals(1, v.size());
            assertNotSame(fd1, v.iterator().next());
            
            assertEquals(library.getFileDesc(copy1), v.iterator().next());
        } finally {
            copy1.delete();
        }
        
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(f1, e.getOldFile());
        assertEquals(fd1, e.getOldValue());        
        assertEquals(copy1, e.getFile());
        assertEquals(library.getFileDesc(copy1), e.getFileDesc());
        assertEquals(Type.FILE_CHANGED, e.getType());
    }
    
    public void testFileRenamesWithDuplicates() throws Exception {
        FileView v = viewManager.getFileViewForId(id1);
        c1.add(f1);
        c1.addFriend(id1);

        c2.add(f1);
        c2.addFriend(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());

        Listener l = new Listener();
        v.addListener(l);
        
        File copy1 = new File("f1 copy.txt").getCanonicalFile();
        CommonUtils.copyFile(f1, copy1);
        try {
            FileManagerTestUtils.assertFileRenames(library, f1, copy1);
            assertEquals(1, v.size());
            assertNotSame(fd1, v.iterator().next());
            
            assertEquals(library.getFileDesc(copy1), v.iterator().next());
        } finally {
            copy1.delete();
        }    
        
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(f1, e.getOldFile());
        assertEquals(fd1, e.getOldValue());        
        assertEquals(copy1, e.getFile());
        assertEquals(library.getFileDesc(copy1), e.getFileDesc());
        assertEquals(Type.FILE_CHANGED, e.getType());
    }
    
    public void testRenameFails() throws Exception {
        FileView v = viewManager.getFileViewForId(id1);
        c1.add(f1);
        c1.addFriend(id1);
        assertEquals(1, v.size());
        assertEquals(fd1, v.iterator().next());

        Listener l = new Listener();
        v.addListener(l);
        
        File fake = new File("fake").getCanonicalFile();
        FileManagerTestUtils.assertFileRenameFails(FileViewChangeFailedException.Reason.NOT_MANAGEABLE, library, f1, fake);
        assertEquals(0, v.size());
        
        FileViewChangeEvent e = l.getEventAndClear();
        assertEquals(fd1, e.getFileDesc());
        assertEquals(f1, e.getFile());
        assertEquals(Type.FILE_REMOVED, e.getType());
    }
    
    private static class Listener implements EventListener<FileViewChangeEvent> {
        private final List<FileViewChangeEvent> changes = new ArrayList<FileViewChangeEvent>();

        @Override
        public void handleEvent(FileViewChangeEvent event) {
            changes.add(event);
        }
        
        FileViewChangeEvent getEventAndClear() {
            if(changes.isEmpty()) {
                throw new AssertionFailedError("no changes!");
            } else if(changes.size() > 1) {
                throw new AssertionFailedError("More than 1 change: " + changes);
            } else {
                return changes.remove(0);
            }
        }
        
        List<FileViewChangeEvent> getEventsAndClear(int expected) {
            if(changes.isEmpty()) {
                throw new AssertionFailedError("no changes!");
            } else if(changes.size() != expected) {
                throw new AssertionFailedError("Unexpected changecount: " + changes.size() + ", changes: " + changes);
            } else {
                List<FileViewChangeEvent> list = new ArrayList<FileViewChangeEvent>(changes);
                changes.clear();
                return list;
            }
        }
        
        void assertNoChanges() {
            assertTrue(changes.toString(), changes.isEmpty());
        }
    }
}

