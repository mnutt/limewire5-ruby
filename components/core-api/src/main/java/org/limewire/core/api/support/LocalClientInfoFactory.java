package org.limewire.core.api.support;


public interface LocalClientInfoFactory {

    /** Constructs a new LocalClientInfo with the given names. */
    public LocalClientInfo createLocalClientInfo(Throwable bug, String threadName, String detail,
            boolean fatal);

}