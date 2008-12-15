package org.limewire.core.impl.connection;

import java.util.Properties;

import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.friend.FriendPresence;

/**
 * Mock implementation of a ConnectionItem.
 */
public class MockConnectionItem implements ConnectionItem {

    @Override
    public boolean isAddressResolved() {
        return false;
    }

    @Override
    public void setAddressResolved(boolean resolved) {
    }
    
    @Override
    public FriendPresence getFriendPresence() {
        return null;
    }

    @Override
    public Properties getHeaderProperties() {
        return null;
    }

    @Override
    public String getHostName() {
        return null;
    }

    @Override
    public void setHostName(String hostName) {
    }

    @Override
    public float getMeasuredDownstreamBandwidth() {
        return 0;
    }

    @Override
    public float getMeasuredUpstreamBandwidth() {
        return 0;
    }

    @Override
    public int getNumMessagesReceived() {
        return 0;
    }

    @Override
    public int getNumMessagesSent() {
        return 0;
    }

    @Override
    public long getNumReceivedMessagesDropped() {
        return 0;
    }

    @Override
    public int getNumSentMessagesDropped() {
        return 0;
    }
    
    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public int getQueryRouteTableEmptyUnits() {
        return 0;
    }

    @Override
    public double getQueryRouteTablePercentFull() {
        return 0;
    }

    @Override
    public int getQueryRouteTableSize() {
        return 0;
    }

    @Override
    public int getQueryRouteTableUnitsInUse() {
        return 0;
    }

    @Override
    public float getReadLostFromSSL() {
        return 0;
    }

    @Override
    public float getReadSavedFromCompression() {
        return 0;
    }

    @Override
    public float getSentLostFromSSL() {
        return 0;
    }

    @Override
    public float getSentSavedFromCompression() {
        return 0;
    }

    @Override
    public Status getStatus() {
        return null;
    }

    @Override
    public long getTime() {
        return 0;
    }

    @Override
    public String getUserAgent() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isOutgoing() {
        return false;
    }

    @Override
    public boolean isPeer() {
        return false;
    }

    @Override
    public boolean isUltrapeerConnection() {
        return false;
    }

    @Override
    public boolean isUltrapeer() {
        return false;
    }

    @Override
    public void update() {
    }

}
