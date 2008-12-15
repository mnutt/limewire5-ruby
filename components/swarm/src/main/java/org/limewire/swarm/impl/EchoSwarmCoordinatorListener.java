/**
 * 
 */
package org.limewire.swarm.impl;

import org.limewire.collection.Range;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmCoordinatorListener;
import org.limewire.swarm.SwarmFileSystem;

/**
 * Class used to track what is happening inside of the swarm coordinator. It
 * echos out events as they happen.
 * 
 * 
 */
public final class EchoSwarmCoordinatorListener implements SwarmCoordinatorListener {
    public void blockLeased(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block leased: " + block.toString() + "/" + block.getLength());
    }

    public void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block verification failed: " + block.toString() + "/"
                + block.getLength());
    }

    public void blockVerified(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block verified: " + block.toString() + "/" + block.getLength());
    }

    public void blockWritten(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block written: " + block.toString() + "/" + block.getLength());
    }

    public void blockUnleased(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block unleased: " + block.toString() + "/" + block.getLength());
    }

    public void downloadCompleted(SwarmCoordinator swarmCoordinator, SwarmFileSystem swarmDownload) {
        System.out.println("download complete");
    }

    public void blockPending(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block pending: " + block.toString() + "/" + block.getLength());
    }

    public void blockUnpending(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block unpending: " + block.toString() + "/" + block.getLength());
    }
}