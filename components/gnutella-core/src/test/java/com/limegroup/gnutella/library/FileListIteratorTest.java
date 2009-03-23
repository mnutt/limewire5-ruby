package com.limegroup.gnutella.library;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.limewire.util.BaseTestCase;
import org.limewire.collection.IntSet;
import com.limegroup.gnutella.URN;
import junit.framework.Test;

/**
 * Unit test for {@link FileListIterator}
 *
 */
public class FileListIteratorTest extends BaseTestCase {

    private ManagedFileListStub managedList;
    private IntSet indices;

    private URN hash1, hash2, hash3, hash4;
    private FileDesc fd1, fd2, fd3, fd4;

    public FileListIteratorTest(String name) {
        super(name);
        this.managedList = new ManagedFileListStub();
        this.indices = new IntSet();
    }


    @Override
    protected void setUp() throws Exception {

        // create FileDescs which are used in every test
        hash1 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSASUSH");
        hash2 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSANITA");
        hash3 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQABOALT");
        hash4 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5BERKELEY");

        fd1 = new FileDescStub("0", hash1, 0);
        fd2 = new FileDescStub("1", hash2, 1);
        fd3 = new FileDescStub("2", hash3, 2);
        fd4 = new FileDescStub("3", hash4, 3);


    }

    public static Test suite() {
        return buildTestSuite(FileListIteratorTest.class);
    }


    public void testEmptyList() throws Exception {
        assertEquals(0, managedList.size());
        FileListIterator iter = new FileListIterator(managedList, indices);
        assertEquals(0, iterateHowMany(iter));
    }


    public void testAllIndicesInManagedFileList() throws Exception {

        managedList.add(fd1);
        managedList.add(fd2);
        managedList.add(fd3);
        managedList.add(fd4);

        indices.add(fd1.getIndex());
        indices.add(fd2.getIndex());
        indices.add(fd3.getIndex());
        indices.add(fd4.getIndex());

        FileListIterator iter = new FileListIterator(managedList, indices);
        assertEquals(4, iterateHowMany(iter));
    }

    public void testSomeIndicesNotInManagedFileList() throws Exception {
        managedList.add(fd1);
        managedList.add(fd2);
        managedList.add(fd3);
        managedList.add(fd4);

        indices.add(fd1.getIndex());
        indices.add(fd2.getIndex());
        indices.add(fd4.getIndex());

        FileListIterator iter = new FileListIterator(managedList, indices);
        assertEquals(3, iterateHowMany(iter));

        // test that iterating past hasNext() false yields a NoSuchElementException
        try {
            iter.next();
            fail("Expected a NoSuchElementException!");
        } catch (NoSuchElementException e) {
            // got expected exception!
        }

    }

    /**
     * iterate, returning the total iterated
     * and making sure none of the FileDesc objects are null
     *
     * @param iter Iterator to use
     * @return total number of FileDesc objects iterated thru
     */
    private int iterateHowMany(Iterator<FileDesc> iter) {
        int count = 0;

        while (iter.hasNext()) {
            FileDesc fd = iter.next();
            assertNotNull(fd);
            count++;
        }
        return count;
    }
}
