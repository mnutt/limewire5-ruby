package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.core.settings.SharingSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileCollection;

public class IncompleteFileManagerTest extends LimeTestCase {
    
    @Inject private IncompleteFileManager incompleteFileManager;
    private RemoteFileDesc rfd1, rfd2;
    @Inject private VerifyingFileFactory verifyingFileFactory;
    @Inject private Injector injector;
    @Inject private IncompleteFileCollection incompleteFileCollection;
    
    public IncompleteFileManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(IncompleteFileManagerTest.class);
    }
    
    @Override
    public void setUp() {
        LimeTestUtils.createInjector(LimeTestUtils.createModule(this));
    }

    /** @param urn a SHA1 urn, or null */
    public RemoteFileDesc newRFD(String name, int size, String urn) {
       try {
           Set<URN> urns=new HashSet<URN>(1);
           if (urn!=null) 
               urns.add(URN.createSHA1Urn(urn));
           return injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(new ConnectableImpl("18.239.0.144", 6346, false), 13l, name, size, new byte[16],
                56, 4, true, null, urns, false, "", -1);
       } catch (IOException e) {
           fail("Invalid URN", e);
           return null;
       }
    }

    /////////////////////////////////////////////////////////////

	public void testLegacy() throws Throwable {
        File file=new File(getSaveDirectory(), "T-748-test.txt");
        Iterator iter=null;
        VerifyingFile vf = verifyingFileFactory.createVerifyingFile(748);
        //0 blocks
        assertNull(incompleteFileManager.getEntry(file));
        assertEquals(0, incompleteFileManager.getBlockSize(file));
        //1 block
        vf.addInterval(Range.createRange(0,10));
        incompleteFileManager.addEntry(file,vf, true);
        assertEquals(11, incompleteFileManager.getBlockSize(file));//full inclusive now
        iter=incompleteFileManager.getEntry(file).getBlocks().iterator();
        assertEquals(Range.createRange(0, 10), iter.next());
        assertTrue(! iter.hasNext());
        
        SharingSettings.INCOMPLETE_PURGE_TIME.setValue(26);
        File young=new FakeTimedFile(25);
        File old=new FakeTimedFile(27);
        assertTrue(!isOld(young));
        assertTrue(isOld(old));
    }

    public void testTempName() throws Throwable {
        assertEquals("T-748-abc def",
                     tempName("abc def", 748, 0));
        assertEquals("T-748-abc def (2)",
                     tempName("abc def", 748, 2));
        assertEquals("T-748-abc.txt",
                     tempName("abc.txt", 748, 1));
        assertEquals("T-748-abc (2).txt",
                     tempName("abc.txt", 748, 2));
        assertEquals("T-748-.txt",
                     tempName(".txt", 748, 1));
        assertEquals("T-748- (2).txt",
                     tempName(".txt", 748, 2));
    }
    

    /** Different name or size ==> different temp file */
    public void testGetFile_DifferentSize() throws Throwable {
        rfd1=newRFD("some file name", 1839, null);
        rfd2=newRFD("some file name", 1223, null);
        assertTrue(! IncompleteFileManager.same(rfd1, rfd2));
        File tmp1=incompleteFileManager.getFile(rfd1);
        File tmp2=incompleteFileManager.getFile(rfd2);
        assertNotEquals(tmp2, tmp1);
    }

    /** 
     * You should be able to resume to a file with same hash but different name.
     */
    public void testGetFile_DifferentNameSameHash() throws Throwable {
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        rfd2=newRFD("another file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        assertTrue(IncompleteFileManager.same(rfd1, rfd2));
        File tmp1=incompleteFileManager.getFile(rfd1);
        File tmp2=incompleteFileManager.getFile(rfd2);
        assertEquals(tmp1, tmp2);
    }

    /** 
     * You should NOT be able to resume to file w/ same name but different hash.
     */
    public void testGetFile_SameNameDifferentHash() throws Throwable {
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        rfd2=newRFD("some file name", 1839, 
                    "urn:sha1:LSTHIPQGSGSZTS5FJPAKPZWUGYQYPFBU");
        assertTrue(! IncompleteFileManager.same(rfd1, rfd2));
        File tmp1=incompleteFileManager.getFile(rfd1);
        File tmp2=incompleteFileManager.getFile(rfd2);
        assertNotEquals(tmp2, tmp1);
    }

    /** Risky resumes are allowed: no hashes */
    public void testGetFile_NoHash() throws Throwable {
        rfd1=newRFD("some file name", 1839, null);
        rfd2=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        assertTrue(IncompleteFileManager.same(rfd1, rfd2));
        File tmp1=incompleteFileManager.getFile(rfd1);
        File tmp2=incompleteFileManager.getFile(rfd2);
        assertEquals(tmp1, tmp2);
    }

    /** Checks that removeEntry clears blocks AND hashes. */
    public void testRemoveEntry() throws Throwable {       
        //Populate IFM with a hash.
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB"); 
        File tmp1=incompleteFileManager.getFile(rfd1);
        VerifyingFile vf=verifyingFileFactory.createVerifyingFile(1839);
        incompleteFileManager.addEntry(tmp1, vf, true);

        //After deleting entry there should be no more blocks...
        incompleteFileManager.removeEntry(tmp1);      
        assertNull(incompleteFileManager.getEntry(tmp1));

        //...and same temp file should be used for different hash. [sic]
        rfd2=newRFD("some file name", 1839, 
                    "urn:sha1:LSTHIPQGSGSZTS5FJPAKPZWUGYQYPFBU");
        File tmp2=incompleteFileManager.getFile(rfd2);
        assertEquals(tmp1, tmp2);
        assertEquals(tmp2, incompleteFileManager.getFile(newRFD("some file name", 1839, null)));
    }
    
    /**
     * Checks that addEntry & removeEntry notify the FileManager of the
     * added / removed incomplete file.
     */
    public void testFileManagerIsNotified() throws Exception {
        assertEquals(0, incompleteFileCollection.size()); // begin with 0 shared.
        
        //Populate IFM with a hash.
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File tmp1=incompleteFileManager.getFile(rfd1);
        VerifyingFile vf=verifyingFileFactory.createVerifyingFile(1839);
        incompleteFileManager.addEntry(tmp1, vf, true);
        
        assertEquals(1, incompleteFileCollection.size()); // 1 added.
        
        // make sure it's associated with a URN.
        URN urn = URN.createSHA1Urn(    
            "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        FileDesc fd = incompleteFileCollection.getFileDesc(urn);
        assertNotNull(urn);
        assertInstanceof(com.limegroup.gnutella.library.IncompleteFileDesc.class, fd);
        
        incompleteFileManager.removeEntry(tmp1);
        
        assertEquals(0, incompleteFileCollection.size()); // back to 0 shared.
    }   

    public void testCompletedHash_NotFound() throws Throwable{
        File tmp2=new File("T-1839-some file name");
        assertNull(incompleteFileManager.getCompletedHash(tmp2));
    }

    public void testCompletedHash_Found() throws Throwable {
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File tmp1=incompleteFileManager.getFile(rfd1);
        try {
            URN urn=URN.createSHA1Urn( 
                "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
            assertEquals(urn, incompleteFileManager.getCompletedHash(tmp1));
        } catch (IOException e) {
            fail("Couldn't make URN", e);
        }
    }

    public void testCompletedName() throws Throwable {
        File tmp1=new File("T-1839-some file.txt");
        assertEquals("some file.txt", IncompleteFileManager.getCompletedName(tmp1));
    }

    public void testCompletedName_IllegalArgument() throws Throwable {
        try {
            IncompleteFileManager.getCompletedName(new File("no dash.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            IncompleteFileManager.getCompletedName(new File("T-one dash.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            IncompleteFileManager.getCompletedName(new File("T-123-"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }
    }

    public void testCompletedSize() throws Throwable {
        File tmp1=new File("T-1839-some file.txt");
        assertEquals(1839, IncompleteFileManager.getCompletedSize(tmp1));
    }

    public void testCompletedSize_IllegalArgument() throws Throwable {
        try {
            IncompleteFileManager.getCompletedSize(new File("no dash.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            IncompleteFileManager.getCompletedSize(new File("T-one dash.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            IncompleteFileManager.getCompletedSize(new File("T--no number.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            IncompleteFileManager.getCompletedSize(new File("T-x-bad number.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }
    }

    /** Tests that hash information is purged when calling purge(true). */
    public void testPurgeHash_Yes() throws Throwable {
        //These files have the same hash, but no blocks have been written.
        RemoteFileDesc rfd1=newRFD("file name.txt", 1839, 
                                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        RemoteFileDesc rfd1b=newRFD("other file.txt", 1839, 
                                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File file1=incompleteFileManager.getFile(rfd1);
        file1.delete(); // getFile will create it, we don't want it created.
        File file1b=incompleteFileManager.getFile(rfd1b);
        file1b.delete();
        assertEquals(file1, file1b);

        //These files have the same hash, but blocks have been written to disk.
        RemoteFileDesc rfd2=newRFD("another name.txt", 1839, 
                                   "urn:sha1:LSTHIPQGSSZGTS5FJUPAKPZWUGYQYPFB");
        RemoteFileDesc rfd2b=newRFD("yet another file.txt", 1839, 
                                   "urn:sha1:LSTHIPQGSSZGTS5FJUPAKPZWUGYQYPFB");
        File file2=incompleteFileManager.getFile(rfd2);
        file2.delete();
        File file2b=incompleteFileManager.getFile(rfd2b);
        file2b.delete();
        assertEquals(file2, file2b);
        try {
            file2.createNewFile();
        } catch (IOException e) {
            fail("Couldn't create "+file2, e);
        }

        //After purging, only hashes associated with files that exists remain.
        List<File> emptyFiles = Collections.emptyList();
        incompleteFileManager.initialPurge(emptyFiles);
        File file1c=incompleteFileManager.getFile(rfd1b);
        assertNotEquals(file1b, file1c);
        File file2c=incompleteFileManager.getFile(rfd2b);
        assertEquals(file2b ,file2c);
        file2.delete();
    }

    /** Tests that hash information is not purged when calling purge(false). */
    public void testPurgeHash_No() throws Throwable {
        RemoteFileDesc rfd1=newRFD("file name.txt", 1839, 
                                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        RemoteFileDesc rfd2=newRFD("other file.txt", 1839, 
                                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File file1=incompleteFileManager.getFile(rfd1);
        File file2=incompleteFileManager.getFile(rfd2);
        assertEquals(file1, file2);
        incompleteFileManager.purge();             //Does nothing
        File file2b=incompleteFileManager.getFile(rfd2);
        assertEquals(file2, file2b);
    }

	private static String tempName(String s, int one, int two) throws Throwable {
	    try {
            return (String)PrivilegedAccessor.invokeMethod(
                IncompleteFileManager.class, "tempName", 
                new Object[] {s, new Long(one), new Integer(two)},
                new Class[] {String.class, Long.TYPE, Integer.TYPE});
        } catch(Exception e) {
            if ( e.getCause() != null ) 
                throw e.getCause();
            else throw e;
        }
    }
    
	private static boolean isOld(File f) throws Throwable {
	    try {
            return ((Boolean)PrivilegedAccessor.invokeMethod(
                IncompleteFileManager.class, "isOld", 
                new Object[] {f},
                new Class[] {File.class} )).booleanValue();
        } catch(Exception e) {
            if ( e.getCause() != null ) 
                throw e.getCause();
            else throw e;
        }
    }    
    
    static class FakeTimedFile extends File {
        private long days;
        FakeTimedFile(int days) {
            super("whatever.txt");
            this.days=days;
        }

        @Override
        public long lastModified() {
            //30 days ago
            return System.currentTimeMillis()-days*24l*60l*60l*1000l;
        }
    }       
}
