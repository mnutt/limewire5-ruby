package org.limewire.http;

import junit.framework.TestCase;

import org.apache.http.message.BasicHeader;
import org.limewire.http.RangeHeaderInterceptor.Range;

public class RangeHeaderInterceptorTest extends TestCase {

    public void testProcessSingleRange() throws Exception {
        RangeHeaderInterceptor interceptor = new RangeHeaderInterceptor();
        interceptor.process(new BasicHeader("Range", "bytes=1-"), null);
        assertTrue(interceptor.hasRequestedRanges());
        Range[] ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(1, ranges.length);
        assertEquals(1, ranges[0].getStartOffset(100));
        assertEquals(99, ranges[0].getEndOffset(100));

        interceptor = new RangeHeaderInterceptor();
        interceptor.process(new BasicHeader("Range", "bytes=  1  -   1"), null);
        ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(1, ranges.length);
        assertEquals(1, ranges[0].getStartOffset(100));
        assertEquals(1, ranges[0].getEndOffset(100));

        interceptor = new RangeHeaderInterceptor();
        interceptor.process(new BasicHeader("Range", "bytes=    -   2"), null);
        ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(1, ranges.length);
        assertEquals(98, ranges[0].getStartOffset(100));
        assertEquals(99, ranges[0].getEndOffset(100));
        
        interceptor = new RangeHeaderInterceptor();
        interceptor.process(new BasicHeader("Range", "bytes  4  -   4"), null);
        ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(1, ranges.length);
        assertEquals(4, ranges[0].getStartOffset(100));
        assertEquals(4, ranges[0].getEndOffset(100));
    }

    public void testProcessMultipleRanges() throws Exception {
        RangeHeaderInterceptor interceptor = new RangeHeaderInterceptor();
        interceptor.process(new BasicHeader("Range", "bytes=1-,2-5"), null);
        Range[] ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(2, ranges.length);
        assertEquals(1, ranges[0].getStartOffset(100));
        assertEquals(99, ranges[0].getEndOffset(100));
        assertEquals(2, ranges[1].getStartOffset(100));
        assertEquals(5, ranges[1].getEndOffset(100));

        interceptor = new RangeHeaderInterceptor();
        interceptor.process(new BasicHeader("Range", "bytes=1-1,1-1,-4"), null);
        ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(3, ranges.length);
        assertEquals(1, ranges[0].getStartOffset(100));
        assertEquals(1, ranges[0].getEndOffset(100));
        assertEquals(1, ranges[1].getStartOffset(100));
        assertEquals(1, ranges[1].getEndOffset(100));
        assertEquals(96, ranges[2].getStartOffset(100));
        assertEquals(99, ranges[2].getEndOffset(100));
    }

    public void testProcessMultipleHeaders() throws Exception {
        RangeHeaderInterceptor interceptor = new RangeHeaderInterceptor();
        interceptor.process(new BasicHeader("Range", "bytes=1-"), null);
        Range[] ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(1, ranges.length);
        assertEquals(1, ranges[0].getStartOffset(100));
        assertEquals(99, ranges[0].getEndOffset(100));
        
        interceptor.process(new BasicHeader("Range", "bytes=2-4,4-5"), null);
        ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(3, ranges.length);
        assertEquals(1, ranges[0].getStartOffset(100));
        assertEquals(99, ranges[0].getEndOffset(100));
        assertEquals(2, ranges[1].getStartOffset(100));
        assertEquals(4, ranges[1].getEndOffset(100));
        assertEquals(4, ranges[2].getStartOffset(100));
        assertEquals(5, ranges[2].getEndOffset(100));
    }

    public void testProcessBigValues() throws Exception {
        RangeHeaderInterceptor interceptor = new RangeHeaderInterceptor();
        interceptor.process(new BasicHeader("Range", "bytes=-" + Long.MAX_VALUE), null);
        Range[] ranges = interceptor.getRequestedRanges();
        assertNotNull(ranges);
        assertEquals(1, ranges.length);
        assertEquals(0, ranges[0].getStartOffset(100));
        assertEquals(99, ranges[0].getEndOffset(100));
        assertEquals(0, ranges[0].getStartOffset(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE - 1, ranges[0].getEndOffset(Long.MAX_VALUE));
    }

    public void testProcessNegativeOffsets() throws Exception {
        RangeHeaderInterceptor interceptor = new RangeHeaderInterceptor();
        try {
            interceptor.process(new BasicHeader("Range", "bytes=--1"), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }       
        try {
            interceptor.process(new BasicHeader("Range", "bytes=1--1"), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
        try {
            interceptor.process(new BasicHeader("Range", "bytes=-1-1"), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
        try {
            interceptor.process(new BasicHeader("Range", "bytes=1-1-"), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
    }
    
    public void testProcessInvalidFormat() throws Exception {
        RangeHeaderInterceptor interceptor = new RangeHeaderInterceptor();
        try {
            interceptor.process(new BasicHeader("Range", "bytes=1-abc"), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
        try {
            interceptor.process(new BasicHeader("Range", "bytes=abc"), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
        try {
            interceptor.process(new BasicHeader("Range", "bytes=2-1"), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
        try {
            interceptor.process(new BasicHeader("Range", "bytes="), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
        try {
            interceptor.process(new BasicHeader("Range", "bytes "), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
        try {
            interceptor.process(new BasicHeader("Range", "abc"), null);
            fail("Expected MalformedHeaderException");
        } catch (MalformedHeaderException expected) {
        }
    }

    public void testGetRequestedRanges() {
        RangeHeaderInterceptor interceptor = new RangeHeaderInterceptor();
        assertNull(interceptor.getRequestedRanges());
        assertFalse(interceptor.hasRequestedRanges());
    }

    public void testStartEndOffset() {
        // last 100 bytes
        Range range = new RangeHeaderInterceptor.Range(-1, 100);
        assertEquals(0, range.getStartOffset(100));
        assertEquals(99, range.getEndOffset(100));
        assertEquals(1, range.getStartOffset(101));
        assertEquals(100, range.getEndOffset(101));
        assertEquals(0, range.getStartOffset(99));
        assertEquals(98, range.getEndOffset(99));

        // first 100 bytes
        range = new RangeHeaderInterceptor.Range(0, 99);
        assertEquals(0, range.getStartOffset(100));
        assertEquals(99, range.getEndOffset(100));
        assertEquals(0, range.getStartOffset(101));
        assertEquals(99, range.getEndOffset(101));
        assertEquals(99, range.getEndOffset(1000));
        assertEquals(0, range.getStartOffset(99));
        assertEquals(98, range.getEndOffset(99));

        // byte 4-8
        range = new RangeHeaderInterceptor.Range(4, 8);
        assertEquals(4, range.getStartOffset(100));
        assertEquals(8, range.getEndOffset(100));
        assertEquals(4, range.getStartOffset(9));
        assertEquals(8, range.getEndOffset(9));
        assertEquals(4, range.getStartOffset(8));
        assertEquals(7, range.getEndOffset(8));
        assertEquals(-1, range.getStartOffset(4));
        assertEquals(-1, range.getStartOffset(1));
        assertEquals(-1, range.getStartOffset(0));
        
        // byte 0
        range = new RangeHeaderInterceptor.Range(0, 0);
        assertEquals(0, range.getStartOffset(100));
        assertEquals(0, range.getEndOffset(100));
        assertEquals(-1, range.getStartOffset(0));
        assertEquals(0, range.getStartOffset(1));
        assertEquals(0, range.getEndOffset(1));
    }
    
}
