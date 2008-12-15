package com.limegroup.gnutella.rudp.messages;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;

public class RUDPMessageHandlerHelper {
    
    public static Class[] getMessageClasses() {
        Class[] handledMessageClasses = new Class[] {
                LimeAckMessageImpl.class,
                LimeDataMessageImpl.class,
                LimeFinMessageImpl.class,
                LimeKeepAliveMessageImpl.class,
                LimeSynMessageImpl.class,
        };
        return handledMessageClasses;
    }
    
    public static void setHandlers(MessageRouter messageRouter, MessageHandler ack, MessageHandler data,
                                   MessageHandler fin, MessageHandler keepAlive,
                                   MessageHandler syn) throws Exception {
        messageRouter.setUDPMessageHandler(LimeAckMessageImpl.class, ack);
        messageRouter.setUDPMessageHandler(LimeDataMessageImpl.class, data);
        messageRouter.setUDPMessageHandler(LimeFinMessageImpl.class, fin);
        messageRouter.setUDPMessageHandler(LimeKeepAliveMessageImpl.class, keepAlive);
        messageRouter.setUDPMessageHandler(LimeSynMessageImpl.class, syn);
    }
    
    public static void addHandler(MessageRouter messageRouter, MessageHandler handler) {
        messageRouter.addUDPMessageHandler(LimeAckMessageImpl.class, handler);
        messageRouter.addUDPMessageHandler(LimeDataMessageImpl.class, handler);
        messageRouter.addUDPMessageHandler(LimeFinMessageImpl.class, handler);
        messageRouter.addUDPMessageHandler(LimeKeepAliveMessageImpl.class, handler);
        messageRouter.addUDPMessageHandler(LimeSynMessageImpl.class, handler);
    }
    
    public static void setHandlerFields(MessageRouter messageRouter, Object object, String ack, String data,
                                   String fin, String keepAlive, String syn) throws Exception {
        set(messageRouter, object, ack,       LimeAckMessageImpl.class);
        set(messageRouter, object, data,      LimeDataMessageImpl.class);
        set(messageRouter, object, fin,       LimeFinMessageImpl.class);
        set(messageRouter, object, keepAlive, LimeKeepAliveMessageImpl.class);
        set(messageRouter, object, syn,       LimeSynMessageImpl.class);
    }
    
    private static void set(MessageRouter messageRouter, Object o, String field, Class<? extends Message> clazz) throws Exception {
        PrivilegedAccessor.setValue(o, field, messageRouter.getUDPMessageHandler(clazz));
    }

}
