package org.limewire.core.api.connection;

import org.limewire.listener.DefaultSourceEvent;

public class FirewallStatusEvent extends DefaultSourceEvent<FirewallStatus> {

    public FirewallStatusEvent(FirewallStatus source) {
        super(source);
    }
}
