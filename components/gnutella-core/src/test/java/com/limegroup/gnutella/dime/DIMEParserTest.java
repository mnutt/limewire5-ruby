package com.limegroup.gnutella.dime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.limewire.util.StringUtils;

import junit.framework.Test;


/**
 * Tests for DIMEParser.
 */
public final class DIMEParserTest extends org.limewire.gnutella.tests.LimeTestCase {

	/**
	 * Constructs a new test instance for responses.
	 */
	public DIMEParserTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DIMEParserTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testNext() throws Exception {
	    ByteArrayInputStream in;
	    ByteArrayOutputStream out;
	    Iterator parser;
	    DIMERecord one, two;
	    DIMERecord readOne;
	    
	    // test an empty stream.
	    in = new ByteArrayInputStream(new byte[0]);
	    parser = new DIMEParser(in);
	    try {
	        parser.next();
	        fail("expected exception");
        } catch(NoSuchElementException nsee) {
            assertEquals("eof", nsee.getMessage());
        }
        
        // test a stream that didn't have a 'first' message.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, null);
        one.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        try {
            parser.next();
            fail("expected exception");
        } catch(NoSuchElementException nsee) {
            assertEquals("middle of stream.", nsee.getMessage());
        }
        
        // test we can read a single message.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sab"));
        one.setFirstRecord(true);
        one.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        Object next = parser.next();
        assertInstanceof(DIMERecord.class, next);
        readOne = (DIMERecord)next;
        assertEquals(StringUtils.toAsciiBytes("sab"), readOne.getData());
        try {
            parser.next();
            fail("expected exception");
        } catch(NoSuchElementException nsee) {
            assertEquals("eof", nsee.getMessage());
        }
        
        // test that once we get a message with ME we stop.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sam"));
        one.setFirstRecord(true);
        one.setLastRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("bad"));
        one.write(out);
        two.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        next = parser.next();
        assertInstanceof(DIMERecord.class, next);
        readOne = (DIMERecord)next;
        assertEquals(StringUtils.toAsciiBytes("sam"), readOne.getData());
        try {
            parser.next();
            fail("expected exception");
        } catch(NoSuchElementException nsee) {
            assertEquals("already read last message.", nsee.getMessage());
        }
        
        // test multiple 'first' messages fail.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sab"));
        one.setFirstRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("bas"));
        two.setFirstRecord(true);
        one.write(out);
        two.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        next = parser.next();
        assertInstanceof(DIMERecord.class, next);
        readOne = (DIMERecord)next;
        assertEquals(StringUtils.toAsciiBytes("sab"), readOne.getData());
        try {
            parser.next();
            fail("expected exception");
        } catch(NoSuchElementException nsee) {
            assertEquals("two first records.", nsee.getMessage());
        }
    }
    
    public void testNextRecord() throws Exception {
	    ByteArrayInputStream in;
	    ByteArrayOutputStream out;
	    DIMEParser parser;
	    DIMERecord one, two;
	    DIMERecord readOne;
	    
	    // test an empty stream.
	    in = new ByteArrayInputStream(new byte[0]);
	    parser = new DIMEParser(in);
	    try {
	        parser.nextRecord();
	        fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("eof", nsee.getMessage());
        }
        
        // test a stream that didn't have a 'first' message.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, null);
        one.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        try {
            parser.nextRecord();
            fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("middle of stream.", nsee.getMessage());
        }
        
        // test we can read a single message.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sab"));
        one.setFirstRecord(true);
        one.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        readOne = parser.nextRecord();
        assertEquals(StringUtils.toAsciiBytes("sab"), readOne.getData());
        try {
            parser.nextRecord();
            fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("eof", nsee.getMessage());
        }
        
        // test that once we get a message with ME we stop.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sam"));
        one.setFirstRecord(true);
        one.setLastRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("bad"));
        one.write(out);
        two.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        readOne = parser.nextRecord();
        assertEquals(StringUtils.toAsciiBytes("sam"), readOne.getData());
        try {
            parser.nextRecord();
            fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("already read last message.", nsee.getMessage());
        }
        
        // test multiple 'first' messages fail.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sab"));
        one.setFirstRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("bas"));
        two.setFirstRecord(true);
        one.write(out);
        two.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        readOne = parser.nextRecord();
        assertEquals(StringUtils.toAsciiBytes("sab"), readOne.getData());
        try {
            parser.nextRecord();
            fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("two first records.", nsee.getMessage());
        }
    }
    
    public void testGetRecords() throws Exception {
	    ByteArrayInputStream in;
	    ByteArrayOutputStream out;
	    DIMEParser parser;
	    DIMERecord one, two;
	    DIMERecord readOne;
	    List readList;
	    
	    // test an empty stream.
	    in = new ByteArrayInputStream(new byte[0]);
	    parser = new DIMEParser(in);
	    try {
	        parser.nextRecord();
	        fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("eof", nsee.getMessage());
        }
        
        // test a stream that didn't have a 'first' message.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
                                null, null, null, null);
        one.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        try {
            parser.nextRecord();
            fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("middle of stream.", nsee.getMessage());
        }
        
        // test multiple 'first' messages fail.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sab"));
        one.setFirstRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("bas"));
        two.setFirstRecord(true);
        one.write(out);
        two.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        try {
            parser.getRecords();
            fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("two first records.", nsee.getMessage());
        }        
        
        // test the simple correct case with one message (first & last)
        // test we can read a single message.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sab"));
        one.setFirstRecord(true);
        one.setLastRecord(true);
        one.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        readList = parser.getRecords();
        assertEquals(1, readList.size());
        readOne = (DIMERecord)readList.get(0);
        assertEquals(StringUtils.toAsciiBytes("sab"), readOne.getData());
        // Make sure the next getRecords is empty.
        readList = parser.getRecords();
        assertEquals(0, readList.size());
        
        // test that we ignore further messages after the last message
        // is read.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("bas"));
        one.setFirstRecord(true);
        one.setLastRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sab"));
        one.write(out);
        two.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        readList = parser.getRecords();
        assertEquals(1, readList.size());
        readOne = (DIMERecord)readList.get(0);
        assertEquals(StringUtils.toAsciiBytes("bas"), readOne.getData());
        // Make sure the next getRecords is empty.
        readList = parser.getRecords();
        assertEquals(0, readList.size());
        
        // test that a stream without an end fails.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("sab"));
        one.setFirstRecord(true);
        one.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        try {
            parser.getRecords();
            fail("expected exception");
        } catch(IOException nsee) {
            assertEquals("eof", nsee.getMessage());
        }
    }
    
    public void testHasNext() throws Exception {
        ByteArrayInputStream in;
	    ByteArrayOutputStream out;
	    DIMEParser parser;
	    DIMERecord one, two, three;
	    DIMERecord readOne, readTwo, readThree;
	    
        // write out a few messages.
        out = new ByteArrayOutputStream();
        one = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("one"));
        one.setFirstRecord(true);
        two = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							 null, null, null, StringUtils.toAsciiBytes("two"));
        three = new DIMERecord(DIMERecord.TYPE_UNCHANGED,
							   null, null, null, StringUtils.toAsciiBytes("three"));
        three.setLastRecord(true);
        one.write(out);
        two.write(out);
        three.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        parser = new DIMEParser(in);
        assertTrue(parser.hasNext());
        readOne = parser.nextRecord();
        assertEquals(StringUtils.toAsciiBytes("one"), readOne.getData());
        assertTrue(parser.hasNext());
        readTwo = parser.nextRecord();
        assertEquals(StringUtils.toAsciiBytes("two"), readTwo.getData());
        assertTrue(parser.hasNext());
        readThree = parser.nextRecord();
        assertEquals(StringUtils.toAsciiBytes("three"), readThree.getData());
        assertFalse(parser.hasNext());
        try {
            parser.nextRecord();
            fail("expected exception");
        } catch(IOException ioe) {
            assertEquals("already read last message.", ioe.getMessage());
        }
    }
    
    public void testRemove() throws Exception {
        Iterator p = new DIMEParser(new ByteArrayInputStream(new byte[0]));
        try {
            p.remove();
            fail("expected exception");
        } catch(UnsupportedOperationException expected) {}
    }
}