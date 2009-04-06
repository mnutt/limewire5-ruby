package com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import org.limewire.collection.Comparators;
import org.limewire.core.settings.FilterSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.filters.response.ResponseFilterFactory;
import com.limegroup.gnutella.search.SearchResultHandler;

@Singleton
public class SpamServicesImpl implements SpamServices {
    
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<IPFilter> ipFilter;
    private final Provider<URNFilter> urnFilter;
    private final SpamFilterFactory spamFilterFactory;
    private final UDPReplyHandlerCache udpReplyHandlerCache;
    private final SearchResultHandler searchResultHandler;
    private final ResponseFilterFactory responseFilterFactory;

    @Inject
    public SpamServicesImpl(Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter, Provider<URNFilter> urnFilter,
            SpamFilterFactory spamFilterFactory,
            UDPReplyHandlerCache udpReplyHandlerCache,
            SearchResultHandler searchResultHandler,
            ResponseFilterFactory responseFilterFactory) {
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
        this.urnFilter = urnFilter;
        this.spamFilterFactory = spamFilterFactory;
        this.udpReplyHandlerCache = udpReplyHandlerCache;
        this.searchResultHandler = searchResultHandler;
        this.responseFilterFactory = responseFilterFactory;
    }
    
    public void adjustSpamFilters() {
        searchResultHandler.setResponseFilter(responseFilterFactory.createResponseFilter());
        
        udpReplyHandlerCache.setPersonalFilter(spamFilterFactory.createPersonalFilter());
        
        //Just replace the spam filters.  No need to do anything
        //fancy like incrementally updating them.
        for(RoutedConnection c : connectionManager.get().getConnections()) {
            if(ipFilter.get().allow(c.getAddress())) {
                c.setPersonalFilter(spamFilterFactory.createPersonalFilter());
                c.setRouteFilter(spamFilterFactory.createRouteFilter());
            } else {
                // If the connection isn't allowed now, close it.
                c.close();
            }
        }
        
        // TODO: notify DownloadManager & UploadManager about new banned IP ranges
    }

    public void reloadIPFilter() {
        ipFilter.get().refreshHosts(new IPFilter.IPFilterCallback() {
            public void ipFiltersLoaded() {
                adjustSpamFilters();
            }
        });
    }
    
    public void reloadURNFilter() {
        urnFilter.get().refreshURNs();
    }


    public boolean isAllowed(InetAddress host) {
        return ipFilter.get().allow(host.getAddress());
    }

    public void blockHost(String host) {
        // FIXME move into IPFilter
        // FIXME synchronize access to setting properly?
        String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.get();
        Arrays.sort(bannedIPs, Comparators.stringComparator());

        if (Arrays.binarySearch(bannedIPs, host, Comparators.stringComparator()) < 0) {
            String[] more_banned = new String[bannedIPs.length + 1];
            System.arraycopy(bannedIPs, 0, more_banned, 0, bannedIPs.length);
            more_banned[bannedIPs.length] = host;
            FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(more_banned);
            reloadIPFilter();
        }
    }

    public void unblockHost(String host) {
        // FIXME move into IPFilter
        // FIXME synchronize access to setting properly?
        String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES
                .get();
        List<String> bannedList = Arrays.asList(bannedIPs);
        if (bannedList.remove(host)) {
            FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(bannedList.toArray(new String[0]));
            reloadIPFilter();
        }
    }

}
