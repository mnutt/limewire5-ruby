package org.limewire.xmpp.client.impl;

import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPConnectionListenerMock implements RegisteringEventListener<XMPPConnectionEvent> {

    public void handleEvent(XMPPConnectionEvent event) {
    }

    @Inject
    public void register(ListenerSupport<XMPPConnectionEvent> listenerSupport) {
        listenerSupport.addListener(this);
    }
}
