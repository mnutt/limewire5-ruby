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
        updateHandler.addListener(this);
        this.uiListeners = listeners;
    }
    
    @Override
    public void handleEvent(UpdateEvent event) {
        UpdateInformation info = new UpdateInformationImpl(event.getSource().getButton1Text(),
                event.getSource().getButton2Text(), event.getSource().getUpdateCommand(),
                event.getSource().getUpdateText(), event.getSource().getUpdateTitle(), event.getSource().getUpdateURL());

        uiListeners.broadcast(new org.limewire.core.api.updates.UpdateEvent(info, org.limewire.core.api.updates.UpdateEvent.Type.UPDATE));
    }
}
