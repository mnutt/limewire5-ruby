package com.limegroup.gnutella.connection;

import org.limewire.io.GUID;
import org.limewire.listener.DefaultSourceTypeEvent;


public class GnutellaConnectionEvent extends DefaultSourceTypeEvent<GnutellaConnection, GnutellaConnection.EventType> {
    private final GUID guid;

    public GnutellaConnectionEvent(GnutellaConnection source, GnutellaConnection.EventType event, GUID guid) {
        super(source, event);
        this.guid = guid;
    }

    /**
     * Returns the guid of this peer. 
     */
    // TODO remove 
    public GUID getGuid() {
        return guid;
    }
}
