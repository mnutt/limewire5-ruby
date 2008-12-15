package org.limewire.swarm;

/**
 * A job for writing data. The job is expected to asynchronously write data to
 * disk. Some data sources might need to be told to stop while we are blocking
 * on disk access, etc.
 */
public interface SwarmWriteJobControl {

    /**
     * Pauses input from reaching write job.
     */
    public void pause();

    /**
     * Resumes input to reach write job.
     */
    public void resume();

}
