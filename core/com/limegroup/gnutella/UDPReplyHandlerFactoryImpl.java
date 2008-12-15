package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.filters.SpamFilter;

@Singleton
public class UDPReplyHandlerFactoryImpl implements UDPReplyHandlerFactory {

    private final UDPService udpService;

    @Inject
    public UDPReplyHandlerFactoryImpl(UDPService udpService) {
        this.udpService = udpService;
    }
    
    public UDPReplyHandler createUDPReplyHandler(InetSocketAddress addr, SpamFilter personalFilter) {
        return new UDPReplyHandler(addr, personalFilter, udpService);
    }

    public UDPReplyHandler createUDPReplyHandler(InetAddress addr, int port, SpamFilter personalFilter) {
        return new UDPReplyHandler(addr, port, personalFilter, udpService);
    }
}

