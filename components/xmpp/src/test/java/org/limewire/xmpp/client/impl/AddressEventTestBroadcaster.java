package org.limewire.xmpp.client.impl;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.AddressEvent;

public class AddressEventTestBroadcaster implements ListenerSupport<AddressEvent> {
    
    public final EventListenerList<AddressEvent> listeners =
        new EventListenerList<AddressEvent>();

    public void addListener(EventListener<AddressEvent> addressEventEventListener) {
        listeners.addListener(addressEventEventListener);
    }

    public boolean removeListener(EventListener<AddressEvent> addressEventEventListener) {
        return listeners.removeListener(addressEventEventListener);
    }
}
