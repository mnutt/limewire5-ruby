package com.limegroup.gnutella.filters;

import java.net.SocketAddress;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IP;

/**
 * Defines an interface to find out if an IP address is banned. 
 */

public interface IPFilter extends SpamFilter {
    
    public void refreshHosts(IPFilterCallback callback);

    /**
     * @return true if there are black listed hosts in the filter.
     */
    public boolean hasBlacklistedHosts();
    
    /**
     * Calculates the first set bit in the distance between an IPv4 address and
     * the ranges represented by this list.
     * 
     * This is equivalent to floor(log2(distance)) + 1.
     *  
     * @param ip an IPv4 address, represented as an IP object with a /32 netmask.
     * @return an int on the interval [0,31], or 32 if the address is whitelisted.
     */
    public int logMinDistanceTo(IP ip);
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param ip address in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP address out of it.
     */   
    public boolean allow(IP ip);
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param addr an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP address out of it.
     */
    public boolean allow(SocketAddress addr);
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param addr an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP address out of it.
     */
    public boolean allow(String addr);
    
    /**
     * Checks if the address is of type {@link Connectable} and checks
     * if the ip address is banned.
     * 
     * Can be extended to handle other address types if they have the notion
     * of a public/unique ip address.
     * 
     * @return if this host is allowed or the type of address can't be filtered,
     *  false if it is banned or we are unable to create correct IP address out of it.
     */
    public boolean allow(Address address);
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param addr an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP address out of it.
     */
    public boolean allow(byte [] addr);
    
    /**
     * Updates the hosts in the IP filter.
     */
    public void refreshHosts();
    
    /**
     * Defines an interface for loading IP Filters through a callback.
     */
    public interface IPFilterCallback {
        public void ipFiltersLoaded();
    }

}
