package org.limewire.core.impl.xmpp;

import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

public class MockListenerSupport implements ListenerSupport<XMPPConnectionEvent>{

    public EventListener<XMPPConnectionEvent> listener;
    @Override
    public void addListener(EventListener<XMPPConnectionEvent> listener) {
        this.listener = listener;
    }

    @Override
    public boolean removeListener(EventListener<XMPPConnectionEvent> listener) {
        // TODO Auto-generated method stub
        return false;
    }
}
