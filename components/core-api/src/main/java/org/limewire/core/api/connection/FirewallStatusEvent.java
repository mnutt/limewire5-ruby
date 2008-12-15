package org.limewire.core.api.connection;

import org.limewire.listener.AbstractSourcedEvent;

public class FirewallStatusEvent extends AbstractSourcedEvent<FirewallStatus> {

    public FirewallStatusEvent(FirewallStatus source) {
        super(source);
    }
}
