package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.SynMessage;

public class StubSynMessage extends StubRUDPMessage implements SynMessage {

    public int getProtocolVersionNumber() {
        // TODO Auto-generated method stub
        return 0;
    }

    public byte getSenderConnectionID() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Role getRole() {
        return Role.UNDEFINED;
    }

}
