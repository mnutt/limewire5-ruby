package org.limewire.core.impl.xmpp;

import java.util.ArrayList;

public class MockIdleTime implements IdleTime {

    public ArrayList<Long> getIdleTimeReturn = new ArrayList<Long>();
    @Override
    public long getIdleTime() {
        if (!getIdleTimeReturn.isEmpty()) {
            return getIdleTimeReturn.remove(0);
        }
        supportsIdleTimeReturn = false;
        return 0;
    }

    public boolean supportsIdleTimeReturn;
    @Override
    public boolean supportsIdleTime() {
        return supportsIdleTimeReturn;
    }
}
