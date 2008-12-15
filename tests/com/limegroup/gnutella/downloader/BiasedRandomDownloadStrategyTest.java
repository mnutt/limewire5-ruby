package com.limegroup.gnutella.downloader;


import java.util.Random;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.SystemUtils;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;


public class BiasedRandomDownloadStrategyTest extends LimeTestCase {
    
    /** Smallest number that can be subtracted from 1.0f 
     * and get something different from 1.0f.  Note that
     * this differs slightly from the standard IEE 754
     * definition of EPSILON by using subtraction instead
     * of addition.
     */
    private static float EPSILON;
    static {
        float epsilon = 1.0f;
        while((1.0f-epsilon)!=1.0f) {
            EPSILON = epsilon;
            epsilon /= 2.0f;
        }
    }
    
    private long fileSize;

    private long blockSize;

    private TestPredeterminedRandom prng;

    private SelectionStrategy strategy;

    private IntervalSet availableBytes;
    
    public BiasedRandomDownloadStrategyTest(String s) {
        super(s);
    }
    
    public static Test suite() {
        return buildTestSuite(BiasedRandomDownloadStrategyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setUp() throws Exception {
        fileSize = 1024 * 1024 * 5; // 5MB
        blockSize = 128 * 1024; // 128k
        prng = new TestPredeterminedRandom();
        strategy = createBiasedRandomStrategy(fileSize, prng);
        availableBytes = IntervalSet.createSingletonSet(0, fileSize - 1);
    }

    /////////////// Tests /////////////////////////////////
    /** Test behavior for a file just over MIN_PREVIEW_BYTES (1 MB).
     *  Make sure downloads are sequential before MIN_PREVIEW_BYTES,
     *  and then never sequential after MIN_PREVIEW_BYTES.
     */
    public void testSmallFile() throws Exception {
        assertEquals("Disabling of idle time has not worked.  This test cannot pass.",
                0L, SystemUtils.getIdleTime());
        long previewLimit = ((Integer)
                PrivilegedAccessor.getValue(strategy,"MIN_PREVIEW_BYTES")
                ).longValue(); // 1 MB
        fileSize = previewLimit + 2 * blockSize + 7;
        availableBytes = IntervalSet.createSingletonSet(0, fileSize-1);
        // Cut out everything below 1MB, except 2 blocks at the beginning
        // and end of that range.
        availableBytes.delete(Range.createRange(2*blockSize-7, previewLimit-blockSize-2));
        // 4 arbitrary floats for our 4 sequential downloads... make sure to test
        // 1.0 and 0.0
        float[] floats = {1.0f-EPSILON, 0.0f, 0.3728f, 0.18281828f};
        prng.setFloats(floats);
        
        strategy = createBiasedRandomStrategy(fileSize, prng);
        Range[] expectations = new Range[4];
        // full block chunk
        expectations[0] = Range.createRange(0, blockSize-1);
        // partial block chunk
        expectations[1] = Range.createRange(blockSize, 2*blockSize-8);
        // Skip a few...
        // single byte chunk
        expectations[2] = Range.createRange(previewLimit-blockSize-1);
        // full block
        expectations[3] = Range.createRange(previewLimit-blockSize,
                previewLimit-1);
        
        // Check that we got our 4 sequential downloads
        testAssignments(strategy, availableBytes, fileSize,
                blockSize, expectations);
        
        // Now test that we go into non-sequential downloads
        // Set the float so that we get a non-sequential download durring the
        // 50% random phase.
        prng.setFloat(0.500001f);
        // Set the random index location and a non-sequential random location
        prng.setInt(3);
        prng.setLong(1);
        Range assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        // Check that it wasn't a sequential download
        assertNotEquals("Got a sequential download when it should have been random", 
                previewLimit, assignment.getLow());
        
        // Check that we cannot still force a sequential download, since the preview
        // length is now greater than 50%
        prng.setFloat(0.0001f);
        prng.setInt(7);
        prng.setLong(1);
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        assertNotEquals("Expected non-sequential assignment",
                availableBytes.getFirst().getLow(),
                assignment.getLow());
    }
    
    /** Test behavior for a file just over twice MIN_PREVIEW_BYTES.
     *  Make sure downloads are sequential before MIN_PREVIEW_BYTES, 
     *  50% random after MIN_PREVIEW_BYTES, and 100% random after the
     *  first 50% of the file has been downloaded.
     */
    public void testMediumFile() throws Exception {
        assertEquals("Disabling of idle time has not worked.  This test cannot pass.",
                0L, SystemUtils.getIdleTime());
        long previewLimit = ((Integer)
                PrivilegedAccessor.getValue(strategy,"MIN_PREVIEW_BYTES")
                ).longValue(); // 1 MB
        fileSize = 2*previewLimit + 3;
        availableBytes = IntervalSet.createSingletonSet(0, fileSize-1);
        // Cut out everything below 1MB, except 2 blocks at the beginning
        // and end of that range.
        availableBytes.delete(Range.createRange(2*blockSize-7, previewLimit-blockSize-2));
        // 4 arbitrary floats for our 4 sequential downloads... make sure to test
        // 1.0 and 0.0
        float[] floats = {1.0f-EPSILON, 0.6298f, 0.77f, 0.0f};
        prng.setFloats(floats);
        
        strategy = createBiasedRandomStrategy(fileSize, prng);
        Range[] expectations = new Range[4];
        // full block chunk
        expectations[0] = Range.createRange(0, blockSize-1);
        // partial block chunk
        expectations[1] = Range.createRange(blockSize, 2*blockSize-8);
        // Skip a few...
        // single byte chunk
        expectations[2] = Range.createRange(previewLimit-blockSize-1);
        // full block
        expectations[3] = Range.createRange(previewLimit-blockSize,
                previewLimit-1);
        
        // Check that we got our 4 sequential downloads
        testAssignments(strategy, availableBytes, fileSize,
                blockSize, expectations);
        
        // Now test that we go into non-sequential downloads
        // Set the float so that we get a non-sequential download durring the
        // 50% random phase.
        prng.setFloat(0.500001f);
        // Set the random index location and a non-sequential random location
        prng.setInt(3);
        prng.setLong(1);
        Range assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        // Check that it wasn't a sequential download
        assertNotEquals("Got a sequential download when it should have been random.", 
                previewLimit, assignment.getLow());
        
        // Check that we still force a sequential download while the preview
        // length is less than 50%
        prng.setFloat(0.5f-EPSILON);
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        assertEquals("Expected sequential assignment.",
                availableBytes.getFirst().getLow(),
                assignment.getLow());
        assertEquals("Expected a full block assignment",
                blockSize, assignment.getHigh()-assignment.getLow()+1);
        availableBytes.delete(assignment);
        
        // This last download pushed us over 50% previewable.
        // Test that we can no longer force the download to be non-random
        prng.setFloat(1.0f-EPSILON); // Nearly highest probability of being sequential
        prng.setInt(5);  // index into random locations array
        prng.setLong(1); // random location block offset
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        assertNotEquals("Expected non-sequential assignment",
                availableBytes.getFirst().getLow(),
                assignment.getLow());
        assertEquals("Expected a full block assignment",
                blockSize, assignment.getHigh()-assignment.getLow()+1);
    }
    
    /** Test behavior for a file where fileSize * MIN_PREVIEW_FRACTION
     * is just over MIN_PREVIEW_BYTES. 
     */
    public void testLargeFile() throws Exception {
        assertEquals("Disabling of idle time has not worked.  This test cannot pass.",
                0L, SystemUtils.getIdleTime());
        long minPreviewBytes = ((Integer)
                PrivilegedAccessor.getValue(strategy,"MIN_PREVIEW_BYTES")
                ).longValue(); // 1 MB
        double minPreviewFraction = ((Float)
                PrivilegedAccessor.getValue(strategy,"MIN_PREVIEW_FRACTION")
                ).doubleValue();
        fileSize = (long) ((minPreviewBytes / minPreviewFraction)+2*blockSize);
        
        // For this test, fileSize has been defined such that the preview limit
        // will be fileSize * minPreviewFraction
        long previewLimit = (long) Math.ceil(fileSize*minPreviewFraction);
        
        long alignedPreviewLimit = previewLimit + blockSize;
        alignedPreviewLimit -= alignedPreviewLimit % blockSize;
        
        availableBytes = IntervalSet.createSingletonSet(0, fileSize-1);
        // Cut out everything below previewFraction, except 2 blocks at the beginning
        // and end of that range.
        availableBytes.delete(Range.createRange(2*blockSize-7, 
                alignedPreviewLimit-blockSize-2));
        // 4 arbitrary floats for our 4 sequential downloads... make sure to test
        // 1.0 and 0.0
        float[] floats = {1.0f-EPSILON, 0.6298f, 0.77f, 0.0f};
        prng.setFloats(floats);
        
        strategy = createBiasedRandomStrategy(fileSize, prng);
        Range[] expectations = new Range[4];
        // full block chunk
        expectations[0] = Range.createRange(0, blockSize-1);
        // partial block chunk
        expectations[1] = Range.createRange(blockSize, 2*blockSize-8);
        // Skip a few...
        // single byte chunk
        expectations[2] = Range.createRange(alignedPreviewLimit-blockSize-1);
        // full block
        expectations[3] = Range.createRange(alignedPreviewLimit-blockSize,
                alignedPreviewLimit-1);
        
        // Check that we got our 4 sequential downloads
        testAssignments(strategy, availableBytes, fileSize,
                blockSize, expectations);
        
        // Now test that we go into non-sequential downloads
        // Set the float so that we get a non-sequential download durring the
        // 50% random phase.
        prng.setFloat(0.500001f);
        // Set the random index location and a non-sequential random location
        prng.setInt(3);
        prng.setLong(1);
        Range assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        // Check that it wasn't a sequential download
        assertNotEquals("Got a sequential download when it should have been random.", 
                previewLimit, assignment.getLow());
        
        // Check that we still force a sequential download while the preview
        // length is less than 50%
        prng.setFloat(0.5f-EPSILON);
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        assertEquals("Expected sequential assignment.",
                availableBytes.getFirst().getLow(),
                assignment.getLow());
        assertEquals("Expected a full block assignment",
                blockSize, assignment.getHigh()-assignment.getLow()+1);
        availableBytes.delete(assignment);
        
        // Fast-forward to the case where the download is 
        // over 50% previewable.
        
        availableBytes.delete(Range.createRange(0, 1+fileSize/2));
        
        // Test that we can no longer force the download to be non-random
        prng.setFloat(1.0f-EPSILON); // Nearly highest probability of being sequential
        prng.setInt(5);  // index into random locations array
        prng.setLong(1); // random location block offset
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        assertNotEquals("Expected non-sequential assignment",
                availableBytes.getFirst().getLow(),
                assignment.getLow());
        assertEquals("Expected a full block assignment",
                blockSize, assignment.getHigh()-assignment.getLow()+1);
    }
    
    /**
     * Test that the biased random downloader downloads in
     * random order if the user is idle, even if none of
     * the file has yet been downloaded.  Also test
     * that if the user becomes non-idle, the system
     * reverts to a sequential download if the number of
     * downloaded previewable bytes is low.
     */
    public void testIdle() throws Exception {
        /* Set up a strategy that thinks the user is idle */
        strategy = new BiasedRandomDownloadStrategy(fileSize) {
            private long idleTime;
            @Override
            protected long getIdleTime() {
                return idleTime;
            }
        };
        PrivilegedAccessor.setValue(strategy, "pseudoRandom", prng);
        PrivilegedAccessor.setValue(strategy, "idleTime", 
                new Long(BiasedRandomDownloadStrategy.MIN_IDLE_MILLISECONDS));
        prng.setLong(1);
        prng.setInt(1);
        
        Range assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        assertNotEquals("Idle download should not be sequential.", 
                0, assignment.getLow());
        
        /*  Make the user non-idle */
        PrivilegedAccessor.setValue(strategy, "idleTime", 
                new Long(BiasedRandomDownloadStrategy.MIN_IDLE_MILLISECONDS-1));
        /* Set prng for the highest probability of a random download */
        prng.setFloat(0.0f);
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes, blockSize);
        
        assertEquals("Non-idle download should be sequential if few bytes"+
                " have been assigned.", 0, assignment.getLow());
    }
    
    
    /**
     * Test that various invalid inputs throw IllegalArgumentException.
     */
    public void testInvalidInputs() {
        // Try an invalid block size
        try {
            strategy.pickAssignment(availableBytes,
                    availableBytes, 0);
            fail("Failed to complain about invalid block size");
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed... do nothing
        }
        
        IntervalSet badNeededBytes = null;
        
        try {
            // createSingletonSet might throw its own IllegalArgumentException
            // so create it outside of the try-catch
            badNeededBytes = IntervalSet.createSingletonSet(-5,10);
            // Try telling the strategy that we need some bytes
            // before the beginning of the file
            try {
                strategy.pickAssignment(availableBytes, badNeededBytes, blockSize);
                fail("Failed to complain about negative Intervals in neededBytes");
            } catch (IllegalArgumentException e) {
                // Wohoo!  Exception thrown... test passed... do nothing
            }
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed... do nothing
        }
        
        badNeededBytes = IntervalSet.createSingletonSet(fileSize,fileSize);
        // Try telling the strategy that we need a byte after the end
        // of the file
        try {
            strategy.pickAssignment(availableBytes,
                    badNeededBytes, blockSize);
            fail("Failed to complain about neededBytes extending past the end of the file");
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed... do nothing
        }
    }

    /**
     * Test that asking for bytes from an empty set results
     * in NoSuch ElementException.
     */
    public void testNoAvailableBytes() {
        // Call with empty set of candidateBytes
        try {
            strategy.pickAssignment(new IntervalSet(),
                    availableBytes, blockSize);
            fail("Failed to complain about no available bytes");
        } catch (java.util.NoSuchElementException e) {
            // Wohoo!  Exception thrown... test passed
        }
    }
    
    ///////////// Helper Methods //////////////////////////
    /**
     * A helper method that simulates chosing blocks to download and removes
     * them from availableBytes.
     * 
     * @param strategy selection strategy to be tested.
     * @param availableBytes is passed to strategy.
     * @param fileSize is passed to strategy.
     * @param blockSize is passed to strategy.
     * @param expectedAssignments are checked against the actual assignments, 
     *      in order.
     */
    private void testAssignments(SelectionStrategy strategy,
            IntervalSet availableBytes, long fileSize, long blockSize,
            Range[] expectedAssignments) {
        for (int i = 0; i < expectedAssignments.length; i++) {
            Range assignment = strategy.pickAssignment(availableBytes,
                    availableBytes, blockSize);
            availableBytes.delete(assignment);
            assertEquals("Wrong assignment for chunk #" + i,
                    expectedAssignments[i], assignment);
        }
    }
     
    /** Creates a BiasedRandomDownloadStrategy, and sets its private static Random. */
    private SelectionStrategy createBiasedRandomStrategy(long fileSize, Random rng)
            throws IllegalAccessException, NoSuchFieldException {
        BiasedRandomDownloadStrategy brds = new BiasedRandomDownloadStrategy(fileSize);
        PrivilegedAccessor.setValue(brds, "pseudoRandom", rng);
        return brds;
    }
}
