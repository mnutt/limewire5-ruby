package com.limegroup.gnutella.auth;

import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.UDPService;

@Singleton
public class IpPortContentAuthorityFactoryImpl implements IpPortContentAuthorityFactory {

    private final Provider<UDPService> udpService;

    @Inject
    public IpPortContentAuthorityFactoryImpl(Provider<UDPService> udpService) {
        this.udpService = udpService;
    }
    
    public IpPortContentAuthority createIpPortContentAuthority(
            IpPort host) {
        return new IpPortContentAuthority(host, udpService.get());
    }

    public IpPortContentAuthority createIpPortContentAuthority(
            String host, int port) {
        return new IpPortContentAuthority(host, port, udpService.get());
    }

}
