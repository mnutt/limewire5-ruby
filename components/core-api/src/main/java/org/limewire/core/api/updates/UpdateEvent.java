package org.limewire.core.api.updates;

import org.limewire.listener.DefaultEvent;

public class UpdateEvent extends DefaultEvent<UpdateInformation, UpdateEvent.Type> {

    public static enum Type {
        UPDATE
    }
    
    public UpdateEvent(UpdateInformation source, Type event) {
        super(source, event);
    }
}
