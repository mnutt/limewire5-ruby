package com.limegroup.gnutella.bootstrap;

import com.limegroup.gnutella.UDPPinger;

public interface UDPHostCacheFactory {

    public UDPHostCache createUDPHostCache(UDPPinger pinger);

    public UDPHostCache createUDPHostCache(long expiryTime, UDPPinger pinger);

}