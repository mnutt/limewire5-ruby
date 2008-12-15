package com.limegroup.gnutella.dime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.limewire.util.ByteUtils;

import junit.framework.Test;


/**
 * Tests for DIMERecord.
 */
public final class DIMERecordTest extends com.limegroup.gnutella.util.LimeTestCase {

	/**
	 * Constructs a new test instance for responses.
	 */
	public DIMERecordTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DIMERecordTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	
    // A DIME Record looks like the following:
    ///////////////////////////////////////////////////////////////////
    // 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 
    // ----------------------------------------------------------------
    //  VERSION |M|M|C|  TYPE |  RSRV |          OPTIONS_LENGTH
    //          |B|E|F|       |       |
    // ----------------------------------------------------------------
    //         ID_LENGTH              |          TYPE_LENGTH
    // ----------------------------------------------------------------
    //                           DATA_LENGTH
    // ----------------------------------------------------------------
    //                        OPTIONS + PADDING
    // ----------------------------------------------------------------
    //                          ID + PADDING
    // ----------------------------------------------------------------
    //                         TYPE + PADDING
    // ----------------------------------------------------------------
    //                         DATA + PADDING
    // ----------------------------------------------------------------
    ///////////////////////////////////////////////////////////////////	
    // TYPES:
    //  UNCHANGED: 0x00
    //  MEDIA_TYPE: 0x01
    //  ABSOLUTE_URI: 0x02
    //  UNKNOWN: 0x03
    //  NONE:  0x04
    
    
    // TODO:  Add tests for options (we currently don't use any options)
	
	public void testCreateFromStream() throws Exception {
	    // Basic DIMERecord, no data at all, correct version.
	    // More complex records are tested in other tests.
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( 0x00 ); // no type + reserved
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	    DIMERecord record = DIMERecord.createFromStream(in);
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
	
	public void testCreate() throws Exception {
	    // Basic DIMERecord, no data at all.
	    // More complex creates are tested in other tests.
	    DIMERecord record = new DIMERecord((byte)0x00, null, 
	                                          null, null, null);
	    assertFalse(record.isFirstRecord());
	    assertFalse(record.isLastRecord());
	    assertEquals(DIMERecord.TYPE_UNCHANGED, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    assertEquals(12, record.getRecordLength());
	    
	    
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ByteArrayInputStream in;
	    
	    record.write(out);
	    in = new ByteArrayInputStream(out.toByteArray());
	    byte[] header = new byte[12];
	    assertEquals(12, in.read(header, 0, 12));
	    assertEquals(0x08, header[0]);
	    assertEquals(0x00, header[1]);
	    for(int i = 2; i < header.length; i++)
	        assertEquals(0, header[i]);
        assertEquals(-1, in.read());
    }   
    
    public void testMessageBeginAndMessageEnd() throws Exception {
        DIMERecord record;
        ByteArrayOutputStream out;
        ByteArrayInputStream in;
	    byte[] header = new byte[12];        
        
        record = new DIMERecord((byte)0x00, null, null, null, null);
	    assertFalse(record.isFirstRecord());
	    assertFalse(record.isLastRecord());
	    
	    record.setFirstRecord(true);
	    assertTrue(record.isFirstRecord());
	    assertFalse(record.isLastRecord());
	    record.setFirstRecord(false);
	    assertFalse(record.isFirstRecord());
	    assertFalse(record.isLastRecord());
	    
	    record.setLastRecord(true);
	    assertFalse(record.isFirstRecord());
	    assertTrue(record.isLastRecord());
	    record.setLastRecord(false);
	    assertFalse(record.isFirstRecord());
	    assertFalse(record.isLastRecord());
	    
	    record.setFirstRecord(true);
	    record.setLastRecord(true);
	    assertTrue(record.isFirstRecord());
	    assertTrue(record.isLastRecord());
	    
	    record.setFirstRecord(true);
	    record.setLastRecord(false);
	    out = new ByteArrayOutputStream();
	    record.write(out);
	    in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(12, in.read(header, 0, 12));
	    assertEquals( 0x08 | (0x01 << 2), header[0]); // test MB flag
	    for(int i = 1; i < header.length; i++)
	        assertEquals(0, header[i]);
	    assertEquals(-1, in.read());
	    
	    record.setFirstRecord(false);
	    record.setLastRecord(true);
	    out = new ByteArrayOutputStream();
	    record.write(out);
	    in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(12, in.read(header, 0, 12));
	    assertEquals( 0x08 | (0x01 << 1), header[0]); // test ME flag
	    for(int i = 1; i < header.length; i++)
	        assertEquals(0, header[i]);
	    assertEquals(-1, in.read());
	    
	    record.setFirstRecord(true);
	    record.setLastRecord(true);
	    out = new ByteArrayOutputStream();
	    record.write(out);
	    in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(12, in.read(header, 0, 12));
	    assertEquals( 0x08 | (0x01 << 2) | (0x01 << 1), header[0]); // ME & MB
	    for(int i = 1; i < header.length; i++)
	        assertEquals(0, header[i]);
	    assertEquals(-1, in.read());	    
	    
	    record.setFirstRecord(false);
	    record.setLastRecord(false);
	    out = new ByteArrayOutputStream();
	    record.write(out);
	    in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(12, in.read(header, 0, 12));
	    assertEquals( 0x08, header[0]); // neither ME nor MB.
	    for(int i = 1; i < header.length; i++)
	        assertEquals(0, header[i]);
	    assertEquals(-1, in.read());		    
    }

	public void testPadding() throws Exception {
	    // Make sure we pad output with the appropriate stuff.
	    DIMERecord record;
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ByteArrayInputStream in;
	    byte[] header = new byte[12];
	    
	    record = new DIMERecord(DIMERecord.TYPE_MEDIA_TYPE,
	                               "4abc".getBytes(), "1".getBytes(),
	                               "2x".getBytes(), "3yz".getBytes());
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
        assertEquals("1", record.getIdentifier());
        assertEquals("1".getBytes(), record.getId());
        assertEquals("2x", record.getTypeString());
        assertEquals("3yz".getBytes(), record.getData());
        assertEquals("4abc".getBytes(), record.getOptions());
        assertEquals(28, record.getRecordLength());
        
        record.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(12, in.read(header, 0, 12)); // must read 12 bytes of the header.
        assertEquals(0x08, header[0]); // version + flags
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, header[1]); // type + reserve
        int optionsLength = ByteUtils.beb2int(header, 2, 2);
        int idLength = ByteUtils.beb2int(header, 4, 2);
        int typeLength = ByteUtils.beb2int(header, 6, 2);
        int dataLength = ByteUtils.beb2int(header, 8, 4);
        assertEquals(4, optionsLength);
        assertEquals(1, idLength);
        assertEquals(2, typeLength);
        assertEquals(3, dataLength);
        // this is the real padding test -- all data is in multiples of 4,
        // with extra 0s at the end for the padding.
        byte[] info = new byte[4];
        assertEquals(4, in.read(info, 0, 4));
        assertEquals( new byte[] { '4', 'a', 'b', 'c' }, info);
        assertEquals(4, in.read(info, 0, 4));
        assertEquals( new byte[] { '1', 0, 0, 0 }, info );
        assertEquals(4, in.read(info, 0, 4));
        assertEquals( new byte[] { '2', 'x', 0, 0 }, info );
        assertEquals(4, in.read(info, 0, 4));
        assertEquals( new byte[] { '3', 'y', 'z', 0 }, info );
        assertEquals(-1, in.read());
        
        //Create a message with some padding and make sure we can read it.
        out = new ByteArrayOutputStream();
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
        in = new ByteArrayInputStream(out.toByteArray());
        record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
        assertEquals("sammy".getBytes(), record.getOptions());
        assertEquals("berlin", record.getIdentifier());
        assertEquals("berlin".getBytes(), record.getId());
        assertEquals("hackers", record.getTypeString());
        assertEquals("limewire".getBytes(), record.getData());
        assertEquals(44, record.getRecordLength());
        
        //Create a message with invalid padding and make sure we throw IOE.
        out = new ByteArrayOutputStream();
        out.write(0x08);
        out.write(DIMERecord.TYPE_MEDIA_TYPE);
        ByteUtils.int2beb(5, out, 2); // options length: 5
        ByteUtils.int2beb(6, out, 2); // id length: 6
        ByteUtils.int2beb(7, out, 2); // type length: 7
        ByteUtils.int2beb(8, out, 4); // data length: 8
        out.write(new byte[] { 's', 'a', 'm', 'm', 'y',  0 ,  0  } );
        out.write(new byte[] { 'b', 'e', 'r', 'l', 'i', 'n',  0 ,  0  } );
        out.write(new byte[] { 'h', 'a', 'c', 'k', 'e', 'r', 's',  0  } );
        out.write(new byte[] { 'l', 'i', 'm', 'e', 'w', 'i', 'r', 'e' } );
        in = new ByteArrayInputStream(out.toByteArray());
        try {
            record = DIMERecord.createFromStream(in);
        } catch(IOException expected) {
            assertEquals("eof", expected.getMessage());
        }
    }
	
	public void testValidTypes() throws Exception {
	    DIMERecord record;
	    ByteArrayOutputStream out;
	    ByteArrayInputStream in;
	    
	    // A - TYPE_UNCHANGED.
	    // Valid only if the 'type' data is 0 length.
	    record = new DIMERecord((byte)0x00,
	                               null, null, null, null);
        assertEquals(DIMERecord.TYPE_UNCHANGED, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    // we already tested the network TYPE_UNCHANGED in testCreateFromStream
	    
	    // Now try a bad case -- TYPE_UNCHANGED with a type passed to it.
	    try {
	        record = new DIMERecord((byte)0x00,
	                                   null, null, "sam".getBytes(), null);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {
            assertEquals("TYPE_UNCHANGED requires 0 type length",
                         expected.getMessage());
        }
        // Try from network too
        try {
    	    out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // version + mb, me, cf (all clear)
    	    out.write( 0x00 ); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    ByteUtils.int2beb(3, out, 2); // type length: 3
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
    	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
    	    in = new ByteArrayInputStream(out.toByteArray());
    	    record = DIMERecord.createFromStream(in);
    	    fail("expected exception.");
        } catch(IOException expected) {
            assertEquals("TYPE_UNCHANGED requires 0 type length",
                         expected.getMessage());
        }
        
        
        // B - TYPE_MEDIA_TYPE
        // Valid always.
        record = new DIMERecord((byte)(0x01 << 4),
                                   null, null, null, null);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( 0x01 << 4 ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    // try with some type data too.
        record = new DIMERecord((byte)(0x01 << 4),
                                   null, null, "sam".getBytes(), null);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
	    assertEquals(3, record.getType().length);
	    assertEquals("sam", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( 0x01 << 4 ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    ByteUtils.int2beb(3, out, 2);
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type data + padding.
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
	    assertEquals(3, record.getType().length);
	    assertEquals("sam", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    // C - Absolute URI, valid always.
        record = new DIMERecord((byte)(0x02 << 4),
                                   null, null, null, null);
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( 0x02 << 4 ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    // try with some type data too.
        record = new DIMERecord((byte)(0x02 << 4),
                                   null, null, "sam".getBytes(), null);
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, record.getTypeId());
	    assertEquals(3, record.getType().length);
	    assertEquals("sam", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( 0x02 << 4 ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    ByteUtils.int2beb(3, out, 2);
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type data + padding.
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, record.getTypeId());
	    assertEquals(3, record.getType().length);
	    assertEquals("sam", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    // D - TYPE_UNKNOWN.
	    // Valid only if the 'type' data is 0 length.
	    record = new DIMERecord((byte)(0x03 << 4),
	                               null, null, null, null);
        assertEquals(DIMERecord.TYPE_UNKNOWN, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());

	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( 0x03 << 4 ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_UNKNOWN, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());

	    
	    // Now try a bad case -- TYPE_UNKNOWN with a type passed to it.
	    try {
	        record = new DIMERecord((byte)(0x03 << 4),
	                                   null, null, "sam".getBytes(), null);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {
            assertEquals("TYPE_UNKNOWN requires 0 type length",
                         expected.getMessage());
        }
        // Try from network too
        try {
    	    out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // version + mb, me, cf (all clear)
    	    out.write( 0x03 << 4); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    ByteUtils.int2beb(3, out, 2); // type length: 3
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
    	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
    	    in = new ByteArrayInputStream(out.toByteArray());
    	    record = DIMERecord.createFromStream(in);
    	    fail("expected exception.");
        } catch(IOException expected) {
            assertEquals("TYPE_UNKNOWN requires 0 type length",
                         expected.getMessage());
        }	    
	    
	    
	    // E - TYPE_NONE
	    // Requires no data & no type.
	    record = new DIMERecord((byte)(0x04 << 4),
	                               null, null, null, null);
        assertEquals(DIMERecord.TYPE_NONE, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());

	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( 0x04 << 4 ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_NONE, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());

	    
	    // Now try a bad case -- TYPE_NONE with a type passed to it.
	    try {
	        record = new DIMERecord((byte)(0x04 << 4),
	                                   null, null, "sam".getBytes(), null);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length",
                         expected.getMessage());
        }
        // Try from network too
        try {
    	    out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // version + mb, me, cf (all clear)
    	    out.write( 0x04 << 4 ); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    ByteUtils.int2beb(3, out, 2); // type length: 3
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
    	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
    	    in = new ByteArrayInputStream(out.toByteArray());
    	    record = DIMERecord.createFromStream(in);
    	    fail("expected exception.");
        } catch(IOException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length",
                         expected.getMessage());
        }
        
        // Try another bad case -- TYPE_NONE with data passed to it.
	    try {
	        record = new DIMERecord((byte)(0x04 << 4),
	                                   null, null, null, "samuel".getBytes());
            fail("expected exception");
        } catch(IllegalArgumentException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length",
                         expected.getMessage());
        }
        // Try from network too
        try {
    	    out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // version + mb, me, cf (all clear)
    	    out.write( 0x04 << 4 ); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    out.write( new byte[] { 0, 0 } ); // type length: 0
    	    ByteUtils.int2beb(6, out, 4); // data length: 6
    	    //data + padding
    	    out.write( new byte[] { 's', 'a', 'm', 'u', 'e', 'l', 0, 0 } );
    	    in = new ByteArrayInputStream(out.toByteArray());
    	    record = DIMERecord.createFromStream(in);
    	    fail("expected exception.");
        } catch(IOException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length",
                         expected.getMessage());
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
	        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	        try {
    	        DIMERecord.createFromStream(in);
	            fail("expected exception");
            } catch(IOException expected) {
                assertEquals("invalid type: " + i, expected.getMessage());
            }
            
            // try also creating not from network.
            try {
                new DIMERecord((byte)type, null, null, null, null);
                fail("expected exception");
            } catch(IllegalArgumentException expected) {
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
    	    out.write( new byte[] { 0, 0 } ); // type length: 0
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	        try {
    	        DIMERecord.createFromStream(in);
	            fail("expected exception");
            } catch(IOException expected) {
                assertEquals("invalid version: " + i, expected.getMessage());
            }
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
    	    out.write( new byte[] { 0, 0 } ); // type length: 0
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	        try {
    	        DIMERecord.createFromStream(in);
	            fail("expected exception");
            } catch(IOException expected) {
                assertEquals("invalid reserved: " + i, expected.getMessage());
            }
        }
    }
    
    public void testDataTooLarge() throws Exception {
        ByteArrayOutputStream out;
        ByteArrayInputStream in;
        
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // mb, me & cf clear.
	    out.write( 0x00 ); // // no type + reserved
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { (byte)0xFF, (byte)0xFF, 
	                            (byte)0xFF, (byte)0xFF } ); // data length: LARGE
        in = new ByteArrayInputStream(out.toByteArray());   
        try {
            DIMERecord.createFromStream(in);
            fail("expected exception");
        } catch(IOException expected) {
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
        in = new ByteArrayInputStream(out.toByteArray());   
        try {
            DIMERecord.createFromStream(in);
            fail("expected exception");
        } catch(IOException expected) {
            assertEquals("data too big.", expected.getMessage());
        }
        
        // can't really test Integer.MAX_VALUE 'cause it's too big to
        // store.
    }     
}