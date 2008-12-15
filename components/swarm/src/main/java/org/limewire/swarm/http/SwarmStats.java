package org.limewire.swarm.http;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SwarmStats {

    private final AtomicInteger numberOfRequests = new AtomicInteger(0);

    private final AtomicInteger numberOfResponses = new AtomicInteger(0);

    private final AtomicInteger numberOfSources = new AtomicInteger(0);

    private final AtomicLong numberOfBytesRequested = new AtomicLong(0);

    public int getNumberOfRequests() {
        return numberOfRequests.intValue();
    }

    public int getNumberOfResponses() {
        return numberOfResponses.intValue();
    }

    public int getNumberOfSources() {
        return numberOfSources.intValue();
    }

    public int incrementNumberOfRequests() {
        return numberOfRequests.incrementAndGet();
    }

    public int incrementNumberOfResponses() {
        return numberOfResponses.incrementAndGet();
    }

    public int incrementNumberOfSources() {
        return numberOfSources.incrementAndGet();
    }

    public long getNumberOfBytesRequested() {
        return numberOfBytesRequested.longValue();
    }

    public long incrementNumberOfBytesRequested(long numberOfBytes) {
        return numberOfBytesRequested.addAndGet(numberOfBytes);
    }

}
