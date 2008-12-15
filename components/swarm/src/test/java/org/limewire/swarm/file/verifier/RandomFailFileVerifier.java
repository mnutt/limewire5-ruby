package org.limewire.swarm.file.verifier;

import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmFileSystem;

/**
 * This class is only used to simulate pieces failing. It is currently set to
 * fail 10 percent of the time.
 */
public class RandomFailFileVerifier implements SwarmBlockVerifier {

    private static final double PERCENTAGE_FAIL = 0.10;

    public List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize) {
        return writtenBlocks.getAllIntervalsAsList();
    }

    public boolean verify(Range range, SwarmFileSystem swarmFile) {
        if (PERCENTAGE_FAIL > Math.random()) {
            return false;
        }

        return true;
    }

}
