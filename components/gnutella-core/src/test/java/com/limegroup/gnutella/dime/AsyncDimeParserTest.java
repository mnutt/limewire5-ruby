package com.limegroup.gnutella.dime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.limewire.util.StringUtils;

import com.limegroup.gnutella.stubs.ReadBufferChannel;

import junit.framework.Test;



/**
 * Tests for AsyncDimeParser.
 */
public final class AsyncDimeParserTest extends org.limewire.gnutella.tests.LimeTestCase {
    

    private ByteArrayOutputStream out;
    private ByteBuffer buffer;
    private ReadBufferChannel channel;
    private AsyncDimeParser parser;
    private DIMERecord one, two;
    private DIMERecord readOne;
    private ByteBuffer zero = ByteBuffer.allocate(0);

    /**
     * Constructs a new test instance for responses.
     */
    public AsyncDimeParserTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AsyncDimeParserTest.class);
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testNoFirstRecord() throws Exception {
        // test a stream that didn't have a 'first' message.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, null);
        one.write(out);
        
        parser = new AsyncDimeParser();
        buffer = ByteBuffer.wrap(out.toByteArray());
        channel = new ReadBufferChannel(buffer);
        try {
            parser.process(channel, zero);
            fail("expected exception");
        } catch(IOException ex) {
            assertEquals("middle of stream.", ex.getMessage());
        }
        
    }
    
    public void testSingleMessageNoEnd() throws Exception {
        // test we can read a single message.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, StringUtils.toUTF8Bytes("sab"));
        one.setFirstRecord(true);
        one.write(out);
        
        parser = new AsyncDimeParser();
        buffer = ByteBuffer.wrap(out.toByteArray());
        channel = new ReadBufferChannel(buffer);
        assertTrue(parser.process(channel, zero));
        
        List list = parser.getRecords();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(one.getRecordLength(), parser.getAmountProcessed());
        readOne = (DIMERecord)list.get(0);
        assertEquals(StringUtils.toUTF8Bytes("sab"), readOne.getData());
    }
    
    public void testSingleMessageBeginAndEnd() throws Exception {
        // test that once we get a message with ME we stop.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, StringUtils.toUTF8Bytes("sam"));
        one.setFirstRecord(true);
        one.setLastRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, StringUtils.toUTF8Bytes("bad"));
        one.write(out);
        two.write(out);

        parser = new AsyncDimeParser();
        buffer = ByteBuffer.wrap(out.toByteArray());
        channel = new ReadBufferChannel(buffer);
        assertFalse(parser.process(channel, zero));
        
        List list = parser.getRecords();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(one.getRecordLength(), parser.getAmountProcessed());
        assertEquals(two.getRecordLength(), buffer.remaining());
        readOne = (DIMERecord)list.get(0);
        assertEquals(StringUtils.toUTF8Bytes("sam"), readOne.getData());
    }
    
    public void testMultipleFirstMessages() throws Exception {
        // test multiple 'first' messages fail.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, StringUtils.toUTF8Bytes("sab"));
        one.setFirstRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, StringUtils.toUTF8Bytes("bas"));
        two.setFirstRecord(true);
        one.write(out);
        two.write(out);

        parser = new AsyncDimeParser();
        buffer = ByteBuffer.wrap(out.toByteArray());
        channel = new ReadBufferChannel(buffer);
        
        try {
            parser.process(channel, zero);
            fail("exception expected");
        } catch(IOException ex) {
            assertEquals("two first records.", ex.getMessage());
        }
    }
}