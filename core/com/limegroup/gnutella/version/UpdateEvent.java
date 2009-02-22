package com.limegroup.gnutella.version;

import org.limewire.listener.DefaultSourceTypeEvent;

public class UpdateEvent extends DefaultSourceTypeEvent<UpdateInformation, UpdateEvent.Type> {

    public static enum Type {
        UPDATE
    }
    
    public UpdateEvent(UpdateInformation source, Type event) {
        super(source, event);
    }
}
