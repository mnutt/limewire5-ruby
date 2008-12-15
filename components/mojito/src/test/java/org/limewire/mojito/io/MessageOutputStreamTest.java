package org.limewire.mojito.io;

import java.io.ByteArrayOutputStream;

import junit.framework.TestSuite;

import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;

public class MessageOutputStreamTest extends BaseTestCase {

    public MessageOutputStreamTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(MessageOutputStreamTest.class);
    }

    /**
     * Ensures that only the number 0 is written out for empty strings. 
     */
    public void testWriteEmptyDHTString() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MessageOutputStream messageStream = new MessageOutputStream(out);
        
        messageStream.writeDHTString("");
        
        byte[] writtenBytes = out.toByteArray();
        assertEquals(2, writtenBytes.length);
        assertEquals(0, ByteUtils.beb2short(writtenBytes, 0));
    }
    
     
}
