package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public class EchoSwarmSourceListener implements SwarmSourceListener {

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connected(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connected(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        System.out.println("connected: " + source);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connectFailed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectFailed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        System.out.println("connetionFailed: " + source);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connectionClosed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectionClosed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        System.out.println("connectionClosed: " + source);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#responseProcessed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource, org.limewire.swarm.SwarmStatus)
     */
    public void responseProcessed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        System.out.println("responseProcessed: " + source + " status: " + status);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#finished(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void finished(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        System.out.println("finished: " + source);
    }

}
