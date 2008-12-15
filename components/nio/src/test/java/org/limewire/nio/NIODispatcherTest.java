package org.limewire.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.limewire.nio.observer.StubAcceptChannelObserver;
import org.limewire.nio.observer.StubConnectObserver;
import org.limewire.nio.observer.StubReadConnectObserver;
import org.limewire.nio.observer.StubReadObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;

public class NIODispatcherTest extends BaseTestCase {
    
    private int LISTEN_PORT = 9999;
    private ServerSocket LISTEN_SOCKET;
    private InetSocketAddress LISTEN_ADDR;
    
    public NIODispatcherTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(NIODispatcherTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
        LISTEN_SOCKET = new ServerSocket();
        LISTEN_SOCKET.setReuseAddress(true);
        LISTEN_ADDR = new InetSocketAddress("127.0.0.1", LISTEN_PORT);
        LISTEN_SOCKET.bind(LISTEN_ADDR, 0);
    }
    
    @Override
    public void tearDown() throws Exception {
        try {
            LISTEN_SOCKET.close();
        } catch(IOException ignored) {}
    }
    
    // note: this simulates a connect timeout by never actually trying to connect (except on linux).
    // it's really frickin' hard to try a connect and have it timeout because:
    // a) if you have a socket listening & try to connect, even if it isn't accepting, it will connect
    // b) if you don't have a socket listening and try to connect, it will immediate throw a ConnectException
    // c) on linux, if the socket isn't trying to connect to someone, it will throw a NoConnectPendingException
    
    
    public void testSimpleConnectTimeout() throws Exception {
        StubConnectObserver observer = new StubConnectObserver();
        SocketChannel channel = observer.getChannel();
        if(OSUtils.isLinux())
            channel.connect(new InetSocketAddress("www.google.com", 9999));
            
        NIODispatcher.instance().registerConnect(channel, observer, 3000);
        assertNull(observer.getIoException());
        assertNull(observer.getSocket());
        assertFalse(observer.isShutdown());
        
        Thread.sleep(3500);
        assertFalse(channel.isConnected());
        assertNull(observer.getSocket());
        Exception iox = observer.getIoException();
        assertNotNull(iox);
        assertInstanceof(SocketTimeoutException.class, iox);
        assertEquals("operation timed out (3000)", iox.getMessage()); // stringent, but useful.
        assertTrue(observer.isShutdown());
        
        channel.close();
    }
    

    public void testMultipleConnectTimeout() throws Exception {
        StubConnectObserver o1 = new StubConnectObserver();
        SocketChannel c1 = o1.getChannel();
        StubConnectObserver o2 = new StubConnectObserver();
        SocketChannel c2 = o2.getChannel();        

        if(OSUtils.isLinux()) {
            c1.connect(new InetSocketAddress("www.google.com", 9999));
            c2.connect(new InetSocketAddress("www.google.com", 9999));
        }
        
        NIODispatcher.instance().registerConnect(c1, o1, 3000);
        NIODispatcher.instance().registerConnect(c2, o2, 2000);

        Exception x1;
        Exception x2;
        
        Thread.sleep(2500);
        x2 = o2.getIoException();
        assertFalse(c2.isConnected());
        assertNotNull(x2);
        assertInstanceof(SocketTimeoutException.class, x2);
        assertEquals("operation timed out (2000)", x2.getMessage()); // stringent, but useful.
        assertNull(o2.getSocket());
        assertTrue(o2.isShutdown());  
        assertFalse(o1.isShutdown());
        assertNull(o1.getIoException());
        assertNull(o1.getSocket());
        
        Thread.sleep(1000);
        x1 = o1.getIoException();
        assertFalse(c1.isConnected());
        assertNotNull(x1);
        assertInstanceof(SocketTimeoutException.class, x1);
        assertEquals("operation timed out (3000)", x1.getMessage()); // stringent, but useful.
        assertNull(o1.getSocket());
        assertTrue(o1.isShutdown());
        
        c1.close();
        c2.close();
    }
    
    public void testConnectPreventsTimeout() throws Exception {
        if(OSUtils.isLinux())
            return; // it is impossible to run this test on linux.
        
        StubConnectObserver observer = new StubConnectObserver();
        SocketChannel channel = observer.getChannel();
        
        NIODispatcher.instance().registerConnect(channel, observer, 3000);
        channel.connect(LISTEN_ADDR);
        Thread.sleep(1000);
        assertTrue(channel.isConnected());
        assertNull(observer.getIoException());
        assertNotNull(observer.getSocket());
        assertFalse(observer.isShutdown());
        
        Thread.sleep(2500);
        assertTrue(channel.isConnected());
        assertEquals(0, interestOps(channel));
        Exception iox = observer.getIoException();
        assertNull(iox);
        
        channel.close();
    }
    
    public void testAcceptChannel() throws Exception {
        if(OSUtils.isLinux())
            return; // it is impossible to run this test on linux.
        
        LISTEN_SOCKET.close();
        ServerSocketChannel channel = ServerSocketChannel.open();
        LISTEN_SOCKET = channel.socket();
        channel.configureBlocking(false);
        LISTEN_SOCKET.setReuseAddress(true);
        LISTEN_SOCKET.bind(new InetSocketAddress(LISTEN_PORT));
        StubAcceptChannelObserver observer = new StubAcceptChannelObserver();
        NIODispatcher.instance().registerAccept(channel, observer);
        
        SocketChannel c1 = new StubConnectObserver().getChannel();
        SocketChannel c2 = new StubConnectObserver().getChannel();
        SocketChannel c3 = new StubConnectObserver().getChannel();
        
        assertEquals(0, observer.getChannels().size());
        NIODispatcher.instance().registerConnect(c1, new StubConnectObserver(), 0);
        c1.connect(LISTEN_ADDR);
        Thread.sleep(300);
        assertEquals(1, observer.getChannels().size());
        SocketChannel r1 = observer.getNextSocketChannel();
        assertEquals(c1.socket().getLocalPort(), r1.socket().getPort());
        assertFalse(r1.isBlocking());
        assertTrue(r1.isConnected());
        c1.close();
        
        // make sure we can handle more'n one connect at a time.
        NIODispatcher.instance().registerConnect(c2, new StubConnectObserver(), 0);
        NIODispatcher.instance().registerConnect(c3, new StubConnectObserver(), 0);
        c2.connect(LISTEN_ADDR);
        c3.connect(LISTEN_ADDR);
        Thread.sleep(300);
        assertEquals(2, observer.getChannels().size());
        SocketChannel r2 = observer.getNextSocketChannel();
        SocketChannel r3 = observer.getNextSocketChannel();
        // swap r2 & r3 if we read them out of order.
        if(c2.socket().getLocalPort() != r2.socket().getPort()) {
            SocketChannel temp = r3;
            r3 = r2;
            r2 = temp;
        }
        assertEquals(c2.socket().getLocalPort(), r2.socket().getPort());
        assertFalse(r2.isBlocking());
        assertTrue(r2.isConnected());        
        assertEquals(c3.socket().getLocalPort(), r3.socket().getPort());
        assertFalse(r3.isBlocking());
        assertTrue(r3.isConnected());
        
        c2.close();
        c3.close();
    }
    
    public void testLevelTriggeredness() throws Exception {
        StubReadConnectObserver o1 = new StubReadConnectObserver();
        SocketChannel c1 = o1.getChannel();
        connect(c1, LISTEN_ADDR, o1);
        
        Socket accepted = LISTEN_SOCKET.accept();
        
        // First make sure we get notified of data & can read it.
        accepted.getOutputStream().write("OUT".getBytes()); 
        accepted.getOutputStream().flush();
        NIODispatcher.instance().registerRead(c1, o1);
        // Let the NIO thread pump a few times to make sure we read it.
        NIOTestUtils.waitForNIO();
        NIOTestUtils.waitForNIO();
        // Really this should be handled in 1 notify, but there's no harm
        // in the network sucking and it taking more.
        assertGreaterThan(0, o1.getReadsHandled());
        assertLessThanOrEquals(3, o1.getReadsHandled());
        ByteBuffer buffer = o1.getReadBuffer();
        assertEquals(3, buffer.position());
        assertEquals("OUT", new String(buffer.array(), 0, 3));
        
        // Now that we've read everything, wait a for more cycle and make sure
        // no more events came in.
        int priorReadsHandled = o1.getReadsHandled();
        NIOTestUtils.waitForNIO();
        NIOTestUtils.waitForNIO();
        assertEquals(priorReadsHandled, o1.getReadsHandled());
        
        // Okay, our precondition is pretty well defined now...
        // Tell the reader to ignore reading from the actual buffer,
        // but keep interest on and see how many read notifications we get...
        o1.setIgnoreReadData(true);
        accepted.getOutputStream().write("A".getBytes());
        accepted.getOutputStream().flush();
        for(int i = 0; i < 10; i++)
            NIOTestUtils.waitForNIO();
        // Wait a whole bunch of cycles...
        // We should be notified about this one pending byte of read data
        // many, many times.
        // (If the selector was edge-triggered, we would only be notified once).
        assertGreaterThan(priorReadsHandled + 5, o1.getReadsHandled());
        
        // Just to do some stricter testing, also make sure that if we turn
        // interest off & back on again, we'll continue to get notifications
        // about the existing data...
        NIODispatcher.instance().interestRead(c1, false);
        NIOTestUtils.waitForNIO();
        priorReadsHandled = o1.getReadsHandled();
        for(int i = 0; i < 10; i++)
            NIOTestUtils.waitForNIO();
        //Make sure we got no notifications while reading was off
        assertEquals(priorReadsHandled, o1.getReadsHandled());
        
        // Now turn it back on and make sure notifications skyrocket again.
        NIODispatcher.instance().interestRead(c1, true);
        for(int i = 0; i < 10; i++)
            NIOTestUtils.waitForNIO();
        assertGreaterThan(priorReadsHandled + 5, o1.getReadsHandled());
        
        c1.close();
        accepted.close();
    }
    
    public void testLevelTriggerednessIfChannelHadSomeReadingDone() throws Exception {
        StubReadConnectObserver o1 = new StubReadConnectObserver();
        SocketChannel c1 = o1.getChannel();
        connect(c1, LISTEN_ADDR, o1);
        
        Socket accepted = LISTEN_SOCKET.accept();
        
        // First make sure we get notified of data & can read it.
        accepted.getOutputStream().write("OUT".getBytes()); 
        accepted.getOutputStream().flush();
        NIODispatcher.instance().registerRead(c1, o1);
        // Let the NIO thread pump a few times to make sure we read it.
        NIOTestUtils.waitForNIO();
        NIOTestUtils.waitForNIO();
        // Really this should be handled in 1 notify, but there's no harm
        // in the network sucking and it taking more.
        assertGreaterThan(0, o1.getReadsHandled());
        assertLessThanOrEquals(3, o1.getReadsHandled());
        ByteBuffer buffer = o1.getReadBuffer();
        assertEquals(3, buffer.position());
        assertEquals("OUT", new String(buffer.array(), 0, 3));
        
        // Now that we've read everything, wait a for more cycle and make sure
        // no more events came in.
        int priorReadsHandled = o1.getReadsHandled();
        NIOTestUtils.waitForNIO();
        NIOTestUtils.waitForNIO();
        assertEquals(priorReadsHandled, o1.getReadsHandled());
        
        // Okay, our precondition is pretty well defined now...
        // Tell the reader to ignore reading from the actual buffer,
        // but keep interest on and see how many read notifications we get...
        o1.getReadBuffer().clear();
        o1.setAmountToRead(1);
        accepted.getOutputStream().write("ABC".getBytes());
        accepted.getOutputStream().flush();
        for(int i = 0; i < 10; i++)
            NIOTestUtils.waitForNIO();
        // Wait a whole bunch of cycles...
        // We should be notified about this one pending byte of read data
        // many, many times.
        // (If the selector was edge-triggered, we would only be notified once).
        assertGreaterThan(priorReadsHandled + 5, o1.getReadsHandled());
        // Make sure that when we consumed data, it was on the first notification
        assertEquals(priorReadsHandled + 1, o1.getReadsHandledAtLastConsume());
        assertEquals(1, o1.getReadBuffer().position());
        assertEquals("A", new String(o1.getReadBuffer().array(), 0, 1));
        
        // Just to do some stricter testing, also make sure that if we turn
        // interest off & back on again, we'll continue to get notifications
        // about the existing data...
        NIODispatcher.instance().interestRead(c1, false);
        NIOTestUtils.waitForNIO();
        priorReadsHandled = o1.getReadsHandled();
        for(int i = 0; i < 10; i++)
            NIOTestUtils.waitForNIO();
        //Make sure we got no notifications while reading was off
        assertEquals(priorReadsHandled, o1.getReadsHandled());
        
        // Now turn it back on and make sure notifications skyrocket again.
        o1.getReadBuffer().clear();
        NIODispatcher.instance().interestRead(c1, true);
        for(int i = 0; i < 10; i++)
            NIOTestUtils.waitForNIO();
        assertGreaterThan(priorReadsHandled + 5, o1.getReadsHandled());
        assertEquals(priorReadsHandled + 1, o1.getReadsHandledAtLastConsume());
        assertEquals(1, o1.getReadBuffer().position());
        assertEquals("B", new String(o1.getReadBuffer().array(), 0, 1));
        
        c1.close();
        accepted.close();
    }
    
    
    public void testSimpleReadTimeout() throws Exception {
        StubReadObserver o1 = new StubReadObserver();
        SocketChannel c1 = o1.getChannel();
        c1.connect(LISTEN_ADDR);
        
        o1.setReadTimeout(1023);
        NIODispatcher.instance().registerRead(c1, o1);
        o1.waitForIOException(2000);
        assertInstanceof(SocketTimeoutException.class, o1.getIox());
        assertEquals("operation timed out (1023)", o1.getIox().getMessage());
        assertTrue(o1.isShutdown());
        assertEquals(0, o1.getReadsHandled());
        
        c1.close();
    }
    
    public void testNoTimeoutIfInterestOff() throws Exception {
        StubReadObserver o1 = new StubReadObserver();
        SocketChannel c1 = o1.getChannel();
        c1.connect(LISTEN_ADDR);
        
        o1.setReadTimeout(1000);
        NIODispatcher.instance().registerRead(c1, o1);
        Thread.sleep(200); // let it register.
        NIODispatcher.instance().interestRead(c1, false);
        o1.waitForIOException(2000);
        assertNull(o1.getIox());
        assertFalse(o1.isShutdown());
        assertEquals(0, o1.getReadsHandled());
        
        c1.close();
    }
    
    public void testChangingTimeoutByInterest() throws Exception {
        StubReadObserver o1 = new StubReadObserver();
        SocketChannel c1 = o1.getChannel();
        c1.connect(LISTEN_ADDR);
        
        o1.setReadTimeout(1000);
        NIODispatcher.instance().registerRead(c1, o1);
        Thread.sleep(200); // let it register.
        o1.setReadTimeout(2005);
        NIODispatcher.instance().interestRead(c1, true);
        o1.waitForIOException(4000);
        assertInstanceof(SocketTimeoutException.class, o1.getIox());
        assertEquals("operation timed out (2005)", o1.getIox().getMessage());
        assertTrue(o1.isShutdown());
        assertEquals(0, o1.getReadsHandled());
        
        c1.close();
    }
    
    public void testTimeoutUpsAfterReads() throws Exception {
        StubReadConnectObserver o1 = new StubReadConnectObserver();
        SocketChannel c1 = o1.getChannel();
        if(OSUtils.isLinux()) {
            connect(c1, LISTEN_ADDR, o1); // can't do a portion of the test on linux.
        } else {
            o1.setReadTimeout(1000);
            NIODispatcher.instance().registerConnect(c1, o1, 1000);
            c1.connect(LISTEN_ADDR);
            o1.waitForEvent(1000);
            assertEquals(c1.socket(), o1.getSocket());
            assertTrue(c1.isConnected());
        }
        
        // Now accept on the server socket.
        LISTEN_SOCKET.setSoTimeout(5000);
        Socket accepted = LISTEN_SOCKET.accept();
        
        // Wait 2 seconds & make sure it didn't disco
        Thread.sleep(2000);
        assertTrue(c1.isConnected());
        assertFalse(o1.isShutdown());
        assertNull(o1.getIoException());
        assertEquals(0, o1.getReadsHandled());
        
        // Turn interest on in reading & send some data.
        NIODispatcher.instance().interestRead(c1, true);
        o1.setReadTimeout(2000); // change timeout length so we can make sure the exception matched
        accepted.getOutputStream().write(new byte[100]);
        Thread.sleep(500);
        assertGreaterThanOrEquals(1, o1.getReadsHandled());
        
        // Now make sure that since interest is still on, it'll IOX after a second.
        Thread.sleep(2500);
        assertInstanceof(SocketTimeoutException.class, o1.getIoException());
        assertEquals("operation timed out (2000)", o1.getIoException().getMessage());
        assertGreaterThanOrEquals(o1.getLastReadTime() + 1000, o1.getIoxTime());
        
        c1.close();
    }
    
    public void testTimeoutBecomesSmaller() throws Exception {
        StubReadConnectObserver o1 = new StubReadConnectObserver();
        SocketChannel c1 = o1.getChannel();
        if(OSUtils.isLinux())
            connect(c1, LISTEN_ADDR, o1); // can't do a portion of the test on linux.
        else {
            NIODispatcher.instance().registerConnect(c1, o1, 1000);
            c1.connect(LISTEN_ADDR);
            o1.waitForEvent(1000);
            assertEquals(c1.socket(), o1.getSocket());
            assertTrue(c1.isConnected());
        }
        
        // Now accept on the server socket.
        LISTEN_SOCKET.setSoTimeout(5000);
        Socket accepted = LISTEN_SOCKET.accept();
        
        // Wait 2 seconds & make sure it didn't disco
        Thread.sleep(2000);
        assertTrue(c1.isConnected());
        assertFalse(o1.isShutdown());
        assertNull(o1.getIoException());
        assertEquals(0, o1.getReadsHandled());
        
        // Turn interest on in reading & send some data.
        o1.setReadTimeout(5000);
        NIODispatcher.instance().interestRead(c1, true);
        o1.setReadTimeout(1000); // change timeout length so we can make sure the exception matched
        accepted.getOutputStream().write(new byte[100]);
        Thread.sleep(500);
        assertGreaterThanOrEquals(1, o1.getReadsHandled());
        
        // Now make sure that since interest is still on, it'll IOX after a second.
        Thread.sleep(1500);
        assertInstanceof(SocketTimeoutException.class, o1.getIoException());
        assertEquals("operation timed out (1000)", o1.getIoException().getMessage());
        
        // Wait a bit longer and make sure the msg didn't change on the IOX
        Thread.sleep(5500);
        assertEquals("operation timed out (1000)", o1.getIoException().getMessage());
        
        c1.close();        
    }
    
    public void testOtherSelectors() throws Exception {
        Selector stub = new StubSelector();
        NIODispatcher.instance().registerSelector(stub, StubChannel.class);
        try {
            StubReadConnectObserver observer = new StubReadConnectObserver();
            StubChannel channel = new StubChannel();
            observer.setChannel(channel);
            Socket socket = new Socket();
            channel.setSocket(socket);
            NIODispatcher.instance().registerConnect(channel, observer, 0);
            NIODispatcher.instance().wakeup();
            Thread.sleep(300);
            assertEquals(SelectionKey.OP_CONNECT, interestOps(channel));
            channel.setReadyOps(SelectionKey.OP_CONNECT);
            NIODispatcher.instance().wakeup();
            observer.waitForEvent(1000);
            assertSame(socket, channel.socket());
            assertEquals(0, interestOps(channel));

            assertEquals(0, observer.getReadsHandled());
            NIODispatcher.instance().interestRead(channel, true);
            assertEquals(SelectionKey.OP_READ, interestOps(channel));
            channel.setReadyOps(SelectionKey.OP_READ);
            NIODispatcher.instance().wakeup();
            observer.waitForEvent(2000);
            assertGreaterThanOrEquals(1, observer.getReadsHandled());
            
            channel.close();
        } finally {
            NIODispatcher.instance().removeSelector(stub);
        }
    }
    
    /**
     * tests that scheduling tasks works and that they are executed on
     * the dispatcher thread.
     */
    public void testSchedulingTasks() throws Exception {
    	// first get a ref to the dispatch thread
    	final AtomicReference<Thread>t = new AtomicReference<Thread>();
    	NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
    		public void run() {
    			t.set(Thread.currentThread());
    		}
    	});
    	
    	while(t.get() == null)
    		Thread.sleep(10);
    	
    	final CountDownLatch executed = new CountDownLatch(1);
    	Future<?> f = NIODispatcher.instance().getScheduledExecutorService().schedule(new Runnable() {
    		public void run() {
    			// should be executed on the dispatch thread
    			assertSame(t.get(), Thread.currentThread());
    			executed.countDown();
    		}
    	}, 500, TimeUnit.MILLISECONDS);

    	// and not get executed until the timeout elapses
    	assertFalse(executed.await(480,TimeUnit.MILLISECONDS));
    	assertFalse(f.isDone());
    	assertTrue(executed.await(40,TimeUnit.MILLISECONDS));
    	assertTrue(f.isDone());
    }
    
    /** Tests that re-registration discards old attachments. */
    public void testReRegisterDiscardsOldAttachment() throws Exception {
        StubReadObserver o1 = new StubReadObserver();
        SocketChannel c1 = o1.getChannel();
        c1.connect(LISTEN_ADDR);
        
        o1.setReadTimeout(1023);
        NIODispatcher.instance().registerRead(c1, o1);
        StubReadObserver o2 = new StubReadObserver();
        o2.setReadTimeout(1001);
        NIODispatcher.instance().registerRead(c1, o2);
        
        o2.waitForIOException(2000);
        assertInstanceof(SocketTimeoutException.class, o2.getIox());
        assertEquals("operation timed out (1001)", o2.getIox().getMessage());
        assertTrue(o2.isShutdown());
        assertEquals(0, o2.getReadsHandled());
        
        assertNull(o1.getIox());
        assertFalse(o1.isShutdown());
        assertEquals(0, o1.getReadsHandled());
        
        c1.close();
    }
    
    private void connect(SocketChannel c, SocketAddress a, StubReadConnectObserver o) throws Exception {
        if(!c.connect(a)) {
            NIODispatcher.instance().registerConnect(c, o, 1000);
            o.waitForEvent(1000);
            assertEquals(c.socket(), o.getSocket());
            assertTrue(c.isConnected());
        }
    }
    
    private int interestOps(final SelectableChannel channel) throws Exception {
        Future<Integer> f = NIODispatcher.instance().getScheduledExecutorService().submit(new Callable<Integer>() {
            public Integer call() throws Exception {
                Selector selector = (Selector)PrivilegedAccessor.invokeMethod(
                        NIODispatcher.instance(), "getSelectorFor", new Object[] {channel }, new Class[] { SelectableChannel.class });
                return channel.keyFor(selector).interestOps();
            }
        });
        return f.get();
    }

}
