package com.limegroup.gnutella.dht.io;

import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherListener;
import org.limewire.mojito.messages.DHTMessage.OpCode;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;

/**
 * Creates {@link LimeMessageDispatcherImpl}s.
 */
@Singleton
public class LimeMessageDispatcherFactoryImpl implements
        MessageDispatcherFactory {

    private final Provider<com.limegroup.gnutella.MessageDispatcher> messageDispatcher;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<SecureMessageVerifier> secureMessageVerifier;
    private final Provider<UDPService >udpService;
    private final MessageFactory messageFactory;
    
    @InspectionPoint("dht sent messages")
    private final Message.MessageCounter sentDHTMessageCounter = new Message.MessageCounter(50);

    @InspectionPoint("dht received messages")
    private final Message.MessageCounter receivedDHTMessageCounter = new Message.MessageCounter(50);
    
    @InspectionPoint("dht sent messages histogram")
    private final InspectionHistogram<OpCode> sentMessagesHistogram = new InspectionHistogram<OpCode>();
    
    @InspectionPoint("dht received messages histogram")
    private final InspectionHistogram<OpCode> receivedMessagesHistogram = new InspectionHistogram<OpCode>();
    
    private final CountingMessageDispatcherListener dispatcherListener = new CountingMessageDispatcherListener();

    @Inject
    public LimeMessageDispatcherFactoryImpl(
            Provider<com.limegroup.gnutella.MessageDispatcher> messageDispatcher,
            Provider<MessageRouter> messageRouter,
            Provider<SecureMessageVerifier> secureMessageVerifier, Provider<UDPService> udpService,
            MessageFactory messageFactory) {
        this.messageDispatcher = messageDispatcher;
        this.messageRouter = messageRouter;
        this.secureMessageVerifier = secureMessageVerifier;
        this.udpService = udpService;
        this.messageFactory = messageFactory;
    }

    public MessageDispatcher create(Context context) {
         LimeMessageDispatcherImpl messageDispatcherImpl = new LimeMessageDispatcherImpl(context, udpService,
                secureMessageVerifier, messageRouter, messageDispatcher, messageFactory);
         messageDispatcherImpl.addMessageDispatcherListener(dispatcherListener);
         return messageDispatcherImpl;
    }
    
    private class CountingMessageDispatcherListener implements MessageDispatcherListener {

        public void handleMessageDispatcherEvent(MessageDispatcherEvent event) {
            switch (event.getEventType()) {
            case MESSAGE_RECEIVED:
                receivedDHTMessageCounter.countMessage((Message)event.getMessage());
                receivedMessagesHistogram.count(event.getMessage().getOpCode());
                break;
            case MESSAGE_SENT:
                sentDHTMessageCounter.countMessage((Message)event.getMessage());
                sentMessagesHistogram.count(event.getMessage().getOpCode());
                break;
            }
        }
        
    }

}
