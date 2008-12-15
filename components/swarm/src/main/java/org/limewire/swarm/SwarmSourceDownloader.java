package org.limewire.swarm;

import java.io.IOException;

/**
 * Handles downloading from given swarm sources.
 * 
 * It is expected to handle multiple sources that are being added to it.
 */
public interface SwarmSourceDownloader {

    /**
     * Adds a source for download.
     */
    void addSource(SwarmSource source);

    /**
     * Starts this source handler.
     */
    void start() throws IOException;

    /**
     * Shuts down this source handler.
     */
    void shutdown() throws IOException;

    /**
     * Returns whether has been started and not shutdown yet.
     */
    boolean isActive();

    /**
     * Returns whether all necessary data has been downloaded.
     */
    boolean isComplete();

    /**
     * Returns the measured bandwidth for its downloads.
     * 
     * @param downstream if true return downstream bandwidth, otherwise upstream
     */
    float getMeasuredBandwidth(boolean downstream);

}
