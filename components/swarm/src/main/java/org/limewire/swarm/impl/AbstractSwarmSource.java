package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public abstract class AbstractSwarmSource implements SwarmSource {

    private final SwarmSourceListenerList listenerList;

    public AbstractSwarmSource() {
        listenerList = new SwarmSourceListenerList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#addListener(org.limewire.swarm.
     * SwarmSourceListener)
     */
    public void addListener(SwarmSourceListener listener) {
        listenerList.addListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#removeListener(org.limewire.swarm.
     * SwarmSourceListener)
     */
    public void removeListener(SwarmSourceListener listener) {
        listenerList.removeListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#connected(org.limewire.swarm.
     * SwarmSourceHandler)
     */
    public void connected(SwarmSourceDownloader swarmSourceHandler) {
        listenerList.connected(swarmSourceHandler, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#connectFailed(org.limewire.swarm.
     * SwarmSourceHandler)
     */
    public void connectFailed(SwarmSourceDownloader swarmSourceHandler) {
        listenerList.connectFailed(swarmSourceHandler, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#connectionClosed(org.limewire.swarm.
     * SwarmSourceHandler)
     */
    public void connectionClosed(SwarmSourceDownloader swarmSourceHandler) {
        listenerList.connectionClosed(swarmSourceHandler, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.SwarmSource#finished(org.limewire.swarm.SwarmSourceHandler
     * )
     */
    public void finished(SwarmSourceDownloader swarmSourceHandler) {
        listenerList.finished(swarmSourceHandler, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#responseProcessed(org.limewire.swarm.
     * SwarmSourceHandler, org.limewire.swarm.SwarmStatus)
     */
    public void responseProcessed(SwarmSourceDownloader swarmSourceHandler, SwarmStatus status) {
        listenerList.responseProcessed(swarmSourceHandler, this, status);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#isFinished()
     */
    public boolean isFinished() {
        return false;
    }

}
