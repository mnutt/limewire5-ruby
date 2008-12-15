package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

import org.limewire.nio.statemachine.IOState;

import junit.framework.Test;

import com.limegroup.gnutella.stubs.ReadBufferChannel;
import com.limegroup.gnutella.util.LimeTestCase;

public class ReadHandshakeStateTest extends LimeTestCase {
    
    public ReadHandshakeStateTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ReadHandshakeStateTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSimpleProcess() throws Exception {
        String testString = "FIRST LINE\r\n" +
                            "Header1: Value1\r\n" +
                            "Header2: Value2\r\n" +
                            "\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(testString.getBytes()); 
        ReadBufferChannel channel = new ReadBufferChannel(buffer);
        
        
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        ReadHandshakeTester tester = new ReadHandshakeTester(support);
        assertFalse(tester.process(channel, scratch));
        assertTrue(tester.isProcessedConnectLine());
        assertTrue(tester.isProcessedHeaders());
        assertEquals("FIRST LINE", tester.getConnectLine());
        assertEquals("", tester.getCurrentHeader());
        assertTrue(tester.isDoneConnect());
        
        Properties props = support.getReadHandshakeResponse().props();
        assertEquals(2, props.size());
        assertEquals("Value1", props.get("Header1"));
        assertEquals("Value2", props.get("Header2"));
        assertFalse(tester.isWriting());
        assertTrue(tester.isReading());
    }
    
    public void testComplexProcess() throws Exception {
        String testString = "FIRST LINE\r\n" +
                            "Header1: Value1\r\n" +
                            "Header2: Value2\r\n" +
                            "UnknownData\r\n" +
                            "\r\n" +
                            "Extra Data Leftover";
        ByteBuffer buffer = ByteBuffer.wrap(testString.getBytes());
        ReadBufferChannel channel = new ReadBufferChannel(buffer);

        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        ReadHandshakeTester tester = new ReadHandshakeTester(support);
        
        buffer.limit("FIRS".length());
        assertTrue(tester.process(channel, scratch));
        assertFalse(tester.isProcessedConnectLine());
        assertFalse(tester.isProcessedHeaders());
        assertEquals("FIRS", tester.getCurrentHeader());
        assertFalse(tester.isDoneConnect());
        
        buffer.limit("FIRST LINE\r\n".length());
        assertTrue(tester.process(channel, scratch));
        assertTrue(tester.isProcessedConnectLine());
        assertFalse(tester.isProcessedHeaders());
        assertEquals("", tester.getCurrentHeader());
        assertEquals("FIRST LINE", tester.getConnectLine());
        assertTrue(tester.isDoneConnect());
        
        buffer.limit("FIRST LINE\r\nHeader1: Value1".length());
        assertTrue(tester.process(channel, scratch));
        assertFalse(tester.isProcessedHeaders());
        assertEquals("Header1: Value1", tester.getCurrentHeader());
        assertEquals(0, support.getReadHandshakeResponse().props().size());
        
        buffer.limit("FIRST LINE\r\nHeader1: Value1\r\n".length());
        assertTrue(tester.process(channel, scratch));
        assertFalse(tester.isProcessedHeaders());
        assertEquals("", tester.getCurrentHeader());
        assertEquals(1, support.getReadHandshakeResponse().props().size());
        assertEquals("Value1", support.getReadHandshakeResponse().props().get("Header1"));
        
        buffer.limit("FIRST LINE\r\nHeader1: Value1\r\nHeader2: Value2\r\nUnknownData\r\n".length());
        assertTrue(tester.process(channel, scratch));
        assertFalse(tester.isProcessedHeaders());
        assertEquals("", tester.getCurrentHeader());
        assertEquals(2, support.getReadHandshakeResponse().props().size());
        assertEquals("Value1", support.getReadHandshakeResponse().props().get("Header1"));
        assertEquals("Value2", support.getReadHandshakeResponse().props().get("Header2"));
        
        buffer.limit(testString.length());
        assertFalse(tester.process(channel, scratch));
        assertTrue(tester.isProcessedHeaders());
        assertEquals("", tester.getCurrentHeader());
        assertEquals(2, support.getReadHandshakeResponse().props().size());
        assertEquals("Value1", support.getReadHandshakeResponse().props().get("Header1"));
        assertEquals("Value2", support.getReadHandshakeResponse().props().get("Header2"));
        
        assertEquals("Extra Data Leftover".length(), scratch.position());
        scratch.flip();
        assertEquals("Extra Data Leftover", new String(scratch.array(), 0, scratch.limit()));
    }
    
    public void testEOF() throws Exception {
        String testString = "FIRST LINE\r\n" +
                            "Header1: Value1\r\n" +
                            "Header2: Value2\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(testString.getBytes());
        ReadBufferChannel channel = new ReadBufferChannel(buffer, true);

        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        ReadHandshakeTester tester = new ReadHandshakeTester(support);
        try {
            tester.process(channel, scratch);
            fail("should have IOXd");
        } catch(IOException iox) {
            assertEquals("EOF", iox.getMessage());
        }
        
        assertTrue(tester.isProcessedConnectLine());
        assertFalse(tester.isProcessedHeaders());

        Properties props = support.getReadHandshakeResponse().props();
        assertEquals(2, props.size());
        assertEquals("Value1", props.get("Header1"));
        assertEquals("Value2", props.get("Header2"));
    }
    
    public void testReadRequestStateProcessConnectLineSucceeds() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA CONNECT/0.6\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadRequestState(support);
        assertTrue(state.process(channel, scratch));
    }
    
    public void testReadRequestStateProcessConnectLineFails() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA CONNECT/0.5\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadRequestState(support);
        try {
            state.process(channel, scratch);
            fail("should have IOXd");
        } catch(IOException iox) {
            assertEquals("not a valid connect string!", iox.getMessage());
        }
    }
    
    public void testReadRequestStateProcessAbsurdConnectLine() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("HTTP/1.1 GET\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadRequestState(support);
        try {
            state.process(channel, scratch);
            fail("should have IOXd");
        } catch(IOException iox) {
            assertEquals("not a valid connect string!", iox.getMessage());
        }
    }
    
    public void testReadRequestStateProcessNewerConnectLine() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA CONNECT/0.7\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadRequestState(support);
        assertTrue(state.process(channel, scratch));
    }
    
    public void testReadResponseStateProcessConnectLineSucceeds() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA/0.6 200 OK\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadResponseState(support);
        assertTrue(state.process(channel, scratch));
    }
    
    public void testReadResponseStateProcessConnectLineSucceedsEvenWhenBadCode() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA/0.6 400 Failed\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadResponseState(support);
        assertTrue(state.process(channel, scratch));
    }    
    
    public void testReadResponseStateProcessConnectLineFailsBadVersion() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA/0.5\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadResponseState(support);
        try {
            state.process(channel, scratch);
            fail("should have IOXd");
        } catch(IOException iox) {
            assertEquals("Bad connect string", iox.getMessage());
        }
    }
    
    public void testReadResponseStateProcessAbsurdConnectLine() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("HTTP/1.1 200 OK\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadResponseState(support);
        try {
            state.process(channel, scratch);
            fail("should have IOXd");
        } catch(IOException iox) {
            assertEquals("Bad connect string", iox.getMessage());
        }
    }
    
    public void testReadResponseStateProcessHeadersSucceeds() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA/0.6 200 OK\r\n\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadResponseState(support);
        assertFalse(state.process(channel, scratch));
    }
    
    public void testReadResponseStateProcessHeadersBadCodeFails() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA/0.6 303 Failed\r\n\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        IOState state = new ReadHandshakeState.ReadResponseState(support);
        try {
            state.process(channel, scratch);
            fail("should have failed!");
        } catch(NoGnutellaOkException ngok) {
            assertEquals(303, ngok.getCode());
        }
    }    
    
    public void testReadResponseStateProcessConnectLineCrawler() throws Exception {
        ReadBufferChannel channel = new ReadBufferChannel("GNUTELLA/0.6\r\n".getBytes());
        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        support.processReadHeader("Crawler: 0.1");
        IOState state = new ReadHandshakeState.ReadResponseState(support);
        try {
            state.process(channel, scratch);
            fail("should have IOXd");
        } catch(IOException iox) {
            assertEquals("crawler", iox.getMessage());
        }       
    }
    
    private static class ReadHandshakeTester extends ReadHandshakeState {
        private boolean processedConnectLine;
        private boolean processedHeaders;
        
        ReadHandshakeTester(HandshakeSupport support) {
            super(support);
        }
        
        @Override
        protected void processConnectLine() throws IOException {
            this.processedConnectLine = true;
        }

        @Override
        protected void processHeaders() throws IOException {
            this.processedHeaders = true;
        }

        public boolean isProcessedConnectLine() {
            return processedConnectLine;
        }

        public boolean isProcessedHeaders() {
            return processedHeaders;
        }
        
        public String getConnectLine() {
            return connectLine;
        }
        
        public String getCurrentHeader() {
            return currentHeader.toString();
        }
        
        public boolean isDoneConnect() {
            return doneConnect;
        }        
    }
}
