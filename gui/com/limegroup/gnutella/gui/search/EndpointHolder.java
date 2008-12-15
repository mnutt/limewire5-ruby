package com.limegroup.gnutella.gui.search;


import java.util.HashSet;
import java.util.Set;

import org.limewire.core.settings.FilterSettings;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;



/**
 * Holds one or more hosts.  Used for displaying the IP.
 */
class EndpointHolder implements Comparable<EndpointHolder> {
    
    /**
     * String for "Multiple"
     */
    private static final String MULTIPLE =
        I18n.tr("Multiple");    

    /**
     * The host this represents.
     */
    private final String _hostName;
    
    /**
     * The port of this host.
     */
    private final int _port;

    /**
     * Whether or not this IP is private.
     */
    private boolean _isPrivate;
    
    /**
     * The tag to display.
     */
    private String _tag;
    
    /**
     * The hosts this holds.
     */
    private Set<String> _hosts;
    
    /**
     * Builds an EndpointHolder with the specified host/port.
     */
    EndpointHolder(final String host, int port, boolean replyToMCast) {
        _hostName = host;
        _port = port;
        _isPrivate = !replyToMCast
                && GuiCoreMediator.getNetworkInstanceUtils().isPrivateAddress(host);
        _tag = host;
    }
    
    void addHost(final String host, int port) {
        if(_hosts == null) {
            _hosts = new HashSet<String>();
            _hosts.add(_hostName + ":" + _port);
        }
        
        _hosts.add(host + ":" + port);
        updateTag();
        _isPrivate = false;
    }
    
    void updateTag() {
        int size = _hosts != null ? _hosts.size() : 1;
        if (size > 1) {
            _tag = MULTIPLE + " (" + size + ")";
        }
    }
    
    void addHosts(Set<? extends IpPort> alts) {
        if(_hosts == null) {
            _hosts = new HashSet<String>();
            _hosts.add(_hostName + ":" + _port);
        }
        // only add a few altlocs per reply
        int added = 0;
        for (IpPort host : alts) {
        	if (added >= FilterSettings.MAX_ALTS_TO_DISPLAY.getValue()) {
        		break;
        	}
        	_hosts.add(host.getAddress() + ":" + host.getPort());
            ++added;
        }
        updateTag();
        _isPrivate = false;
    }
    
    /**
     * Gets the set of hosts.
     */
    Set<String> getHosts() {
        return _hosts;
    }
    
    /**
     * Returns the number of locations this holder knows about. The location
     * count value is based on core's understanding of how many results have
     * been received, which does not take into account duplicates; thus we
     * subtract the number of duplicates from this value before returning.
     */
    int getNumLocations() {
        return _hosts == null ? 1 : _hosts.size();
    }
    
    /**
     * Whether or not this endpoint represents a private address.
     */
    boolean isPrivateAddress() {
        return _isPrivate;
    }

    /**
     * Returns the tag of this holder.
     */
    @Override
    public String toString() {
        return _tag;
    }
    
    public int compareTo(EndpointHolder other) {
        int n1 = getNumLocations(), n2 = other.getNumLocations();
        if(n1 == 1 && n2 == 1)
            return _tag.compareTo(other._tag);
        else
            return n1 - n2;
    }
}
    
