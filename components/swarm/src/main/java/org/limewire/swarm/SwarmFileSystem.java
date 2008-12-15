package org.limewire.swarm;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Represents a collection of files on top the local file system. Maps between
 * the actual underlying filesystem and the view provided here.
 */
public interface SwarmFileSystem extends Closeable {

    /**
     * Writes data from the byteBuffer to the fileSystem, starting at 'start'.
     * Writes as much as the has available, and returns the amount written.
     */
    long write(ByteBuffer byteBuffer, long start) throws IOException;

    /**
     * Reads data from the fileSystem starting at 'start' into the given buffer.
     * Reads as much as is available or will fit into the buffer, and returns
     * the amount read.
     */
    long read(ByteBuffer byteBuffer, long start) throws IOException;

    /**
     * Closes this filesystem, cleaning up resources used for reading/writing.
     */
    void close() throws IOException;

    /**
     * Closes the given swarmFile for writing/reading.
     */
    void closeSwarmFile(SwarmFile swarmFile) throws IOException;

    /**
     * Initializes this filesystem for use.
     */
    void initialize() throws IOException;

    /**
     * Returns the complete size of this filesystem. All files contained within.
     */
    long getCompleteSize();

    /**
     * Retrieves the swarm file located at the given position. null if no such file exists.
     */
    SwarmFile getSwarmFile(long position);

    /**
     * Returns all the swarm files represented by this file system.
     */
    List<SwarmFile> getSwarmFiles();

}
