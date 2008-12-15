package com.limegroup.gnutella.rudp.messages;

import java.nio.ByteBuffer;

import org.limewire.rudp.messages.AckMessage;
import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.FinMessage;
import org.limewire.rudp.messages.KeepAliveMessage;
import org.limewire.rudp.messages.MessageFormatException;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.rudp.messages.RUDPMessage.OpCode;
import org.limewire.rudp.messages.SynMessage.Role;

public class StubRUDPMessageFactory implements RUDPMessageFactory {
    
    private OpCode createNextMessage;
    
    void setNextMessageToCreate(OpCode createNextMessage) {
        this.createNextMessage = createNextMessage;
    }

    public AckMessage createAckMessage(byte connectionID, long sequenceNumber,
            long windowStart, int windowSpace) {
        return new StubAckMessage();
    }

    public DataMessage createDataMessage(byte connectionID,
            long sequenceNumber, ByteBuffer chunk) {
        return new StubDataMessage();
    }

    public FinMessage createFinMessage(byte connectionID, long sequenceNumber,
            byte reasonCode) {
        return new StubFinMessage();
    }

    public KeepAliveMessage createKeepAliveMessage(byte connectionID,
            long windowStart, int windowSpace) {
        return new StubKeepAliveMessage();
    }

    public RUDPMessage createMessage(ByteBuffer... data)
            throws MessageFormatException {
        
        if(createNextMessage == null)
            return new StubRUDPMessage();
        
        switch(createNextMessage) {
        case OP_ACK:       return new StubAckMessage(); 
        case OP_DATA:      return new StubDataMessage();
        case OP_FIN:       return new StubFinMessage();
        case OP_KEEPALIVE: return new StubKeepAliveMessage();
        case OP_SYN:       return new StubSynMessage();
        }
        
        // never hit.
        return null;
    }

    @Override
    public SynMessage createSynMessage(byte connectionID, Role role) {
        return new StubSynMessage();
    }

    @Override
    public SynMessage createSynMessage(byte connectionID, byte theirConnectionID, Role role) {
        return new StubSynMessage();
    }

}
