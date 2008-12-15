package org.limewire.swarm.file.verifier;

import java.util.Collections;
import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmFileSystem;

/**
 * NoOp implmentation of the verifier. It does not find any verifiable ranges.
 * 
 */
public class NoOpFileVerifier implements SwarmBlockVerifier {

    /**
     * Always returns an empty list of verifiable ranges.
     */
    public List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize) {
        return Collections.emptyList();
    }

    /**
     * @throws UnsupportedOperationException
     */
    public boolean verify(Range range, SwarmFileSystem swarmFile) {
        throw new UnsupportedOperationException();
    }

}
