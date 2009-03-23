package org.limewire.core.impl.updates;

import org.limewire.core.api.updates.UpdateInformation;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.version.UpdateEvent;
import com.limegroup.gnutella.version.UpdateHandler;

@Singleton
public class UpdateListener implements EventListener<UpdateEvent> {

    private final EventBroadcaster<org.limewire.core.api.updates.UpdateEvent> uiListeners;
    
    @Inject
    public UpdateListener(UpdateHandler updateHandler, EventBroadcaster<org.limewire.core.api.updates.UpdateEvent> listeners) {
        this.uiListeners = listeners;
        updateHandler.addListener(this);
    }
    
    @Override
    public void handleEvent(UpdateEvent event) {
        com.limegroup.gnutella.version.UpdateInformation eventData = event.getData();
        UpdateInformation info = new UpdateInformationImpl(eventData.getButton1Text(),
                eventData.getButton2Text(), eventData.getUpdateCommand(),
                eventData.getUpdateText(), eventData.getUpdateTitle(), eventData.getUpdateURL());

        uiListeners.broadcast(new org.limewire.core.api.updates.UpdateEvent(info, org.limewire.core.api.updates.UpdateEvent.Type.UPDATE));
    }
}
