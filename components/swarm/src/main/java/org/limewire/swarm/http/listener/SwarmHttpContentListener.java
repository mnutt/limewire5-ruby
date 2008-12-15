package org.limewire.swarm.http.listener;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;
import org.limewire.swarm.http.SwarmHttpContentImpl;
import org.limewire.swarm.http.SwarmHttpUtils;
import org.limewire.util.Objects;

public class SwarmHttpContentListener implements ResponseContentListener {

    private static final Log LOG = LogFactory.getLog(SwarmHttpContentListener.class);

    private final SwarmCoordinator swarmCoordinator;

    private boolean finished;

    private Range leaseRange;

    private SwarmWriteJob writeJob;

    private final SwarmFile swarmFile;

    public SwarmHttpContentListener(SwarmCoordinator fileCoordinator, SwarmFile swarmFile,
            Range leaseRange) {
        this.swarmCoordinator = Objects.nonNull(fileCoordinator, "fileCoordinator");
        this.leaseRange = Objects.nonNull(leaseRange, "leaseRange");
        this.swarmFile = swarmFile;
    }

    public void contentAvailable(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        if (!swarmCoordinator.isComplete()) {
            LOG.trace("contentAvailable called");

            if (finished) {
                String message = "Already finished.";
                LOG.warn(message);
                throw new IOException(message);
            }

            if (!decoder.isCompleted()) {
                if (writeJob == null) {
                    writeJob = swarmCoordinator.createWriteJob(leaseRange, createControl(ioctrl));
                }
                try {
                    writeJob.write(new SwarmHttpContentImpl(decoder));
                } catch (IOException e) {
                    LOG.warn(e.getMessage(), e);
                    throw e;
                }
            }
        } else {
            LOG.warn("contentAvailable called when swarmCoordinator already complete.");
        }
    }

    private SwarmWriteJobControl createControl(final IOControl finalIOControl) {
        SwarmWriteJobControl callBack = new SwarmWriteJobControl() {
            public void pause() {
                finalIOControl.suspendInput();
                finalIOControl.suspendOutput();
            }

            public void resume() {
                finalIOControl.requestOutput();
                finalIOControl.requestInput();
            }
        };
        return callBack;
    }

    public void finished() {
        LOG.trace("finished");
        if (!finished) {
            finished = true;
            if (leaseRange != null) {
                swarmCoordinator.unlease(leaseRange);
                leaseRange = null;
            }
        }
    }

    public void initialize(HttpResponse response) throws IOException {
        if (finished) {
            String message = "Already finished";
            LOG.warn(message);
            throw new IOException(message);
        }

        Range actualRange = SwarmHttpUtils.parseContentRange(response);
        long startByte = swarmFile.getStartBytePosition();

        actualRange = Range.createRange(startByte + actualRange.getLow(), startByte
                + actualRange.getHigh());
        validateActualRangeAndShrinkExpectedRange(actualRange);
    }

    private void validateActualRangeAndShrinkExpectedRange(Range actualRange) throws IOException {
        if (actualRange.getLow() < leaseRange.getLow()
                || actualRange.getHigh() > leaseRange.getHigh()) {
            String message = "Invalid actual range.  Expected: " + leaseRange + ", Actual: "
                    + actualRange;
            LOG.warn(message);
            throw new IOException(message);
        }

        if (!actualRange.equals(leaseRange)) {
            leaseRange = swarmCoordinator.renewLease(leaseRange, actualRange);
        }
    }
}
