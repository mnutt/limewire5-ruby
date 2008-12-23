package com.limegroup.gnutella.auth;


import java.net.UnknownHostException;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.Message;

/** A ContentAuthority that sends to a single IpPort. */
public class IpPortContentAuthority implements ContentAuthority {

    private static Log LOG = LogFactory.getLog(IpPortContentAuthority.class);
    
    private IpPort authority;
    
    /** host/port to store which'll be used when initializing, if necessary */
    private String host;
    private int port;

    private final UDPService udpService;

    /** Constructs the authority with the given IpPort. 
     * @param service */
    IpPortContentAuthority(IpPort host, UDPService udpService) {
        this.authority = host;
        this.udpService = udpService;
        this.host = host.getAddress();
        this.port = host.getPort();
    }
    
    /**
     * Constructs the authority with the given host/port.
     * You must call initialize prior to sending a message.
     * @param udpService 
     */
    IpPortContentAuthority(String host, int port, UDPService udpService) {
        this.host = host;
        this.port = port;
        this.udpService = udpService;
    }

    /** Sends a message to the authority. */
    public void send(Message m) {
        LOG.debugf("sending {0} to {1}", m, authority);
        udpService.send(m, authority);
    }

    /** Constructs the authority from the host/port if necessary */
    public boolean initialize() {
        if (authority == null) {
            try {
                authority = new IpPortImpl(host, port);
            } catch (UnknownHostException uhe) {
                return false;
                // ignored.
            }
        }
        
        return true;
    }
    
    public IpPort getIpPort() {
        return authority;
    }

}
