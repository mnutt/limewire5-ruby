package org.limewire.swarm;

import org.limewire.collection.Range;

/**
 * Listener for events in the SwarmCoordinator.
 * 
 */
public interface SwarmCoordinatorListener {

    /**
     * When the download is completed the SwarmCoordinator will fire
     * downloadCompleted to all the attached listeners.
     */
    void downloadCompleted(SwarmCoordinator swarmCoordinator, SwarmFileSystem fileSystem);

    /**
     * When a block fails verification the blockVerificationFailed event will
     * fire to the attached listeners.
     */
    void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block passes verification the blockVerified event will fire to the
     * attached listeners.
     */
    void blockVerified(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is written to disk the blockWritten event will fire to the
     * attached listeners.
     */
    void blockWritten(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is leased the blockLeased event will fire to the attached
     * listeners.
     */
    void blockLeased(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is unLeased the blockUnleased event will fire to the
     * attached listeners.
     */
    void blockUnleased(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is unpended the blockUnpending event will fire to the
     * attached listeners.
     */
    void blockUnpending(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is pending write the blockPending event will fire to the
     * attached listeners.
     */
    void blockPending(SwarmCoordinator swarmCoordinator, Range block);
}
