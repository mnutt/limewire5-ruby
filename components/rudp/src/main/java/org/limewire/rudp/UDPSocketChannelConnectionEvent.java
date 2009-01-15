package org.limewire.rudp;

import org.limewire.listener.DefaultEvent;

public class UDPSocketChannelConnectionEvent extends DefaultEvent<UDPSocketChannel, ConnectionState> {
    public UDPSocketChannelConnectionEvent(UDPSocketChannel source, ConnectionState event) {
        super(source, event);
    }
}
