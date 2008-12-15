package com.limegroup.gnutella.gui;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.bugs.LocalClientInfo;
import com.limegroup.gnutella.bugs.SessionInfo;

/** A factory for creating LocalClientInfo objects. */
@Singleton
public class LocalClientInfoFactoryImpl implements LocalClientInfoFactory {
    
    private final Provider<SessionInfo> sessionInfo;
    
    @Inject
    public LocalClientInfoFactoryImpl(Provider<SessionInfo> info) {
        this.sessionInfo = info;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.gui.LocalClientInfoFactory#createLocalClientInfo(java.lang.Throwable, java.lang.String, java.lang.String, boolean)
     */
    public LocalClientInfo createLocalClientInfo(Throwable bug, String threadName, String detail, boolean fatal) {
        return new LocalClientInfo(bug, threadName, detail, fatal, sessionInfo.get());
    }

}
