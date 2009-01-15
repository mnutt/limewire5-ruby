package org.limewire.core.impl.support;

import org.limewire.core.api.support.SessionInfo;

public class MockSessionInfo implements SessionInfo {

    @Override
    public boolean acceptedIncomingConnection() {
        return false;
    }

    @Override
    public boolean canDoFWT() {
        return false;
    }

    @Override
    public boolean canReceiveSolicited() {
        return false;
    }

    @Override
    public long getByteBufferCacheSize() {
        return 0;
    }

    @Override
    public long getContentResponsesSize() {
        return 0;
    }

    @Override
    public long getCreationCacheSize() {
        return 0;
    }

    @Override
    public long getCurrentUptime() {
        return 0;
    }

    @Override
    public long getDiskControllerByteCacheSize() {
        return 0;
    }

    @Override
    public int getDiskControllerQueueSize() {
        return 0;
    }

    @Override
    public long getDiskControllerVerifyingCacheSize() {
        return 0;
    }

    @Override
    public int getNumActiveDownloads() {
        return 0;
    }

    @Override
    public int getNumActiveUploads() {
        return 0;
    }

    @Override
    public int getNumConnectionCheckerWorkarounds() {
        return 0;
    }

    @Override
    public int getNumIndividualDownloaders() {
        return 0;
    }

    @Override
    public int getNumLeafToUltrapeerConnections() {
        return 0;
    }

    @Override
    public int getNumOldConnections() {
        return 0;
    }

    @Override
    public int getNumQueuedUploads() {
        return 0;
    }

    @Override
    public int getNumUltrapeerToLeafConnections() {
        return 0;
    }

    @Override
    public int getNumUltrapeerToUltrapeerConnections() {
        return 0;
    }

    @Override
    public int getNumWaitingDownloads() {
        return 0;
    }

    @Override
    public int getNumberOfPendingTimeouts() {
        return 0;
    }

    @Override
    public int getNumberOfWaitingSockets() {
        return 0;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public long[] getSelectStats() {
        return null;
    }

    @Override
    public int getSharedFileListSize() {
        return 0;
    }

    @Override
    public int getManagedFileListSize() {
        return 0;
    }

    @Override
    public int getAllFriendsFileListSize() {
        return 0;
    }

    @Override
    public int getSimppVersion() {
        return 0;
    }

    @Override
    public String getUploadSlotManagerInfo() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isGUESSCapable() {
        return false;
    }

    @Override
    public boolean isLifecycleLoaded() {
        return false;
    }

    @Override
    public boolean isShieldedLeaf() {
        return false;
    }

    @Override
    public boolean isSupernode() {
        return false;
    }

    @Override
    public boolean isUdpPortStable() {
        return false;
    }

    @Override
    public int lastReportedUdpPort() {
        return 0;
    }

    @Override
    public int receivedIpPong() {
        return 0;
    }

}
