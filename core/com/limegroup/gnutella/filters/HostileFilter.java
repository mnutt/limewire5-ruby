package com.limegroup.gnutella.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.IP;
import org.limewire.io.NetworkInstanceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HostileFilter extends  AbstractIPFilter {

    private static final Log LOG = LogFactory.getLog(HostileFilter.class);
        
    private volatile IPList hostileHosts = new IPList();
    
    private final NetworkInstanceUtils networkInstanceUtils;
    
    @Inject
    public HostileFilter(NetworkInstanceUtils networkInstanceUtils) {
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    /**
     * Refresh the IPFilter's instance.
     */
    @Override
    public void refreshHosts(IPFilterCallback callback) {
        refreshHosts();
        callback.ipFiltersLoaded();
    }
    
    @Override
    public void refreshHosts() {
        LOG.info("refreshing hosts at hostile level");
        // Load hostile, making sure the list is valid
        IPList newHostile = new IPList();
        if(!FilterSettings.USE_NETWORK_FILTER.getValue()) {
            hostileHosts = newHostile;
            return;
        }
        String [] allHosts = FilterSettings.HOSTILE_IPS.getValue();
        try {
            for(String ip : allHosts)
                newHostile.add(new IP(ip));
            if(newHostile.isValidFilter(false, networkInstanceUtils)) {
                LOG.debug("filter was valid");
                hostileHosts = newHostile;
            } else {
                LOG.debug("filter was invalid");
            }
        } catch(IllegalArgumentException badSimpp){
            LOG.debug("SIMPP was invalid", badSimpp);
        }
    }
    
    @Override
    public boolean hasBlacklistedHosts() {
        return !hostileHosts.isEmpty();
    }
    
    @Override
    public int logMinDistanceTo(IP ip) {
        return hostileHosts.logMinDistanceTo(ip);
    }
    
    @Override
    protected boolean allowImpl(IP ip) {
        return !hostileHosts.contains(ip);
    }
}
