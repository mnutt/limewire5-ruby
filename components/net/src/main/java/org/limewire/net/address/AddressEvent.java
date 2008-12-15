package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.listener.DefaultEvent;

public class AddressEvent extends DefaultEvent<Address, Address.EventType> {

    public AddressEvent(Address source, Address.EventType event) {
        super(source, event);
    }
}
