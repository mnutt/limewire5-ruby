package org.limewire.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface SwarmContent {

    /**
     * Reads data from this swarmContent object into the specified buffer.
     * Returns the number of bytes read. -1 if there is no more to read.
     */
    public int read(ByteBuffer byteBuffer) throws IOException;
}
