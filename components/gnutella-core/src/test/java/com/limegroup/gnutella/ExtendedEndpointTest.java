package com.limegroup.gnutella;

import java.io.StringWriter;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Iterator;

import org.limewire.core.settings.ApplicationSettings;

import junit.framework.Test;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;

/**
 * Unit tests for ExtendedEndpoint.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public class ExtendedEndpointTest extends org.limewire.gnutella.tests.LimeTestCase {
    private ExtendedEndpoint e;
    private Comparator comparator;

    public ExtendedEndpointTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ExtendedEndpointTest.class);
    }

    @Override
    public void setUp() {
        TestExtendedEndpoint.now=1;
        e=new TestExtendedEndpoint("127.0.0.1", 6346, 3492);
        comparator=ExtendedEndpoint.priorityComparator();
    }

    /** Tests behavior needed by our code. */
    public void testNumberFormatException() {
        try {
            Integer.parseInt("");
            fail("No NumberFormatException");
        } catch (NumberFormatException e) {
        }
        try {
            Long.parseLong("");
            fail("No NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }
    
    public void testAssertionsEnabled() {
        try {
            assert(false);
            fail("Assertions are not enabled");
        } catch (AssertionError expected) {}
    }

    //////////////////////////// Successes and Failures ////////////////////

    public void testConnectionSuccesses() {
        Iterator iter=e.getConnectionSuccesses();
        assertTrue(! iter.hasNext());

        TestExtendedEndpoint.now=1000;              //first value: 1000
        e.recordConnectionSuccess();
        iter=e.getConnectionSuccesses();
        assertEquals(new Long(1000), iter.next());
        assertTrue(!iter.hasNext());
        
        TestExtendedEndpoint.now=1001;              //coalesce: 1001
        e.recordConnectionSuccess();
        iter=e.getConnectionSuccesses();
        assertEquals(new Long(1001), iter.next());
        assertTrue(!iter.hasNext());
        
        TestExtendedEndpoint.now+=ExtendedEndpoint.WINDOW_TIME+1;  //big, 10001
        e.recordConnectionSuccess();
        iter=e.getConnectionSuccesses();
        assertEquals(new Long(TestExtendedEndpoint.now), iter.next());
        assertEquals(new Long(1001), iter.next());
        assertTrue(!iter.hasNext());        

        assertTrue(! e.getConnectionFailures().hasNext());
    }

    public void testConnectionFailures() {
        TestExtendedEndpoint.now=0;
        e.recordConnectionFailure();
        TestExtendedEndpoint.now+=ExtendedEndpoint.WINDOW_TIME; //1x
        e.recordConnectionFailure();
        TestExtendedEndpoint.now+=ExtendedEndpoint.WINDOW_TIME; //2x
        e.recordConnectionFailure();
        TestExtendedEndpoint.now+=ExtendedEndpoint.WINDOW_TIME; //3x
        e.recordConnectionFailure();

        Iterator iter=e.getConnectionFailures();
        assertEquals(new Long(3*ExtendedEndpoint.WINDOW_TIME), iter.next());
        assertEquals(new Long(2*ExtendedEndpoint.WINDOW_TIME), iter.next());
        assertEquals(new Long(1*ExtendedEndpoint.WINDOW_TIME), iter.next());
        assertTrue(!iter.hasNext());        

        assertTrue(! e.getConnectionSuccesses().hasNext());
    }   


    /////////////////////////// Reading and Writing /////////////////////////

    public void testWriteNormal() throws Exception {
        TestExtendedEndpoint.now=100;
        e.recordConnectionSuccess();
        TestExtendedEndpoint.now=113;
        e.recordConnectionFailure();
        TestExtendedEndpoint.now+=ExtendedEndpoint.WINDOW_TIME; //1x
        e.recordConnectionFailure();
        e.setDHTVersion(0);
        e.setDHTMode(DHTMode.INACTIVE);
        StringWriter out=new StringWriter();
        e.write(out);
        //Window time is hard-coded below.
        assertEquals("127.0.0.1:6346,3492,1,100,86400113;113,"
                     + ApplicationSettings.DEFAULT_LOCALE.get() 
                     + ",,0,INACTIVE,,\n",
                     out.toString());
    }

    public void testReadNormal() throws Exception {
        ExtendedEndpoint e=ExtendedEndpoint.read(
            "127.0.0.1:6348,3492,1, 100,86400113;113\n");
        assertEquals("127.0.0.1", e.getAddress());
        assertEquals(6348, e.getPort());
        assertEquals(3492, e.getDailyUptime());
        assertEquals(1, e.getTimeRecorded());
        Iterator iter=e.getConnectionSuccesses();
        assertEquals(100, ((Long)iter.next()).longValue());
        assertTrue(!iter.hasNext());
        iter=e.getConnectionFailures();
        assertEquals(86400113, ((Long)iter.next()).longValue());
        assertEquals(113, ((Long)iter.next()).longValue());
        assertTrue(!iter.hasNext());
    }

    public void testWriteUnknown() throws Exception  {
        long now=System.currentTimeMillis();
        e=new ExtendedEndpoint("127.0.0.1", 6346);
        assertEquals(ExtendedEndpoint.DEFAULT_DAILY_UPTIME, 
                     e.getDailyUptime());
        //Allow a fudge factor of 0.5 second in comparison
        assertEquals(now, (float)e.getTimeRecorded(), 500.f);
        String timeString=Long.toString(e.getTimeRecorded());
        StringWriter out=new StringWriter();
        e.write(out);
        //Window time is hard-coded below.
        assertEquals("127.0.0.1:6346,,"+timeString+",,"
                     + "," + ApplicationSettings.DEFAULT_LOCALE.get()
                     + ",,,,,\n",
                     out.toString());
    }

   public void testReadUnknown() throws Exception {
        ExtendedEndpoint e=ExtendedEndpoint.read(
            "127.0.0.1:6348,,A,, 86400113;113 \n");
        assertEquals("127.0.0.1", e.getAddress());
        assertEquals(6348, e.getPort());
        assertEquals(ExtendedEndpoint.DEFAULT_DAILY_UPTIME, 
                     e.getDailyUptime());
        assertEquals(ExtendedEndpoint.DEFAULT_TIME_RECORDED, 
                     e.getTimeRecorded());
        Iterator iter=e.getConnectionSuccesses();
        assertTrue(!iter.hasNext());
        iter=e.getConnectionFailures();
        assertEquals(86400113, ((Long)iter.next()).longValue());
        assertEquals(113, ((Long)iter.next()).longValue());
        assertTrue(!iter.hasNext());
        e=ExtendedEndpoint.read(
            "127.0.0.1:6348,,A,, 86400113;113,,,3,ACTIVE,\n");
        assertTrue(e.supportsDHT());
        assertEquals(3, e.getDHTVersion());
        assertEquals(DHTMode.ACTIVE, e.getDHTMode());
    }
   
   public void testReadWriteUnknown() throws Exception {
       ExtendedEndpoint e = ExtendedEndpoint.read("127.0.0.1:6348,,\n");
       assertEquals(ExtendedEndpoint.DEFAULT_DAILY_UPTIME,
               e.getDailyUptime());
       assertEquals(ExtendedEndpoint.DEFAULT_TIME_RECORDED,
               e.getTimeRecorded());
       StringWriter writer = new StringWriter();
       e.write(writer);
       assertTrue(writer.toString().startsWith("127.0.0.1:6348,,0,"));
   }

   public void testReadOldStyle() throws Exception {
        ExtendedEndpoint e=ExtendedEndpoint.read(
            "127.0.0.1:6348");
        assertEquals("127.0.0.1", e.getAddress());
        assertEquals(6348, e.getPort());
        assertEquals(ExtendedEndpoint.DEFAULT_DAILY_UPTIME, 
                     e.getDailyUptime());
        assertEquals(ExtendedEndpoint.DEFAULT_TIME_RECORDED, 
                     e.getTimeRecorded());
        Iterator iter=e.getConnectionSuccesses();
        assertTrue(!iter.hasNext());
        iter=e.getConnectionFailures();
        assertTrue(!iter.hasNext());
    }
    
    public void testUDPHostCacheEndpoints() throws Exception {
        ExtendedEndpoint e= ExtendedEndpoint.read("1.3.4.5:6348,,1097611864117,,,en,1");
        assertTrue(e.isUDPHostCache());
        assertEquals(1, e.getUDPHostCacheFailures());
        e.recordUDPHostCacheFailure();
        assertEquals(2, e.getUDPHostCacheFailures());
        e.recordUDPHostCacheSuccess();
        assertEquals(0, e.getUDPHostCacheFailures());
        StringWriter writer = new StringWriter();
        e.write(writer);
        assertEquals("1.3.4.5:6348,,1097611864117,,,en,0,,,,\n", writer.toString());
        e.recordUDPHostCacheFailure();
        e.recordUDPHostCacheFailure();
        e.recordUDPHostCacheFailure();
        writer = new StringWriter();
        e.write(writer);
        assertEquals("1.3.4.5:6348,,1097611864117,,,en,3,,,,\n", writer.toString());
        e.setUDPHostCache(false);
        assertFalse(e.isUDPHostCache());
        try {
            e.recordUDPHostCacheFailure();
            fail("expected AssertionError");
        } catch(AssertionError expected) {}
        try {
            e.recordUDPHostCacheSuccess();
            fail("expected AssertionError");
        } catch(AssertionError expected) {}
        writer = new StringWriter();
        e.write(writer);
        assertEquals("1.3.4.5:6348,,1097611864117,,,en,,,,,\n", writer.toString());
        
        e = ExtendedEndpoint.read("www.limewire.org:6346,,,,,en,0,");
        assertTrue(e.isUDPHostCache());
        assertEquals(0, e.getUDPHostCacheFailures());
    }
    
    public void testTLSEndpoints() throws Exception {
        ExtendedEndpoint e= ExtendedEndpoint.read("1.3.4.5:6348,,1097611864117,,,en,,,,1,");
        assertTrue(e.isTLSCapable());
        StringWriter writer = new StringWriter();
        e.write(writer);
        assertEquals("1.3.4.5:6348,,1097611864117,,,en,,,,1,\n", writer.toString());
        e.setTLSCapable(false);
        writer = new StringWriter();
        e.write(writer);
        assertEquals("1.3.4.5:6348,,1097611864117,,,en,,,,,\n", writer.toString());        
        e = ExtendedEndpoint.read("1.3.4.5:6348,,1097611864117,,,en,,,,,");
        assertFalse(e.isTLSCapable());
        e.setTLSCapable(true);
        writer = new StringWriter();
        e.write(writer);
        assertEquals("1.3.4.5:6348,,1097611864117,,,en,,,,1,\n", writer.toString());
    }
    
    public void testReadingStuff() throws Exception {
        ExtendedEndpoint.read("1.2.3.4:6346");
        ExtendedEndpoint.read("1.2.3.4");
        try {
            ExtendedEndpoint.read("www.limewire.org");
            fail("read");
        } catch(ParseException pe) {}
        try {
            ExtendedEndpoint.read("<html>1.2.3.4");
            fail("read");
        } catch(ParseException pe) {}
        try {
            ExtendedEndpoint.read("nothing.nowhere");
            fail("read");
        } catch(ParseException pe) {}
        try {
            ExtendedEndpoint.read("1.3.nothing.4");
            fail("read");
        } catch(ParseException pe) {}
        try {
            ExtendedEndpoint.read("0.0.0.0");
            fail("read");
        } catch(ParseException pe) {}
    }
    
    /////////////////////////// Comparators //////////////////////////////
    
    public void testComparatorConnectBoth() {
        //Bad connected at time=0 but failed at time=1
        TestExtendedEndpoint.now=0;
        ExtendedEndpoint bad=new TestExtendedEndpoint("18.239.0.146", 6346, 1000);
        bad.recordConnectionSuccess();
        TestExtendedEndpoint.now=1;
        bad.recordConnectionFailure();
        
        //Good failed at time=0 but succeeded at time=1.  (Uptime doesn't matter)
        TestExtendedEndpoint.now=0;
        ExtendedEndpoint good=new TestExtendedEndpoint("18.239.0.147", 6347, 100);
        good.recordConnectionFailure();
        TestExtendedEndpoint.now=1;
        good.recordConnectionSuccess();

        assertTrue(comparator.compare(bad, good)<0);
        assertTrue(comparator.compare(good, bad)>0);   
    }
    
    public void testComparatorConnectOneSuccess() {
        //Bad connected at time=0 but failed at time=1
        ExtendedEndpoint bad=new TestExtendedEndpoint("18.39.0.146",6346,1000);
        
        //Good failed at time=0 but succeeded at time=1.  (Uptime doesn't matter)
        ExtendedEndpoint good=new TestExtendedEndpoint("18.39.0.147",6347,100);
        good.recordConnectionSuccess();

        assertLessThan("bad not less than good",
            0, comparator.compare(bad, good));
        assertGreaterThan("good not greater than bad",
            0, comparator.compare(good, bad));   
    }
    
    public void testComparatorConnectOneFailure() {
        //Bad connected at time=0 but failed at time=1
        ExtendedEndpoint bad=new TestExtendedEndpoint("18.39.0.146",6346, 1000);
        bad.recordConnectionFailure();        

        //Good failed at time=0 but succeeded at time=1.  (Uptime doesn't matter)
        ExtendedEndpoint good=new TestExtendedEndpoint("18.39.0.147",6347,1000);

        assertLessThan("bad not less than good",
            0, comparator.compare(bad, good));
        assertGreaterThan("good not greater than bad",
            0, comparator.compare(good, bad));   
    }

    public void testComparatorUptimeEq() {
        ExtendedEndpoint a=new ExtendedEndpoint("18.239.0.146", 6346, 1000);
        ExtendedEndpoint b=new ExtendedEndpoint("18.239.0.147", 6347, 1000);
        assertEquals(0, comparator.compare(a, b));
        assertEquals(0, comparator.compare(b, a));
    }

    public void testComparatorUptimeNeq() {
        ExtendedEndpoint bad=new ExtendedEndpoint("18.239.0.146", 6346, 999);
        ExtendedEndpoint good=new ExtendedEndpoint("18.239.0.147", 6347, 1000);
        assertLessThan("bad not less than good",
            0, comparator.compare(bad, good));
        assertGreaterThan("good not greater than bad",
            0, comparator.compare(good, bad)); 
    }

}

/** Fakes the system time. */
class TestExtendedEndpoint extends ExtendedEndpoint {
    static long now;

    TestExtendedEndpoint(String host, int port, int dailyUptime) {
        super(host, port, dailyUptime);
    }

    @Override
    protected long now() {
        return now;
    }
}
