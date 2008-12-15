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

public class BlockingOutgoingHandshakerTest extends LimeTestCase {
    
    public BlockingOutgoingHandshakerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BlockingOutgoingHandshakerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSimpleSuccess() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA/0.6 200 OK DOKIE\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outRequestProps = new Properties();
        outRequestProps.put("OutRequest", "OutRequestValue");
        Properties outResponseProps = new Properties();
        outResponseProps.put("OutResponse", "OutResponseValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "OK!", outResponseProps));
        Handshaker shaker = new BlockingOutgoingHandshaker(outRequestProps, responder, socket, in, out);
        shaker.shake();
        
        HandshakeResponse responseTo = responder.getRespondedTo();
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(1, respondedTo.size());
        assertEquals("ResponseValue", respondedTo.get("ResponseHeader"));
        assertEquals(200, responseTo.getStatusCode());
        assertEquals("OK DOKIE", responseTo.getStatusMessage());
        
        assertTrue(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(1, read.props().size());
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(2, written.props().size());
        assertEquals("OutRequestValue", written.props().get("OutRequest"));
        assertEquals("OutResponseValue", written.props().get("OutResponse"));
        assertEquals("GNUTELLA CONNECT/0.6\r\nOutRequest: OutRequestValue\r\n\r\n" +
                     "GNUTELLA/0.6 200 OK!\r\nOutResponse: OutResponseValue\r\n\r\n", new String(out.toByteArray()));
    }
    
    public void testDiscoOnBadResponder() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA/0.6 200 OK DOKIE\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outRequestProps = new Properties();
        outRequestProps.put("OutRequest", "OutRequestValue");
        Properties outResponseProps = new Properties();
        outResponseProps.put("OutResponse", "OutResponseValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(322, "AARGH!", outResponseProps));
        Handshaker shaker = new BlockingOutgoingHandshaker(outRequestProps, responder, socket, in, out);
        try {
            shaker.shake();
            fail("shouldn't have worked!");
        } catch(NoGnutellaOkException ngok) {
            assertEquals(322, ngok.getCode());
        }
        
        HandshakeResponse responseTo = responder.getRespondedTo();
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(1, respondedTo.size());
        assertEquals("ResponseValue", respondedTo.get("ResponseHeader"));
        assertEquals(200, responseTo.getStatusCode());
        assertEquals("OK DOKIE", responseTo.getStatusMessage());
        
        assertTrue(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(1, read.props().size());
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(2, written.props().size());
        assertEquals("OutRequestValue", written.props().get("OutRequest"));
        assertEquals("OutResponseValue", written.props().get("OutResponse"));
        assertEquals("GNUTELLA CONNECT/0.6\r\nOutRequest: OutRequestValue\r\n\r\n" +
                     "GNUTELLA/0.6 322 AARGH!\r\nOutResponse: OutResponseValue\r\n\r\n", new String(out.toByteArray()));
    }
    
    public void testDiscoOnBadResponseCode() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("GNUTELLA/0.6 544 SHUCKS\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outRequestProps = new Properties();
        outRequestProps.put("OutRequest", "OutRequestValue");
        StubHandshakeResponder responder = new StubHandshakeResponder();
        Handshaker shaker = new BlockingOutgoingHandshaker(outRequestProps, responder, socket, in, out);
        try {
            shaker.shake();
            fail("shouldn't have worked!");
        } catch(NoGnutellaOkException ngok) {
            assertEquals(544, ngok.getCode());
        }
        
        HandshakeResponse responseTo = responder.getRespondedTo();
        assertNull(responseTo);
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(1, read.props().size());
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutRequestValue", written.props().get("OutRequest"));
        assertEquals("GNUTELLA CONNECT/0.6\r\nOutRequest: OutRequestValue\r\n\r\n", new String(out.toByteArray())); 
    }
    
    public void testDiscoOnBadResponseConnectLine() throws Exception {
        Socket socket = new AddressedSocket();
        InputStream in = new ByteArrayInputStream(
                ("HTTP/1.1 345 GET OFF\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n").getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties outRequestProps = new Properties();
        outRequestProps.put("OutRequest", "OutRequestValue");
        StubHandshakeResponder responder = new StubHandshakeResponder();
        Handshaker shaker = new BlockingOutgoingHandshaker(outRequestProps, responder, socket, in, out);
        try {
            shaker.shake();
            fail("shouldn't have worked!");
        } catch(IOException iox) {
            assertEquals("Bad connect string", iox.getMessage());
        }
        
        HandshakeResponse responseTo = responder.getRespondedTo();
        assertNull(responseTo);
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(0, read.props().size());
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutRequestValue", written.props().get("OutRequest"));
        assertEquals("GNUTELLA CONNECT/0.6\r\nOutRequest: OutRequestValue\r\n\r\n", new String(out.toByteArray()));  
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
