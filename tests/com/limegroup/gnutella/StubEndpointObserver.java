package com.limegroup.gnutella;

import com.limegroup.gnutella.HostCatcher.EndpointObserver;

public class StubEndpointObserver implements EndpointObserver {
    private Endpoint endpoint;

    public synchronized void handleEndpoint(Endpoint p) {
        endpoint = p;
        notify();
    }
    
    public synchronized void waitForEndpoint(long time) throws Exception {
        wait(time);
    }

    public synchronized Endpoint getEndpoint() {
        return endpoint;
    }

}
