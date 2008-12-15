package org.limewire.swarm.file.selection;

import java.util.NoSuchElementException;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmBlockSelector;

/**
 * This selection strategy downloads the data in order.
 */
public class ContiguousSelectionStrategy implements SwarmBlockSelector {

    public Range selectAssignment(IntervalSet candidateBytes, IntervalSet neededBytes,
            long blockSize) throws NoSuchElementException {

        // Input validation
        if (blockSize < 1)
            throw new IllegalArgumentException("Block size cannot be " + blockSize);
        if (candidateBytes.isEmpty())
            throw new NoSuchElementException();

        Range candidate = candidateBytes.getFirst();

        // Calculate what the high byte offset should be.
        // This will be at most blockSize-1 bytes greater than the low.
        long alignedHigh = alignHigh(candidate.getLow(), blockSize);

        // alignedHigh >= candidate.low, and therefore we
        // only have to check if alignedHigh > candidate.high.
        if (alignedHigh > candidate.getHigh())
            alignedHigh = candidate.getHigh();

        // Our ideal interval is [candidate.low, alignedHigh]

        // Optimize away creation of new objects, if possible
        Range ret = candidate;
        if (ret.getHigh() != alignedHigh)
            ret = Range.createRange(candidate.getLow(), alignedHigh);

        return ret;
    }

    /** Aligns location to one byte before the next highest block boundary */
    protected long alignHigh(long location, long blockSize) {
        location += blockSize;
        location -= location % blockSize;
        return location - 1;
    }

}
