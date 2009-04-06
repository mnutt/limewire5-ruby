package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ReadObserver;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.stubs.ReadBufferChannel;
import com.limegroup.gnutella.stubs.WriteBufferChannel;

public class AsyncIncomingHandshakerTest extends LimeTestCase {
    
    public AsyncIncomingHandshakerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AsyncIncomingHandshakerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSimpleSuccess() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                StringUtils.toAsciiBytes("GNUTELLA CONNECT/0.6\r\n" +
                "RequestHeader: RequestValue\r\n" +
                "\r\n" +
                "GNUTELLA/0.6 200 OK DOKIE\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n"));
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "OK!", outProps));
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncIncomingHandshaker(responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // do the transfer! -- simulates NIODispatcher notifying
        
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isBadHandshake());
        assertTrue(observer.isHandshakeFinished());
        assertEquals(shaker, observer.getShaker());
        
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(respondedTo.toString(), 1, respondedTo.size());
        assertEquals("RequestValue", respondedTo.get("RequestHeader"));
        
        assertFalse(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(2, read.props().size());
        assertEquals("RequestValue", read.props().get("RequestHeader"));
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutValue", written.props().get("OutHeader"));
        ByteBuffer out = writer.getBuffer();
        assertEquals("GNUTELLA/0.6 200 OK!\r\nOutHeader: OutValue\r\n\r\n", StringUtils.getASCIIString(out.array(), 0, out.limit()));
    }
    
    public void testBelowPointSixFails() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                StringUtils.toAsciiBytes("GNUTELLA CONNECT/0.5\r\n"));
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncIncomingHandshaker(new StubHandshakeResponder(), socket, observer);
        shaker.shake();
        
        socket.exchange(); // do the transfer! -- simulates NIODispatcher notifying
 
        assertFalse(observer.isNoGOK());
        assertTrue(observer.isBadHandshake());
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
    }
    
    public void testAbovePointSixSucceeds() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                StringUtils.toAsciiBytes("GNUTELLA CONNECT/0.7\r\n" +
                "RequestHeader: RequestValue\r\n" +
                "\r\n" +
                "GNUTELLA/0.6 200 OK DOKIE\r\n" +
                "ResponseHeader: ResponseValue\r\n" +
                "\r\n"));
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "OK!", outProps));
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncIncomingHandshaker(responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // do the transfer! -- simulates NIODispatcher notifying
        
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isBadHandshake());
        assertTrue(observer.isHandshakeFinished());
        assertEquals(shaker, observer.getShaker());
        
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(respondedTo.toString(), 1, respondedTo.size());
        assertEquals("RequestValue", respondedTo.get("RequestHeader"));
        
        assertFalse(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(2, read.props().size());
        assertEquals("RequestValue", read.props().get("RequestHeader"));
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutValue", written.props().get("OutHeader"));
        ByteBuffer out = writer.getBuffer();
        assertEquals("GNUTELLA/0.6 200 OK!\r\nOutHeader: OutValue\r\n\r\n", StringUtils.getASCIIString(out.array(), 0, out.limit()));
    }

    public void testCrawlerDiscosEarly() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                StringUtils.toAsciiBytes("GNUTELLA CONNECT/0.6\r\n" +
                 "Crawler: 0.1\r\n" +
                 "\r\n" +
                 // pretend it would have sent a good response,
                 // just to make sure we'll close it anyway.
                 "GNUTELLA/0.6 200 OK DOKIE\r\n" +
                 "ResponseHeader: ResponseValue\r\n" +
                 "\r\n"));
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        StubHandshakeObserver observer = new StubHandshakeObserver();
        // make sure that even 'cause of the bad response, we close due to BadHandshake,
        // not No GOK -- otherwise we can't be sure the response got sent all the way.
        HandshakeResponse response = new StubHandshakeResponse(HandshakeResponse.CRAWLER_CODE, "Failed", new Properties());
        Handshaker shaker = new AsyncIncomingHandshaker(new StubHandshakeResponder(response), socket, observer);
        shaker.shake();
        
        socket.exchange(); // do the transfer! -- simulates NIODispatcher notifying
 
        assertFalse(observer.isNoGOK()); // Not a NGOK
        assertTrue(observer.isBadHandshake());  // Instead a BadHandshake
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
    }
    
    public void testDiscoOnBadResponder() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                StringUtils.toAsciiBytes("GNUTELLA CONNECT/0.6\r\n" +
                 "RequestHeader: RequestValue\r\n" +
                 "\r\n"));
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(599, "NOPE", outProps));    
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncIncomingHandshaker(responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // do the transfer! -- simulates NIODispatcher notifying
 
        assertTrue(observer.isNoGOK());
        assertEquals(599, observer.getCode());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        
        assertEquals(1, shaker.getReadHeaders().props().size());
        assertEquals("RequestValue", shaker.getReadHeaders().props().get("RequestHeader"));
        assertEquals(1, shaker.getWrittenHeaders().props().size());
        assertEquals("OutValue", shaker.getWrittenHeaders().props().get("OutHeader"));
    }
    
    public void testDiscoOnBadResponse() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                StringUtils.toAsciiBytes("GNUTELLA CONNECT/0.6\r\n" +
                 "RequestHeader: RequestValue\r\n" +
                 "\r\n" +
                 "GNUTELLA/0.6 333 SUX\r\n" +
                 "ResponseHeader: ResponseValue\r\n" +
                 "\r\n"));
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "OK!", outProps));
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncIncomingHandshaker(responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // do the transfer! -- simulates NIODispatcher notifying
        
        assertTrue(observer.isNoGOK());
        assertEquals(333, observer.getCode());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(respondedTo.toString(), 1, respondedTo.size());
        assertEquals("RequestValue", respondedTo.get("RequestHeader"));
        
        assertFalse(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(2, read.props().size());
        assertEquals("RequestValue", read.props().get("RequestHeader"));
        assertEquals("ResponseValue", read.props().get("ResponseHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutValue", written.props().get("OutHeader"));
        ByteBuffer out = writer.getBuffer();
        assertEquals("GNUTELLA/0.6 200 OK!\r\nOutHeader: OutValue\r\n\r\n", StringUtils.getASCIIString(out.array(), 0, out.limit()));    
    }
    
    public void testDiscoOnBadResponseConnectLine() throws Exception {
        ReadBufferChannel reader = new ReadBufferChannel(
                StringUtils.toAsciiBytes("GNUTELLA CONNECT/0.6\r\n" +
                 "RequestHeader: RequestValue\r\n" +
                 "\r\n" +
                 "HTTP/1.1 543 WHAT ARE YOU DOING?\r\n" +
                 "ResponseHeader: ResponseValue\r\n" +
                 "\r\n"));
        WriteBufferChannel writer = new WriteBufferChannel(2048);
        MultiplexingSocket socket = new MultiplexingSocket(reader, writer);
        Properties outProps = new Properties();
        outProps.put("OutHeader", "OutValue");
        StubHandshakeResponder responder = new StubHandshakeResponder(new StubHandshakeResponse(200, "OK!", outProps));
        StubHandshakeObserver observer = new StubHandshakeObserver();
        Handshaker shaker = new AsyncIncomingHandshaker(responder, socket, observer);
        shaker.shake();
        
        socket.exchange(); // do the transfer! -- simulates NIODispatcher notifying
        
        assertFalse(observer.isNoGOK());
        assertTrue(observer.isBadHandshake());
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        
        Map respondedTo = responder.getRespondedToProps();
        assertEquals(respondedTo.toString(), 1, respondedTo.size());
        assertEquals("RequestValue", respondedTo.get("RequestHeader"));
        
        assertFalse(responder.isOutgoing());
        
        HandshakeResponse read = shaker.getReadHeaders();
        assertEquals(1, read.props().size());
        assertEquals("RequestValue", read.props().get("RequestHeader"));
        
        HandshakeResponse written = shaker.getWrittenHeaders();
        assertEquals(1, written.props().size());
        assertEquals("OutValue", written.props().get("OutHeader"));
        ByteBuffer out = writer.getBuffer();
        assertEquals("GNUTELLA/0.6 200 OK!\r\nOutHeader: OutValue\r\n\r\n", StringUtils.getASCIIString(out.array(), 0, out.limit()));            
    }

    private static class MultiplexingSocket extends Socket implements NIOMultiplexor {
        private InterestReadableByteChannel baseReader;
        private InterestWritableByteChannel baseWriter;
        private ReadObserver reader;
        private WriteObserver writer;
        
        MultiplexingSocket(InterestReadableByteChannel baseReader, InterestWritableByteChannel baseWriter) {
            this.baseReader = baseReader;
            this.baseWriter = baseWriter;
        }
        
        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch(IOException iox) {
                return null;
            }
        }

        public void setReadObserver(ChannelReadObserver reader) {
            reader.setReadChannel(baseReader);
            this.reader = reader; 
        }

        public void setWriteObserver(ChannelWriter writer) {
            writer.setWriteChannel(baseWriter);
            this.writer = writer;
        }
        
        public void exchange() throws IOException {
            reader.handleRead();
            writer.handleWrite();
            reader.handleRead();
        }
    }
}
