package com.limegroup.gnutella.downloader;


import java.util.Iterator;
import java.util.NoSuchElementException;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;


/** 
 * Selection strategy that selects the next available block.  
 * 
 * This is not well tested; it's just here as a testing stub. 
 */
public class TestSequentialStrategy implements SelectionStrategy {
    public Range pickAssignment(IntervalSet availableBytes,
            IntervalSet neededBytes,
            long blockSize) throws NoSuchElementException {
        
        // Construct test cases such that this won't result in getting the first element of an empty list
        Iterator intervalIterator = availableBytes.getAllIntervals();
        
        // If we don't need alignment, we're done
        if (blockSize < 1 && intervalIterator.hasNext())
            return (Range) intervalIterator.next();
        
        // Loop over intervals, looking for a suitable candidate
        while (intervalIterator.hasNext()) {
            Range candidate = (Range) intervalIterator.next();
            // align the high point of the interval
            long alignedHigh = candidate.getLow();
            // step ahead one block size
            alignedHigh += blockSize;
            // cut back to the block boundary
            alignedHigh -= (alignedHigh % blockSize);
            // step back one more byte
            alignedHigh -= 1;
            
            if (alignedHigh <= candidate.getHigh()) {
                return Range.createRange((int)candidate.getLow(), (int) alignedHigh);
            } else {
                return candidate;
            }
        }
        
        // All Intervals have been tested and found to be lacking
        throw new NoSuchElementException("blockSize:"+blockSize+" Intervals:"+availableBytes); 
    }
}
