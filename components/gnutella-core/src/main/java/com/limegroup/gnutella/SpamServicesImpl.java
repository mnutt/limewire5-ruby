package com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.core.settings.FilterSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.filters.response.ResponseFilterFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.search.SearchResultHandler;

@Singleton
public class SpamServicesImpl implements SpamServices, SpamFilter.LoadCallback {

    private static final Log LOG = LogFactory.getLog(SpamServicesImpl.class);
    
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<IPFilter> ipFilter;
    private final Provider<URNFilter> urnFilter;
    private final SpamFilterFactory spamFilterFactory;
    private final SearchResultHandler searchResultHandler;
    private final ResponseFilterFactory responseFilterFactory;
    private volatile SpamFilter personalFilter;
    private final AtomicInteger filtersLoading;

    @Inject
    public SpamServicesImpl(Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter, Provider<URNFilter> urnFilter,
            SpamFilterFactory spamFilterFactory,
            SearchResultHandler searchResultHandler,
            ResponseFilterFactory responseFilterFactory) {
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
        this.urnFilter = urnFilter;
        this.spamFilterFactory = spamFilterFactory;
        this.searchResultHandler = searchResultHandler;
        this.responseFilterFactory = responseFilterFactory;
        personalFilter = spamFilterFactory.createPersonalFilter();
        filtersLoading = new AtomicInteger(0);
    }

    @Override
    public void spamFilterLoaded() {
        // Wait until all filters have loaded
        if(filtersLoading.decrementAndGet() == 0)
            adjustSpamFilters();
    }
    
    @Override
    public void adjustSpamFilters() {
        LOG.trace("Adjusting spam filters");
        personalFilter = spamFilterFactory.createPersonalFilter();
        searchResultHandler.setResponseFilter(responseFilterFactory.createResponseFilter());
        
        // Replace the route filter on each connection
        for(RoutedConnection c : connectionManager.get().getConnections()) {
            if(ipFilter.get().allow(c.getAddress())) {
                c.setRouteFilter(spamFilterFactory.createRouteFilter());
            } else {
                // If the connection isn't allowed now, close it.
                c.close();
            }
        }

        // TODO: notify DownloadManager & UploadManager about new banned IP ranges
    }
    
    @Override
    public void reloadIPFilter() {
        LOG.trace("Reloading IP filter");
        filtersLoading.addAndGet(1);
        ipFilter.get().refreshHosts(this);
    }

    @Override
    public void reloadSpamFilters() {
        LOG.trace("Reloading spam filters");
        filtersLoading.addAndGet(2);
        ipFilter.get().refreshHosts(this);
        urnFilter.get().refreshURNs(this);
    }

    @Override
    public boolean isAllowed(InetAddress host) {
        return ipFilter.get().allow(host.getAddress());
    }

    @Override
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
            reloadSpamFilters();
        }
    }

    @Override
    public void unblockHost(String host) {
        // FIXME move into IPFilter
        // FIXME synchronize access to setting properly?
        String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.get();
        List<String> bannedList = Arrays.asList(bannedIPs);
        if (bannedList.remove(host)) {
            FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(bannedList.toArray(new String[0]));
            reloadSpamFilters();
        }
    }

    @Override
    public boolean isPersonalSpam(Message m) {
        return !personalFilter.allow(m);
    }
}
