package org.limewire.swarm.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmBlockSelector;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;
import org.limewire.swarm.VerificationException;
import org.limewire.swarm.impl.AbstractSwarmCoordinator;
import org.limewire.swarm.impl.LoggingSwarmCoordinatorListener;
import org.limewire.util.Objects;

/**
 * A {@link FileCoordinator} reads/writes the files using the given
 * {@link SwarmFileSystem}.
 * 
 * This implementation expects the writeService to use either in-place execution
 * or a single thread. If multiple threads are used, verifying may incorrectly
 * reverify multiple times.
 */
public class FileCoordinatorImpl extends AbstractSwarmCoordinator {

    private static final Log LOG = LogFactory.getLog(FileCoordinatorImpl.class);

    /** The minimum blocksize to lease. */
    private static final long DEFAULT_BLOCK_SIZE = 16 * 1024;

    /** The minimum block size to use. */
    private final long blockSize;

    /** All ranges that are out on lease. */
    private final IntervalSet leasedBlocks;

    /** The blocks that were written to disk. */
    private final IntervalSet writtenBlocks;

    /** The blocks that were verified after being written to disk. */
    private final IntervalSet verifiedBlocks;

    /** Blocks that are pending to be written to disk. */
    private final IntervalSet pendingBlocks;

    /** The strategy for selecting new leased ranges. */
    private final SwarmBlockSelector blockSelector;

    /** The file writer. */
    private final SwarmFileSystem fileSystem;

    /** The ExecutorService to use for writing. */
    private final ExecutorService writeService;

    /** The file verifier. */
    private final SwarmBlockVerifier swarmBlockVerifier;

    /** The amount of data that was lost to corruption. */
    private long amountLost;

    /** A simple lock. */
    private final Object LOCK = new Object();

    public FileCoordinatorImpl(SwarmFileSystem fileSystem, SwarmBlockVerifier swarmFileVerifier,
            ExecutorService writeService, SwarmBlockSelector selectionStrategy) {
        this(fileSystem, swarmFileVerifier, writeService, selectionStrategy, DEFAULT_BLOCK_SIZE);
    }

    public FileCoordinatorImpl(SwarmFileSystem fileSystem, SwarmBlockVerifier swarmFileVerifier,
            ExecutorService writeService, SwarmBlockSelector selectionStrategy, long blockSize) {
        this.blockSelector = Objects.nonNull(selectionStrategy, "selectionStrategy");
        this.fileSystem = Objects.nonNull(fileSystem, "fileSystem");
        this.writeService = Objects.nonNull(writeService, "writeService");
        this.swarmBlockVerifier = Objects.nonNull(swarmFileVerifier, "swarmFileVerifier");
        assert blockSize > 0;
        this.leasedBlocks = new IntervalSet();
        this.writtenBlocks = new IntervalSet();
        this.pendingBlocks = new IntervalSet();
        this.verifiedBlocks = new IntervalSet();
        this.blockSize = blockSize;
        if(LOG.isDebugEnabled() || LOG.isTraceEnabled()) {
            addListener(new LoggingSwarmCoordinatorListener());
        }
    }

    public Range leasePortion(IntervalSet availableRanges) {
        return lease(availableRanges, blockSize, blockSelector);
    }

    public Range leasePortion(IntervalSet availableRanges, SwarmBlockSelector swarmSelector) {
        return lease(availableRanges, blockSize, blockSelector);
    }

    protected Range lease(IntervalSet availableRanges, long blockSize,
            SwarmBlockSelector swarmSelector) {
        // Lease modifies, so clone.
        if (availableRanges != null) {
            try {
                availableRanges = availableRanges.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        IntervalSet neededBytes = new IntervalSet();
        availableRanges = getAvailableRangesForLease(availableRanges, neededBytes);

        if (availableRanges.isEmpty()) {
            return null;
        }

        // Pick a range, add it to leased, and exit.
        Range chosen;
        try {
            chosen = swarmSelector.selectAssignment(availableRanges, neededBytes, blockSize);
        } catch (NoSuchElementException nsee) {
            return null;
        }

        synchronized (LOCK) {
            addLease(chosen);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Leasing: " + chosen + ", from available: " + availableRanges + ", needed: "
                    + neededBytes);
        return chosen;
    }

    public void unlease(Range range) {
        synchronized (LOCK) {
            // don't assert hasLease(range); there are valid times where unlease will
            // be called and we don't have the lease
            deleteLease(range);
        }
    }

    public void pending(Range range) {
        synchronized (LOCK) {
            assert hasLease(range);
            deleteLease(range);
            addPending(range);
        }
    }

    private void addLease(Range chosen) {
        leasedBlocks.add(chosen);
        listeners().blockLeased(chosen);
    }

    private boolean hasLease(Range range) {
        return leasedBlocks.contains(range);
    }

    private void addPending(Range range) {
        pendingBlocks.add(range);
        listeners().blockPending(range);
    }

    private void deleteLease(Range range) {
        leasedBlocks.delete(range);
        listeners().blockUnleased(range);
    }

    private void deletePending(Range range) {
        pendingBlocks.delete(range);
        listeners().blockUnpending(range);
    }

    private boolean hasPending(Range range) {
        return pendingBlocks.contains(range);
    }

    private void addWritten(Range writtenRange) {
        writtenBlocks.add(writtenRange);
    }

    public void unpending(Range range) {
        synchronized (LOCK) {
            assert hasPending(range);
            deletePending(range);
        }
    }

    public void wrote(Range writtenRange) {
        List<Range> verifiableRanges;
        boolean complete;
        synchronized (LOCK) {
            assert hasPending(writtenRange);
            deletePending(writtenRange);
            addWritten(writtenRange);
            verifiableRanges = swarmBlockVerifier.scanForVerifiableRanges(writtenBlocks, fileSystem
                    .getCompleteSize());
            complete = verifiableRanges.isEmpty() && isComplete();
        }

        listeners().blockWritten(writtenRange);

        if (complete) {
            listeners().downloadCompleted(fileSystem);
        }
        verifyRanges(verifiableRanges);
    }

    /**
     * Returns true if this is complete either because all data is in
     * writtenBlocks, or all data is in verifiedBlocks.
     * 
     * LOCK is held for the duration of this method call.
     */
    public boolean isComplete() {
        synchronized (LOCK) {
            IntervalSet blocksToCheck = null;
            if (verifiedBlocks.isEmpty()) {
                blocksToCheck = writtenBlocks;
            } else if (writtenBlocks.isEmpty()) {
                blocksToCheck = verifiedBlocks;
            }

            return blocksToCheck != null && blocksToCheck.getNumberOfIntervals() == 1
                    && blocksToCheck.getSize() == fileSystem.getCompleteSize();
        }
    }

    private void verifyRanges(List<Range> verifiableRanges) {
        if (LOG.isDebugEnabled() && !verifiableRanges.isEmpty()) {
            LOG.debug("Verifying ranges: " + verifiableRanges);
        }

        boolean complete = false;
        for (Range rangeToVerify : verifiableRanges) {
            boolean verified;
            try {
                verified = swarmBlockVerifier.verify(rangeToVerify, fileSystem);
            } catch (VerificationException e) {
                LOG.warn(e.getMessage(), e);
                verified = false;
            }
            synchronized (LOCK) {
                assert writtenBlocks.contains(rangeToVerify);
                writtenBlocks.delete(rangeToVerify);
                if (verified) {
                    verifiedBlocks.add(rangeToVerify);
                    complete = isComplete();
                    listeners().blockVerified(rangeToVerify);
                    handleVerifiedPieces();
                } else {
                    listeners().blockVerificationFailed(rangeToVerify);
                    // TODO: Add a toggle for keeping lost ranges, and do not
                    // count if doing a 'full scan'.
                    amountLost += rangeToVerify.getHigh() - rangeToVerify.getLow() + 1;

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Lost range: " + rangeToVerify + ", total lost: " + amountLost);
                    }
                }
            }
        }

        if (complete) {
            listeners().downloadCompleted(fileSystem);
        }
    }

    private void handleVerifiedPieces() {
        List<SwarmFile> swarmFiles = fileSystem.getSwarmFiles();
        for (SwarmFile swarmFile : swarmFiles) {
            Range fileRange = Range.createRange(swarmFile.getStartBytePosition(), swarmFile
                    .getEndBytePosition());
            if (verifiedBlocks.contains(fileRange)) {
                try {
                    fileSystem.closeSwarmFile(swarmFile);
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Error closing swarmFile: " + swarmFile, e);
                    }
                }
            }
        }
    }

    public long getAmountVerified() {
        synchronized (LOCK) {
            return verifiedBlocks.getSize();
        }
    }

    public long getAmountLost() {
        synchronized (LOCK) {
            return amountLost;
        }
    }

    // TODO: enable support for scanning the existing ranges on disk
    public void reverify() {
        final List<Range> verifiableRanges;
        synchronized (LOCK) {
            writtenBlocks.add(verifiedBlocks);
            verifiedBlocks.clear();
            // As an optimization, only scan for ranges if we have no pending
            // blocks.
            // (This works because a pending range implies wrote will be called,
            // and wrote will trigger a verification.)
            if (pendingBlocks.getNumberOfIntervals() == 0 && !writtenBlocks.isEmpty()) {
                verifiableRanges = swarmBlockVerifier.scanForVerifiableRanges(writtenBlocks,
                        fileSystem.getCompleteSize());
            } else {
                verifiableRanges = Collections.emptyList();
            }
        }

        if (!verifiableRanges.isEmpty()) {
            writeService.execute(new Runnable() {
                public void run() {
                    verifyRanges(verifiableRanges);
                }
            });
        }
    }

    // TODO: enable support for scanning the existing ranges on disk
    public void verify() {
        final List<Range> verifiableRanges;
        synchronized (LOCK) {
            // As an optimization, only scan for ranges if we have no pending
            // blocks.
            // (This works because a pending range implies wrote will be called,
            // and wrote will trigger a verification.)
            if (pendingBlocks.getNumberOfIntervals() == 0 && !writtenBlocks.isEmpty()) {
                verifiableRanges = swarmBlockVerifier.scanForVerifiableRanges(writtenBlocks,
                        fileSystem.getCompleteSize());
            } else {
                verifiableRanges = Collections.emptyList();
            }
        }

        if (!verifiableRanges.isEmpty()) {
            writeService.execute(new Runnable() {
                public void run() {
                    verifyRanges(verifiableRanges);
                }
            });
        }
    }

    public boolean isRangeAvailableForLease() {
        return isRangeAvailableForLease(null);
    }

    public boolean isRangeAvailableForLease(IntervalSet availableRanges) {
        return !getAvailableRangesForLease(availableRanges, null).isEmpty();
    }

    /**
     * Mutates availableRanges to leave only the blocks left that can be leased.
     * If availableRanges is null, this assumes everything is available.
     * 
     * Also mutates neededBytes, to leave only what is needed.
     **/
    protected IntervalSet getAvailableRangesForLease(IntervalSet availableRanges,
            IntervalSet neededBytes) {
        if (availableRanges == null)
            availableRanges = IntervalSet.createSingletonSet(0, fileSystem.getCompleteSize() - 1);

        // Figure out which blocks we still need to assign
        if (neededBytes == null) {
            neededBytes = IntervalSet.createSingletonSet(0, fileSystem.getCompleteSize() - 1);
        } else {
            neededBytes.add(Range.createRange(0, fileSystem.getCompleteSize() - 1));
        }

        synchronized (LOCK) {
            neededBytes.delete(leasedBlocks);
            neededBytes.delete(writtenBlocks);
            neededBytes.delete(pendingBlocks);
            neededBytes.delete(verifiedBlocks);
        }
        // Calculate the intersection of neededBytes and availableBytes
        availableRanges.delete(neededBytes.invert(fileSystem.getCompleteSize()));
        return availableRanges;
    }

    public SwarmWriteJob createWriteJob(Range range, SwarmWriteJobControl callback) {
        return new SwarmWriteJobImpl(range, this, writeService, callback);
    }

    public long write(Range range, ByteBuffer swarmContent) throws IOException {
        // TODO unlock certain portions allow multiple writes at teh same time
        synchronized (LOCK) {
            long position = range.getLow();
            long startRange = range.getLow();
            long endRange = startRange - swarmContent.position() + swarmContent.limit() - 1;
            Range pendingRange = Range.createRange(startRange, endRange);
            pending(pendingRange);
            long bytesWritten = fileSystem.write(swarmContent, position);
            wrote(pendingRange);
            return bytesWritten;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (LOCK) {
            // TODO handle possible running write jobs.
            // TODO we can cancel the running write jobs through the scheduler
            // TODO or we can just wait for the jobs to finish
            fileSystem.close();
        }
    }

    public Range renewLease(Range oldLease, Range newLease) {
        synchronized (LOCK) {
            assert hasLease(oldLease);
            assert newLease.isSubrange(oldLease);
            deleteLease(oldLease);
            addLease(newLease);
            return newLease;
        }

    }

    public SwarmFile getSwarmFile(Range range) {
        return fileSystem.getSwarmFile(range.getLow());
    }

}
