package org.limewire.swarm;

/**
 * Representation of status for a swarm source download.
 */
public interface SwarmStatus {

    /**
     * Returns true if the status is good to continue.
     */
    public boolean isOk();

    /**
     * Returns true if there has been an error.
     */
    public boolean isError();

    /**
     * Returns true if we are finished downloading from this source.
     */
    public boolean isFinished();
}
