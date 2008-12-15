package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmCoordinatorListener;

/**
 * Abstract Swarm Coordinator that will hold some common functionality accross
 * Coordinators.
 * 
 */
public abstract class AbstractSwarmCoordinator implements SwarmCoordinator {

    /** List of listeners. */
    private final SwarmCoordinatorListenerList listeners;

    public AbstractSwarmCoordinator() {
        listeners = new SwarmCoordinatorListenerList(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.limewire.swarm.SwarmCoordinator#addListener(org.limewire.swarm.
     * SwarmCoordinatorListener)
     */
    public void addListener(SwarmCoordinatorListener swarmListener) {
        listeners.add(swarmListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.SwarmCoordinator#removeListener(org.limewire.swarm
     * .SwarmCoordinatorListener)
     */
    public void removeListener(SwarmCoordinatorListener swarmListener) {
       listeners.remove(swarmListener);
    }

    /**
     * Returns the listeners for this coordinator.
     */
    protected SwarmCoordinatorListenerList listeners() {
        return listeners;
    }
}
