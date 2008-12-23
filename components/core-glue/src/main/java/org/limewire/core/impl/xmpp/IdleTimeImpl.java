package org.limewire.core.impl.xmpp;

import org.limewire.util.SystemUtils;

import com.google.inject.Singleton;

@Singleton
class IdleTimeImpl implements IdleTime {

    @Override
    public long getIdleTime() {
        return SystemUtils.getIdleTime();
    }

    @Override
    public boolean supportsIdleTime() {
        return SystemUtils.supportsIdleTime();
    }
}
