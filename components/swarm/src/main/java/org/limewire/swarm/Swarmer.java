package org.limewire.swarm;

import org.limewire.nio.observer.Shutdownable;

/**
 * The swarm is responsible for registering various SwarmSourceHandlers and
 * providing an interface to download a file from multiple sources in a swarm.
 * 
 * There is no order between calls to {@link #addSource(SwarmSource)} and 
 * {@link #start()} or {@link #shutdown()}.
 * {@link #register(SwarmSourceType, SwarmSourceDownloader)} must be called before
 * anything else.
 */
public interface Swarmer extends Shutdownable {
    /**
     * Adds the given source to the swarm and delegates it the responsible
     * {@link SwarmSourceDownloader}.
     * 
     * @throws NullPointerException if there is no register swarm handler for the
     *         given swarm source
     */
    void addSource(SwarmSource source);

    /**
     * Registers a swarmSourceHandler to the given swarmSourceType. Needs to be
     * called before any sources are added to the swarmer.
     */
    void register(SwarmSourceType type, SwarmSourceDownloader sourceHandler);

    /**
     * Starts the swarmer and its handlers.
     */
    void start();

    /**
     * Shuts down the swarmer and its handlers.
     */
    void shutdown();

    /**
     * Returns the aggregate measures bandwidth from its handler.
     * 
     * @param downstream if true return downstream bandwidth, otherwise upstream
     */
    float getMeasuredBandwidth(boolean downstream);

    /**
     * Returns true if there is a handler for the given SwarmSourceType.
     */
    boolean hasDownloaderRegistered(SwarmSourceType type);

    SwarmCoordinator getCoordinator();

}
