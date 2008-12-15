package org.limewire.io;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Unit tests for IP, IPFilter, IPList
 */
public class IPTest extends BaseTestCase {
    byte[] guid;

    public IPTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IPTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private byte[] bytes(int one, int two, int three, int four) {
        return new byte[] { (byte)one, (byte)two, (byte)three, (byte)four };
    }

    public void testIPLegacy() {
        IP a=new IP("1.1.1.*");
        IP b=new IP("1.1.1.2");
        IP c=new IP("1.1.2.1");
        IP b1 = new IP(bytes(1,1,1,2));
        IP c1 = new IP(bytes(1,1,2,1));
        assertTrue(a.contains(b));
        assertTrue(! b.contains(a));
        assertTrue(! a.contains(c));
        assertTrue(! c.contains(a));
        assertTrue(b.contains(b1));
        assertTrue(c.contains(c1));
        assertTrue(b1.contains(b));
        assertTrue(c1.contains(c));

        assertNotEquals(a,c);
        assertNotEquals(a,b);
        assertNotEquals(b,a);
        assertNotEquals(b,c);
        assertEquals(a, a);
        assertEquals(b,b);
        assertEquals(b, b1);
        assertEquals(c, c1);
        assertEquals(b.hashCode(), b1.hashCode());
        assertEquals(c.hashCode(), c1.hashCode());
        assertNotEquals("asdf", a);
        IP d=new IP("1.1.1.0/255.255.255.0");
        IP e=new IP("1.1.1.0/24");
        IP f=new IP("1.1.1/24");
        assertEquals(a,d);
        assertEquals(d,a);
        assertEquals(a.hashCode(),d.hashCode());
        assertEquals(a,e);
        assertEquals(e,a);
        assertEquals(a.hashCode(),e.hashCode());
        assertEquals(a,f);
        assertEquals(f,a);
        assertEquals(a.hashCode(),f.hashCode());
        assertEquals(d,e);
        assertEquals(f,e);
        
        try {
            new IP("this should fail.");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("821.1.1.0");
            fail("illegal argument excepted.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1*.1.0");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.256.0.0");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1.1.1/33");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1.1.1/-1");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}    
        
        try {
            new IP("1.1..1");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1.1.1/255.255.256.255");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1234.1.1");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1.1.1/255.1234.255.255");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP(new byte[] {0,0,0});
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP(new byte[] {0,0,0,0,0});
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
    }


    public void testGetDistanceTo() {
        IP ipRange = new IP("18.194.0.1/16");
        IP ip = new IP("18.194.0.37");
        assertEquals("Distance between an IP and its subnet should be zero.", 0, ipRange.getDistanceTo(ip));
        ip = new IP("128.0.0.1");
        assertLessThan("Expect the high-order bit of the xor distance to be set", 0 , ipRange.getDistanceTo(ip));
        
        ipRange = new IP("0.0.0.0/16");
        ip      = new IP("127.255.255.255"); // = Integer.MAX_VALUE
        assertEquals(0x7FFF0000, ipRange.getDistanceTo(ip));
        
        IP ipRange2 = new IP("10.1.2.3/8");
        assertEquals("Incorrect distance between two ranges", 0x0A000000,ipRange.getDistanceTo(ipRange2));
    }
}

