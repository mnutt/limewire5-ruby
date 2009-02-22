package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.listener.DefaultSourceTypeEvent;

public class AddressEvent extends DefaultSourceTypeEvent<Address, Address.EventType> {

    public AddressEvent(Address source, Address.EventType event) {
        super(source, event);
    }
}
