package com.limegroup.bittorrent.disk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.DiskException;

public class LoggingDiskListener implements DiskManagerListener {
    private static final Log LOG = LogFactory.getLog(LoggingDiskListener.class);

    public void chunkVerified(int id) {
        LOG.debug("chunkVerified: " + id);

    }

    public void diskExceptionHappened(DiskException e) {
        LOG.debug("diskExceptionHappened: " + e.getMessage());

    }

    public void verificationComplete() {
        LOG.debug("verificationComplete");

    }
}