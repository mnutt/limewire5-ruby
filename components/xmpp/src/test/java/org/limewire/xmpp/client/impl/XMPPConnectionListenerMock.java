package org.limewire.xmpp.client.impl;

import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPConnectionListenerMock implements RegisteringEventListener<FriendConnectionEvent> {

    public void handleEvent(FriendConnectionEvent event) {
    }

    @Inject
    public void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(this);
    }
}
