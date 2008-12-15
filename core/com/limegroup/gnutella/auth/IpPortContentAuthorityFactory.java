package com.limegroup.gnutella.auth;

import org.limewire.io.IpPort;

public interface IpPortContentAuthorityFactory {

    public IpPortContentAuthority createIpPortContentAuthority(IpPort host);

    public IpPortContentAuthority createIpPortContentAuthority(String host,
            int port);

}