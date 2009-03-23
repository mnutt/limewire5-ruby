package org.limewire.core.api.connection;

import org.limewire.listener.DefaultDataTypeEvent;

public class FirewallTransferStatusEvent extends DefaultDataTypeEvent<FirewallTransferStatus, FWTStatusReason> {

    public FirewallTransferStatusEvent(FirewallTransferStatus status, FWTStatusReason reason) {
        super(status, reason);
    }
}
