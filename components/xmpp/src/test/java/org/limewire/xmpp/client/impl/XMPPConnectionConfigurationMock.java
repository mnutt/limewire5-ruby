package org.limewire.xmpp.client.impl;

import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.RosterEvent;

public class XMPPConnectionConfigurationMock implements XMPPConnectionConfiguration {
    private final String username;
    private final String password;
    private final String serviceName;
    private final EventListener<RosterEvent> rosterListener;

    public XMPPConnectionConfigurationMock(String username, String password,
            String serviceName, EventListener<RosterEvent> rosterListener) {
        this.username = username;
        this.password = password;
        this.serviceName = serviceName;
        this.rosterListener = rosterListener;
    }
    
    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public String getUserInputLocalID() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public String getLabel() {
        return serviceName;
    }

    @Override
    public String getCanonicalizedLocalID() {
        return getUserInputLocalID();
    }

    @Override
    public String getNetworkName() {
        return getServiceName();
    }

    @Override
    public String getResource() {
        return "LimeWire";
    }
    
    @Override
    public EventListener<RosterEvent> getRosterListener() {
        return rosterListener;
    }
}