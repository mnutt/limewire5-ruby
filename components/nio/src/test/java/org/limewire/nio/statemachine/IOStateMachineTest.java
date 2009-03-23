package org.limewire.nio.statemachine;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.nio.channel.ReadBufferChannel;
import org.limewire.nio.channel.WriteBufferChannel;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

@SuppressWarnings("unchecked")
public class IOStateMachineTest extends BaseTestCase {
    
    public IOStateMachineTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IOStateMachineTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testInterestSetCorrectly() {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubIOStateObserver observer = new StubIOStateObserver();
        StubIOState state1 = new StubIOState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        StubIOState state2 = new StubIOState();
        state2.setWriting(true);
        state2.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        states.add(state2);
        
        IOStateMachine shake = new IOStateMachine(observer, states);
        
        assertFalse(readChannel.isInterested());
        shake.setReadChannel(readChannel);
        assertTrue(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        shake.setWriteChannel(writeChannel);
        assertTrue(writeChannel.interested());
 
        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());
        
        // First state is reading, so let's see what happens if we set a write hit.
        readChannel.interestRead(false);
        shake.handleWrite();
        assertFalse(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        assertFalse(state1.isProcessed());
        assertFalse(state2.isProcessed());
 
        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());

        // Now try a correct read...
        readChannel.interestRead(true);
        shake.handleRead();
        assertTrue(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        assertTrue(state1.isProcessed());
        assertFalse(state2.isProcessed());
        state1.clear();

        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());
        
        // What'll happen when this state finishes...
        state1.setReturnTrueOnProcess(false);
        shake.handleRead();
        assertFalse(readChannel.isInterested());
        assertTrue(writeChannel.interested());
        assertTrue(state1.isProcessed());
        assertFalse(state2.isProcessed());
        state1.clear();

        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());
        
        // State is now state2, which is writing.. let's see what happens if we hit a read.
        writeChannel.interestWrite(null, false);
        shake.handleRead();
        assertFalse(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        assertFalse(state1.isProcessed());
        assertFalse(state2.isProcessed());
 
        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());
        
        // Now let's try a correct write...
        writeChannel.interestWrite(null, true);
        shake.handleWrite();
        assertFalse(readChannel.isInterested());
        assertTrue(writeChannel.interested());
        assertFalse(state1.isProcessed());
        assertTrue(state2.isProcessed());
        state2.clear();
 
        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());
        
        // And if the write is set to not continue?
        state2.setReturnTrueOnProcess(false);
        shake.handleWrite();
        assertFalse(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        assertFalse(state1.isProcessed());
        assertTrue(state2.isProcessed());
        state2.clear();
        
        assertTrue(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());        
    }
    
    public void testStateIOX() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubIOStateObserver observer = new StubIOStateObserver();
        StubIOState state1 = new StubIOState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        IOStateMachine shake = new IOStateMachine(observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        state1.setThrowIOX(true);
        shake.handleRead();
        
        assertFalse(observer.isStatesFinished());
        assertNotNull(observer.getIox());
        assertFalse(observer.isShutdown());
        
        observer.clear();
        shake.shutdown(); // make sure it doesn't call stuff again
        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());
    }
    
    public void testShutdownShakerWhileOpen() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubIOStateObserver observer = new StubIOStateObserver();
        StubIOState state1 = new StubIOState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        IOStateMachine shake = new IOStateMachine(observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        shake.shutdown();
        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());
        
        // no more processing after this point!
        shake.handleRead();
        assertFalse(state1.isProcessed());
    }
    
    public void testShutdownShakerWhileClosed() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubIOStateObserver observer = new StubIOStateObserver();
        StubIOState state1 = new StubIOState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        IOStateMachine shake = new IOStateMachine(observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        readChannel.setClosed(true);
        shake.shutdown();
        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertTrue(observer.isShutdown());
        
        observer.clear();
        shake.shutdown(); // make sure it doesn't call stuff again
        assertFalse(observer.isStatesFinished());
        assertNull(observer.getIox());
        assertFalse(observer.isShutdown());        
        
        // no more processing after this point!
        shake.handleRead();
        assertFalse(state1.isProcessed());        
    }
    
    public void testReadBuffer() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubIOStateObserver observer = new StubIOStateObserver();
        StubIOState state1 = new StubIOState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        IOStateMachine shake = new IOStateMachine(observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        state1.setDataToPutInBuffer(StringUtils.toAsciiBytes("DATA"));
        shake.handleRead();
        
        ByteBuffer buffer = ByteBuffer.allocate(5);
        shake.read(buffer);
        assertEquals("DATA\0", StringUtils.getASCIIString(buffer.array()));
        assertEquals(4, buffer.position());
    }

}
