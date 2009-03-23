package org.limewire.core.api.connection;

import org.limewire.listener.DefaultDataEvent;

public class FirewallStatusEvent extends DefaultDataEvent<FirewallStatus> {

    public FirewallStatusEvent(FirewallStatus data) {
        super(data);
    }
}
