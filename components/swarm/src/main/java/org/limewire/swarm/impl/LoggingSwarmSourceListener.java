package org.limewire.swarm.impl;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public class LoggingSwarmSourceListener implements SwarmSourceListener {

    private static final Log LOG = LogFactory.getLog(LoggingSwarmSourceListener.class);

    /*
     * (non-Javadoc)
     * 
     * @seeorg.limewire.swarm.SwarmSourceListener#connected(org.limewire.swarm.
     * SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connected(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        LOG.tracef("connected: {0}", source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.SwarmSourceListener#connectFailed(org.limewire.swarm
     * .SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectFailed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        LOG.tracef("connetionFailed:  {0}", source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.SwarmSourceListener#connectionClosed(org.limewire.
     * swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectionClosed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        LOG.tracef("connectionClosed:  {0}", source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.SwarmSourceListener#responseProcessed(org.limewire
     * .swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource,
     * org.limewire.swarm.SwarmStatus)
     */
    public void responseProcessed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        LOG.tracef("responseProcessed:  {0} status:  {1}", source, status);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.limewire.swarm.SwarmSourceListener#finished(org.limewire.swarm.
     * SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void finished(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        LOG.tracef("finished: {0}", source);
    }

}
