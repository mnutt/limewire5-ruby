package org.limewire.core.api.updates;

import org.limewire.listener.DefaultDataTypeEvent;

public class UpdateEvent extends DefaultDataTypeEvent<UpdateInformation, UpdateEvent.Type> {

    public static enum Type {
        UPDATE
    }
    
    public UpdateEvent(UpdateInformation data, Type event) {
        super(data, event);
    }
}
