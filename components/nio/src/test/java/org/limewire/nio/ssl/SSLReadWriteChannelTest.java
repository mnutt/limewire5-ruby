package org.limewire.nio.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import junit.framework.Test;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ManagedThread;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.ReadBufferChannel;
import org.limewire.nio.channel.WriteBufferChannel;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.util.BufferUtils;

public class SSLReadWriteChannelTest extends BaseTestCase {
    
    public SSLReadWriteChannelTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SSLReadWriteChannelTest.class);
    }
    
    @Override
    public void tearDown() throws Exception {
        // Make sure the NIODispatcher queue is flushed after each test.
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {public void run() {}}).get();
    }
    
    public void testClientTLS() throws Exception {
        doTLSTest(false);
    }
    
    public void testServerTLS() throws Exception {
        doTLSTest(true);
    }
    
    //NOTE -- Many of the tests offload initializing, writing & reading to the NIODispatcher
    //        Thread.  This is because after the SSLReadWriteChannel finishes a task, if there
    //        was buffered read data it will trigger a read in that thread.  If there are other
    //        reads occurring in other threads, that can cause internal corruption in the
    //        SSLEngine.  So, all reading/writing/initializing must be done in a single thread.    
    
    public void testClientTLSFails() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        SSLReadWriteChannel channel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        channel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
        
        ReadBufferChannel readSink = new ReadBufferChannel("NOT TLS DATA!".getBytes());
        WriteBufferChannel writeSink = new WriteBufferChannel(300);
        channel.setReadChannel(readSink);
        channel.setWriteChannel(writeSink);
        new WriteBufferChannel("OUTGOING".getBytes(), channel);
        
        // Trigger the beginning of the handshake.
        channel.handleWrite();
        
        ByteBuffer readTo = ByteBuffer.allocate(200);
        try {
            channel.read(readTo);
            fail("should have failed!");
        } catch(SSLException expected) {}
        
        writeSink.close();
        readSink.close();
        channel.shutdown();
    }
    
    public void testServerTLSFails() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        SSLReadWriteChannel channel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        channel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
        
        ReadBufferChannel readSink = new ReadBufferChannel("NOT TLS DATA!".getBytes());
        channel.setReadChannel(readSink);
        
        ByteBuffer readTo = ByteBuffer.allocate(200);
        try {
            channel.read(readTo);
            fail("should have failed!");
        } catch(SSLException expected) {}
        
        readSink.close();
        channel.shutdown();
    }
    
    public void testNonBlockingTLSExchange() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel clientChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        final SSLReadWriteChannel serverChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                clientChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
                serverChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
            }
        }).get();
        
        Pipe clientToServer = Pipe.open();
        Pipe serverToClient = Pipe.open();
        
        serverToClient.source().configureBlocking(false);
        serverToClient.sink().configureBlocking(false);
        clientToServer.source().configureBlocking(false);
        clientToServer.sink().configureBlocking(false);
        
        clientChannel.setReadChannel(new IRWrapper(serverToClient.source()));
        clientChannel.setWriteChannel(new IWWrapper(clientToServer.sink()));
        serverChannel.setReadChannel(new IRWrapper(clientToServer.source()));
        serverChannel.setWriteChannel(new IWWrapper(serverToClient.sink()));
        
        String serverOut = "I AM A SERVER\r\n, HELLO\r\n";
        String clientOut = "A CLIENT I AM\r\n, GOODBYE\r\n";
        new WriteBufferChannel(serverOut.getBytes(), serverChannel);
        new WriteBufferChannel(clientOut.getBytes(), clientChannel);
        
        final ByteBuffer clientRead = ByteBuffer.allocate(100);
        final ByteBuffer serverRead = ByteBuffer.allocate(100);
        for(int i = 0; i < 10; i++) {
            NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
                public void run() {
                    try {
                        clientChannel.read(clientRead);
                        serverChannel.read(serverRead);
                        clientChannel.handleWrite();
                        serverChannel.handleWrite();
                    } catch(IOException iox) {
                        throw new RuntimeException(iox);
                    }
                }
            }).get();
            Thread.sleep(100);
        }
        
        assertEquals(serverOut, new String(clientRead.array(), 0, clientRead.position()));
        assertEquals(clientOut, new String(serverRead.array(), 0, serverRead.position()));
        
        serverToClient.source().close();
        serverToClient.sink().close();
        clientToServer.source().close();
        clientToServer.sink().close(); 
        clientChannel.shutdown();
        serverChannel.shutdown();
    }
    
    public void testShutdown() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel clientChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        final SSLReadWriteChannel serverChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                clientChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
                serverChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
            }
        }).get();
        
        Pipe clientToServer = Pipe.open();
        Pipe serverToClient = Pipe.open();
        
        serverToClient.source().configureBlocking(false);
        serverToClient.sink().configureBlocking(false);
        clientToServer.source().configureBlocking(false);
        clientToServer.sink().configureBlocking(false);
        
        clientChannel.setReadChannel(new IRWrapper(serverToClient.source()));
        clientChannel.setWriteChannel(new IWWrapper(clientToServer.sink()));
        serverChannel.setReadChannel(new IRWrapper(clientToServer.source()));
        serverChannel.setWriteChannel(new IWWrapper(serverToClient.sink()));
        
        String serverOut = "I AM A SERVER\r\n, HELLO\r\n";
        String clientOut = "A CLIENT I AM\r\n, GOODBYE\r\n";
        new WriteBufferChannel(serverOut.getBytes(), serverChannel);
        new WriteBufferChannel(clientOut.getBytes(), clientChannel);
        
        final ByteBuffer clientRead = ByteBuffer.allocate(100);
        final ByteBuffer serverRead = ByteBuffer.allocate(100);
        for(int i = 0; i < 10; i++) {
            NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
                public void run() {
                    try {
                        clientChannel.shutdown(); // these do nothing, since the Pipes are still open
                        serverChannel.shutdown();
                        clientChannel.read(clientRead);
                        serverChannel.read(serverRead);
                        clientChannel.handleWrite();
                        serverChannel.handleWrite();
                    } catch(IOException iox) {
                        throw new RuntimeException(iox);
                    }
                }
            }).get();
            Thread.sleep(100);
        }
        
        assertEquals(serverOut, new String(clientRead.array(), 0, clientRead.position()));
        assertEquals(clientOut, new String(serverRead.array(), 0, serverRead.position()));
        
        serverToClient.source().close();
        serverToClient.sink().close();
        clientToServer.source().close();
        clientToServer.sink().close(); 
        
        clientChannel.shutdown(); // now they do something.
        serverChannel.shutdown();
        
        try {
            clientChannel.read(BufferUtils.getEmptyBuffer());
            fail("expected ClosedChannelException");
        } catch(ClosedChannelException expected) {}
        
        try {
            serverChannel.read(BufferUtils.getEmptyBuffer());
            fail("expected ClosedChannelException");
        } catch(ClosedChannelException expected) {}
        
        try {
            clientChannel.write(BufferUtils.getEmptyBuffer());
            fail("expected ClosedChannelException");
        } catch(ClosedChannelException expected) {}
        
        try {
            serverChannel.write(BufferUtils.getEmptyBuffer());
            fail("expected ClosedChannelException");
        } catch(ClosedChannelException expected) {}
        
        try {
            clientChannel.handleWrite();
            fail("expected ClosedChannelException");
        } catch(ClosedChannelException expected) {}
        
        try {
            serverChannel.handleWrite();
            fail("expected ClosedChannelException");
        } catch(ClosedChannelException expected) {}
        
    }
    
    public void testShutdownCancelsInitialize() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel channel1 = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        final SSLReadWriteChannel channel2 = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        
        ByteBufferCache cache = NIODispatcher.instance().getBufferCache();
        cache.clearCache();
        // Test shutting down before initialize -- no session or buffers created.
        channel1.shutdown();
        SSLSession session1 = NIODispatcher.instance().getScheduledExecutorService().submit(new Callable<SSLSession>() {
            public SSLSession call() {
                channel1.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
                return channel1.getSession();
            }
        }).get();
        assertNull(session1);
        assertEquals(0, cache.getHeapCacheSize());
        
        cache.clearCache();
        // And initializing before shutting down will create the buffers / session,
        // but later shutting will will then remove them.
        SSLSession session2 = NIODispatcher.instance().getScheduledExecutorService().submit(new Callable<SSLSession>() {
            public SSLSession call() {
                channel2.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
                return channel2.getSession();
            }
        }).get();
        assertNotNull(session2);
        assertEquals(0, cache.getHeapCacheSize()); // objects are still leased.
        // Now if we shutdown the channel, the buffers are returned.
        channel2.shutdown();
        // wait for the shutdown to process completely.
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {public void run() {}}).get();
        assertNotEquals(0, cache.getHeapCacheSize());
        // A little stricter than necessary: check the correct size was taken.
        assertEquals(session2.getPacketBufferSize() + session2.getPacketBufferSize(), cache.getHeapCacheSize());
    }
    
    public void testParentInterestOffDoesntKillHandshake() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel clientChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        final SSLReadWriteChannel serverChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                clientChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
                serverChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
            }
        }).get();
        
        Pipe clientToServer = Pipe.open();
        Pipe serverToClient = Pipe.open();
        
        serverToClient.source().configureBlocking(false);
        serverToClient.sink().configureBlocking(false);
        clientToServer.source().configureBlocking(false);
        clientToServer.sink().configureBlocking(false);
        
        final IRWrapper clientReadSink = new IRWrapper(serverToClient.source());
        final IWWrapper clientWriteSink = new IWWrapper(clientToServer.sink());
        final IRWrapper serverReadSink = new IRWrapper(clientToServer.source());
        final IWWrapper serverWriteSink = new IWWrapper(serverToClient.sink());
        clientChannel.setReadChannel(clientReadSink);
        clientChannel.setWriteChannel(clientWriteSink);
        serverChannel.setReadChannel(serverReadSink);
        serverChannel.setWriteChannel(serverWriteSink);
        
        String serverOut = "I AM A SERVER\r\n, HELLO\r\n";
        String clientOut = "A CLIENT I AM\r\n, GOODBYE\r\n";
        final InterestWritableByteChannel serverSource = new WriteBufferChannel(serverOut.getBytes(), serverChannel);
        final InterestWritableByteChannel clientSource = new WriteBufferChannel(clientOut.getBytes(), clientChannel);
        
        final ByteBuffer clientRead = ByteBuffer.allocate(100);
        final ByteBuffer serverRead = ByteBuffer.allocate(100);
        for(int i = 0; i < 10; i++) {
            NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
                public void run() {
                    try {
                        // If the various interests were on, which would
                        // only be from handshaking, them turn them off
                        // and make sure that the interest stays on --
                        // That is, make sure that handshaking remains
                        // interested, despite what the callers want.
                        
                        clientChannel.read(clientRead);
                        if(clientReadSink.getLastReadInterest()) {
                            clientChannel.interestRead(false);
                            assertTrue(clientReadSink.getLastReadInterest());
                        }
                        
                        serverChannel.read(serverRead);
                        if(serverReadSink.getLastReadInterest()) {
                            serverChannel.interestRead(false);
                            assertTrue(serverReadSink.getLastReadInterest());
                        }
                        
                        clientChannel.handleWrite();
                        if(clientWriteSink.getLastWriteInterest()) {
                            clientChannel.interestWrite(null, false);
                            assertTrue(clientWriteSink.getLastWriteInterest());
                            // Now turn interest back on, to make sure it keeps
                            // the handle to the writer.
                            clientChannel.interestWrite(clientSource, true);
                        }                        
                        
                        serverChannel.handleWrite();
                        if(serverWriteSink.getLastWriteInterest()) {
                            serverChannel.interestWrite(null, false);
                            assertTrue(serverWriteSink.getLastWriteInterest());
                            serverChannel.interestWrite(serverSource, true);
                        }
                    } catch(IOException iox) {
                        throw new RuntimeException(iox);
                    }
                }
            }).get();
            Thread.sleep(100);
        }
        
        assertEquals(serverOut, new String(clientRead.array(), 0, clientRead.position()));
        assertEquals(clientOut, new String(serverRead.array(), 0, serverRead.position()));
        
        serverToClient.source().close();
        serverToClient.sink().close();
        clientToServer.source().close();
        clientToServer.sink().close(); 
        clientChannel.shutdown();
        serverChannel.shutdown();
    }
    
    public void testHasBufferedOutput() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel clientChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        final SSLReadWriteChannel serverChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                clientChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
                serverChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
            }
        }).get();
        
        Pipe clientToServer = Pipe.open();
        Pipe serverToClient = Pipe.open();
        
        serverToClient.source().configureBlocking(false);
        serverToClient.sink().configureBlocking(false);
        clientToServer.source().configureBlocking(false);
        clientToServer.sink().configureBlocking(false);
        
        IWWrapper clientWriteSink = new IWWrapper(clientToServer.sink());
        clientChannel.setReadChannel(new IRWrapper(serverToClient.source()));
        clientChannel.setWriteChannel(clientWriteSink);
        serverChannel.setReadChannel(new IRWrapper(clientToServer.source()));
        serverChannel.setWriteChannel(new IWWrapper(serverToClient.sink()));
        
        ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes());
        assertFalse(clientChannel.hasBufferedOutput());
        clientChannel.write(buffer);
        assertTrue(clientChannel.hasBufferedOutput());
        clientChannel.handleWrite();
        assertFalse(clientChannel.hasBufferedOutput());
        clientChannel.handleWrite();
        assertFalse(clientChannel.hasBufferedOutput());
        clientChannel.write(ByteBuffer.allocate(0));
        assertFalse(clientChannel.hasBufferedOutput());
        clientWriteSink.hasBufferedOutput = true;
        assertTrue(clientChannel.hasBufferedOutput());
        
        serverToClient.source().close();
        serverToClient.sink().close();
        clientToServer.source().close();
        clientToServer.sink().close(); 
        clientChannel.shutdown();
        serverChannel.shutdown();
    }

    public void testEOFDuringUnderflow() throws Exception {
        ExecutorService executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel clientChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), executor);
        final SSLReadWriteChannel serverChannel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), executor);
        
        executor.submit(new Runnable() {
            public void run() {
                clientChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
                serverChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
            }
        }).get();
        
        Pipe clientToServer = Pipe.open();
        Pipe serverToClient = Pipe.open();
        
        serverToClient.source().configureBlocking(false);
        serverToClient.sink().configureBlocking(false);
        clientToServer.source().configureBlocking(false);
        clientToServer.sink().configureBlocking(false);
        
        clientChannel.setReadChannel(new IRWrapper(serverToClient.source()));
        clientChannel.setWriteChannel(new IWWrapper(clientToServer.sink()));
        serverChannel.setReadChannel(new IRWrapper(clientToServer.source(), 264));
        serverChannel.setWriteChannel(new IWWrapper(serverToClient.sink()));
        
        String clientOut = "I AM A CLIENT\r\n, HELLO\r\n";
        new WriteBufferChannel(new byte[0], serverChannel);
        new WriteBufferChannel(clientOut.getBytes(), clientChannel);
        
        final ByteBuffer clientRead = ByteBuffer.allocate(100);
        final ByteBuffer serverRead = ByteBuffer.allocate(100);
        final AtomicInteger lastServerReadAmount = new AtomicInteger(Integer.MAX_VALUE);
        final AtomicInteger lastClientReadAmount = new AtomicInteger(Integer.MAX_VALUE);
        for(int i = 0; i < 10; i++) {
            executor.submit(new Callable<Void>() {
                public Void call() throws IOException {
                    clientChannel.handleWrite();
                    return null;
                }
            }).get();
            executor.submit(new Callable<Void>() {
                public Void call() throws IOException {
                    lastServerReadAmount.set(serverChannel.read(serverRead));
                    return null;
                }
            }).get();
            executor.submit(new Callable<Void>() {
                public Void call() throws IOException {
                    serverChannel.handleWrite();
                    return null;
                }
            }).get();
            executor.submit(new Callable<Void>() {
                public Void call() throws IOException {
                    lastClientReadAmount.set(clientChannel.read(clientRead));
                    return null;
                }
            }).get();
        }
        
        assertEquals(0, clientRead.position());
        assertEquals(0, serverRead.position());
        assertEquals(-1, lastServerReadAmount.get());
        assertEquals(0, lastClientReadAmount.get());
        
        serverToClient.source().close();
        serverToClient.sink().close();
        clientToServer.source().close();
        clientToServer.sink().close(); 
        clientChannel.shutdown();
        serverChannel.shutdown();
    }    
        
    // TODO: Test underflows & overflows
    
    
    // The idea behind this is to connect one SSLSocket to a plain socket,
    // since that's the only way we can read what the built-in SSLSocket
    // is sending, and read all input to the SSLRWChannel as well as writing
    // any output it wants back through the socket.
    // The channel is either in server or client mode, with the blocking socket
    // in the opposite mode.
    private void doTLSTest(final boolean testServer) throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel channel = new SSLReadWriteChannel(context, executor, NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        
        SSLSocketFactory sslFactory = context.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket)sslFactory.createSocket();
        sslSocket.setUseClientMode(!testServer);
        sslSocket.setWantClientAuth(false);
        sslSocket.setNeedClientAuth(false);
        sslSocket.setEnabledCipherSuites(new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" } );
        
        ServerSocket server = new ServerSocket();
        server.bind(null);
        sslSocket.connect(server.getLocalSocketAddress());
        final Socket accepted = server.accept();
        
        // Wrap the channel around the accepted socket.
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                channel.initialize(accepted.getRemoteSocketAddress(), new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, testServer, false);
            }
        }).get(); // wait for this to complete.
        IRWrapper irw = new IRWrapper(accepted.getInputStream());
        IWWrapper iww = new IWWrapper(accepted.getOutputStream());
        channel.setReadChannel(irw);
        channel.setWriteChannel(iww);
        
        doDataTest(channel, sslSocket, irw, iww, 1);
        
        final AtomicBoolean rehandshakeCompleted = new AtomicBoolean(false);
        HandshakeCompletedListener listener = new HandshakeCompletedListener() {
            public void handshakeCompleted(HandshakeCompletedEvent event) {
                rehandshakeCompleted.set(true);
            }
        };
        sslSocket.addHandshakeCompletedListener(listener);
        sslSocket.startHandshake();        
        doDataTest(channel, sslSocket, irw, iww, 2);
        sslSocket.removeHandshakeCompletedListener(listener);
        if(!testServer)
            assertTrue(rehandshakeCompleted.get());
        
        final AtomicBoolean rehandshake2Completed = new AtomicBoolean(false);
        listener = new HandshakeCompletedListener() {
            public void handshakeCompleted(HandshakeCompletedEvent event) {
                rehandshake2Completed.set(true);
            }
        };
        sslSocket.getSession().invalidate();
        sslSocket.addHandshakeCompletedListener(listener);
        sslSocket.startHandshake();
        doDataTest(channel, sslSocket, irw, iww, 3);
        sslSocket.removeHandshakeCompletedListener(listener);
        if(testServer)
            assertTrue(rehandshake2Completed.get());
       
        sslSocket.close();
        server.close();
        accepted.close();
        channel.close();
        channel.shutdown();
    }
    
    private void doDataTest(final SSLReadWriteChannel channel, final SSLSocket sslSocket, 
                            IRWrapper irw, IWWrapper iww, int iteration) throws Exception {       
        final String INCOMING = "THIS IS A TEST\r\n";
        final String OUTGOING = "OUTGOING DATA\r\n";
        
        // This will trigger an outgoing write.
        new WriteBufferChannel(OUTGOING.getBytes(), channel);
        
        final byte[] b = new byte[100];        
        Thread t = new ManagedThread(new Runnable() {
            public void run() {
                try {                    
                    sslSocket.getInputStream().read(b);
                    sslSocket.getOutputStream().write(INCOMING.getBytes());
                } catch(IOException iox) {
                    throw new RuntimeException(iox);
                }
            }
        });
        t.start();
        
        final ByteBuffer channelRead = ByteBuffer.allocate(100);
        // Try short pumps of data until the handshake is complete.
        for(int i = 0; i < 10; i++) {
            NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
                public void run() {
                    try {
                        channel.read(channelRead);
                        channel.handleWrite();
                    } catch(IOException iox) {
                        throw new RuntimeException(iox);
                    }
                }
            }).get();
            Thread.sleep(100);
        }

        t.join(10000);
        
        assertEquals(INCOMING, new String(channelRead.array(), 0, channelRead.position()));
        assertEquals(OUTGOING, new String(b, 0, OUTGOING.length()));
        
        assertEquals(irw.getTotalRead(), channel.getReadBytesConsumed());
        assertEquals(INCOMING.length() * iteration, channel.getReadBytesProduced());
        assertEquals(iww.getTotalWrote(), channel.getWrittenBytesProduced());
        assertEquals(OUTGOING.length() * iteration, channel.getWrittenBytesConsumed());
        assertGreaterThan(channel.getReadBytesProduced(), channel.getReadBytesConsumed());
        assertGreaterThan(channel.getWrittenBytesConsumed(), channel.getWrittenBytesProduced());
    }

    private static class IRWrapper implements InterestReadableByteChannel {
        private final ReadableByteChannel channel;
        private final InputStream in;
        private volatile boolean lastReadInterest;
        private volatile int totalRead;
        private final int totalAllowedToRead;
        
        IRWrapper(InputStream in) {
            this(in, Integer.MAX_VALUE);
        }
        
        public IRWrapper(InputStream in, int totalAllowedToRead) {
            this(in, Channels.newChannel(in), totalAllowedToRead);
        }
        
        public IRWrapper(ReadableByteChannel channel) {
            this(null, channel, Integer.MAX_VALUE);
        }
        
        public IRWrapper(ReadableByteChannel channel, int totalAllowedToRead) {
            this(null, channel, totalAllowedToRead);
        }
        
        public IRWrapper(InputStream in, ReadableByteChannel channel, int totalAllowedToRead) {
            this.channel = channel;
            this.in = in;
            this.totalAllowedToRead = totalAllowedToRead;
        }
        
        public boolean getLastReadInterest() {
            return lastReadInterest;
        }

        public void interestRead(boolean status) {
            lastReadInterest = status;
        }
        
        public int getTotalRead() {
            return totalRead;
        }

        public int read(ByteBuffer dst) throws IOException {
            if(totalRead >= totalAllowedToRead)
                return -1;
            
            if(in == null || in.available() > 0) { // prevents blocking
                int oldLimit = dst.limit();
                if(dst.remaining() + totalRead > totalAllowedToRead)
                    dst.limit(totalAllowedToRead - totalRead + dst.position());
                int read = channel.read(dst);
                dst.limit(oldLimit);
                totalRead += read;
                return read;
            } else
                return 0;
        }

        public void close() throws IOException {
            channel.close();            
        }

        public boolean isOpen() {
            return channel.isOpen();
        }
    }
    
    private static class IWWrapper implements InterestWritableByteChannel {
        private final WritableByteChannel channel;
        private volatile boolean lastWriteInterest;
        private volatile int totalWrote;
        boolean hasBufferedOutput = false;
        
        public IWWrapper(OutputStream out) {
            this.channel = Channels.newChannel(out);
        }
        
        public IWWrapper(WritableByteChannel channel) {
            this.channel = channel;
        }
        
        public boolean getLastWriteInterest() {
            return lastWriteInterest;
        }
        
        public void interestWrite(WriteObserver observer, boolean status) {
            lastWriteInterest = status;
        }
        
        public int getTotalWrote() { 
            return totalWrote;
        }

        public int write(ByteBuffer src) throws IOException {
            int wrote = channel.write(src); // assumes this will never block
            totalWrote += wrote;
            return wrote;
        }

        public void close() throws IOException {
            channel.close();
        }

        public boolean isOpen() {
            return channel.isOpen();
        }

        public boolean handleWrite() throws IOException {
            return true;
        }

        public void handleIOException(IOException iox) {
        }

        public void shutdown() {
        }

        public boolean hasBufferedOutput() {
            return hasBufferedOutput;
        }
        
    }
    
}
