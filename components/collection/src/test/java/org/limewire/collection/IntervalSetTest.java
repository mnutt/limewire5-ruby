package org.limewire.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;

/**
 * Unit tests for <code>IntervalSet</code>.
 */
public class IntervalSetTest extends BaseTestCase {

    private static final Log LOG = LogFactory.getLog(IntervalSetTest.class);

    private IntervalSet iSet;

    private Interval interval;

    private List list;

    private Iterator iter;

    public IntervalSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IntervalSetTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setUp() {
        iSet = new IntervalSet();
    }

    public void testDecode() throws Exception {
        // file is [0,1023], all verified
        iSet.decode(1024, 1);
        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(0, iSet.getFirst().getLow());
        assertEquals(1023, iSet.getFirst().getHigh());

        // file is [0,1024], all verified
        iSet.clear();
        iSet.decode(1025, 1);
        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(0, iSet.getFirst().getLow());
        assertEquals(1024, iSet.getFirst().getHigh());

        // file is [0,1024], verified only [0-1023]
        iSet.clear();
        iSet.decode(1025, 2);
        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(0, iSet.getFirst().getLow());
        assertEquals(1023, iSet.getFirst().getHigh());

        // examples from the wiki page
        // http://www.limewire.org/wiki/index.php?title=HashTreeRangeEncoding

        // file is [0, 10 * 1024 -1], verified is [0, 4 * 1024 -1]
        iSet.clear();
        iSet.decode(10 * 1024, 4);
        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(0, iSet.getFirst().getLow());
        assertEquals(4 * 1024 - 1, iSet.getFirst().getHigh());

        // same file, range [4 * 1024, 10 * 1024 - 1]
        iSet.clear();
        iSet.decode(10 * 1024, 5, 12);

        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(4 * 1024, iSet.getFirst().getLow());
        assertEquals(10 * 1024 - 1, iSet.getFirst().getHigh());

        // 20k file, ranges [0,16k] and [19k,20k]
        iSet.clear();
        iSet.decode(20 * 1024, 2);
        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(0 * 1024, iSet.getFirst().getLow());
        assertEquals(16 * 1024 - 1, iSet.getFirst().getHigh());

        iSet.decode(20 * 1024, 51);
        assertEquals(2, iSet.getNumberOfIntervals());
        assertEquals(0 * 1024, iSet.getFirst().getLow());
        assertEquals(16 * 1024 - 1, iSet.getFirst().getHigh());
        assertEquals(19 * 1024, iSet.getLast().getLow());
        assertEquals(20 * 1024 - 1, iSet.getLast().getHigh());

        // add node 12 and we have the full file
        iSet.decode(20 * 1024, 12);
        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(0 * 1024, iSet.getFirst().getLow());
        assertEquals(20 * 1024 - 1, iSet.getFirst().getHigh());

    }

    public void testEncode() throws Exception {

        // file is [0,1023], all verified
        iSet.add(new Interval(0, 1023));
        Collection<Integer> encoded = iSet.encode(1024);
        assertEquals(1, encoded.size());
        assertTrue(encoded.contains(1));

        // file is [0,1024], verified only [0,1023]
        encoded = iSet.encode(1025);
        assertEquals(1, encoded.size());
        assertTrue(encoded.contains(2));

        // file is [0,1024] all verified
        iSet.add(new Interval(1024));
        encoded = iSet.encode(1025);
        assertEquals(1, encoded.size());
        assertTrue(encoded.toString(), encoded.contains(1));

        // empty file will be empty regardless of size
        iSet.clear();
        encoded = iSet.encode(1000);
        assertEquals(0, encoded.size());
        encoded = iSet.encode(2025);
        assertEquals(0, encoded.size());

        // examples from the wiki page
        // http://www.limewire.org/wiki/index.php?title=HashTreeRangeEncoding

        // file is [0, 10 * 1024 -1], verified is [0, 4 * 1024 -1]
        iSet.add(new Interval(0, 4 * 1024 - 1));
        encoded = iSet.encode(11 * 1024);
        assertEquals(encoded.toString(), 1, encoded.size());
        assertTrue(encoded.toString(), encoded.contains(4));

        // same file, range [4 * 1024, 10 * 1024 - 1]
        iSet.clear();
        iSet.add(new Interval(4 * 1024, 10 * 1024 - 1));
        encoded = iSet.encode(11 * 1024);
        assertEquals(encoded.toString(), 2, encoded.size());
        assertTrue(encoded.toString(), encoded.contains(5));
        assertTrue(encoded.toString(), encoded.contains(12));

        // 20k file, ranges [0,16k] and [19k,20k]
        iSet.clear();
        iSet.add(new Interval(0, 16 * 1024 - 1));
        iSet.add(new Interval(19 * 1024, 20 * 1024 - 1));
        encoded = iSet.encode(20 * 1024);
        assertEquals(encoded.toString(), 2, encoded.size());
        assertTrue(encoded.toString(), encoded.contains(2));
        assertTrue(encoded.toString(), encoded.contains(51));
    }

    public void testSmallRangesIgnored() throws Exception {
        iSet.add(new Interval(0, 100)); // small range in beginning
        iSet.add(new Interval(10 * 1024, 20 * 1024 - 1)); // large range
        iSet.add(new Interval(30 * 1024, 30 * 1024 + 99)); // small range at end

        Collection<Integer> encoded = iSet.encode(30 * 1024 + 100);

        IntervalSet decoded = new IntervalSet();
        for (int i : encoded)
            decoded.decode(30 * 1024 + 100, i);

        // small range at beginning is gone, the rest are there
        assertEquals(2, decoded.getNumberOfIntervals());
        assertTrue(decoded.contains(new Interval(10 * 1024, 20 * 1024 - 1)));
        assertTrue(decoded.contains(new Interval(30 * 1024, 30 * 1024 + 99)));

        // a non-allignable end range should be ignored too
        iSet.clear();
        iSet.add(new Interval(1025, 1199)); // too small to be alligned
        encoded = iSet.encode(1200);
        assertTrue(encoded.isEmpty());

        iSet.clear();
        iSet.add(new Interval(0, 1023));
        encoded = iSet.encode(1024);
        assertEquals(1, encoded.size());
        assertTrue(encoded.contains(1));

        iSet.clear();
        iSet.add(new Interval(0, 1022));
        encoded = iSet.encode(1024);
        assertEquals(0, encoded.size());
    }

    public void testRangesAlligned() throws Exception {
        iSet.add(new Interval(500, 2500));
        Collection<Integer> encoded = iSet.encode(3 * 1024);

        IntervalSet decoded = new IntervalSet();
        for (int i : encoded)
            decoded.decode(3 * 1024, i);

        // only one range - [1024, 2047]
        assertEquals(1, decoded.getNumberOfIntervals());
        assertEquals(1024, decoded.getFirst().getLow());
        assertEquals(2047, decoded.getFirst().getHigh());

        // repeat test with long values
        iSet.clear();
        long longBase = 0x1FFFFFFC00L;
        iSet.add(Range.createRange(longBase + 500, longBase + 2500));
        encoded = iSet.encode(longBase + 3 * 1024);

        decoded = new IntervalSet();
        for (int i : encoded)
            decoded.decode(longBase + 3 * 1024, i);

        assertEquals(1, decoded.getNumberOfIntervals());
        assertEquals(longBase + 1024, decoded.getFirst().getLow());
        assertEquals(longBase + 2047, decoded.getFirst().getHigh());

        // repeat with end range
        iSet.clear();
        iSet.add(Range.createRange(900, 1099));
        encoded = iSet.encode(1100);
        decoded = new IntervalSet();
        for (int i : encoded)
            decoded.decode(1100, i);
        assertEquals(1, decoded.getNumberOfIntervals());
        assertEquals(1024, decoded.getFirst().getLow());
        assertEquals(1099, decoded.getFirst().getHigh());

        // test LWC-1229
        // [low -- 1kb boundary -- high], high - low < 2048
        iSet.clear();
        iSet.add(Range.createRange(2, 1026));
        encoded = iSet.encode(2000);
        assertEquals(0, encoded.size());
    }

    // getNumberOfIntervals is used in pretty much every
    // test, so it should be tested first
    public void testGetNumberOfIntervals() {
        iSet = IntervalSet.createSingletonSet(0, 4);
        iSet.add(new Interval(17));
        iSet.add(new Interval(20));
        iSet.add(new Interval(28));
        iSet.add(new Interval(31, 32));
        // [0-4], [17-17], [20-20], [28-28], [31-32]
        assertEquals(5, iSet.getNumberOfIntervals());
        iSet = new IntervalSet();
        assertEquals(0, iSet.getNumberOfIntervals());
        iSet.add(new Interval(17, 192000));
        assertEquals(1, iSet.getNumberOfIntervals());
        iSet.delete(new Interval(129));
        assertEquals(2, iSet.getNumberOfIntervals());
    }

    public void testCreateSingletonSet() {
        iSet = IntervalSet.createSingletonSet(17, 28417);
        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(new Interval(17, 28417), iSet.getFirst());
    }

    public void testBasic() throws Exception {
        iSet = new IntervalSet();
        interval = new Interval(40, 45);
        iSet.add(interval);
        assertEquals("add method broken", 1, iSet.getNumberOfIntervals());
        assertEquals("getSize() broken", 6, iSet.getSize());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 0, interval.getLow());
        assertEquals("getNeededInterval broken", 39, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 46, interval.getLow());
        assertEquals("getNeededInterval broken", 99, interval.getHigh());
    }

    public void testLowerBoundaryColation() throws Exception {
        iSet.add(new Interval(40, 45));
        interval = new Interval(35, 39);
        iSet.add(interval);
        assertEquals("lower boundry colating failed", 1, iSet.getNumberOfIntervals());
        interval = getIntervalAt(0);
        assertEquals("lower boundry colating failed", 45, interval.getHigh());
        assertEquals("lower boundry colating failed", 35, interval.getLow());
        assertEquals("getSize() broken", 11, iSet.getSize());
    }

    public void testLowerOverlapColation() throws Exception {
        iSet.add(new Interval(35, 45));
        interval = new Interval(30, 37);
        iSet.add(interval);
        assertEquals("lower overlap colating failed", 1, iSet.getNumberOfIntervals());
        interval = getIntervalAt(0);
        assertEquals("lower boundry colating failed", 45, interval.getHigh());
        assertEquals("lower boundry colating failed", 30, interval.getLow());
    }

    public void testLowerNonOverlap() throws Exception {
        iSet.add(new Interval(30, 45));
        interval = new Interval(20, 25);
        iSet.add(interval);
        assertEquals("lower non-overlap add failed", 2, iSet.getNumberOfIntervals());
        assertEquals("getSize() broken", 22, iSet.getSize());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 0, interval.getLow());
        assertEquals("getNeededInterval broken", 19, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 26, interval.getLow());
        assertEquals("getNeededInterval broken", 29, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 46, interval.getLow());
        assertEquals("getNeededInterval broken", 99, interval.getHigh());
    }

    public void testUpperNonOverlap() throws Exception {
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        interval = new Interval(50, 60);
        iSet.add(interval);
        assertEquals("upper non-overlap add failed", 3, iSet.getNumberOfIntervals());
        assertEquals("getSize() broken", 33, iSet.getSize());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 0, interval.getLow());
        assertEquals("getNeededInterval broken", 19, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 26, interval.getLow());
        assertEquals("getNeededInterval broken", 29, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 46, interval.getLow());
        assertEquals("getNeededInterval broken", 49, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 61, interval.getLow());
        assertEquals("getNeededInterval broken", 99, interval.getHigh());
    }

    public void testGetOverlapIntervalsMidRange() throws Exception {
        // ////////////////getOverlapIntervals tests//////////
        // Note: Test all the cases for getOverlapIntervals, before continuing
        // Case a
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(23, 32);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 2, list.size());
        interval = (Interval) list.get(0);// first interval
        assertEquals("getOverlapIntervals broken", 23, interval.getLow());
        assertEquals("getOverlapIntervals broken", 25, interval.getHigh());
        interval = (Interval) list.get(1);
        assertEquals("getOverlapIntervals broken", 30, interval.getLow());
        assertEquals("getOverlapIntervals broken", 32, interval.getHigh());
    }

    public void testGetOverlapIntervalsBoundary() throws Exception {
        // Case a.1
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(25, 30);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 2, list.size());
        interval = (Interval) list.get(0);// first interval
        assertEquals("getOverlapIntervals broken", 25, interval.getLow());
        assertEquals("getOverlapIntervals broken", 25, interval.getHigh());
        interval = (Interval) list.get(1);
        assertEquals("getOverlapIntervals broken", 30, interval.getLow());
        assertEquals("getOverlapIntervals broken", 30, interval.getHigh());
    }

    public void testGetOverlapIntervalsLowRange() throws Exception {
        // Case b
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(16, 23);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 1, list.size());
        interval = (Interval) list.get(0);
        assertEquals("getOverlapIntervals broken", 20, interval.getLow());
        assertEquals("getOverlapIntervals broken", 23, interval.getHigh());
    }

    public void testGetOverlapIntervalsLowBoundary() throws Exception {
        // case b.1
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(16, 20);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 1, list.size());
        interval = (Interval) list.get(0);
        assertEquals("getOverlapIntervals broken", 20, interval.getLow());
        assertEquals("getOverlapIntervals broken", 20, interval.getHigh());
    }

    public void testGetOverlapIntervalsHighRange() throws Exception {
        // case c
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(23, 29);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 1, list.size());
        interval = (Interval) list.get(0);
        assertEquals("getOverlapIntervals broken", 23, interval.getLow());
        assertEquals("getOverlapIntervals broken", 25, interval.getHigh());
    }

    public void testGetOverlapIntervalsHighBoundary() throws Exception {
        // case c.1
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(25, 29);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 1, list.size());
        interval = (Interval) list.get(0);
        assertEquals("getOverlapIntervals broken", 25, interval.getLow());
        assertEquals("getOverlapIntervals broken", 25, interval.getHigh());
    }

    public void testGetOverlapIntervalsNoRange() throws Exception {
        // case d
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(13, 19);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("reported false overlap", 0, list.size());
        // case e
        interval = new Interval(26, 29);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("reported false overlap", 0, list.size());
    }

    public void testGetOverlapIntervalsMidAllRanges() throws Exception {
        // case f
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(23, 53);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 3, list.size());
        interval = (Interval) list.get(0);
        assertEquals(23, interval.getLow());
        assertEquals(25, interval.getHigh());
        interval = (Interval) list.get(1);
        assertEquals(30, interval.getLow());
        assertEquals(45, interval.getHigh());
        interval = (Interval) list.get(2);
        assertEquals(50, interval.getLow());
        assertEquals(53, interval.getHigh());
    }

    public void testGetOverlapIntervalsAllRanges() throws Exception {
        // case g
        iSet.add(new Interval(20, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(16, 65);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 3, list.size());
        interval = (Interval) list.get(0);
        assertEquals(20, interval.getLow());
        assertEquals(25, interval.getHigh());
        interval = (Interval) list.get(1);
        assertEquals(30, interval.getLow());
        assertEquals(45, interval.getHigh());
        interval = (Interval) list.get(2);
        assertEquals(50, interval.getLow());
        assertEquals(60, interval.getHigh());
    }

    public void testAddHighColation() throws Exception {
        // /OK, back to testing add.
        iSet = IntervalSet.createSingletonSet(20, 25);
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 60));
        interval = new Interval(54, 70);
        iSet.add(interval);
        assertEquals(3, iSet.getNumberOfIntervals());
        interval = getIntervalAt(2);
        assertEquals(50, interval.getLow());
        assertEquals(70, interval.getHigh());
        assertEquals("getSize() broken", 43, iSet.getSize());
    }

    public void testAddHighBoundaryColation() throws Exception {
        iSet = IntervalSet.createSingletonSet(20, 25);
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 70));
        interval = new Interval(71, 75);
        iSet.add(interval);
        assertEquals(3, iSet.getNumberOfIntervals());
        interval = getIntervalAt(2);
        assertEquals(50, interval.getLow());
        assertEquals(75, interval.getHigh());
        assertEquals("getSize() broken", 48, iSet.getSize());
        // test some boundry conditions [20-25],[30-45],[50-75]
        interval = new Interval(75, 80);
        iSet.add(interval);
        assertEquals(3, iSet.getNumberOfIntervals());
        interval = getIntervalAt(2);
        assertEquals(50, interval.getLow());
        assertEquals(80, interval.getHigh());
    }

    public void testAddLowBoundaryColation() {
        iSet = IntervalSet.createSingletonSet(20, 25);
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 80));
        interval = new Interval(15, 20);
        iSet.add(interval);
        assertEquals(3, iSet.getNumberOfIntervals());
        interval = getIntervalAt(0);
        assertEquals(15, interval.getLow());
        assertEquals(25, interval.getHigh());
        assertEquals("getSize() broken", 58, iSet.getSize());
    }

    public void testGetNeededIntervals() {
        iSet = IntervalSet.createSingletonSet(15, 25);
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 80));
        iter = iSet.getNeededIntervals(100);
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 0, interval.getLow());
        assertEquals("getNeededInterval broken", 14, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 26, interval.getLow());
        assertEquals("getNeededInterval broken", 29, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 46, interval.getLow());
        assertEquals("getNeededInterval broken", 49, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 81, interval.getLow());
        assertEquals("getNeededInterval broken", 99, interval.getHigh());
    }

    public void testAddOverlappingInterval() {
        iSet.add(new Interval(15, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(50, 80));
        interval = new Interval(49, 81);
        iSet.add(interval);
        assertEquals(3, iSet.getNumberOfIntervals());
        interval = getIntervalAt(2);
        assertEquals(49, interval.getLow());
        assertEquals(81, interval.getHigh());
        assertEquals("getSize() broken", 60, iSet.getSize());
    }

    public void testAddInternalInterval() {
        iSet.add(new Interval(15, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(49, 81));
        interval = new Interval(55, 60);
        iSet.add(interval);
        assertEquals(3, iSet.getNumberOfIntervals());
        interval = getIntervalAt(2);
        assertEquals(49, interval.getLow());
        assertEquals(81, interval.getHigh());
    }

    public void testAddFullBetweenInterval() {
        iSet.add(new Interval(15, 25));
        iSet.add(new Interval(30, 45));
        iSet.add(new Interval(49, 81));
        interval = new Interval(26, 29);
        iSet.add(interval);
        assertEquals(2, iSet.getNumberOfIntervals());
        interval = getIntervalAt(0);
        assertEquals(15, interval.getLow());
        assertEquals(45, interval.getHigh());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 0, interval.getLow());
        assertEquals("getNeededInterval broken", 14, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 46, interval.getLow());
        assertEquals("getNeededInterval broken", 48, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 82, interval.getLow());
        assertEquals("getNeededInterval broken", 99, interval.getHigh());
    }

    public void testGetNeededIntervalsAgain() {
        iSet.add(new Interval(15, 45));
        iSet.add(new Interval(49, 81));
        interval = new Interval(3, 5);
        iSet.add(interval);
        interval = new Interval(7, 9);
        iSet.add(interval);
        iter = iSet.getNeededIntervals(100);
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 0, interval.getLow());
        assertEquals("getNeededInterval broken", 2, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 6, interval.getLow());
        assertEquals("getNeededInterval broken", 6, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 10, interval.getLow());
        assertEquals("getNeededInterval broken", 14, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 46, interval.getLow());
        assertEquals("getNeededInterval broken", 48, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 82, interval.getLow());
        assertEquals("getNeededInterval broken", 99, interval.getHigh());
    }

    public void testAddFullLowCoveringInterval() {
        iSet.add(new Interval(3, 5));
        iSet.add(new Interval(7, 9));
        iSet.add(new Interval(15, 45));
        iSet.add(new Interval(49, 81));
        assertEquals(4, iSet.getNumberOfIntervals());
        interval = new Interval(2, 17);
        iSet.add(interval);
        assertEquals(2, iSet.getNumberOfIntervals());
        interval = getIntervalAt(0);
        assertEquals(2, interval.getLow());
        assertEquals(45, interval.getHigh());
        assertEquals("getSize() broken", 77, iSet.getSize());
    }

    public void testAddCoverFromMiddle() {
        iSet.add(new Interval(2, 45));
        iSet.add(new Interval(49, 81));
        interval = new Interval(40, 50);
        iSet.add(interval);
        assertEquals(1, iSet.getNumberOfIntervals());
        // {[2-81]}
        interval = getIntervalAt(0);
        assertEquals(2, interval.getLow());
        assertEquals(81, interval.getHigh());
        assertEquals("getSize() broken", 80, iSet.getSize());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 0, interval.getLow());
        assertEquals("getNeededInterval broken", 1, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals("getNeededInterval broken", 82, interval.getLow());
        assertEquals("getNeededInterval broken", 99, interval.getHigh());
    }

    public void testGetNeededWithNoneAndSome() {
        iSet.clear();
        iSet.add(new Interval(0, 5));
        iter = iSet.getNeededIntervals(6);
        assertTrue(!iter.hasNext());
        iSet.add(new Interval(6, 10));
        iter = iSet.getNeededIntervals(20);
        interval = (Interval) iter.next();
        assertEquals(11, interval.getLow());
        assertEquals(19, interval.getHigh());
        assertTrue(!iter.hasNext());
    }

    public void testSetUpForDelete() {
        // test delete() method
        iSet.clear();
        iSet.add(new Interval(0, 4));
        iSet.add(new Interval(8, 12));
        iSet.add(new Interval(16, 20));
        iSet.add(new Interval(24));
        iSet.add(new Interval(28, 32));
        iSet.add(new Interval(36, 40));
        // [0-4], [8-12], [16-20], [24-24], [28-32], [36-40]
        assertEquals(6, iSet.getNumberOfIntervals());
        iter = iSet.getAllIntervals();
        interval = (Interval) iter.next();
        assertEquals(0, interval.getLow());
        assertEquals(4, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(8, interval.getLow());
        assertEquals(12, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(16, interval.getLow());
        assertEquals(20, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(24, interval.getLow());
        assertEquals(24, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(28, interval.getLow());
        assertEquals(32, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(36, interval.getLow());
        assertEquals(40, interval.getHigh());
    }

    public void testDeleteNothing() {
        iSet.add(new Interval(0, 4));
        iSet.add(new Interval(8, 12));
        iSet.add(new Interval(16, 20));
        iSet.add(new Interval(24, 24));
        iSet.add(new Interval(28, 32));
        iSet.add(new Interval(36, 40));
        iSet.delete(new Interval(5, 7));
        assertEquals(6, iSet.getNumberOfIntervals());
        iter = iSet.getAllIntervals();
        interval = (Interval) iter.next();
        assertEquals(0, interval.getLow());
        assertEquals(4, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(8, interval.getLow());
        assertEquals(12, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(16, interval.getLow());
        assertEquals(20, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(24, interval.getLow());
        assertEquals(24, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(28, interval.getLow());
        assertEquals(32, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(36, interval.getLow());
        assertEquals(40, interval.getHigh());
    }

    public void testDeleteBoundaries() {
        iSet.add(new Interval(0, 4));
        iSet.add(new Interval(8, 12));
        iSet.add(new Interval(16, 20));
        iSet.add(new Interval(24, 24));
        iSet.add(new Interval(28, 32));
        iSet.add(new Interval(36, 40));
        iSet.delete(new Interval(12, 16));
        assertEquals(6, iSet.getNumberOfIntervals());
        iter = iSet.getAllIntervals();
        interval = (Interval) iter.next();
        assertEquals(0, interval.getLow());
        assertEquals(4, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(8, interval.getLow());
        assertEquals(11, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(17, interval.getLow());
        assertEquals(20, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(24, interval.getLow());
        assertEquals(24, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(28, interval.getLow());
        assertEquals(32, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(36, interval.getLow());
        assertEquals(40, interval.getHigh());
    }

    public void testDeleteFullInterval() {
        iSet.add(new Interval(0, 4));
        iSet.add(new Interval(8, 11));
        iSet.add(new Interval(17, 20));
        iSet.add(new Interval(24, 24));
        iSet.add(new Interval(28, 32));
        iSet.add(new Interval(36, 40));
        iSet.delete(new Interval(24, 25));
        assertEquals(5, iSet.getNumberOfIntervals());
        iter = iSet.getAllIntervals();
        interval = (Interval) iter.next();
        assertEquals(0, interval.getLow());
        assertEquals(4, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(8, interval.getLow());
        assertEquals(11, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(17, interval.getLow());
        assertEquals(20, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(28, interval.getLow());
        assertEquals(32, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(36, interval.getLow());
        assertEquals(40, interval.getHigh());
    }

    public void testDeleteSplitsIntoTwo() {
        iSet.add(new Interval(0, 4));
        iSet.add(new Interval(8, 11));
        iSet.add(new Interval(17, 20));
        iSet.add(new Interval(28, 32));
        iSet.add(new Interval(36, 40));
        iSet.delete(new Interval(29, 30));
        assertEquals(6, iSet.getNumberOfIntervals());
        iter = iSet.getAllIntervals();
        interval = (Interval) iter.next();
        assertEquals(0, interval.getLow());
        assertEquals(4, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(8, interval.getLow());
        assertEquals(11, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(17, interval.getLow());
        assertEquals(20, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(28, interval.getLow());
        assertEquals(28, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(31, interval.getLow());
        assertEquals(32, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(36, interval.getLow());
        assertEquals(40, interval.getHigh());
    }

    public void testDeleteFullUpper() {
        iSet.add(new Interval(0, 4));
        iSet.add(new Interval(8, 11));
        iSet.add(new Interval(17, 20));
        iSet.add(new Interval(28, 28));
        iSet.add(new Interval(31, 32));
        iSet.add(new Interval(36, 40));
        iSet.delete(new Interval(35, 41));
        assertEquals(5, iSet.getNumberOfIntervals());
        iter = iSet.getAllIntervals();
        interval = (Interval) iter.next();
        assertEquals(0, interval.getLow());
        assertEquals(4, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(8, interval.getLow());
        assertEquals(11, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(17, interval.getLow());
        assertEquals(20, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(28, interval.getLow());
        assertEquals(28, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(31, interval.getLow());
        assertEquals(32, interval.getHigh());
    }

    public void testDeleteIntervalSet() {
        iSet.add(new Interval(0, 4));
        iSet.add(new Interval(8, 11));
        iSet.add(new Interval(17, 20));
        iSet.add(new Interval(28, 28));
        iSet.add(new Interval(31, 32));
        IntervalSet toDelete = new IntervalSet();
        toDelete.add(new Interval(8, 15));
        toDelete.add(new Interval(18, 19));
        iSet.delete(toDelete);
        assertEquals(5, iSet.getNumberOfIntervals());
        iter = iSet.getAllIntervals();
        interval = (Interval) iter.next();
        assertEquals(0, interval.getLow());
        assertEquals(4, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(17, interval.getLow());
        assertEquals(17, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(20, interval.getLow());
        assertEquals(20, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(28, interval.getLow());
        assertEquals(28, interval.getHigh());
        interval = (Interval) iter.next();
        assertEquals(31, interval.getLow());
        assertEquals(32, interval.getHigh());
    }

    public void testDeleteLarge() throws Exception {
        // to test the narrowRange method we need a min number of intervals
        for (int i = 0; i < IntervalSet.LINEAR * 2 + 4; i += 2)
            iSet.add(Range.createRange(0x1L << i, (0x1L << (i + 1)) - 1));
        assertGreaterThan(IntervalSet.LINEAR, iSet.getNumberOfIntervals());
        iSet.delete(new Interval(20, 80));
        List<Range> listr = iSet.getAllIntervalsAsList();
        assertEquals(1, listr.get(0).getLow());
        assertEquals(1, listr.get(0).getHigh());
        assertEquals(4, listr.get(1).getLow());
        assertEquals(7, listr.get(1).getHigh());
        assertEquals(16, listr.get(2).getLow());
        assertEquals(19, listr.get(2).getHigh());
        assertEquals(81, listr.get(3).getLow());
        assertEquals(127, listr.get(3).getHigh());

        // the rest unchanged
        for (int i = 4; i < listr.size(); i++) {
            assertEquals(0x1L << (i * 2), listr.get(i).getLow());
            assertEquals((0x1L << (i * 2 + 1)) - 1, listr.get(i).getHigh());
        }

    }

    public void testGetFirstAndIsEmpty() {
        iSet = IntervalSet.createSingletonSet(0, 4);
        iSet.add(new Interval(17));
        iSet.add(new Interval(20));
        iSet.add(new Interval(28));
        iSet.add(new Interval(31, 32));
        // [0-4], [17-17], [20-20], [28-28], [31-32]
        assertEquals(new Interval(0, 4), iSet.getFirst());
        iSet.delete(iSet.getFirst());
        assertFalse(iSet.isEmpty());
        assertEquals(new Interval(17, 17), iSet.getFirst());
        iSet.delete(iSet.getFirst());
        assertFalse(iSet.isEmpty());
        assertEquals(new Interval(20, 20), iSet.getFirst());
        iSet.delete(iSet.getFirst());
        assertFalse(iSet.isEmpty());
        assertEquals(new Interval(28, 28), iSet.getFirst());
        iSet.delete(iSet.getFirst());
        assertFalse(iSet.isEmpty());
        assertEquals(new Interval(31, 32), iSet.getFirst());
        iSet.delete(iSet.getFirst());
        assertTrue(iSet.isEmpty());
        try {
            iSet.getFirst();
            fail("expected exception");
        } catch (NoSuchElementException nsee) {
        }
    }

    public void testGetLast() {
        iSet = IntervalSet.createSingletonSet(0, 4);
        iSet.add(new Interval(17));
        iSet.add(new Interval(20));
        iSet.add(new Interval(28));
        iSet.add(new Interval(31, 32));
        // [0-4], [17-17], [20-20], [28-28], [31-32]
        assertEquals(5, iSet.getNumberOfIntervals());
        assertEquals(new Interval(31, 32), iSet.getLast());
        iSet.delete(iSet.getLast());
        assertEquals(new Interval(28, 28), iSet.getLast());
        iSet.delete(iSet.getLast());
        assertEquals(new Interval(20, 20), iSet.getLast());
        iSet.delete(iSet.getLast());
        assertEquals(new Interval(17, 17), iSet.getLast());
        iSet.delete(iSet.getLast());
        assertEquals(new Interval(0, 4), iSet.getLast());
        iSet.delete(iSet.getLast());
        try {
            iSet.getLast();
            fail("expected exception");
        } catch (NoSuchElementException nsee) {
        }
    }

    public void testInvert() {
        iSet = new IntervalSet();
        // no intervals
        assertEquals(0, iSet.getNumberOfIntervals());
        IntervalSet inverted = iSet.invert(100);
        assertEquals(1, inverted.getAllIntervalsAsList().size());
        assertEquals(new Interval(0, 99), inverted.getAllIntervals().next());
        iSet.add(new Interval(0, 100));
        inverted = iSet.invert(0);
        assertEquals(0, inverted.getAllIntervalsAsList().size());

        iSet.clear();
        iSet.add(new Interval(10, 20));
        iSet.add(new Interval(50, 60));
        inverted = iSet.invert(100);
        assertEquals(3, inverted.getNumberOfIntervals());
        List<Range> invl = inverted.getAllIntervalsAsList();
        assertEquals(0, invl.get(0).getLow());
        assertEquals(9, invl.get(0).getHigh());
        assertEquals(21, invl.get(1).getLow());
        assertEquals(49, invl.get(1).getHigh());
        assertEquals(61, invl.get(2).getLow());
        assertEquals(99, invl.get(2).getHigh());

        inverted = iSet.invert(50);
        assertEquals(2, inverted.getNumberOfIntervals());
        invl = inverted.getAllIntervalsAsList();
        assertEquals(0, invl.get(0).getLow());
        assertEquals(9, invl.get(0).getHigh());
        assertEquals(21, invl.get(1).getLow());
        assertEquals(49, invl.get(1).getHigh());
    }

    public void testClone() throws CloneNotSupportedException {
        iSet = IntervalSet.createSingletonSet(0, 100);
        // [0-100]
        assertEquals(1, iSet.getNumberOfIntervals());
        IntervalSet clone = iSet.clone();
        assertEquals(1, clone.getAllIntervalsAsList().size());
        assertEquals(new Interval(0, 100), clone.getAllIntervals().next());
        clone.delete(clone.getFirst());
        assertEquals(0, clone.getAllIntervalsAsList().size());
        assertEquals(1, iSet.getNumberOfIntervals());
        assertEquals(new Interval(0, 100), getIntervalAt(0));
    }

    public void testToBytes() throws Exception {
        IntervalSet set = new IntervalSet();
        Random r = new Random();

        for (int i = 1; i < 10; i++) {
            int low = Math.abs(r.nextInt(100));
            Interval inter = new Interval(low, low + i);
            set.add(inter);
        }

        IntervalSet.ByteIntervals asByte = set.toBytes();
        assertEquals(0, asByte.longs.length);

        IntervalSet set2 = IntervalSet.parseBytes(asByte.ints, asByte.longs);
        assertEquals(set.getSize(), set2.getSize());

        assertEquals(set.getAllIntervalsAsList(), set2.getAllIntervalsAsList());

        // test long ranges
        set.add(Range.createRange(0xFFFFFFFFF0l, 0xFFFFFFFFFFl));
        asByte = set.toBytes();
        assertEquals(10, asByte.longs.length);
        // check manually
        assertEquals(0xFFFFFFFFF0l, ByteUtils.beb2long(asByte.longs, 0, 5));
        assertEquals(0xFFFFFFFFFFl, ByteUtils.beb2long(asByte.longs, 5, 5));
        // and check parsing
        set2 = IntervalSet.parseBytes(asByte.ints, asByte.longs);
        assertEquals(set.getSize(), set2.getSize());
        assertEquals(set.getAllIntervalsAsList(), set2.getAllIntervalsAsList());

        // the large interval will be last
        Range large = set2.getLast();
        assertEquals(0xFFFFFFFFF0l, large.getLow());
        assertEquals(0xFFFFFFFFFFl, large.getHigh());
    }

    // Test failure cases from beta testing
    public void testContains() {
        LOG.info("-- testing contains()");

        iSet = IntervalSet.createSingletonSet(1542374, 1572863);
        iSet.add(new Interval(1645963, 1835007));
        iSet.add(new Interval(2077669, 2228223));
        assertTrue(iSet.contains(new Interval(1542374, 1543397)));
    }

    public void testContainsAny() throws Exception {
        iSet.add(new Interval(10, 20));
        assertTrue(iSet.containsAny(new Interval(0, 10)));
        assertTrue(iSet.containsAny(new Interval(20, 30)));
        assertTrue(iSet.containsAny(new Interval(0, 30)));
        assertTrue(iSet.containsAny(new Interval(15, 16)));
    }

    // /// Private helper methods /////

    private Interval getIntervalAt(int i) {
        return (Interval) iSet.getAllIntervalsAsList().get(i);
    }

}