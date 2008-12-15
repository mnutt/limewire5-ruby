package com.limegroup.gnutella.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.IOUtils;
import org.limewire.io.IP;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


/**
 * Blocks messages and hosts based on IP address.  
 */
@Singleton
public final class LocalIPFilter extends AbstractIPFilter {
    
    private static final Log LOG = LogFactory.getLog(LocalIPFilter.class);
    
    private volatile IPList badHosts;
    private volatile IPList goodHosts;
    /** List contained in hostiles.txt if any.  Loaded on startup only */ 
    private final IPList hostilesTXTHosts = new IPList();
    
    private final IPFilter hostileNetworkFilter;
    private final ScheduledExecutorService ipLoader;
    /** Marker for whether or not hostiles need to be loaded. */
    private volatile boolean shouldLoadHostiles;
    
    private volatile long whitelistings; // # of times we whitelisted an ip 
    private volatile long blacklistings; // # of times we blacklisted an ip 
    private volatile long netblockings;  // # of times net blacklisted an ip 
    private volatile long implicitings;  // # of times an ip was implicitly allowed
    
    /** Constructs an IPFilter that automatically loads the content. */
    @Inject
    public LocalIPFilter(@Named("hostileFilter") IPFilter hostileNetworkFilter, 
            @Named("backgroundExecutor") ScheduledExecutorService ipLoader) {
        this.hostileNetworkFilter = hostileNetworkFilter;
        this.ipLoader = ipLoader;
        
        File hostiles = new File(CommonUtils.getUserSettingsDir(), "hostiles.txt");
        shouldLoadHostiles = hostiles.exists();
        
        hostileNetworkFilter.refreshHosts();
        refreshHosts();
    }
    
    public void refreshHosts() {
        refreshHosts(null);
    }
    
    public void refreshHosts(final IPFilterCallback callback) {
        Runnable load = new Runnable() {
            public void run() {
                hostileNetworkFilter.refreshHosts();
                refreshHostsImpl();
                if (callback != null)
                    callback.ipFiltersLoaded();
            }
        };
        if (!shouldLoadHostiles) 
            load.run();
        else 
            ipLoader.execute(load);
    }
    
    /** Does the work of setting new good  & bad hosts. */
    private void refreshHostsImpl() {
        LOG.debug("refreshing hosts");
        
        // Load basic bad...
        IPList newBad = new IPList();
        String[] allHosts = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++) {
            newBad.add(allHosts[i]);
        }
        
        // Load basic good...
        IPList newGood = new IPList();
        allHosts = FilterSettings.WHITE_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++) {
            newGood.add(allHosts[i]);
        }

        // Load data from hostiles.txt (if it wasn't already loaded!)...
        if(shouldLoadHostiles) {
            shouldLoadHostiles = false;
            
            LOG.debug("loading hostiles");
            File hostiles = new File(CommonUtils.getUserSettingsDir(), "hostiles.txt");
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(hostiles));
                String read = null;
                while( (read = reader.readLine()) != null) {
                    hostilesTXTHosts.add(read);
                }
            } catch(IOException ignored) {
                LOG.debug("iox loading hostiles",ignored);
            } finally {
                IOUtils.close(reader);
            }
        }
        
        badHosts = new MultiIPList(newBad, hostilesTXTHosts);
        goodHosts = newGood;
    }
    
    /** Determiens if any blacklisted hosts exist. */
    public boolean hasBlacklistedHosts() {
        return 
          (FilterSettings.USE_NETWORK_FILTER.getValue() && hostileNetworkFilter.hasBlacklistedHosts())
          || !badHosts.isEmpty();
    }
    
    /** The logmin distance to bad or hostile ips. */
    public int logMinDistanceTo(IP ip) {
        // If the address is on the whitelist, return the maximum distance
        if(goodHosts.logMinDistanceTo(ip) == 0)
            return 32;
        int min = badHosts.logMinDistanceTo(ip);
        if(FilterSettings.USE_NETWORK_FILTER.getValue())
            min = Math.min(min, hostileNetworkFilter.logMinDistanceTo(ip));
        return min;
    }
    
    @Override
    protected boolean allowImpl(IP ip) {
        if (goodHosts.contains(ip)) {
            whitelistings++;
            return true;
        }

        if (badHosts.contains(ip)) {
            blacklistings++;
            return false;
        }

        if (FilterSettings.USE_NETWORK_FILTER.getValue() && !hostileNetworkFilter.allow(ip)) {
            netblockings++;
            return false;
        }

        implicitings++;
        return true;
    }
    
    @InspectableContainer
    @SuppressWarnings("unused")
    private class IPFilterInspectable {
        
        @InspectionPoint("ip filter counts")
        private final Inspectable counts = new Inspectable() {
            public Object inspect() {
                Map<String,Object> ret = new HashMap<String, Object>();
                ret.put("white",whitelistings);
                ret.put("block",blacklistings);
                ret.put("netblock", netblockings);
                ret.put("implicit", implicitings);
                return ret;
            }
        };
    }

}



