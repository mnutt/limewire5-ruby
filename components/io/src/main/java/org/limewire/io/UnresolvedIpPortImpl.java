package org.limewire.io;

import java.net.UnknownHostException;

import org.limewire.util.Objects;

public class UnresolvedIpPortImpl implements UnresolvedIpPort {
    
    private final String host;
    private final int port;

    public UnresolvedIpPortImpl(String host, int port) {
        this.host = Objects.nonNull(host, "host");
        this.port = port;
    }

    @Override
    public IpPort resolve() throws UnknownHostException {
        return new IpPortImpl(host, port);
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getAddress() {
        return host;
    }
}
