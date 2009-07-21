package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAddFails;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAdds;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertContainsFiles;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileChangedFails;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileChanges;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileRenameFails;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileRenames;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertLoads;
import static com.limegroup.gnutella.library.FileManagerTestUtils.change;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createFakeTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createHiddenTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewNamedTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createPartialCopy;
import static com.limegroup.gnutella.library.FileManagerTestUtils.getUrn;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.settings.ContentSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.StubContentResponseObserver;
import com.limegroup.gnutella.auth.UrnValidator;
import com.limegroup.gnutella.messages.vendor.ContentResponse;

public class LibraryTest extends LimeTestCase {

    private LibraryImpl fileList;
    private UrnValidator urnValidator;
    private Injector injector;

    private File f1, f2, f3, f4, f5;
    private List<FileDesc> fds;

    public LibraryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LibraryTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        fileList = (LibraryImpl) injector.getInstance(Library.class);
        urnValidator = injector.getInstance(UrnValidator.class);
        fileList.initialize();
    }

    public void testNoManagedFiles() {
        assertEquals(0, fileList.size());
        assertFalse(fileList.iterator().hasNext());
        assertFalse(fileList.pausableIterable().iterator().hasNext());
    }

    public void testContentManagerActive() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        f4 = createNewTestFile(23, _scratchDir);

        URN u1 = getUrn(f1);
        URN u2 = getUrn(f2);
        URN u3 = getUrn(f3);
        URN u4 = getUrn(f4);

        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);
        ContentManager cm = injector.getInstance(ContentManager.class);
        cm.start();

        // request the urn so we can use the response.
        cm.request(u1, new StubContentResponseObserver(), 1000);
        cm.handleContentResponse(new ContentResponse(u1, false));

        assertAddFails(FileViewChangeFailedException.Reason.INVALID_URN, fileList, f1);
        assertAdds(fileList, f2, f3, f4);

        assertEquals("unexpected # of files", 3, fileList.size());
        assertFalse(fileList.contains(f1));
        assertTrue(fileList.contains(f2));
        assertTrue(fileList.contains(f3));
        assertTrue(fileList.contains(f4));

        // test invalid content response.
        urnValidator.validate(u2);
        cm.handleContentResponse(new ContentResponse(u2, false));
        assertFalse(fileList.contains(f2));
        assertEquals(2, fileList.size());

        // test valid content response.
        urnValidator.validate(u3);
        cm.handleContentResponse(new ContentResponse(u3, true));
        assertTrue(fileList.contains(f3));
        assertEquals(2, fileList.size());

        // test valid content response.
        urnValidator.validate(u4);
        Thread.sleep(10000);
        assertTrue(fileList.contains(f4));
        assertEquals(2, fileList.size());

        // Make sure adding a new file to be shared doesn't work if it
        // returned bad before.
        assertAddFails(FileViewChangeFailedException.Reason.INVALID_URN, fileList, f2);
        assertFalse("shouldn't be shared", fileList.contains(f2));
    }
    
    public void testOneManagedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1);
        assertEquals(1, fileList.size());
        assertFalse(fileList.remove(f3));
        assertEquals(f1, fileList.getFileDescForIndex(0).getFile());
        
        assertLoads(fileList);
        assertEquals(1, fileList.size());
        assertEquals(f1, fileList.getFileDescForIndex(0).getFile());
    }

    public void testRemovingOneFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f2);

        // Remove file that's shared. Back to 1 file.
        assertEquals(2, fileList.size());
        assertFalse(fileList.remove(f3));
        assertTrue(fileList.remove(f2));
        assertEquals(1, fileList.size());
        
        assertLoads(fileList);
        assertEquals(1, fileList.size());
        assertTrue(fileList.contains(f1));
    }

    public void testAddAnotherFileDifferentIndex() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);

        assertAdds(fileList, f1, f2);
        assertTrue(fileList.remove(f2));
        assertAdds(fileList, f3);

        assertEquals(2, fileList.size());
        assertNotNull(fileList.getFileDescForIndex(0));
        assertNotNull(fileList.getFileDescForIndex(2));
        assertNull(fileList.getFileDescForIndex(1));
        
        fds = CollectionUtils.listOf(fileList);
        assertContainsFiles(fds, f1, f3);
        
        assertLoads(fileList);
        fds = CollectionUtils.listOf(fileList);
        assertContainsFiles(fds, f1, f3);        
    }

    public void testRenameFiles() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f3);
        assertEquals(2, fileList.size());        
        
        assertFileRenameFails(FileViewChangeFailedException.Reason.NOT_MANAGEABLE, fileList, f1, new File(_scratchDir, "!<invalid file>"));
        assertEquals(1, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f3);
        
        assertFileRenames(fileList, f3, f2);
        assertEquals(1, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f2);

        assertFileRenameFails(FileViewChangeFailedException.Reason.OLD_WASNT_MANAGED, fileList, f1, f3);
        
        assertLoads(fileList);
        assertEquals(1, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f2);        
    }

    public void testChangeFile() throws Exception {
        f1 = createNewNamedTestFile(100, "name", _scratchDir);
        f2 = createNewTestFile(10, _scratchDir);
        assertAdds(fileList, f1);
        assertEquals(1, fileList.size());
        
        FileDesc fd = fileList.getFileDesc(f1);
        URN urn = fd.getSHA1Urn();
        assertSame(fd, fileList.getFileDescsMatching(urn).get(0));
        
        change(f1);
        assertFileChanges(fileList, f1);
        assertEquals(1, fileList.size());
        assertNotEquals(urn, fileList.getFileDesc(f1).getSHA1Urn());
        assertNotSame(fd, fileList.getFileDesc(f1));
        assertNotSame(fd, fileList.getFileDescsMatching(fileList.getFileDesc(f1).getSHA1Urn()).get(0));
        
        f1.delete();
        assertFileChangedFails(FileViewChangeFailedException.Reason.NOT_MANAGEABLE, fileList, f1);
        assertEquals(0, fileList.size());
        
        assertFileChangedFails(FileViewChangeFailedException.Reason.OLD_WASNT_MANAGED, fileList, f2);
        assertEquals(0, fileList.size());
        
        assertLoads(fileList);
        assertEquals(0, fileList.size());     
    }

    public void testIgnoreHugeFiles() throws Exception {
        long maxSize = 0xFFFFFFFFFFL; // 1TB.

        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(11, _scratchDir);
        assertAdds(fileList, f1, f2);

        // Try to add a huge file. (It will be ignored.)
        f3 = createFakeTestFile(maxSize + 1l, _scratchDir);
        assertAddFails(FileViewChangeFailedException.Reason.NOT_MANAGEABLE, fileList, f3);
        
        // Add really big files.
        f4 = createFakeTestFile(maxSize - 1, _scratchDir);
        f5 = createFakeTestFile(maxSize, _scratchDir);
        assertAdds(fileList, f4, f5);
        
        assertEquals(4, fileList.size());
        
        assertLoads(fileList);
        assertEquals(4, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f1, f2, f4, f5);
    }

    public void testIgnoreHiddenFiles() throws Exception {
        // Create some ordinary files and add them
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(1, _scratchDir);
        assertAdds(fileList, f1, f2);
        assertEquals(2, fileList.size());

        // Try to add a hidden file
        f3 = createHiddenTestFile(1, _scratchDir);
        assertAddFails(FileViewChangeFailedException.Reason.NOT_MANAGEABLE, fileList, f3);
        assertEquals(2, fileList.size());
        
        assertLoads(fileList);
        assertEquals(2, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f1, f2);
    }

    public void testIgnoreMisnamedFiles() throws Exception {
        // Create some ordinary files and add them
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(1, _scratchDir);
        assertAdds(fileList, f1, f2);
        assertEquals(2, fileList.size());

        // Try to add a misnamed file - it's an ASF but claims to be a FOO.
        // The test file is in the public domain:
        // http://www.archive.org/details/DovKaplanKolNidreKolNidrewma
        File asf =
            TestUtils.getResourceFile("com/limegroup/gnutella/resources/Kol_Nidre.wma");
        f3 = createPartialCopy(asf, "foo", _scratchDir, 1024);
        assertAddFails(FileViewChangeFailedException.Reason.DANGEROUS_FILE, fileList, f3);
        assertEquals(2, fileList.size());
        
        assertLoads(fileList);
        assertEquals(2, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f1, f2);
    }
    
    public void testPausableIterator() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f2);

        assertEquals(2, fileList.size());
        Iterator<FileDesc> it = fileList.pausableIterable().iterator();
        FileDesc fd = it.next();
        assertEquals(fd.getFileName(), f1.getName());
        fd = it.next();
        assertEquals(fd.getFileName(), f2.getName());
        assertFalse(it.hasNext());
        try {
            fd = it.next();
            fail("Expected NoSuchElementException, got: " + fd);
        } catch (NoSuchElementException expected) {}

        it = fileList.pausableIterable().iterator();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        it.next();
        fileList.remove(f2);
        assertFalse(it.hasNext());

        it = fileList.pausableIterable().iterator();
        fd = it.next();
        assertFalse(it.hasNext());
        
        assertAdds(fileList, f3);
        it = fileList.pausableIterable().iterator();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        assertLoads(fileList);
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("should have thrown");
        } catch(NoSuchElementException expected) {}
    }
    
}
