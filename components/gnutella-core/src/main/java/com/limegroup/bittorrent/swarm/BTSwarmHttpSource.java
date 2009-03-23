package com.limegroup.bittorrent.swarm;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmStatus;
import org.limewire.swarm.http.SwarmHttpSource;

public class BTSwarmHttpSource extends SwarmHttpSource {

    // private static final int MAX_RESPONSES = 10;

    private final AtomicInteger responsesProcessed;

    public BTSwarmHttpSource(URI uri, long fileSize) {
        super(uri, fileSize);
        this.responsesProcessed = new AtomicInteger(0);
    }

    @Override
    public void responseProcessed(SwarmSourceDownloader swarmSourceHandler, SwarmStatus status) {
        responsesProcessed.addAndGet(1);
        super.responseProcessed(swarmSourceHandler, status);
    }

    @Override
    public boolean isFinished() {
        // TODO override the finished logic.
        // after refactoring the BTConnectionFetcher/ManagedTorrent logic
        // we can use this to iteratively add sources to the swarmer.
        // sources that will automatically remove themselves after a certain
        // amount of requests have been made
        // then we later have teh connection fetcher check to see if the source
        // should be added again.
        // return responsesProcessed.get() >= MAX_RESPONSES;
        return false;
    }

}
