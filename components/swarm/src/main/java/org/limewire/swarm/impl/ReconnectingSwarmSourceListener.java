package org.limewire.swarm.impl;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public class ReconnectingSwarmSourceListener implements SwarmSourceListener {

    private static final Log LOG = LogFactory.getLog(ReconnectingSwarmSourceListener.class);

    private SwarmStatus connectionStatus = null;

    @Override
    public void connected(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        LOG.tracef("connected: {0}", source);
    }

    @Override
    public void connectFailed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        LOG.tracef("connectFailed: {0}", source);
    }

    @Override
    public void connectionClosed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        connectionClosed(swarmSourceHandler, source, connectionStatus);
    }

    private void connectionClosed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        // TODO we can be smarter about reconnecting
        // check the explicit error and try reconnecting
        // or allow a few errors before letting the connection close

        if (status != null && status.isFinished() || swarmSourceHandler.isComplete()
                || source.isFinished()) {
            LOG.tracef("finished, not reconnecting: {0} status: {1}", source, status);
        } else if (status != null && status.isError()) {
            LOG.tracef("error, not reconnecting: {0} status: {1}", source, status);
        } else if (status == null || status.isOk()) {
            LOG.tracef("reconnecting: {0} status: {1}", source, status);
            // re-add this source back to the swarmer
            swarmSourceHandler.addSource(source);
        } else {
            LOG.tracef("connectionClosed: {0} status: {1}", source, status);
        }
    }

    @Override
    public void responseProcessed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        connectionStatus = status;
        LOG.tracef("responseProcessed: {0} status: {1}", source, status);
    }

    @Override
    public void finished(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        connectionStatus = new FinishedSwarmStatus();
        LOG.tracef("finished: {0}", source);
    }

}
