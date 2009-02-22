package org.limewire.rudp;

import org.limewire.listener.DefaultSourceTypeEvent;

public class UDPSocketChannelConnectionEvent extends DefaultSourceTypeEvent<UDPSocketChannel, ConnectionState> {
    public UDPSocketChannelConnectionEvent(UDPSocketChannel source, ConnectionState event) {
        super(source, event);
    }
}
