package org.limewire.swarm.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.limewire.collection.Range;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.nio.ByteBufferCache;
import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;

/**
 * 
 * 
 *
 */
public class SwarmWriteJobImpl implements SwarmWriteJob {

    private static final Log LOG = LogFactory.getLog(SwarmWriteJobImpl.class);

    private final ExecutorService jobScheduler;

    private final SwarmCoordinator fileCoordinator;

    private final Object scheduleLock = new Object();

    private Future<Void> scheduledJob;

    private final Range range;

    private Range writeRange = null;

    private final SwarmWriteJobControl callback;

    private final ByteBufferCache byteBufferCache;

    private static final int BUFFER_SIZE = 1024 * 16;

    /**
     * Constructor for a SwarmWriteJobImpl, it will write the data for a given
     * range of bytes, by making subsequent calls to this jobs write method.
     * 
     * @param range
     * @param fileCoordinator
     * @param jobScheduler
     * @param callback
     */
    public SwarmWriteJobImpl(Range range, SwarmCoordinator fileCoordinator,
            ExecutorService jobScheduler, SwarmWriteJobControl callback) {
        this.jobScheduler = jobScheduler;
        this.fileCoordinator = fileCoordinator;
        this.range = range;
        this.writeRange = range;
        this.callback = callback;
        this.byteBufferCache = new ByteBufferCache();
    }

    /**
     * Cancels this write job and any pending scheduled tasks.
     */
    public void cancel() {
        synchronized (scheduleLock) {
            LOG.debug("Cancelling Write Job");
            if (scheduledJob != null) {
                scheduledJob.cancel(false);
                scheduledJob = null;
            }
            fileCoordinator.unpending(range);
        }
    }

    /**
     * Schedules a write job to write the data to disk.
     * 
     * @param content
     * @return the number of bytes to be written
     * 
     */
    public long write(final SwarmContent content) throws IOException {
        long written = 0;
        synchronized (scheduleLock) {

            final ByteBuffer networkUnblockingBuffer = byteBufferCache.getHeap(BUFFER_SIZE);
            content.read(networkUnblockingBuffer);
            networkUnblockingBuffer.flip();
            written = networkUnblockingBuffer.limit();
            LOG.tracef("Going to write: {0} bytes.", written);
            scheduledJob = jobScheduler.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    writeData(range, networkUnblockingBuffer);
                    return null;
                }
            });

        }

        return written;
    }

    /**
     * Writes data from the buffer into a temporary buffer, and then to disk.
     * This roundabout way of writing is required to ensure that no shared lock
     * is used both during disk & network I/O. (Disk I/O is inherently blocking,
     * and can stall network I/O otherwise.)
     * 
     * @param range
     * 
     * @param buffer
     */
    private void writeData(Range range, ByteBuffer buffer) throws IOException {
        synchronized (scheduleLock) {
            try {
                long bytesWritten = 0;
                long totalWritten = 0;
                do {

                    bytesWritten = fileCoordinator.write(writeRange, buffer);

                    long newLow = writeRange.getLow() + bytesWritten;
                    long newHigh = writeRange.getHigh();

                    LOG.trace("Re-requesting I/O");
                    callback.resume();

                    if (newLow <= newHigh) {
                        Range newRange = Range.createRange(newLow, newHigh);
                        this.writeRange = newRange;
                    }

                    totalWritten += bytesWritten;
                } while (buffer.hasRemaining());
                LOG.tracef("wrote: {0} bytes", totalWritten);
            } finally {
                byteBufferCache.release(buffer);
                callback.resume();
            }

        }

    }
}