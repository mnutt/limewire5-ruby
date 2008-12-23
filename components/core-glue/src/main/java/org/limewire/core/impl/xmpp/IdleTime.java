package org.limewire.core.impl.xmpp;

public interface IdleTime {
    boolean supportsIdleTime();
    
    long getIdleTime();
}
