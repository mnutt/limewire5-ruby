package com.limegroup.gnutella;


import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileDescFactory;
import com.limegroup.gnutella.library.IncompleteFileDesc;

@SuppressWarnings("unchecked")
public class IncompleteFileDescTest extends LimeTestCase {
    
    private IncompleteFileDesc ifd;
    private String fileName = "ifd.txt";
    private URN urn;
    private Set urns;
    private VerifyingFile vf;
    private VerifyingFileFactory verifyingFileFactory;
    private FileDescFactory fileDescFactory;

    public IncompleteFileDescTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(IncompleteFileDescTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
       
        urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        urns = new HashSet(1);
        urns.add(urn);
        
        Injector injector = LimeTestUtils.createInjector();
        verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
        fileDescFactory = injector.getInstance(FileDescFactory.class);
        vf = verifyingFileFactory.createVerifyingFile();
        
        ifd = fileDescFactory.createIncompleteFileDesc(
            new File(fileName),
            urns,
            0,
            fileName,
            1981, vf);
		
    }
    
    public void testGetAvailableRanges() throws Exception {
        // no ranges now.
        assertEquals("bytes", ifd.getAvailableRanges());
        
        // add a small range and ensure it doesn't get listed.
        Range small = Range.createRange(0);
        addInterval(small);
        assertEquals(ifd.getAvailableRanges(),
            "bytes", ifd.getAvailableRanges());
        
        Range notLargeEnough = Range.createRange(0, 102398);
        addInterval(notLargeEnough);
        assertEquals(ifd.getAvailableRanges(),
            "bytes", ifd.getAvailableRanges());
        
        // extend from the middle ...
        Range extended = Range.createRange(102300, 102500);
        addInterval(extended);
        assertEquals(ifd.getAvailableRanges(),
            "bytes 0-102499", ifd.getAvailableRanges());
        
        // add one not connected ...
        Range other = Range.createRange(102550, 204950);
        addInterval(other);
        assertEquals(ifd.getAvailableRanges(),
            "bytes 0-102499, 102550-204949", ifd.getAvailableRanges());
    }
    
    public void testLoadResponseRanges() throws Exception {
        Range small = Range.createRange(0, 200000);
        addUnverifiedInterval(small);
        
        urns.add(UrnHelper.TTROOT);
        ifd = fileDescFactory.createIncompleteFileDesc(
                new File(fileName),
                urns,
                0,
                fileName,
                1981, vf);
        
        IntervalSet i = new IntervalSet();
        assertFalse(ifd.loadResponseRanges(i));
        assertEquals(1,i.getNumberOfIntervals());
        assertEquals(0,i.getFirst().getLow());
        assertEquals(200000,i.getFirst().getHigh());
        
        // now add some verified ranges
        Range verified = Range.createRange(300000, 500000);
        addInterval(verified);
        
        // should only return the verified range
        i = new IntervalSet();
        assertTrue(ifd.loadResponseRanges(i));
        assertEquals(1,i.getNumberOfIntervals());
        assertEquals(300000,i.getFirst().getLow());
        assertEquals(500000,i.getFirst().getHigh());
    }
    
    public void testisRangeSatisfiable() throws Exception {
        // no ranges.
        

           assertFalse( ifd.isRangeSatisfiable(0, 0) );   
        assertFalse( ifd.isRangeSatisfiable(0, 150) );
        
        // add a range.
        Range small = Range.createRange(0);
        addInterval(small);
        assertTrue( ifd.isRangeSatisfiable(0, 0) );
        
        Range medium = Range.createRange(0, 102399);
        addInterval(medium);
        assertTrue( ifd.isRangeSatisfiable(0, 102399) );
        assertTrue( ifd.isRangeSatisfiable(50,100000) );
        assertFalse( ifd.isRangeSatisfiable(0, 102401) );
        assertFalse( ifd.isRangeSatisfiable( 102399, 102401) );
        assertFalse( ifd.isRangeSatisfiable(102400, 102500) );
        
        // extend from the middle ...
        Range extended = Range.createRange(102300, 102500);
        addInterval(extended);
        assertTrue( ifd.isRangeSatisfiable(0, 102500) );
        assertFalse( ifd.isRangeSatisfiable(1,102501) );
        
        // add one not connected ...
        Range other = Range.createRange(102550, 204950);
        addInterval(other);
        assertTrue( ifd.isRangeSatisfiable( 102550, 204950) );
        assertFalse( ifd.isRangeSatisfiable(102399, 102550) );
    }
    
    private void addInterval(Range i) throws Exception {
        IntervalSet set = (IntervalSet)
            PrivilegedAccessor.getValue(vf,"verifiedBlocks");
        
        set.add(i);
    }
    
    private void addUnverifiedInterval(Range i) throws Exception {
        IntervalSet set = (IntervalSet)
            PrivilegedAccessor.getValue(vf,"partialBlocks");
        
        set.add(i);
    }
}
        