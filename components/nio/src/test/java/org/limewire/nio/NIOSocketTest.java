package org.limewire.nio;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelReader;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.StubConnectObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.util.BufferUtils;
import org.limewire.util.PrivilegedAccessor;

/**
 * Tests that NIOSocket delegates events correctly.
 */
public final class NIOSocketTest extends BaseTestCase {
    
    private static final int PORT = 9999;
    
    private volatile static int blockFailTime = -1;

	public NIOSocketTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(NIOSocketTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    public void testDelayedGetInputStream() throws Exception {
        ServerSocket server = new ServerSocket(PORT, 0);
        try {
            server.setReuseAddress(true);
            InetSocketAddress addr = new InetSocketAddress("127.0.0.1", PORT);
            NIOSocket socket = new NIOSocket();
            socket.setSoTimeout(1000);
            socket.connect(addr);
            
            server.setSoTimeout(1000);
            Socket accepted = server.accept();
            byte[] rnd = new byte[100];
            new Random().nextBytes(rnd);
            accepted.getOutputStream().write(rnd); // this'll go immediately into the buffer
            
            ICROAdapter icro = new ICROAdapter();
            ByteBuffer read = icro.getReadBuffer();
            assertEquals(0, read.position());
            
            socket.setReadObserver(icro);
            NIOTestUtils.waitForNIO();
            
            assertEquals(100, read.position()); // data was transferred to the reader.
            for(int i = 0; i < 100; i++)
                assertEquals(rnd[i], read.get(i));
            
            InputStream stream = socket.getInputStream();
            byte[] readData = new byte[100];
            assertEquals(100, stream.read(readData));
            assertEquals(rnd, readData);
            
            assertEquals(0, read.position()); // moved to the InputStream
            
            new Random().nextBytes(rnd);
            accepted.getOutputStream().write(rnd); // write some more, make sure it goes to stream
            
            NIOTestUtils.waitForNIO();
            assertEquals(0, read.position());
            assertEquals(100, stream.read(readData));
            assertEquals(rnd, readData);
            
            socket.close();
        } finally {
            server.close();
        }        
    }
	
	// tests to make sure that calling setReadObserver
	// will gobble any data that wasn't gobbled in blocking mode.
	public void testSetReadObserver() throws Exception {
	    byte[] data = new byte[127];
	    for(byte i = 0; i < data.length; i++)
	        data[i] = i;
	    
	    Listener listener = new Listener(PORT);
	    NIOSocket socket = new NIOSocket("127.0.0.1", PORT);
	    Stream stream = listener.getStream();
	    stream.write(data);
	    
	    InputStream in = socket.getInputStream();
	    byte[] readIn = new byte[57];
	    in.read(readIn);
	    for(int i = 0; i < readIn.length; i++)
	        assertEquals(i, readIn[i]);
	        
	    NIOTestUtils.waitForNIO();
	    
	    // Make sure that the NIOInputStream did gobble up the data.
	    // This is checking the internal implementation in NIOSocket, so
	    // should the implementation ever change, the test may need to be updated.
	    // (This portion is only checking that the blocking mode did read it up.)
	    NIOInputStream nioInput = (NIOInputStream)PrivilegedAccessor.getValue(socket, "reader");
	    ByteBuffer buffered = (ByteBuffer)PrivilegedAccessor.getValue(nioInput, "buffer");
	    assertEquals(buffered.toString(), data.length - readIn.length, buffered.position());
	    // End internal implementation test.
	    
	    
	    ReadTester reader = new ReadTester();
	    socket.setReadObserver(reader);
	    NIOTestUtils.waitForNIO(); // let the NIO thread pump since setReadObserver is invokedLater
	    ByteBuffer remaining = reader.getRead();
	    assertEquals(remaining.toString(), data.length - readIn.length, remaining.remaining());
	    for(int i = readIn.length; i < data.length; i++)
	        assertEquals(i, remaining.get());
	        
        assertInstanceof(SocketInterestReadAdapter.class, reader.getReadChannel());
        assertSame(socket.getChannel(), ((SocketInterestReadAdapter)reader.getReadChannel()).getChannel());
        
        socket.close();
        stream.socket.close();
    }
    
    public void testSetReadObserverGoesThroughChains() throws Exception {
        NIOSocket socket = new NIOSocket();
        Object channel = socket.getChannel();
        
        RCROAdapter entry = new RCROAdapter();
        socket.setReadObserver(entry);
        NIOTestUtils.waitForNIO();
        assertInstanceof(SocketInterestReadAdapter.class, entry.getReadChannel());
        assertSame(channel, ((SocketInterestReadAdapter)entry.getReadChannel()).getChannel());
        
        RCRAdapter chain1 = new RCRAdapter();
        entry.setReadChannel(chain1);
        socket.setReadObserver(entry);
        NIOTestUtils.waitForNIO();
        assertInstanceof(SocketInterestReadAdapter.class, chain1.getReadChannel());
        assertSame(channel, ((SocketInterestReadAdapter)chain1.getReadChannel()).getChannel());
        assertSame(chain1, entry.getReadChannel());
        
        RCRAdapter chain2 = new RCRAdapter();
        chain1.setReadChannel(chain2);
        socket.setReadObserver(entry);
        NIOTestUtils.waitForNIO();
        assertInstanceof(SocketInterestReadAdapter.class, chain2.getReadChannel());
        assertSame(channel, ((SocketInterestReadAdapter)chain2.getReadChannel()).getChannel());        
        assertSame(chain2, chain1.getReadChannel());
        assertSame(chain1, entry.getReadChannel());
        
        socket.close();
    }
    
    public void testSetReadObserverKeepsInterest() throws Exception {
        ServerSocket server = new ServerSocket(PORT, 0);
        try {
            NIOSocket socket = new NIOSocket("localhost", PORT);
            try {
                socket.setSoTimeout(10);

                ChannelReadObserver observer = new RCROAdapter() {
                    @Override
                    public void handleRead() throws IOException {
                        source.interestRead(false);
                    }
                };
                socket.setReadObserver(observer);
                Thread.sleep(20);
                NIOTestUtils.waitForNIO();
                assertFalse(socket.isClosed());
            } finally {
                socket.close();
            }
        } finally {
            server.close();
        }
    }

    public void testBlockingConnect() throws Exception {
        NIOSocket socket = new NIOSocket();
        socket.connect(new InetSocketAddress("www.google.com", 80));
        assertTrue(socket.isConnected());
        socket.close();
        NIOTestUtils.waitForNIO();
        assertFalse(socket.isConnected());
    }
    
    public void testBlockingConnectAfterShutdown() throws Exception {
        NIOSocket socket = new NIOSocket();
        socket.shutdown();
        
        try {
            socket.connect(new InetSocketAddress("www.google.com", 80), 1000);
            fail("shouldn't have connected shutdown socket");
        } catch (ConnectException expected) {
        } catch (SocketTimeoutException e) {
            fail("expected ConnectTimeoutException");
        } finally {
            socket.close();
        }
    }

    public void testBlockingConnectFailing() throws Exception {
        NIOSocket socket = new NIOSocket();
        
        // Measure time for testNonBlockingConnectFailing()
        long start = System.currentTimeMillis();
        try {
            // Google has a firewall that drops packets so this should timeout
            // this test will fail if Google ever returns connection refused instead   
            socket.connect(new InetSocketAddress("www.google.com", 9999));
            fail("shouldn't have connected");
        } catch(ConnectException iox) {
            assertFalse(socket.isConnected());
        } finally {
            socket.close();
        }
        
        long end = System.currentTimeMillis();
        blockFailTime = (int)(end - start);

        //System.out.println("Time: " + (end-start));
    }
    
    public void testBlockingConnectTimesOut() throws Exception {
        NIOSocket socket = new NIOSocket();
        try {
            // Google has a firewall that drops packets so this should timeout
            // this test will fail if Google ever returns connection refused instead   
            socket.connect(new InetSocketAddress("www.google.com", 9999), 1000);
            fail("shouldn't have connected");
        } catch(SocketTimeoutException iox) {
            assertEquals("operation timed out (1000)", iox.getMessage());
        } finally {
            socket.close();
        }
    }
    
    public void testNonBlockingConnect() throws Exception {
        NIOSocket socket = new NIOSocket();
        StubConnectObserver observer = new StubConnectObserver(); 
        socket.connect(new InetSocketAddress("www.google.com", 80), 5000, observer);
        observer.waitForResponse(5500);
        assertTrue(socket.isConnected());
        assertSame(socket, observer.getSocket());
        assertFalse(observer.isShutdown());
        assertNull(observer.getIoException());
        socket.close();
        NIOTestUtils.waitForNIO();
        assertFalse(observer.isShutdown()); // doesn't get both connect & shutdown
        assertFalse(socket.isConnected());
    }
    
    public void testNonBlockingConnectFailing() throws Exception {
        NIOSocket socket = new NIOSocket();
        StubConnectObserver observer = new StubConnectObserver(); 
        
        // IMPORTANT: The waitForResponse() timeout must be 
        // set to about the same time that testBlockingConnectFailing() 
        // needs to pass the test which depends on the operatin system
        // and the remote host.
        if(blockFailTime < 0)
            blockFailTime = 80000; // if it wasn't set from above test, make one up. 
        
        socket.connect(new InetSocketAddress("www.google.com", 9999), blockFailTime+11000, observer);
        observer.waitForResponse(blockFailTime+1000);
        
        assertTrue(observer.isShutdown());
        assertNull(observer.getSocket());
        assertNull(observer.getIoException()); // NIOSocket swallows the IOX.
        assertFalse(socket.isConnected());
        
        socket.close();
    }
    
    public void testNonBlockingConnectTimesOut() throws Exception {
        NIOSocket socket = new NIOSocket();
        StubConnectObserver observer = new StubConnectObserver();
        socket.connect(new InetSocketAddress("www.google.com", 9999), 1000, observer);
        observer.waitForResponse(1500);
        assertTrue(observer.isShutdown());
        assertNull(observer.getSocket());
        assertNull(observer.getIoException()); // NIOSocket swallows the IOX.
        assertFalse(socket.isConnected());
        
        socket.close();
    }
    
    public void testSoTimeoutUsedForNonBlockingRead() throws Exception {
        ServerSocket server = new ServerSocket(PORT, 0);
        try {
            server.setReuseAddress(true);
            InetSocketAddress addr = new InetSocketAddress("127.0.0.1", PORT);
            NIOSocket socket = new NIOSocket();
            socket.setSoTimeout(1000);
            socket.connect(addr);
            
            server.setSoTimeout(1000);
            Socket accepted = server.accept();
            accepted.getOutputStream().write(new byte[100]); // this'll go immediately into the buffer
            socket.getInputStream().read(new byte[100]);
            Thread.sleep(2000);
            assertTrue(!socket.isClosed()); // didn't close 'cause we're using stream reading
            
            accepted.getOutputStream().write(new byte[1]); // give it some data just to make sure it has
            socket.setReadObserver(new ReadTester());
            Thread.sleep(2000);
            assertTrue(socket.isClosed()); // closed because we switched to NB reading w/ timeout set
            
            socket.close();
        } finally {
            server.close();
        }
        
    }

    private static class Listener {
        
        private ServerSocket server;
        private volatile Socket accepted;
        
        Listener(int port) throws Exception {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));
            server.setSoTimeout(30 * 1000);
            Thread thread = ThreadExecutor.newManagedThread(new Runnable() {
                public void run() {
                    try {
                        accepted = server.accept();
                    } catch(IOException iox) {
                        fail(iox);
                    }
                    try {
                        server.close();
                    } catch(IOException ignored) {}
                }
            });
            thread.start();
        }
        
        Stream getStream() throws Exception {
            // Wait 1 second for accepted to be gotten.
            for(int i = 0; i < 10; i++) {
                if(accepted != null)
                    break;
                Thread.sleep(100);
            }
            return new Stream(accepted);
        }
    }
    
    private static class Stream {
        private final Socket socket;
        
        Stream(Socket socket) {
            if(socket == null)
                throw new NullPointerException("no null sockets allowed");
            this.socket = socket;
        }
        
        void write(final byte[] data) {
            Thread thread = ThreadExecutor.newManagedThread(new Runnable() {
                public void run() {
                    try {
                        socket.getOutputStream().write(data);
                        socket.getOutputStream().flush();
                    } catch(IOException iox) {
                        fail(iox);
                    }
                }
            });
            thread.start();
        }
    }
    
    @SuppressWarnings("unused")
    private static class WriteTester implements ChannelWriter {
        private ByteBuffer buffer;
        private InterestWritableByteChannel channel;

        public WriteTester(ByteBuffer buffer, InterestWritableByteChannel channel) {
            this.buffer = buffer;
            this.channel = channel;
            if(channel != null)
                channel.interestWrite(this, true);
        }
        
        public WriteTester(byte[] data, InterestWritableByteChannel channel) {
            this(ByteBuffer.wrap(data), channel);
        }
        
        public WriteTester(byte[] data, int off, int len, InterestWritableByteChannel channel) {
            this(ByteBuffer.wrap(data, off, len), channel);
        }
        
        public WriteTester() {
            this(ByteBuffer.allocate(0), null);
        }
        
        public synchronized InterestWritableByteChannel getWriteChannel() { return channel; }
        
        public synchronized void setWriteChannel(InterestWritableByteChannel channel) {
            this.channel = channel;
            channel.interestWrite(this, true);
        }
            
        public boolean handleWrite() throws IOException {
            while(buffer.hasRemaining() && channel.write(buffer) > 0);
            return buffer.hasRemaining();
        }
        
        public void shutdown() {}
        
        public void handleIOException(IOException iox) {
            throw (RuntimeException)new UnsupportedOperationException("not implemented").initCause(iox);
        }
    }
    
    private static class ReadTester implements ChannelReadObserver {
        
        private InterestReadableByteChannel source;
        private ByteBuffer readData = ByteBuffer.allocate(128 * 1024);
        
        // ChannelReader methods.
        public InterestReadableByteChannel getReadChannel() { return source; }
        public void setReadChannel(InterestReadableByteChannel channel) { source = channel; }
        
        // IOErrorObserver methods.
        public void handleIOException(IOException x) { fail(x); }
        
        // ReadObserver methods.
        public void handleRead() throws IOException {
            source.read(readData);
            assertEquals(0, source.read(readData)); // must have finish on first read.
        }
        
        // Shutdownable methods.
        public void shutdown() {}
        
        public ByteBuffer getRead() { return (ByteBuffer)readData.flip(); }
    }
    
    private static class ICROAdapter implements ChannelReadObserver, InterestReadableByteChannel {
        private InterestReadableByteChannel source;

        private ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        public ByteBuffer getReadBuffer() {
            return buffer;
        }

        public InterestReadableByteChannel getReadChannel() {
            return source;
        }

        public void setReadChannel(InterestReadableByteChannel channel) {
            source = channel;
        }

        public int read(ByteBuffer b) {
            return BufferUtils.transfer(buffer, b);
        }

        public void close() throws IOException {
            source.close();
        }

        public boolean isOpen() {
            return source.isOpen();
        }

        public void interestRead(boolean status) {
            source.interestRead(status);
        }

        public void handleRead() throws IOException {
            while (buffer.hasRemaining() && source.read(buffer) != 0);
        }

        public void handleIOException(IOException iox) {
        }

        public void shutdown() {
        }
    }
    
    private static class RCRAdapter implements ChannelReader, InterestReadableByteChannel {
        protected InterestReadableByteChannel source;
        public InterestReadableByteChannel getReadChannel() { return source; }
        public void setReadChannel(InterestReadableByteChannel channel) { source = channel; }
        public int read(ByteBuffer b) throws IOException { return source.read(b); }
        public void close() throws IOException { source.close(); }
        public boolean isOpen() { return source.isOpen(); }
        public void interestRead(boolean status) { source.interestRead(status); }
    }
    
    private static class RCROAdapter extends RCRAdapter implements ChannelReadObserver {
        public void handleRead() throws IOException { source.read(ByteBuffer.allocate(1)); }
        public void handleIOException(IOException iox) {}
        public void shutdown() {}
    }
       
}