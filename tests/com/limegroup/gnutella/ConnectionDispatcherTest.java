package com.limegroup.gnutella;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class ConnectionDispatcherTest extends LimeTestCase {
    
    public ConnectionDispatcherTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ConnectionDispatcherTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testGetMaximumWordSizeAddRemoveAndIsValid() {
        ConnectionDispatcher dispatcher = new ConnectionDispatcherImpl(new SimpleNetworkInstanceUtils());
        assertEquals(0, dispatcher.getMaximumWordSize());
        assertFalse(dispatcher.isValidProtocolWord("333"));
        dispatcher.addConnectionAcceptor(new StubAcceptor(), false, "333");
        assertTrue(dispatcher.isValidProtocolWord("333"));
        assertEquals(3, dispatcher.getMaximumWordSize());
        dispatcher.addConnectionAcceptor(new StubAcceptor(), false, "22", "4444");
        assertTrue(dispatcher.isValidProtocolWord("333"));
        assertTrue(dispatcher.isValidProtocolWord("22"));
        assertTrue(dispatcher.isValidProtocolWord("4444"));
        assertEquals(4, dispatcher.getMaximumWordSize());
        dispatcher.addConnectionAcceptor(new StubAcceptor(), false, "55555", "7777777");
        assertEquals(7, dispatcher.getMaximumWordSize());
        assertTrue(dispatcher.isValidProtocolWord("7777777"));
        dispatcher.removeConnectionAcceptor("7777777");
        assertFalse(dispatcher.isValidProtocolWord("7777777"));
        assertEquals(5, dispatcher.getMaximumWordSize());
        dispatcher.removeConnectionAcceptor("55555", "4444", "22");
        assertFalse(dispatcher.isValidProtocolWord("55555"));
        assertFalse(dispatcher.isValidProtocolWord("4444"));
        assertFalse(dispatcher.isValidProtocolWord("22"));
        assertEquals(3, dispatcher.getMaximumWordSize());
    }
    
    // TODO: write tests for dispatching
    
    private static class StubAcceptor implements ConnectionAcceptor {
        private AtomicInteger accepted = new AtomicInteger();
        private CountDownLatch acceptLatch = new CountDownLatch(1);
        private volatile Thread acceptedThread;
        private volatile String acceptedWord;
        private volatile Socket acceptedSocket;
        private boolean blocking;

        public void acceptConnection(String word, Socket s) {
            acceptedThread = Thread.currentThread();
            acceptedWord = word;
            acceptedSocket = s;
            accepted.getAndIncrement();
            acceptLatch.countDown();
        }
        
        public int getAcceptedCount() {
            return accepted.get();
        }
        
        public boolean waitForAccept() throws Exception {
            return acceptLatch.await(5, TimeUnit.SECONDS);
        }
        
        public Thread getAcceptThread() {
            return acceptedThread;
        }
        
        public String getAcceptedWord() {
            return acceptedWord;
        }
        
        public Socket getAcceptedSocket() {
            return acceptedSocket;
        }

        public boolean isBlocking() {
             return blocking;
        }
        
    }
}