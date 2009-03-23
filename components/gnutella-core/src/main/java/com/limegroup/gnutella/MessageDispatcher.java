package com.limegroup.gnutella;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import org.limewire.inspection.InspectionPoint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Dispatches messages to the MessageRouter.
 */
@Singleton
public class MessageDispatcher {
    
    private final Executor DISPATCH;

    private final MessageRouter messageRouter;
    
    @InspectionPoint("routed messages")
    private final Message.MessageCounter messageCounter = new Message.MessageCounter(LimeWireUtils.isBetaRelease() ? 300 : 30);
    
    @Inject
    public MessageDispatcher(MessageRouter messageRouter, @Named("messageExecutor") Executor dispatch) {
        this.messageRouter = messageRouter;
        this.DISPATCH = dispatch;
    }
    
    /** Dispatches a runnable, to allow arbitrary runnables to be processed on the message thread. */
    public void dispatch(Runnable r) {
        DISPATCH.execute(r);
    }
    
    /**
     * Dispatches a UDP message.
     */
    public void dispatchUDP(Message m, InetSocketAddress addr) {
        DISPATCH.execute(new UDPDispatch(messageRouter, m, addr, messageCounter));
    }
    
    /**
     * Dispatches a Multicast message.
     */
    public void dispatchMulticast(Message m, InetSocketAddress addr) {
        DISPATCH.execute(new MulticastDispatch(messageRouter, m, addr, messageCounter));
    }
    
    /**
     * Dispatches a TCP message.
     */
    public void dispatchTCP(Message m, RoutedConnection conn) {
        DISPATCH.execute(new TCPDispatch(messageRouter, m, conn, messageCounter));
    }
    
    
    private static abstract class Dispatch implements Runnable {
        protected final MessageRouter messageRouter;
        protected final Message m;
        protected final Message.MessageCounter counter;
        
        Dispatch(MessageRouter messageRouter, Message m, 
                Message.MessageCounter counter) {
            this.messageRouter = messageRouter;
            this.m = m;
            this.counter = counter;
        }
        
        public void run() {
            counter.countMessage(m);
            dispatch();
        }
        
        protected abstract void dispatch();
    }
    
    private static class UDPDispatch extends Dispatch {
        
        private final InetSocketAddress addr;

        UDPDispatch(MessageRouter messageRouter, 
                Message m, 
                InetSocketAddress addr,
                Message.MessageCounter counter) {
            super(messageRouter,m, counter);
            this.addr = addr;
        }

        @Override
        protected void dispatch() {
            messageRouter.handleUDPMessage(m, addr);
        }
    }
    
    private static class MulticastDispatch extends Dispatch {
        
        private final InetSocketAddress addr;
        
        MulticastDispatch(MessageRouter messageRouter, 
                Message m, 
                InetSocketAddress addr,
                Message.MessageCounter counter) {
            super(messageRouter,m, counter);
            this.addr = addr;
        }
        
        @Override
        protected void dispatch() {
            messageRouter.handleMulticastMessage(m, addr);
        }
    }
    
    private static class TCPDispatch extends Dispatch {
        
        private final RoutedConnection conn;
        
        TCPDispatch(MessageRouter messageRouter, 
                Message m, 
                RoutedConnection conn,
                Message.MessageCounter counter) {
            super(messageRouter,m, counter);
            this.conn = conn;
        }
        
        @Override
        protected void dispatch() {
            messageRouter.handleMessage(m, conn);
        }
    }
}