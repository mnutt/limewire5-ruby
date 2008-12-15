package com.limegroup.gnutella.rudp.messages;

import java.nio.ByteBuffer;

import org.limewire.rudp.messages.DataMessage;

public class StubDataMessage extends StubRUDPMessage implements DataMessage {

    public ByteBuffer getChunk() {
        // TODO Auto-generated method stub
        return null;
    }

    public ByteBuffer getData1Chunk() {
        // TODO Auto-generated method stub
        return null;
    }

    public ByteBuffer getData2Chunk() {
        // TODO Auto-generated method stub
        return null;
    }

    public byte getDataAt(int i) {
        // TODO Auto-generated method stub
        return 0;
    }

}
