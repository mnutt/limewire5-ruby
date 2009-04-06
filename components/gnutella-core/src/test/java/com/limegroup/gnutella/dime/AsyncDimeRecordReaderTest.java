package com.limegroup.gnutella.dime;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.stubs.ReadBufferChannel;

import junit.framework.Test;


/**
 * Tests for AsyncDimeRecordReader.
 */
public final class AsyncDimeRecordReaderTest extends org.limewire.gnutella.tests.LimeTestCase {

    /**
     * Constructs a new test instance for responses.
     */
    public AsyncDimeRecordReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AsyncDimeRecordReaderTest.class);
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testCreateAsync() throws Exception {
        byte[] data1 = new byte[] { 0x08, 0, 0, 0 }; // version + mb, me, cf, no type + resreved, option length: 0.
        byte[] data2 = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }; // id, type, data length == 0.
        ByteBuffer buffer1 = (ByteBuffer)ByteBuffer.wrap(data1).position(data1.length);
        ByteBuffer buffer2 = ByteBuffer.wrap(data2);
        ReadBufferChannel channel = new ReadBufferChannel(buffer2, true);
        AsyncDimeRecordReader reader = new AsyncDimeRecordReader();
        assertEquals(0, reader.getAmountProcessed());
        assertTrue(buffer2.hasRemaining());
        assertFalse(reader.process(channel, buffer1));
        assertEquals(data1.length + data2.length, reader.getAmountProcessed());
        assertFalse(buffer2.hasRemaining());
        
        DIMERecord record = reader.getRecord();
        assertFalse(record.isFirstRecord());
        assertFalse(record.isLastRecord());
        assertEquals(DIMERecord.TYPE_UNCHANGED, record.getTypeId());
        assertEquals(0, record.getType().length);
        assertEquals("", record.getTypeString());
        assertEquals(0, record.getData().length);
        assertEquals(0, record.getId().length);
        assertEquals(0, record.getOptions().length);
        assertEquals("", record.getIdentifier());
        assertEquals("", record.getIdentifier()); // test again for coverage.
        assertEquals(new HashMap(), record.getOptionsMap());
        assertEquals(new HashMap(), record.getOptionsMap()); // test again.
    }
    
    public void testCreateAsyncLeaveData() throws Exception {
        byte[] data1 = new byte[] { 0x08, 0, 0, 0 }; // version + mb, me, cf, no type + resreved, option length: 0.
        byte[] data2 = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0x1, 0x2, 0x3 }; // id, type, data length == 0, extra data
        ByteBuffer buffer1 = (ByteBuffer)ByteBuffer.wrap(data1).position(data1.length);
        ByteBuffer buffer2 = ByteBuffer.wrap(data2);
        ReadBufferChannel channel = new ReadBufferChannel(buffer2, true);
        AsyncDimeRecordReader reader = new AsyncDimeRecordReader();
        assertEquals(0, reader.getAmountProcessed());
        assertTrue(buffer2.hasRemaining());
        assertFalse(reader.process(channel, buffer1));
        assertEquals(data1.length + data2.length - 3, reader.getAmountProcessed());
        assertTrue(buffer2.hasRemaining());
        assertEquals(3, buffer2.remaining());
        assertEquals(0x1, buffer2.get());
        assertEquals(0x2, buffer2.get());
        assertEquals(0x3, buffer2.get());
        assertFalse(buffer2.hasRemaining());
        
        DIMERecord record = reader.getRecord();
        assertFalse(record.isFirstRecord());
        assertFalse(record.isLastRecord());
        assertEquals(DIMERecord.TYPE_UNCHANGED, record.getTypeId());
        assertEquals(0, record.getType().length);
        assertEquals("", record.getTypeString());
        assertEquals(0, record.getData().length);
        assertEquals(0, record.getId().length);
        assertEquals(0, record.getOptions().length);
        assertEquals("", record.getIdentifier());
        assertEquals("", record.getIdentifier()); // test again for coverage.
        assertEquals(new HashMap(), record.getOptionsMap());
        assertEquals(new HashMap(), record.getOptionsMap()); // test again.
    }
    
    public void testCreateWithDataAndPadding() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x08);
        out.write(DIMERecord.TYPE_MEDIA_TYPE);
        ByteUtils.int2beb(5, out, 2); // options length: 5
        ByteUtils.int2beb(6, out, 2); // id length: 6
        ByteUtils.int2beb(7, out, 2); // type length: 7
        ByteUtils.int2beb(8, out, 4); // data length: 8
        out.write(new byte[] { 's', 'a', 'm', 'm', 'y',  0 ,  0 ,  0  } );
        out.write(new byte[] { 'b', 'e', 'r', 'l', 'i', 'n',  0 ,  0  } );
        out.write(new byte[] { 'h', 'a', 'c', 'k', 'e', 'r', 's',  0  } );
        out.write(new byte[] { 'l', 'i', 'm', 'e', 'w', 'i', 'r', 'e' } );
        ByteBuffer data = ByteBuffer.wrap(out.toByteArray());
        ReadBufferChannel channel = new ReadBufferChannel(data);
        
        AsyncDimeRecordReader reader = new AsyncDimeRecordReader();
        assertFalse(reader.process(channel, ByteBuffer.allocate(0)));
        assertEquals(data.capacity(), reader.getAmountProcessed());
        
        DIMERecord record = reader.getRecord();
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
        assertEquals(StringUtils.toUTF8Bytes("sammy"), record.getOptions());
        assertEquals("berlin", record.getIdentifier());
        assertEquals(StringUtils.toUTF8Bytes("berlin"), record.getId());
        assertEquals("hackers", record.getTypeString());
        assertEquals(StringUtils.toUTF8Bytes("limewire"), record.getData());
        assertEquals(44, record.getRecordLength());
    }
    
    public void testCreateMultiplePasses() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x08);
        out.write(DIMERecord.TYPE_MEDIA_TYPE);
        ByteUtils.int2beb(5, out, 2); // options length: 5
        ByteUtils.int2beb(6, out, 2); // id length: 6
        ByteUtils.int2beb(7, out, 2); // type length: 7
        ByteUtils.int2beb(8, out, 4); // data length: 8
        out.write(new byte[] { 's', 'a', 'm', 'm', 'y',  0 ,  0 ,  0  } );
        out.write(new byte[] { 'b', 'e', 'r', 'l', 'i', 'n',  0 ,  0  } );
        out.write(new byte[] { 'h', 'a', 'c', 'k', 'e', 'r', 's',  0  } );
        out.write(new byte[] { 'l', 'i', 'm', 'e', 'w', 'i', 'r', 'e' } );
        out.write(new byte[] { 0x1, 0x2 } ); // bit of extra.
        
        ByteBuffer data = ByteBuffer.wrap(out.toByteArray());
        ReadBufferChannel channel = new ReadBufferChannel(data);
        ByteBuffer zero = ByteBuffer.allocate(0);
        
        AsyncDimeRecordReader reader = new AsyncDimeRecordReader();
        
        data.limit(5);
        assertTrue(reader.process(channel, zero));
        assertEquals(5, reader.getAmountProcessed());
        assertEquals(5, data.position());
        assertNull(reader.getRecord());
        
        data.limit(14); // finish header, a bit into options
        assertTrue(reader.process(channel, zero));
        assertEquals(14, reader.getAmountProcessed());
        assertEquals(14, data.position());
        assertNull(reader.getRecord());
        
        data.limit(19); // a bit into padding of options
        assertTrue(reader.process(channel, zero));
        assertEquals(19, reader.getAmountProcessed());
        assertEquals(19, data.position());
        assertNull(reader.getRecord());
        
        data.limit(43); // just before the end of data.
        assertTrue(reader.process(channel, zero));
        assertEquals(43, reader.getAmountProcessed());
        assertEquals(43, data.position());
        assertNull(reader.getRecord());
        
        data.limit(46);
        assertFalse(reader.process(channel, zero));
        assertEquals(44, data.position());
        assertEquals(44, reader.getAmountProcessed());        
        
        DIMERecord record = reader.getRecord();
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
        assertEquals(StringUtils.toUTF8Bytes("sammy"), record.getOptions());
        assertEquals("berlin", record.getIdentifier());
        assertEquals(StringUtils.toUTF8Bytes("berlin"), record.getId());
        assertEquals("hackers", record.getTypeString());
        assertEquals(StringUtils.toUTF8Bytes("limewire"), record.getData());
        assertEquals(44, record.getRecordLength());
    }
    
    public void testDIMEExceptionAmountProcessed() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x08);
        out.write(DIMERecord.TYPE_MEDIA_TYPE);
        ByteUtils.int2beb(5, out, 2); // options length: 5
        ByteUtils.int2beb(6, out, 2); // id length: 6
        ByteUtils.int2beb(7, out, 2); // type length: 7
        ByteUtils.int2beb(8, out, 4); // data length: 8
        out.write(new byte[] { 's', 'a', 'm', 'm', 'y',  0 ,  0 ,  0  } );
        out.write(new byte[] { 'b', 'e', 'r', 'l', 'i', 'n',  0 ,  0  } );
        out.write(new byte[] { 'h', 'a', 'c', 'k', 'e', 'r', 's',  0  } );
        out.write(new byte[] { 'l', 'i', 'm', 'e', 'w', 'i', 'r', 'e' } );
        out.write(new byte[] { 0x1, 0x2 } ); // bit of extra.       
    }
    
    public void testInvalidTypes() throws Exception {
        ByteArrayOutputStream out;
        ByteBuffer buffer;
        AsyncDimeRecordReader reader;
        ReadBufferChannel channel;
        ByteBuffer zero = ByteBuffer.allocate(0);
        
        out = new ByteArrayOutputStream();
        out.write( 0x08 ); // version + mb, me, cf (all clear)
        out.write( 0x00 ); // no type + reserved
        out.write( new byte[] { 0, 0 } ); // option length: 0
        out.write( new byte[] { 0, 0 } ); // id length: 0
        ByteUtils.int2beb(3, out, 2); // type length: 3
        out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
        out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
        buffer = ByteBuffer.wrap(out.toByteArray());
        reader = new AsyncDimeRecordReader();
        channel = new ReadBufferChannel(buffer);
        
        assertFalse(reader.process(channel, zero));
        assertEquals(buffer.capacity(), reader.getAmountProcessed());
        
        try {
            reader.getRecord();
            fail("expected exception.");
        } catch(DIMEException expected) {
            assertEquals("TYPE_UNCHANGED requires 0 type length", expected.getMessage());
        }
        
    
    
        out = new ByteArrayOutputStream();
        out.write( 0x08 ); // version + mb, me, cf (all clear)
        out.write( 0x03 << 4); // no type + reserved
        out.write( new byte[] { 0, 0 } ); // option length: 0
        out.write( new byte[] { 0, 0 } ); // id length: 0
        ByteUtils.int2beb(3, out, 2); // type length: 3
        out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
        out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
        buffer = ByteBuffer.wrap(out.toByteArray());
        reader = new AsyncDimeRecordReader();
        channel = new ReadBufferChannel(buffer);
        
        assertFalse(reader.process(channel, zero));
        assertEquals(buffer.capacity(), reader.getAmountProcessed());
        
        try {
            reader.getRecord();
            fail("expected exception.");
        } catch(DIMEException expected) {
            assertEquals("TYPE_UNKNOWN requires 0 type length", expected.getMessage());
        }
        
        out = new ByteArrayOutputStream();
        out.write( 0x08 ); // version + mb, me, cf (all clear)
        out.write( 0x04 << 4 ); // no type + reserved
        out.write( new byte[] { 0, 0 } ); // option length: 0
        out.write( new byte[] { 0, 0 } ); // id length: 0
        ByteUtils.int2beb(3, out, 2); // type length: 3
        out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
        out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
        buffer = ByteBuffer.wrap(out.toByteArray());
        reader = new AsyncDimeRecordReader();
        channel = new ReadBufferChannel(buffer);
        
        assertFalse(reader.process(channel, zero));
        assertEquals(buffer.capacity(), reader.getAmountProcessed());       
        
        try {
            reader.getRecord();
            fail("expected exception.");
        } catch(DIMEException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length", expected.getMessage());
        }
        
        
        out = new ByteArrayOutputStream();
        out.write( 0x08 ); // version + mb, me, cf (all clear)
        out.write( 0x04 << 4 ); // no type + reserved
        out.write( new byte[] { 0, 0 } ); // option length: 0
        out.write( new byte[] { 0, 0 } ); // id length: 0
        out.write( new byte[] { 0, 0 } ); // type length: 0
        ByteUtils.int2beb(6, out, 4); // data length: 6
        out.write( new byte[] { 's', 'a', 'm', 'u', 'e', 'l', 0, 0 } ); //data + padding
        buffer = ByteBuffer.wrap(out.toByteArray());
        reader = new AsyncDimeRecordReader();
        channel = new ReadBufferChannel(buffer);
        
        assertFalse(reader.process(channel, zero));
        assertEquals(buffer.capacity(), reader.getAmountProcessed());       
        
        try {
            reader.getRecord();
            fail("expected exception.");
        } catch(DIMEException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length", expected.getMessage());
        }
    }
    
    public void testBadType() throws Exception {
        // 0 through 4 are valid, so start at 5.
        for(int i = 5; i < 0xF; i++) {
            int type = i << 4;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write( (byte)0x08 ); //version + mb, me & cf.
            out.write( (byte)type ); // bad type + reserved
            out.write( new byte[] { 0, 0 } ); // option length: 0
            out.write( new byte[] { 0, 0 } ); // id length: 0
            out.write( new byte[] { 0, 0 } ); // type length: 0
            out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
            out.write( new byte[] { 0x1, 0x2, 0x0, 0x0 } );
            ByteBuffer buffer = ByteBuffer.wrap(out.toByteArray());
            ReadBufferChannel channel = new ReadBufferChannel(buffer);
            AsyncDimeRecordReader reader = new AsyncDimeRecordReader();
            assertFalse(reader.process(channel, ByteBuffer.allocate(0)));
            try {
                reader.getRecord();
                fail("expected exception, i: " + i);
            } catch(DIMEException expected) {
                assertEquals("invalid type: " + i, expected.getMessage());
            }
        }
    }

    public void testBadVersion() throws Exception {
        //test every invalid version possible.
        for(int i = 0; i < (0xF8 >> 3); i++) {
            if(i == 1) // valid
                continue;
            int version = i << 3;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write( (byte)version ); // mb, me & cf clear.
            out.write( 0x00 ); // no type + reserved
            out.write( new byte[] { 0, 0 } ); // option length: 0
            out.write( new byte[] { 0, 0 } ); // id length: 0
            ByteUtils.int2beb(2, out, 2); // type length: 2
            out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
            out.write( new byte[] { 0x1, 0x2 } ); // type data
            ByteBuffer buffer = ByteBuffer.wrap(out.toByteArray());
            ReadBufferChannel channel = new ReadBufferChannel(buffer);
            AsyncDimeRecordReader reader = new AsyncDimeRecordReader();
            try {
                reader.process(channel, ByteBuffer.allocate(0));
                fail("expected exception");
            } catch(DIMEException expected) {
                assertEquals("invalid version: " + i, expected.getMessage());
            }
            
            assertEquals(12, reader.getAmountProcessed());
            assertEquals(2, buffer.remaining());
        }
    }
    
    public void testBadReserved() throws Exception {
        //test every invalid reserved value possible.
        for(int i = 0; i < 0xF; i++) {
            if(i == 0) // valid
                continue;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write( 0x08 ); // mb, me & cf clear.
            out.write( (byte)i ); // no type + [invalid] reserved
            out.write( new byte[] { 0, 0 } ); // option length: 0
            out.write( new byte[] { 0, 0 } ); // id length: 0
            ByteUtils.int2beb(2, out, 2); // type length: 2
            out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
            out.write( new byte[] { 0x1, 0x2 } ); // type data
            ByteBuffer buffer = ByteBuffer.wrap(out.toByteArray());
            ReadBufferChannel channel = new ReadBufferChannel(buffer);
            AsyncDimeRecordReader reader = new AsyncDimeRecordReader();
            try {
                reader.process(channel, ByteBuffer.allocate(0));
                fail("expected exception");
            } catch(DIMEException expected) {
                assertEquals("invalid reserved: " + i, expected.getMessage());
            }
            
            assertEquals(12, reader.getAmountProcessed());
            assertEquals(2, buffer.remaining());
        }
    }
    
    public void testDataTooLarge() throws Exception {
        ByteArrayOutputStream out;
        ByteBuffer buffer;
        ReadBufferChannel channel;
        AsyncDimeRecordReader reader;
        
        out = new ByteArrayOutputStream();
        out.write( 0x08 ); // mb, me & cf clear.
        out.write( 0x00 ); // // no type + reserved
        out.write( new byte[] { 0, 0 } ); // option length: 0
        out.write( new byte[] { 0, 0 } ); // id length: 0
        out.write( new byte[] { 0, 0 } ); // type length: 0
        out.write( new byte[] { (byte)0xFF, (byte)0xFF, 
                                (byte)0xFF, (byte)0xFF } ); // data length: LARGE
        buffer = ByteBuffer.wrap(out.toByteArray());
        channel = new ReadBufferChannel(buffer);
        reader = new AsyncDimeRecordReader();
        try {
            reader.process(channel, ByteBuffer.allocate(0));
            fail("expected exception");
        } catch(DIMEException expected) {
            assertEquals("data too big.", expected.getMessage());
        }
        
        // Test boundary cases.
        out = new ByteArrayOutputStream();
        out.write( 0x08 ); // mb, me & cf clear.
        out.write( 0x00 ); // // no type + reserved
        out.write( new byte[] { 0, 0 } ); // option length: 0
        out.write( new byte[] { 0, 0 } ); // id length: 0
        out.write( new byte[] { 0, 0 } ); // type length: 0
        // one over Integer.MAX_VALUE (in big endian format)
        out.write( new byte[] { (byte)0x80, (byte)0x00, 
                                (byte)0x00, (byte)0x00 } );
        buffer = ByteBuffer.wrap(out.toByteArray());
        channel = new ReadBufferChannel(buffer);
        reader = new AsyncDimeRecordReader();
        try {
            reader.process(channel, ByteBuffer.allocate(0));
            fail("expected exception");
        } catch(DIMEException expected) {
            assertEquals("data too big.", expected.getMessage());
        }
        
        // can't really test Integer.MAX_VALUE 'cause it's too big to
        // store.
    }    
}