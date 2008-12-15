package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmStatus;

public class FinishedSwarmStatus implements SwarmStatus {

    public boolean isError() {
        return false;
    }

    public boolean isOk() {
        return true;
    }

    public boolean isFinished() {
        return true;
    }

}
