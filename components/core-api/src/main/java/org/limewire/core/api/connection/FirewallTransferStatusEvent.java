package org.limewire.core.api.connection;

import org.limewire.listener.DefaultSourceTypeEvent;

public class FirewallTransferStatusEvent extends DefaultSourceTypeEvent<FirewallTransferStatus, FWTStatusReason> {

    public FirewallTransferStatusEvent(FirewallTransferStatus status, FWTStatusReason reason) {
        super(status, reason);
    }
}
