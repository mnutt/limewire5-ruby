package com.limegroup.gnutella.rudp.messages;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.rudp.messages.AckMessage;
import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.FinMessage;
import org.limewire.rudp.messages.KeepAliveMessage;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.rudp.messages.SynMessage.Role;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;

public class LimeRUDPMessageParserTest extends LimeTestCase {
    
    public LimeRUDPMessageParserTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimeRUDPMessageParserTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testParseMessages() throws Exception {
        LimeRUDPMessageFactory factory = new LimeRUDPMessageFactory(new DefaultMessageFactory());
        LimeRUDPMessageParser parser = new LimeRUDPMessageParser(factory);
        
        doReadWriteTest(parser, factory.createAckMessage((byte)1, 1, 1, 1), AckMessage.class);
        doReadWriteTest(parser, factory.createDataMessage((byte)1, 1, buffer(503)), DataMessage.class);
        doReadWriteTest(parser, factory.createFinMessage((byte)1, 1, (byte)1), FinMessage.class);
        doReadWriteTest(parser, factory.createKeepAliveMessage((byte)1, 1, 1), KeepAliveMessage.class);
        doReadWriteTest(parser, factory.createSynMessage((byte)1, Role.UNDEFINED), SynMessage.class);
        doReadWriteTest(parser, factory.createSynMessage((byte)1, (byte)1, Role.UNDEFINED), SynMessage.class);
    }
    
    private void doReadWriteTest(MessageParser p, RUDPMessage m, Class expected) throws Exception {
        Class clazz = m.getClass();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        byte[] b = out.toByteArray();
        assertGreaterThanOrEquals(23, b.length);
        byte[] header = new byte[23];
        byte[] payload = new byte[b.length - 23];
        System.arraycopy(b, 0, header, 0, header.length);
        System.arraycopy(b, 23, payload, 0, payload.length);
        Message parsed = p.parse(header, payload, Network.UNKNOWN, (byte)1, null);
        assertInstanceof(AbstractLimeRUDPMessage.class, parsed);
        assertEquals(clazz, parsed.getClass());
        assertNotSame(parsed, m);
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        parsed.write(out2);
        assertEquals(b, out2.toByteArray());
        assertInstanceof(expected, parsed);
        
        // For DataMsg, do an extra check that the payload is the same reference.
        if(m instanceof DataMessage) {
            DataMessage dm = (DataMessage)parsed;
            assertNotSame(header, dm.getData1Chunk().array());
            assertSame(payload, dm.getData2Chunk().array());
        }
    }
    
    private ByteBuffer buffer(int length) {
        byte[] b = new byte[length];
        Random r = new Random();
        r.nextBytes(b);
        return ByteBuffer.wrap(b);
    }
}
