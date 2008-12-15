package org.limewire.core.api.connection;

import org.limewire.listener.DefaultEvent;

public class FirewallTransferStatusEvent extends DefaultEvent<FirewallTransferStatus, FWTStatusReason> {

    public FirewallTransferStatusEvent(FirewallTransferStatus status, FWTStatusReason reason) {
        super(status, reason);
    }
}
