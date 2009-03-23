package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.listener.DefaultDataTypeEvent;

public class AddressEvent extends DefaultDataTypeEvent<Address, Address.EventType> {

    public AddressEvent(Address data, Address.EventType event) {
        super(data, event);
    }
}
