package org.limewire.swarm;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

public interface SwarmCoordinator extends Closeable {

    /**
     * Leases just a portion of data, only allowing ranges in availableRanges.
     * If no data is available for leasing, this returns null.
     */
    Range leasePortion(IntervalSet availableRanges);

    /**
     * Leases just a portion of data using the given selector, only allowing
     * ranges in availableRanges. If no data is available for leasing, this
     * returns null.
     */
    Range leasePortion(IntervalSet availableRanges, SwarmBlockSelector selector);

    /**
     * Returns a previously leased range. This will allow other sources to lease
     * that range.
     */
    void unlease(Range range);

    /**
     * Writes the given range to disk using the supplied buffer. The buffer might not contain
     * The full range to be written, the number of bytes written will be returned and a modified
     * range can be passed in to write additional bytes.
     * @param range
     * @param content
     * @return the number of bytes writen.
     * @throws IOException
     */
    long write(Range range, ByteBuffer content) throws IOException;

    
    /**
     * Constructs a new WriteJob that will be used to write data. As data
     * becomes available, {@link SwarmWriteJob#write(Content)} can be called,
     * informing the job that data is available. If the job cannot consume data,
     * it will suspend I/O. When the job can begin consuming data again, it will
     * resume I/O.
     * 
     * It is expected that the job will call {@link #pending(Range)} when it
     * consumed content, and will eventually call {@link #wrote(Range)} after
     * data has successfully been written to disk.
     * 
     * @param length
     */
    SwarmWriteJob createWriteJob(Range range, SwarmWriteJobControl callback);

    /**
     * Marks a range as pending a write. The range is still not leasable, but
     * now cannot be unleased. To release a pending range (without writing it),
     * use {@link #unpending(Range)}.
     */
    void pending(Range range);

    /**
     * Marks a range as no longer pending a write. The range is re-available for
     * leasing.
     */
    void unpending(Range range);

    /**
     * Signals a range as being written. It is removed from pending and
     * unavailable for future leases. If verification is active, this can
     * trigger a verification, leading to {@link #getAmountVerified()} returning
     * &gt; 0.
     */
    void wrote(Range range);

    /**
     * Returns the total amount of data that has been verified thus far.
     */
    long getAmountVerified();

    /**
     * Returns the total amount of data that was discarded because it could not
     * be verified.
     */
    long getAmountLost();

    /**
     * Triggers a verification on all written data.
     */
    void verify();

    /**
     * Triggers a verification of all written data and previously verified data.
     */
    void reverify();

    /**
     * Adds a listener for swarm completion.
     * 
     * The listener will be notified when the file has completely been
     * downloaded. If verification is not active, the listener will be notified
     * once all ranges are written. If verification is active, the listener will
     * be notified once all ranges are verified.
     */
    void addListener(SwarmCoordinatorListener swarmListener);
    
    /**
     * Removes a listener from this swarm coordinator.
     */
    void removeListener(SwarmCoordinatorListener swarmListener);

    /**
     * Returns true if all the pieces have been written to disk.
     */
    boolean isComplete();

    /**
     * Renews the oldLease with the newLease. New lease must be a subset of oldLease.
     * {@link #unlease(Range)} is called with the oldLease, then {@link #lease(Range)} 
     * is called with the newLease.
     * @param oldLease
     * @param newLease
     * @return the newly leased Range.
     */
    Range renewLease(Range oldLease, Range newLease);

    /**
     * Returns the swarm file containing the beginning of the range or null
     * if there is none.
     */
    SwarmFile getSwarmFile(Range range);

}
