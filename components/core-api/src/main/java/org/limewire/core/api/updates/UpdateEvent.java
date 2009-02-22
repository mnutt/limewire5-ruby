package org.limewire.core.api.updates;

import org.limewire.listener.DefaultSourceTypeEvent;

public class UpdateEvent extends DefaultSourceTypeEvent<UpdateInformation, UpdateEvent.Type> {

    public static enum Type {
        UPDATE
    }
    
    public UpdateEvent(UpdateInformation source, Type event) {
        super(source, event);
    }
}
