package org.limewire.nio.ssl;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLException;

import junit.framework.Test;

import org.limewire.nio.NIOServerSocket;
import org.limewire.nio.NIOSocket;
import org.limewire.nio.ProtocolBandwidthTracker;
import org.limewire.util.BaseTestCase;
import org.limewire.util.BufferUtils;

public class SSLUtilsTest extends BaseTestCase {
    
    public SSLUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SSLUtilsTest.class);
    }
    
    
    public void testIsTLSEnabled() throws Exception {
        assertFalse(SSLUtils.isTLSEnabled(new Socket()));
        assertFalse(SSLUtils.isTLSEnabled(new NIOSocket()));
        assertTrue(SSLUtils.isTLSEnabled(new TLSNIOSocket()));
    }
    
    public void testIsStartTLSCapable() throws Exception {
        assertFalse(SSLUtils.isStartTLSCapable(new Socket()));
        assertTrue(SSLUtils.isStartTLSCapable(new NIOSocket()));
        assertTrue(SSLUtils.isStartTLSCapable(new TLSNIOSocket()));
    }
    
    public void testStartTLS() throws Exception {
        try {
            SSLUtils.startTLS(new Socket(), BufferUtils.getEmptyBuffer());
            fail("expected exception");
        } catch(IllegalArgumentException expected) {}
        
        Socket s = new NIOSocket();
        assertTrue(SSLUtils.isStartTLSCapable(s));
        assertFalse(SSLUtils.isTLSEnabled(s));
        s = SSLUtils.startTLS(s, BufferUtils.getEmptyBuffer());
        assertTrue(SSLUtils.isTLSEnabled(s));
        
        try {
            SSLUtils.startTLS(new NIOSocket(), ByteBuffer.wrap(new byte[] { 'N', 'O', 'T', 'T', 'L', 'S' } ));
            fail("expected exception");
        } catch(SSLException expected) {}
        
        ServerSocket ss = new NIOServerSocket();
        ss.setSoTimeout(1000);
        ss.bind(new InetSocketAddress("localhost", 0));
        
        Socket tls = new TLSSocketFactory().createSocket("localhost", ss.getLocalPort());
        tls.getOutputStream().write("OUTPUT".getBytes());
        
        Socket accepted = ss.accept();
        assertFalse(SSLUtils.isTLSEnabled(accepted));
        assertTrue(SSLUtils.isStartTLSCapable(accepted));
        byte[] read = new byte[100];
        int amt = accepted.getInputStream().read(read);
        assertGreaterThan(0, amt);
        assertNotEquals("OUTPUT", new String(read, 0, amt));
        Socket converted = SSLUtils.startTLS(accepted, ByteBuffer.wrap(read, 0, amt));
        amt = converted.getInputStream().read(read);
        assertEquals("OUTPUT".length(), amt);
        assertEquals("OUTPUT", new String(read, 0, amt));
        
        converted.close();
        accepted.close();
        ss.close();
        s.close();
    }
    
    public void testGetSSLBandwidthTracker() throws Exception {
        ProtocolBandwidthTracker t = SSLUtils.getSSLBandwidthTracker(new Socket());
        assertSame(t, SSLUtils.EmptyTracker.instance());// a little stricter check than necessary
        
        t = SSLUtils.getSSLBandwidthTracker(new NIOSocket());
        assertSame(t, SSLUtils.EmptyTracker.instance());// a little stricter check than necessary
        
        ServerSocket listening = new TLSServerSocketFactory().createServerSocket();
        listening.setSoTimeout(1000);
        listening.bind(new InetSocketAddress("localhost", 0));
        
        Socket outgoing = new TLSSocketFactory().createSocket("localhost", listening.getLocalPort());
        Socket incoming = listening.accept();
        
        ProtocolBandwidthTracker outTracker = SSLUtils.getSSLBandwidthTracker(outgoing);
        ProtocolBandwidthTracker inTracker = SSLUtils.getSSLBandwidthTracker(incoming);
        assertNotSame(outTracker, inTracker);
        assertNotSame(outTracker, SSLUtils.EmptyTracker.instance());
        assertNotSame(inTracker, SSLUtils.EmptyTracker.instance());
        
        outgoing.getOutputStream().write("THIS IS OUTPUT".getBytes());
        incoming.getOutputStream().write("INCOMING OUTPUT".getBytes());
        byte[] outRead = new byte[100];
        byte[] inRead = new byte[100];
        int outAmt = outgoing.getInputStream().read(outRead);
        int inAmt = incoming.getInputStream().read(inRead);
        assertEquals("THIS IS OUTPUT".length(), inAmt);
        assertEquals("INCOMING OUTPUT".length(), outAmt);
        
        assertEquals(inAmt, inTracker.getReadBytesProduced());
        assertEquals(outAmt, inTracker.getWrittenBytesConsumed());
        assertEquals(outAmt, outTracker.getReadBytesProduced());
        assertEquals(inAmt, outTracker.getWrittenBytesConsumed());
        
        assertGreaterThan(inAmt, inTracker.getReadBytesConsumed());
        assertGreaterThan(outAmt, inTracker.getWrittenBytesProduced());
        assertGreaterThan(outAmt, outTracker.getReadBytesConsumed());
        assertGreaterThan(inAmt, outTracker.getWrittenBytesProduced());
        
        outgoing.close();
        incoming.close();
        listening.close();
        
    }
}