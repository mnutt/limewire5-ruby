package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.AckMessage;

public class StubAckMessage extends StubRUDPMessage implements AckMessage {

    public void extendWindowStart(long wStart) {
    }

    public int getWindowSpace() {
        return 0;
    }

    public long getWindowStart() {
        return 0;
    }

}
