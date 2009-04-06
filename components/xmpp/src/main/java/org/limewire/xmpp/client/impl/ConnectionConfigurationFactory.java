package org.limewire.xmpp.client.impl;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

/**
 * Creates ConnectionConfigurations for XMPPConnectionConfigurations.  Typically backed by a collection
 * of hosts and ports. 
 */
public interface ConnectionConfigurationFactory {

    /**
     * Used to track state between multiple calls to hasMore and getConnectionConfiguration
     */
    class RequestContext {
        private int numRequests;
        
        int getNumRequests() {
            return numRequests;
        }
        
        void incrementRequests() {
            numRequests++;
        }
    }

    /**
     * @param connectionConfiguration
     * @param requestContext; callers should increment the requests after each call to 
     * getConnectionConfiguration 
     * @return whether there are remaining ConnectionConfigurations that can be retrived
     */
    boolean hasMore(XMPPConnectionConfiguration connectionConfiguration, RequestContext requestContext);
    
    /**
     * lookups up ConnectionConfiguration for the given XMPPConnectionConfiguration
     * @param connectionConfiguration
     * @param requestContext
     * @return a ConnectionConfiguration; never null
     * @throws IllegalStateException is hasMore returns false
     */
    ConnectionConfiguration getConnectionConfiguration(XMPPConnectionConfiguration connectionConfiguration,
                                                       RequestContext requestContext);
}
