package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.limewire.io.IP;
import org.limewire.io.SimpleNetworkInstanceUtils;

public class IPListTest extends com.limegroup.gnutella.util.LimeTestCase {
    public IPListTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IPListTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testIsValid() throws Exception {
        IPList list = new IPList();
        
        //try a private address
        list.add("192.168.1.1");
        assertFalse(list.isValidFilter(false, new SimpleNetworkInstanceUtils()));
        assertTrue(list.isValidFilter(true, null));
        
        //try filling up the space
        list = new IPList();
        list.add("*.*.*.*");
        assertFalse(list.isValidFilter(false, new SimpleNetworkInstanceUtils()));
        
        //try below max space - 2.5% of 4GB = 6 class A networks
        list = new IPList();
        list.add("1.*.*.*");
        list.add("2.*.*.*");
        list.add("3.*.*.*");
        list.add("4.*.*.*");
        list.add("5.*.*.*");
        list.add("6.*.*.*");
        assertTrue(list.isValidFilter(false, new SimpleNetworkInstanceUtils()));
        list.add("7.*.*.*");
        assertFalse(list.isValidFilter(false, new SimpleNetworkInstanceUtils()));
    }
    
    public void testIPListLegacy() {
        IPList iplist = new IPList();

        iplist.add ("192.168.0.1/255.255.255.0");
        iplist.add ("10.0.*.*");

        assertTrue(!iplist.contains (new IP("192.168.1.2/255.255.255.0")));
        assertTrue(iplist.contains (new IP("192.168.0.2")));
        assertTrue(iplist.contains (new IP(bytes(192,168,0,2))));
        assertTrue(iplist.contains (new IP("192.168.0.1")));
        assertTrue(iplist.contains (new IP(bytes(192,168,0,1))));
        assertTrue(!iplist.contains (new IP("192.168.1.1")));
        assertTrue(!iplist.contains (new IP(bytes(192,168,1,1))));
        assertTrue(iplist.contains (new IP("10.0.1.1")));
        assertTrue(iplist.contains (new IP(bytes(10,0,1,1))));
        assertTrue(!iplist.contains (new IP("10.1.0.1")));        
        assertTrue(!iplist.contains (new IP(bytes(10,1,0,1))));
    }
    
    public void testContains() {
        IPList list = new IPList();
        
        list.add("192.168.1.1");
        assertTrue(list.contains(new IP("192.168.1.1")));
        assertFalse(list.contains(new IP("192.168.1.2")));
        assertEquals(1, list.size());
        
        list.add("192.168.2.0/255.255.255.0");
        assertTrue(list.contains(new IP("192.168.1.1")));
        assertTrue(list.contains(new IP("192.168.2.2")));
        assertFalse(list.contains(new IP("192.168.1.2")));
        assertEquals(2, list.size());
        
        list.add("192.168.2.1");
        assertEquals(2, list.size());
        list.add("192.168.2.3");
        assertEquals(2, list.size());
        list.add("192.168.1.255");
        assertEquals(3, list.size());
        list.add("192.168.1.254");
        assertEquals(4, list.size());
        assertTrue(list.contains(new IP("192.168.1.1")));
        assertTrue(list.contains(new IP("192.168.2.2")));
        assertFalse(list.contains(new IP("192.168.1.2")));
        
        
        
        list.add("100.0.0.0/255.0.0.0");
        assertEquals(5, list.size());
        list.add("100.1.2.3");
        assertEquals(5, list.size());
        list.add("100.5.4.3");
        assertEquals(5, list.size());
        assertTrue(list.contains(new IP("100.5.3.2")));
        assertTrue(list.contains(new IP("100.2.3.4")));
        
        assertFalse(list.contains(new IP("99.2.1.3")));
        
        list.add("98.1.2.3");
        assertEquals(6, list.size());
        list.add("98.5.4.3");
        assertEquals(7, list.size());
        list.add("98.0.0.0/255.0.0.0");
        assertEquals(6, list.size());
        assertTrue(list.contains(new IP("98.5.3.2")));
        assertTrue(list.contains(new IP("98.2.3.4")));
        
        assertFalse(list.contains(new IP("99.2.1.3")));
        
        list.add("69.41.173.34");
        assertEquals(7, list.size());
        list.add("69.*.*.*");
        assertEquals(7, list.size());
        
        list.add("*.*.*.*");
        assertEquals(1, list.size());
        assertTrue(list.contains(new IP("98.5.3.2")));
        assertTrue(list.contains(new IP("98.2.3.4")));
        assertTrue(list.contains(new IP("99.2.1.3")));
        assertTrue(list.contains(new IP("100.5.3.2")));
        assertTrue(list.contains(new IP("100.2.3.4")));
        assertTrue(list.contains(new IP("99.2.1.3")));
        assertTrue(list.contains(new IP("192.168.1.1")));
        assertTrue(list.contains(new IP("192.168.2.2")));
        assertTrue(list.contains(new IP("192.168.1.2")));
    }
    
    private byte[] bytes(int one, int two, int three, int four) {
        return new byte[] { (byte)one, (byte)two, (byte)three, (byte)four };
    }
}