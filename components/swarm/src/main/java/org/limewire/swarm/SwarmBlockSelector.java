package org.limewire.swarm;

import java.util.NoSuchElementException;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

public interface SwarmBlockSelector {

    /**
     * Encapsulates an algorithm for deciding which block of a file to download next.  
     * 
     * For efficiency reasons attempts will be made to align the start and end of intervals
     * to block boundaries.  However, there are no guarantees on alignment.
     * 
     * @param availableRanges a representation of the set of 
     *      ranges that are candidates for downloading.  These are the
     *      ranges of the file that a given for download from a given server, minus
     *      the set of ranges that we already have (or have assigned)
     * @param neededRanges a representation of the set of ranges
     *      of the file that have not yet been leased, verified, etc.
     * @param blockSize the maximum size of the returned Range. Any values less than 1 will
     *      be ignored.  The returned Range will in no case span a blockSize boundary.
     *      Any values less than 1 will generate IllegalArgumentExceptions.
     * @return the Range that should be assigned next, which does not span a blockSize boundary
     * @throws NoSuchElementException if passed an empty IntervalSet
     */
    Range selectAssignment(IntervalSet availableRanges, IntervalSet neededRanges, long blockSize);

}
