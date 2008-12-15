package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

import org.limewire.nio.statemachine.IOState;

import junit.framework.Test;

import com.limegroup.gnutella.stubs.WriteBufferChannel;
import com.limegroup.gnutella.util.LimeTestCase;

public class WriteHandshakeStateTest extends LimeTestCase {
    
    public WriteHandshakeStateTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(WriteHandshakeStateTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSimpleProcess() throws Exception {
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        WriteHandshakeTester tester = new WriteHandshakeTester(support, "OUTGOING");
        WriteBufferChannel channel = new WriteBufferChannel(100);
        assertFalse(tester.process(channel, null));
        assertEquals("OUTGOING", channel.getDataAsString());
        assertEquals(1, tester.getCreateOutgoingData());
        assertTrue(tester.isProcessWrittenHeaders());
        assertTrue(tester.isWriting());
        assertFalse(tester.isReading());
    }
    
    public void testComplexProcess() throws Exception {
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        WriteHandshakeTester tester = new WriteHandshakeTester(support, "OUTGOING");
        WriteBufferChannel channel = new WriteBufferChannel(2);
        assertTrue(tester.process(channel, null));
        assertEquals("OU", channel.getDataAsString());
        assertEquals(1, tester.getCreateOutgoingData());
        assertFalse(tester.isProcessWrittenHeaders());
        
        channel.resize(9);
        assertFalse(tester.process(channel, null));
        assertEquals("TGOING", channel.getDataAsString());
        assertEquals(1, tester.getCreateOutgoingData());
        assertTrue(tester.isProcessWrittenHeaders());
    }
    
    public void testWriteResponseHandshakeStateCreateOutgoingDataOutgoing() throws Exception {
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        support.createRemoteResponse("GNUTELLA/0.6 333 HELLO"); // needed to create the remote response in the state
        support.processReadHeader("Header1: Value1");
        Properties props = new Properties();
        props.put("Out", "Value");
        HandshakeResponse response = new StubHandshakeResponse(200, "YAY", props);
        StubHandshakeResponder responder = new StubHandshakeResponder(response);
        IOState state = new WriteHandshakeState.WriteResponseState(support, responder, true);

        WriteBufferChannel channel = new WriteBufferChannel(2048);
        assertFalse(state.process(channel, null));
        assertEquals("GNUTELLA/0.6 200 YAY\r\nOut: Value\r\n\r\n", channel.getDataAsString());
        assertEquals(333, responder.getRespondedTo().getStatusCode());
        assertEquals("HELLO", responder.getRespondedTo().getStatusMessage());
        assertEquals(1, responder.getRespondedToProps().size());
        assertEquals("Value1", responder.getRespondedToProps().get("Header1"));
        assertTrue(responder.isOutgoing());
    }
    
    public void testWriteResponseHandshakeStateCreateOutgoingDataIncoming() throws Exception {
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        support.processReadHeader("Header1: Value1");
        Properties props = new Properties();
        props.put("Out", "Value");
        HandshakeResponse response = new StubHandshakeResponse(200, "YAY", props);
        StubHandshakeResponder responder = new StubHandshakeResponder(response);
        IOState state = new WriteHandshakeState.WriteResponseState(support, responder, false);

        WriteBufferChannel channel = new WriteBufferChannel(2048);
        assertFalse(state.process(channel, null));
        assertEquals("GNUTELLA/0.6 200 YAY\r\nOut: Value\r\n\r\n", channel.getDataAsString());
        assertEquals(1, responder.getRespondedToProps().size());
        assertEquals("Value1", responder.getRespondedToProps().get("Header1"));
        assertFalse(responder.isOutgoing());
    }
    
    public void testWriteResponseHandshakeStateProcessHeadersCrawlerFailsOutgoing() throws Exception {
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        support.createRemoteResponse("GNUTELLA/0.6 333 HELLO"); // needed to create the remote response in the state
        support.processReadHeader("Header1: Value1");
        Properties props = new Properties();
        props.put("Out", "Value");
        HandshakeResponse response = new StubHandshakeResponse(HandshakeResponse.CRAWLER_CODE, "YAY", props);
        StubHandshakeResponder responder = new StubHandshakeResponder(response);
        IOState state = new WriteHandshakeState.WriteResponseState(support, responder, true);

        WriteBufferChannel channel = new WriteBufferChannel(2048);
        try {
            state.process(channel, null);
            fail("should have failed!");
        } catch(NoGnutellaOkException ngok) {
            assertEquals(593, ngok.getCode());
        }
        assertEquals("GNUTELLA/0.6 593 YAY\r\nOut: Value\r\n\r\n", channel.getDataAsString());
        assertEquals(333, responder.getRespondedTo().getStatusCode());
        assertEquals("HELLO", responder.getRespondedTo().getStatusMessage());
        assertEquals(1, responder.getRespondedToProps().size());
        assertEquals("Value1", responder.getRespondedToProps().get("Header1"));
        assertTrue(responder.isOutgoing());
    }
    
    public void testWriteResponseHandshakeStateProcessHeadersCrawlerSucceedsIncoming() throws Exception {
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        support.processReadHeader("Header1: Value1");
        Properties props = new Properties();
        props.put("Out", "Value");
        HandshakeResponse response = new StubHandshakeResponse(HandshakeResponse.CRAWLER_CODE, "YAY", props);
        StubHandshakeResponder responder = new StubHandshakeResponder(response);
        IOState state = new WriteHandshakeState.WriteResponseState(support, responder, false);

        WriteBufferChannel channel = new WriteBufferChannel(2048);
        assertFalse(state.process(channel, null));
        assertEquals("GNUTELLA/0.6 593 YAY\r\nOut: Value\r\n\r\n", channel.getDataAsString());
        assertEquals(1, responder.getRespondedToProps().size());
        assertEquals("Value1", responder.getRespondedToProps().get("Header1"));
        assertFalse(responder.isOutgoing());
    }
    
    public void testWriteResponseHandshakeStateProcessHeadersFailsIncoming() throws Exception {
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        support.processReadHeader("Header1: Value1");
        Properties props = new Properties();
        props.put("Out", "Value");
        HandshakeResponse response = new StubHandshakeResponse(123, "YAY", props);
        StubHandshakeResponder responder = new StubHandshakeResponder(response);
        IOState state = new WriteHandshakeState.WriteResponseState(support, responder, false);

        WriteBufferChannel channel = new WriteBufferChannel(2048);
        try {
            state.process(channel, null);
        } catch(NoGnutellaOkException ngok) {
            assertEquals(123, ngok.getCode());
        }
        assertEquals("GNUTELLA/0.6 123 YAY\r\nOut: Value\r\n\r\n", channel.getDataAsString());
        assertEquals(1, responder.getRespondedToProps().size());
        assertEquals("Value1", responder.getRespondedToProps().get("Header1"));
        assertFalse(responder.isOutgoing());
    }
    
    public void testWriteRequestCreateOutgoingData() throws Exception {
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        Properties props = new Properties();
        props.put("Out", "Value");
        IOState state = new WriteHandshakeState.WriteRequestState(support, props);
        
        WriteBufferChannel channel = new WriteBufferChannel(2048);
        assertFalse(state.process(channel, null));
        assertEquals("GNUTELLA CONNECT/0.6\r\nOut: Value\r\n\r\n", channel.getDataAsString());
    }
        
    private static class WriteHandshakeTester extends WriteHandshakeState {
        private int createOutgoingData;
        private boolean processWrittenHeaders;
        private ByteBuffer outgoingData;

        WriteHandshakeTester(HandshakeSupport support, String data) {
            super(support);
            this.outgoingData = ByteBuffer.wrap(data.getBytes());
        }

        @Override
        protected ByteBuffer createOutgoingData() throws IOException {
            createOutgoingData++;
            return outgoingData;
        }

        @Override
        protected void processWrittenHeaders() throws IOException {
            this.processWrittenHeaders = true;
        }
        
        public int getCreateOutgoingData() {
            return createOutgoingData;
        }

        public boolean isProcessWrittenHeaders() {
            return processWrittenHeaders;
        }        
        
    }
}