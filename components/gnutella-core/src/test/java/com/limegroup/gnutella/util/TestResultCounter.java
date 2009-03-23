package com.limegroup.gnutella.util;

import com.limegroup.gnutella.search.ResultCounter;

/**
 * Helper result counter that we can use to artificially simulate 
 * results for testing.
 */
public final class TestResultCounter implements ResultCounter {
    
    private final int RESULTS;

    /**
     * Creates a default counter that always returns that it has
     * 10 results.
     */
    public TestResultCounter() {
        this(10);
    }

    public TestResultCounter(int results) {
        RESULTS = results;
    }

    public int getNumResults() {
        return RESULTS;
    }
}
