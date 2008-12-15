package com.limegroup.gnutella.handshaking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class BlockingIncomingHandshakerTest extends LimeTestCase {
    
    public BlockingIncomingHandshakerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BlockingIncomingHandshakerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSimpleSuccess() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA CONNECT/0.6\r\n" +
                "RequestHeader: RequestValue\r\n" +
                "\r\n" +
                "GNUTELLA/0.6 200 OK DOKIE\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "OK!", outProps));
        Handshaker shaker = new BlockingIncomingHandshaker(responder, socket, in, out);
        shaker.shake();
        
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(1, respondedTo.size());
        assertEquals("RequestValue", respondedTo.get("RequestHeader"));
        
        assertFalse(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(2, read.props().size());
        assertEquals("RequestValue", read.props().get("RequestHeader"));
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutValue", written.props().get("OutHeader"));
        assertEquals("GNUTELLA/0.6 200 OK!\r\nOutHeader: OutValue\r\n\r\n".getBytes(), out.toByteArray());
    }
    
    public void testBelowPointSixFails() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA CONNECT/0.5\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Handshaker shaker = new BlockingIncomingHandshaker( new StubHandshakeResponder(), socket, in, out);
        try {
            shaker.shake();
            fail("shouldn't have succeeded!");
        } catch(IOException iox) {
            assertEquals("Unexpected connect string: GNUTELLA CONNECT/0.5", iox.getMessage());
        }
    }
    
    public void testAbovePointSixSucceeds() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA CONNECT/0.7\r\n" +
                "RequestHeader: RequestValue\r\n" +
                "\r\n" +
                "GNUTELLA/0.6 200 OK DOKIE\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "OK!", outProps));
        Handshaker shaker = new BlockingIncomingHandshaker(responder, socket, in, out);
        shaker.shake();
        
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(1, respondedTo.size());
        assertEquals("RequestValue", respondedTo.get("RequestHeader"));
        
        assertFalse(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(2, read.props().size());
        assertEquals("RequestValue", read.props().get("RequestHeader"));
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutValue", written.props().get("OutHeader"));
        assertEquals("GNUTELLA/0.6 200 OK!\r\nOutHeader: OutValue\r\n\r\n".getBytes(), out.toByteArray());
    }
    
    public void testCrawlerDiscosEarly() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA CONNECT/0.6\r\n" +
                "Crawler: 0.1\r\n" +
                "\r\n" + // extra \r\n just so we don't disco early
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse());
        Handshaker shaker = new BlockingIncomingHandshaker(responder, socket, in, out);
        
        try {
            shaker.shake();
            fail("shouldn't have succeeded!");
        } catch(IOException iox) {
            assertEquals("crawler", iox.getMessage());
        }
    }
    
    public void testDiscoOnBadResponder() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA CONNECT/0.6\r\n" +
                "RequestHeader: RequestValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(599, "NOPE", outProps));
        Handshaker shaker = new BlockingIncomingHandshaker(responder, socket, in, out);
        try {
            shaker.shake();
            fail("should have failed!");
        } catch(NoGnutellaOkException ngok) {
            assertEquals(599, ngok.getCode());
        }
        
        assertEquals(1, shaker.getReadHeaders().props().size());
        assertEquals("RequestValue", shaker.getReadHeaders().props().get("RequestHeader"));
        assertEquals(1, shaker.getWrittenHeaders().props().size());
        assertEquals("OutValue", shaker.getWrittenHeaders().props().get("OutHeader"));
    }
    
    public void testDiscoOnBadResponse() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA CONNECT/0.6\r\n" +
                "RequestHeader: RequestValue\r\n" +
                "\r\n" +
                "GNUTELLA/0.6 333 SUX\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "SURE", outProps));
        Handshaker shaker = new BlockingIncomingHandshaker(responder, socket, in, out);
        try {
            shaker.shake();
            fail("should have failed!");
        } catch(NoGnutellaOkException ngok) {
            assertEquals(333, ngok.getCode());
        }
        
        assertEquals(2, shaker.getReadHeaders().props().size());
        assertEquals("RequestValue", shaker.getReadHeaders().props().get("RequestHeader"));
        assertEquals("ResponseValue", shaker.getReadHeaders().props().get("ResponseHeader"));
        assertEquals(1, shaker.getWrittenHeaders().props().size());
        assertEquals("OutValue", shaker.getWrittenHeaders().props().get("OutHeader"));    
    }
    
    public void testDiscoOnBadResponseConnectLine() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA CONNECT/0.6\r\n" +
                "RequestHeader: RequestValue\r\n" +
                "\r\n" +
                "HTTP/1.1 543 WHAT ARE YOU DOING?\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "SURE", outProps));
        Handshaker shaker = new BlockingIncomingHandshaker(responder, socket, in, out);
        try {
            shaker.shake();
            fail("should have failed!");
        } catch(IOException iox) {
            assertEquals("Bad connect string", iox.getMessage());
        }
        
        assertEquals(1, shaker.getReadHeaders().props().size());
        assertEquals("RequestValue", shaker.getReadHeaders().props().get("RequestHeader"));
        assertEquals(1, shaker.getWrittenHeaders().props().size());
        assertEquals("OutValue", shaker.getWrittenHeaders().props().get("OutHeader"));    
    }
       
        
    private static class AddressedSocket extends Socket {
        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch(IOException iox) {
                return null;
            }
        }
    }
}
