package org.limewire.xmpp.client.impl;

import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

/**
 * Creates XMPPConnectionImpls.  Used i conjunction with @AssistedInject
 */
public interface XMPPConnectionImplFactory {
    XMPPConnectionImpl createConnection(XMPPConnectionConfiguration configuration,
                                        ListeningExecutorService executorService);
        
}
