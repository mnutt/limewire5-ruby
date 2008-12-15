package org.limewire.core.impl.updates;

import org.limewire.core.api.updates.UpdateEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class MockUpdatesModule extends AbstractModule {

    @Override
    protected void configure() {        
        EventMulticaster<UpdateEvent> uiListeners = new EventMulticasterImpl<UpdateEvent>();
        bind(new TypeLiteral<ListenerSupport<UpdateEvent>>(){}).toInstance(uiListeners);
        bind(new TypeLiteral<EventBroadcaster<UpdateEvent>>(){}).toInstance(uiListeners);
    }
}
