package org.limewire.swarm.file.verifier;

import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmFileSystem;

/**
 * This class is used for testing, it passes all ranges given as verified.
 */
public class VerifyPassFileVerifier implements SwarmBlockVerifier {

    public List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize) {
        return writtenBlocks.getAllIntervalsAsList();
    }

    public boolean verify(Range range, SwarmFileSystem swarmFile) {
        return true;
    }

}
