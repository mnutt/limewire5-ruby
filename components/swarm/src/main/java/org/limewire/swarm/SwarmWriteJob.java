package org.limewire.swarm;

import java.io.IOException;


/**
 * A job for writing data.  The job is expected to asynchronously write data
 * to disk.
 */
public interface SwarmWriteJob {

    /**
     * Notification that content is available in the decoder.
     * 
     * @return the amount of data that was read.
     */
    long write(SwarmContent content) throws IOException;

    /**
     * Cancels this write job and any pending scheduled tasks.
     */
    void cancel();

}
