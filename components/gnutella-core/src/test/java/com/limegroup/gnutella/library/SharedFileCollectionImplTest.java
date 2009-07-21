package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAdds;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewTestFile;

import java.io.File;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Injector;
import com.google.inject.Stage;

/**
 * FriendFileList tests
 */

//TODO: cleanup and remove sleeps
public class SharedFileCollectionImplTest extends LimeTestCase {

    private LibraryImpl managedList;
    private SharedFileCollectionImpl friendList;
    private Injector injector;

    private File f1;
    
    public SharedFileCollectionImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SharedFileCollectionImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        friendList = (SharedFileCollectionImpl)injector.getInstance(FileCollectionManager.class).createNewCollection("test");
        managedList = (LibraryImpl)injector.getInstance(Library.class);
        injector.getInstance(ServiceRegistry.class).initialize();
    }

    public void testNoFiles() {
        assertEquals(0, friendList.size());
        assertFalse(friendList.iterator().hasNext());
        assertFalse(friendList.pausableIterable().iterator().hasNext());
    }
    
    /**
     * Tests adding a single file to a friendList
     */
    public void testAddFile() throws Exception {
        assertEquals(friendList.size(), 0);
        
        f1 = createNewTestFile(1, _scratchDir);
        
        assertAdds(friendList, f1);
        assertTrue(friendList.contains(f1));
        assertEquals(f1, friendList.getFileDescForIndex(0).getFile());
        assertEquals(friendList.size(), 1);
        assertTrue(friendList.iterator().hasNext());
        assertTrue(friendList.pausableIterable().iterator().hasNext());
    }
    
    /**
     * Tests adding a FileDesc to friend list that is already managed
     */
    public void testAddFileDesc() throws Exception {
        assertEquals(friendList.size(), 0);
        
        f1 = createNewTestFile(1, _scratchDir);
        
        assertAdds(managedList, f1);
        
        FileDesc fd = managedList.getFileDesc(f1);
        assertTrue(friendList.add(fd));
        
        assertTrue(friendList.contains(fd));
        assertEquals(fd, friendList.getFileDesc(f1));
        assertEquals(friendList.size(), 1);
        assertTrue(friendList.iterator().hasNext());
        assertTrue(friendList.pausableIterable().iterator().hasNext());
    }

    /**
     * Tests that an IncompleteFileDesc cannot be added to this FriendList
     */
    public void testAddIncompleteFileDesc() throws Exception {
        assertEquals(friendList.size(), 0);
        
        IncompleteFileDesc ifd = new IncompleteFileDescStub();

        assertFalse(friendList.add(ifd));
        
        assertFalse(friendList.contains(ifd));
        assertEquals(friendList.size(), 0);
        assertFalse(friendList.iterator().hasNext());
        assertFalse(friendList.pausableIterable().iterator().hasNext());        
    }
    
    /**
     * Test removing a single file from friendList.
     */
    public void testRemoveFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        
        assertFalse(friendList.remove(f1));
        
        assertAdds(friendList, f1);
        assertEquals(f1, friendList.getFileDescForIndex(0).getFile());
        assertEquals(1, friendList.size());
        
        assertTrue(friendList.contains(f1));
        assertTrue(friendList.remove(f1));
        assertNull(friendList.getFileDescForIndex(0));
        assertEquals(0, friendList.size());
        assertFalse(friendList.contains(f1));
        assertFalse(friendList.iterator().hasNext());
        assertFalse(friendList.pausableIterable().iterator().hasNext());        
    }
    
    /**
     * Test removing a FileDesc from a friendList.
     */
    public void testRemoveFileDesc() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        
        assertFalse(friendList.remove(f1));
        
        assertAdds(friendList, f1);
        assertEquals(f1, friendList.getFileDescForIndex(0).getFile());
        assertEquals(1, friendList.size());
        
        FileDesc fd = friendList.getFileDesc(f1);
        
        assertTrue(friendList.contains(fd));
        
        assertTrue(friendList.remove(fd));
        assertNull(friendList.getFileDescForIndex(0));
        assertEquals(0, friendList.size());        
        assertFalse(friendList.contains(fd));
    }
    
//    public void testAddFolder() throws Exception {
//        assertEquals(friendList.size(), 0);
//        
//        f1 = createNewExtensionTestFile(1, "wav", _scratchDir);
//        f2 = createNewExtensionTestFile(3, "wav", _scratchDir);
//        
//        friendList.addFolder(_scratchDir);
//        List<FileDesc> fdList;
//        
//        fdList = assertAddsFolder(managedList, _scratchDir);
//        Thread.sleep(4000);
//        assertContainsFiles(fdList, f1, f2);
//        assertContainsFiles(managedList, f1, f2);
//        assertAddsFolder(friendList, _scratchDir);
//
//        assertEquals(2, friendList.size());
//        assertEquals(f1, friendList.getFileDescForIndex(0).getFile());
//        assertEquals(f2, friendList.getFileDescForIndex(1).getFile());
//        assertTrue(friendList.iterator().hasNext());
//        assertTrue(friendList.pausableIterable().iterator().hasNext());
//    }
    
//    /**
//     * Tests that smart share categories are mutually
//     * exclusive
//     */
//    public void testSmartSharingCategories() {
//        
//        assertFalse(friendList.isAddNewAudioAlways());
//        assertFalse(friendList.isAddNewImageAlways());
//        assertFalse(friendList.isAddNewVideoAlways());
//        
//        friendList.setAddNewAudioAlways(true);
//    
//        assertTrue(friendList.isAddNewAudioAlways());
//        assertFalse(friendList.isAddNewImageAlways());
//        assertFalse(friendList.isAddNewVideoAlways());
//        
//        friendList.setAddNewImageAlways(true);
//        
//        assertTrue(friendList.isAddNewAudioAlways());
//        assertTrue(friendList.isAddNewImageAlways());
//        assertFalse(friendList.isAddNewVideoAlways());
//        
//        friendList.setAddNewVideoAlways(true);
//        
//        assertTrue(friendList.isAddNewAudioAlways());
//        assertTrue(friendList.isAddNewImageAlways());
//        assertTrue(friendList.isAddNewVideoAlways());
//    }
//    
//    /**
//     * Tests smart sharing a category
//     */
//    public void testSmartSharing() throws Exception {
//        assertEquals(friendList.size(), 0);
//        assertFalse(friendList.isAddNewAudioAlways());
//        
//        f1 = createNewExtensionTestFile(1, "wav", _scratchDir);
//        f2 = createNewExtensionTestFile(3, "txt", _scratchDir);
//        
//        assertAdds(managedList, f1);
//               
//        assertFalse(friendList.contains(f1));
//        assertEquals(0, friendList.size());
//
//        friendList.setAddNewAudioAlways(true);
//        assertTrue(friendList.isAddNewAudioAlways());
//        
//        Thread.sleep(500);
//        
//        assertEquals(1, friendList.size());
//        assertTrue(friendList.contains(f1));
//        assertFalse(friendList.contains(f2));
//        
//        f3 = createNewExtensionTestFile(5, "wav", _scratchDir);
//        assertAdds(managedList, f3);
//        
//        Thread.sleep(500);
//        
//        assertEquals(2, friendList.size());
//        assertTrue(friendList.contains(f3));
//    }
//    
//    /**
//     * Tests stopping smart sharing a category
//     */
//    public void testStopSmartSharing() throws Exception {
//        friendList.setAddNewAudioAlways(true);
//        assertTrue(friendList.isAddNewAudioAlways());
//        
//        
//        f1 = createNewExtensionTestFile(1, "wav", _scratchDir);
//        f2 = createNewExtensionTestFile(3, "txt", _scratchDir);
//        
//        assertAdds(friendList, f1);
//        assertAdds(friendList, f2);
//        
//        assertEquals(2, friendList.size());
//        
//        friendList.setAddNewAudioAlways(false);
//        assertFalse(friendList.isAddNewAudioAlways());
//        
//        Thread.sleep(500);
//        
//        assertEquals(2, friendList.size());
//        
//        f3 = createNewExtensionTestFile(5, "wav", _scratchDir);
//        assertAdds(managedList, f3);
//        
//        Thread.sleep(500);
//        
//        assertEquals(2, friendList.size());
//        assertFalse(friendList.contains(f3));
//    }
   
}
