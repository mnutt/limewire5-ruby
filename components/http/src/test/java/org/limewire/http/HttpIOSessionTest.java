package org.limewire.http;

import junit.framework.TestCase;

import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionBufferStatus;
import org.limewire.http.reactor.HttpChannel;
import org.limewire.http.reactor.HttpIOSession;

public class HttpIOSessionTest extends TestCase {

    public void testEventMask() {
        StubSocket socket = new StubSocket();
        HttpIOSession session = new HttpIOSession(socket, null);
        HttpChannel channel = new StubHttpChannel(session, new MockIOEventDispatch());
        session.setHttpChannel(channel);
        
        assertEquals(0, session.getEventMask());
        session.setEvent(EventMask.WRITE);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.clearEvent(EventMask.READ);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.setEvent(EventMask.READ);
        assertEquals(EventMask.READ_WRITE, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.setEventMask(EventMask.WRITE);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.setEvent(EventMask.WRITE);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.clearEvent(EventMask.WRITE);
        assertEquals(0, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.setEventMask(EventMask.READ);
        assertEquals(EventMask.READ, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.clearEvent(EventMask.READ);
        assertEquals(0, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.setEventMask(EventMask.READ);
        assertEquals(EventMask.READ, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.clearEvent(EventMask.WRITE);
        assertEquals(EventMask.READ, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.setEventMask(EventMask.READ_WRITE);
        session.clearEvent(EventMask.WRITE);
        assertEquals(EventMask.READ, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.setEventMask(EventMask.READ_WRITE);
        session.clearEvent(EventMask.READ);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
    }

    public void testConstructor() {
        try {
            new HttpIOSession(null, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        
        StubSocket socket = new StubSocket();
        HttpIOSession session = new HttpIOSession(socket, null);
        assertSame(socket, session.getSocket());
    }
    
    public void testAttributes() {
        StubSocket socket = new StubSocket();
        HttpIOSession session = new HttpIOSession(socket, null);
        
        assertNull(session.getAttribute("foo"));
        session.setAttribute("foo", "bar");
        assertEquals("bar", session.getAttribute("foo"));
        session.setAttribute("foo", "baz");
        assertEquals("baz", session.getAttribute("foo"));
        assertNull(session.getAttribute("baz"));
        session.removeAttribute("bar");
        assertNull(session.getAttribute("bar"));
    }

    public void testSocketTimeout() throws Exception {
        StubSocket socket = new StubSocket();
        HttpIOSession session = new HttpIOSession(socket, null);
        
        assertEquals(0, session.getSocketTimeout());
        session.setSocketTimeout(100);
        assertEquals(100, session.getSocketTimeout());
        assertEquals(100, socket.getSoTimeout());
    }

    public void testBufferStatus() throws Exception {
        StubSocket socket = new StubSocket();
        HttpIOSession session = new HttpIOSession(socket, null);
        
        assertFalse(session.hasBufferedInput());
        assertFalse(session.hasBufferedOutput());
        
        StubSessionBufferStatus status = new StubSessionBufferStatus();
        session.setBufferStatus(status);
        assertSame(status, session.getBufferStatus());
        assertFalse(session.hasBufferedInput());
        assertFalse(session.hasBufferedOutput());
        status.bufferedInput = true;
        assertTrue(session.hasBufferedInput());
        assertFalse(session.hasBufferedOutput());
        status.bufferedInput = false;
        assertFalse(session.hasBufferedInput());
        assertFalse(session.hasBufferedOutput());
        status.bufferedOutput = true;
        assertFalse(session.hasBufferedInput());
        assertTrue(session.hasBufferedOutput());
        status.bufferedOutput = false;
        assertFalse(session.hasBufferedInput());
        assertFalse(session.hasBufferedOutput());
    }

    public void testShutdown() {        
        StubSocket socket = new StubSocket();
        HttpIOSession session = new HttpIOSession(socket, null);
        assertFalse(session.isClosed());
        session.shutdown();
        assertTrue(session.isClosed());
        session.shutdown();
        assertTrue(session.isClosed());
    }

    public void testClose() {        
        StubSocket socket = new StubSocket();
        HttpIOSession session = new HttpIOSession(socket, null);
        StubHttpChannel channel = new StubHttpChannel(session, new MockIOEventDispatch());
        session.setHttpChannel(channel);
        
        assertFalse(session.isClosed());
        session.close();
        assertTrue(session.isClosed());
        assertTrue(channel.pendingClose);
    }

    private static class StubHttpChannel extends HttpChannel {

        boolean pendingClose;

        public StubHttpChannel(HttpIOSession session,
                IOEventDispatch eventDispatch) {
            super(session, eventDispatch);
        }

        @Override
        public void closeWhenBufferedOutputHasBeenFlushed() {
            this.pendingClose = true;
        }
        
    }

    private static class StubSessionBufferStatus implements SessionBufferStatus {

        private boolean bufferedOutput;
        private boolean bufferedInput;

        public boolean hasBufferedInput() {
            return bufferedInput;
        }

        public boolean hasBufferedOutput() {
            return bufferedOutput;
        }
        
    }
    
}
