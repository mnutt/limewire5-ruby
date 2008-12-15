package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.KeepAliveMessage;

public class StubKeepAliveMessage extends StubRUDPMessage implements
        KeepAliveMessage {

    public void extendWindowStart(long wStart) {
        // TODO Auto-generated method stub

    }

    public int getWindowSpace() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getWindowStart() {
        // TODO Auto-generated method stub
        return 0;
    }

}
