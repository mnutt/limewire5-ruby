package org.limewire.nio.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import junit.framework.Test;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.NIOTestUtils;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.WriteBufferChannel;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

public class TLSNIOSocketTest extends BaseTestCase {
    
    public TLSNIOSocketTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TLSNIOSocketTest.class);
    }
    
    public void testConnectAndReadWriteBlocking() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket server = (SSLServerSocket)factory.createServerSocket(9999);
        server.setNeedClientAuth(false);
        server.setWantClientAuth(false);
        server.setEnabledCipherSuites(new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"});
        
        TLSNIOSocket socket = new TLSNIOSocket("127.0.0.1", 9999);        
        Socket accepted = server.accept();
        
        OutputStream clientOut = socket.getOutputStream();
        clientOut.write(StringUtils.toAsciiBytes("TEST TEST\r\n"));
        clientOut.write(StringUtils.toAsciiBytes("\r\n"));        
        byte[] serverB = new byte[1000];
        int serverRead = accepted.getInputStream().read(serverB);
        assertEquals(13, serverRead);
        assertEquals("TEST TEST\r\n\r\n", StringUtils.getASCIIString(serverB, 0, 13));
        
        accepted.getOutputStream().write(StringUtils.toAsciiBytes("HELLO THIS IS A TEST!"));        
        InputStream clientIn = socket.getInputStream();
        byte[] clientB = new byte[2048];
        int clientRead = clientIn.read(clientB);
        assertEquals(21, clientRead);
        assertEquals("HELLO THIS IS A TEST!", StringUtils.getASCIIString(clientB, 0, 21));
        
        socket.close();
        accepted.close();
        server.close();
    }
    
    public void testConnectAndReadWriteNonBlocking() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket server = (SSLServerSocket)factory.createServerSocket(9999);
        server.setNeedClientAuth(false);
        server.setWantClientAuth(false);
        server.setEnabledCipherSuites(new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"});
        
        TLSNIOSocket socket = new TLSNIOSocket("127.0.0.1", 9999);        
        Socket accepted = server.accept();
        
        WriteBufferChannel clientOut = new WriteBufferChannel();
        socket.setWriteObserver(clientOut);
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {public void run() {}}).get(); //wait for write to set 
        clientOut.setBuffer(ByteBuffer.wrap(StringUtils.toAsciiBytes("TEST TEST\r\n\r\n")));
        byte[] serverB = new byte[1000];
        int serverRead = accepted.getInputStream().read(serverB);
        assertEquals(13, serverRead);
        assertEquals("TEST TEST\r\n\r\n", StringUtils.getASCIIString(serverB, 0, 13));
        
        accepted.getOutputStream().write(StringUtils.toAsciiBytes("HELLO THIS IS A TEST!"));
        
        ReadTester reader = new ReadTester();
        socket.setReadObserver(reader);
        Thread.sleep(500);
        ByteBuffer read = reader.getRead();
        assertEquals(21, read.limit());
        assertEquals("HELLO THIS IS A TEST!", StringUtils.getASCIIString(read.array(), 0, 21));
        
        socket.close();
        accepted.close();
        server.close();
    }
    
    public void testConnectAndReadWriteSwitchBlockingMode() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket server = (SSLServerSocket)factory.createServerSocket(9999);
        server.setNeedClientAuth(false);
        server.setWantClientAuth(false);
        server.setEnabledCipherSuites(new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"});
        
        TLSNIOSocket socket = new TLSNIOSocket("127.0.0.1", 9999);        
        Socket accepted = server.accept();
        
        OutputStream clientOutB = socket.getOutputStream();
        clientOutB.write(StringUtils.toAsciiBytes("TEST TEST\r\n"));
        clientOutB.write("\r\n".getBytes());        
        byte[] serverB = new byte[16];
        int serverRead = accepted.getInputStream().read(serverB);
        assertEquals(13, serverRead);
        assertEquals("TEST TEST\r\n\r\n", StringUtils.getASCIIString(serverB, 0, 13));
        
        accepted.getOutputStream().write(StringUtils.toAsciiBytes("HELLO THIS IS A TEST!"));        
        InputStream clientInB = socket.getInputStream();
        byte[] clientReadB = new byte[15];
        int clientRead = clientInB.read(clientReadB);
        assertEquals(15, clientRead);
        assertEquals("HELLO THIS IS A", StringUtils.getASCIIString(clientReadB, 0, 15));
        
        WriteBufferChannel clientOutNB = new WriteBufferChannel();
        socket.setWriteObserver(clientOutNB);
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {public void run() {}}).get(); //wait for write to set
        clientOutNB.setBuffer(ByteBuffer.wrap(StringUtils.toAsciiBytes("MORE TEST\r\n")));
        serverB = new byte[16];
        serverRead = accepted.getInputStream().read(serverB);
        assertEquals(11, serverRead);
        assertEquals("MORE TEST\r\n", StringUtils.getASCIIString(serverB, 0, 11));
                
        ReadTester reader = new ReadTester();
        socket.setReadObserver(reader);
        Thread.sleep(500);
        ByteBuffer read = reader.getRead();
        assertEquals(6, read.limit());
        assertEquals(" TEST!", StringUtils.getASCIIString(read.array(), 0, 6));
        
        socket.close();
        accepted.close();
        server.close();       
    }
    
    public void testRead10KBNonBlocking() throws Exception {
        // 10 kb
        final byte[] serverBuffer = new byte[10 * 1024];

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket acceptor = (SSLServerSocket)factory.createServerSocket(9999);
        try {
            acceptor.setNeedClientAuth(false);
            acceptor.setWantClientAuth(false);
            acceptor.setEnabledCipherSuites(new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"});

            TLSNIOSocket client = new TLSNIOSocket("127.0.0.1", 9999);
            client.setSoTimeout(500);
            try {
                final Socket server = acceptor.accept();
                try {
                    // if the server is 
                    client.getOutputStream().write(1);
                    server.getOutputStream().write(serverBuffer);
                    
                    InputStream in = client.getInputStream();
                    byte[] buffer = new byte[512];
                    int total = 0;
                    while (total < serverBuffer.length) {
                        int read = in.read(buffer);
                        assertNotEquals(-1, read);
                        total += read;
                    }
                    assertEquals(serverBuffer.length, total);
                } finally {
                    server.close();
                }
            } finally {
                client.close();
            }
        } finally {
            acceptor.close();
        }
    }

    /**
     * Test that if output on the remote side is shutdown after we write
     * something, but before handshaking finishes, that we detect that and close
     * the socket.
     */
    public void testWriteOnlyReadShuts() throws Exception {
        TLSNIOServerSocket server = new TLSNIOServerSocket(9999);        
        TLSNIOSocket socket = new TLSNIOSocket("127.0.0.1", 9999);        
        Socket accepted = server.accept();        
        OutputStream output = socket.getOutputStream();
        output.write(StringUtils.toAsciiBytes("Bugfinder"));
        // IMPORTANT: do not tell accepted to read here, otherwise
        // handshaking could finish before we shutdown output.
        accepted.shutdownOutput();        
        NIOTestUtils.waitForNIO();
        assertTrue(socket.isClosed());
        accepted.close();
        server.close();
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
    
}
