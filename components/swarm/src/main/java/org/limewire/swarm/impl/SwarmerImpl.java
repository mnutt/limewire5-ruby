package org.limewire.swarm.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmSourceType;
import org.limewire.swarm.Swarmer;
import org.limewire.util.Objects;

public class SwarmerImpl implements Swarmer {

    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);

    private final Map<SwarmSourceType, SwarmSourceDownloader> sourceDownloaders;

    private final SwarmCoordinator swarmCoordinator;

    public SwarmerImpl(SwarmCoordinator swarmCoordinator) {
        this.swarmCoordinator = Objects.nonNull(swarmCoordinator, "swarmCoordinator");
        this.sourceDownloaders = Collections
                .synchronizedMap(new HashMap<SwarmSourceType, SwarmSourceDownloader>());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.Swarmer#register(org.limewire.swarm.SwarmSourceType,
     * org.limewire.swarm.SwarmSourceHandler)
     */
    public void register(SwarmSourceType type, SwarmSourceDownloader sourceHandler) {
        sourceDownloaders.put(type, sourceHandler);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.Swarmer#addSource(org.limewire.swarm.SwarmSource)
     */
    public void addSource(SwarmSource source) {
        SwarmSourceType type = source.getType();

        if (!hasDownloaderRegistered(type)) {
            throw new IllegalStateException("No swarm source handler is registered for type: "
                    + type);
        }

        SwarmSourceDownloader sourceDownloader = sourceDownloaders.get(type);
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Adding source: {0}", source);
        }
        sourceDownloader.addSource(source);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.Swarmer#start()
     */
    public void start() {
        for (SwarmSourceDownloader handler : sourceDownloaders.values()) {
            try {
                handler.start();
            } catch (IOException iox) {
                LOG.warnf("Unable to start swarm source handler: {0}", handler);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.Swarmer#shutdown()
     */
    public void shutdown() {
        for (SwarmSourceDownloader handler : sourceDownloaders.values()) {
            try {
                handler.shutdown();
            } catch (IOException iox) {
                LOG.warnf("Unable to shutdown swarm source handler: {0}", handler);
            }
        }
        try {
            swarmCoordinator.close();
        } catch (IOException iox) {
            LOG.warnf("Unable to shutdown swarm coordinator: {0}", swarmCoordinator);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.Swarmer#getMeasuredBandwidth(boolean)
     */
    public float getMeasuredBandwidth(boolean downstream) {
        float bandwidth = 0;
        for (SwarmSourceDownloader handler : sourceDownloaders.values()) {
            bandwidth += handler.getMeasuredBandwidth(downstream);
        }
        return bandwidth;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.Swarmer#hasHandler(org.limewire.swarm.SwarmSourceType)
     */
    public boolean hasDownloaderRegistered(SwarmSourceType type) {
        return sourceDownloaders.containsKey(type);
    }

    @Override
    public SwarmCoordinator getCoordinator() {
        return swarmCoordinator;
    }

}
