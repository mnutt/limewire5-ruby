package com.limegroup.gnutella.rudp.messages;

import java.nio.ByteBuffer;
import java.util.Random;

import org.limewire.rudp.messages.SynMessage.Role;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

import com.limegroup.gnutella.messages.Message;

public class LimeRUDPMessageHelper {

    
    public static Message getAck(int id) {
        return (Message)new LimeRUDPMessageFactory(new DefaultMessageFactory()).createAckMessage((byte)id, 1, 1, 1);
    }
    
    public static Message getData(int id) {
        return (Message)new LimeRUDPMessageFactory(new DefaultMessageFactory()).createDataMessage((byte)id, 1, buffer(509));
    }
    
    public static Message getFin(int id) {
        return (Message)new LimeRUDPMessageFactory(new DefaultMessageFactory()).createFinMessage((byte)id, 1, (byte)1);
    }
    
    public static Message getKeepAlive(int id) {
        return (Message)new LimeRUDPMessageFactory(new DefaultMessageFactory()).createKeepAliveMessage((byte)id, 1, 1);
    }
    
    public static Message getSyn(int id) {
        return (Message)new LimeRUDPMessageFactory(new DefaultMessageFactory()).createSynMessage((byte)35, (byte)id, Role.UNDEFINED);
    }
    
    
    private static ByteBuffer buffer(int len) {
        byte[] b = new byte[len];
        Random r = new Random();
        r.nextBytes(b);
        return ByteBuffer.wrap(b);
    }
}
