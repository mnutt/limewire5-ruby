package org.limewire.swarm;

import org.limewire.collection.Range;

public class VerificationException extends Exception {

    private final Range range;

    public VerificationException(Exception e, Range range) {
        this("Error verifying range: " + range, e, range);
    }

    public VerificationException(String message, Exception e, Range range) {
        super(message, e);
        this.range = range;
    }

    public Range getRange() {
        return range;
    }
}
