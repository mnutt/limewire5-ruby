package org.limewire.xmpp.client.impl;

import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

/**
 * Uses the list of default servers in the XMPPConnectionConfiguration to return
 * a ConnectionConfiguration 
 */
public class FallbackConnectionConfigurationFactory implements ConnectionConfigurationFactory {

    @Override
    public boolean hasMore(XMPPConnectionConfiguration connectionConfiguration, RequestContext requestContext) {
        return requestContext.getNumRequests() < connectionConfiguration.getDefaultServers().size();
    }

    @Override
    public ConnectionConfiguration getConnectionConfiguration(XMPPConnectionConfiguration connectionConfiguration,
                                                              RequestContext requestContext) {
        checkHasMore(connectionConfiguration, requestContext);
        List<UnresolvedIpPort> defaultServers = connectionConfiguration.getDefaultServers();
        UnresolvedIpPort defaultServer = defaultServers.get(requestContext.getNumRequests());
        return new ConnectionConfiguration(defaultServer.getAddress(),
                defaultServer.getPort(), connectionConfiguration.getServiceName());
    }

    private void checkHasMore(XMPPConnectionConfiguration connectionConfiguration, RequestContext requestContext) {
        if(!hasMore(connectionConfiguration, requestContext)) {
            throw new IllegalStateException("no more ConnectionConfigurations");
        }
    }
}
