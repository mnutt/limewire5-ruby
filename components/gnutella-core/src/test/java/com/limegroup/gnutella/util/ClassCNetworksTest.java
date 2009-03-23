package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.util.ByteUtils;

import junit.framework.Test;

public class ClassCNetworksTest extends LimeTestCase {

    public ClassCNetworksTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClassCNetworksTest.class);
    }
    
    public void testCounting() throws Exception {
        ClassCNetworks cnc = new ClassCNetworks();
        cnc.add(InetAddress.getByName("1.2.3.4"),1);
        cnc.add(InetAddress.getByName("1.2.3.5"),2);
        cnc.add(InetAddress.getByName("1.2.4.5"),1);
        cnc.add(0x01020400,5); // 1.2.4.0
        
        List<Map.Entry<Integer,Integer>> top = cnc.getTop();
        assertEquals(2,top.size());
        assertEquals(0x01020400,top.get(0).getKey().intValue());
        assertEquals(6,top.get(0).getValue().intValue());
        assertEquals(0x01020300,top.get(1).getKey().intValue());
        assertEquals(3,top.get(1).getValue().intValue());
        
        List<IpPort> l = new ArrayList<IpPort>();
        l.add(new IpPortImpl("1.2.3.6"));
        l.add(new IpPortImpl("1.2.3.7"));
        l.add(new IpPortImpl("1.2.4.7"));
        l.add(new IpPortImpl("1.2.5.1"));
        
        cnc.addAll(l);
        top = cnc.getTop();
        assertEquals(3,top.size());
        assertEquals(0x01020400,top.get(0).getKey().intValue());
        assertEquals(7,top.get(0).getValue().intValue());
        assertEquals(0x01020300,top.get(1).getKey().intValue());
        assertEquals(5,top.get(1).getValue().intValue());
        assertEquals(0x01020500,top.get(2).getKey().intValue());
        assertEquals(1,top.get(2).getValue().intValue());
    }
    
    public void testMerging() throws Exception {
        ClassCNetworks a = new ClassCNetworks();
        ClassCNetworks b = new ClassCNetworks();
        
        a.add(0x01020300, 1);
        a.add(0x01020400, 1);
        
        b.add(0x01020400, 1);
        b.add(0x01020500, 1);
        
        ClassCNetworks c = new ClassCNetworks();
        c.addAll(a,b);
        List<Map.Entry<Integer,Integer>> top = c.getTop();
        assertEquals(3, top.size());
        assertEquals(0x01020400, top.get(0).getKey().intValue());
        assertEquals(2, top.get(0).getValue().intValue());
        // the other two are in no particular order
        assertEquals(1, top.get(1).getValue().intValue());
        assertEquals(1, top.get(1).getValue().intValue());
    }
    
    public void testInspectable() throws Exception {
        ClassCNetworks cnc = new ClassCNetworks();
        cnc.add(InetAddress.getByName("1.2.3.4"),1);
        cnc.add(InetAddress.getByName("1.2.3.5"),2);
        cnc.add(InetAddress.getByName("1.2.4.5"),1);
        cnc.add(InetAddress.getByName("1.2.5.6"),4);
        
        byte [] b = cnc.getTopInspectable(2);
        assertEquals(16,b.length);
        assertEquals(0x01020500,ByteUtils.beb2int(b, 0));
        assertEquals(4,ByteUtils.beb2int(b, 4));
        assertEquals(0x01020300,ByteUtils.beb2int(b, 8));
        assertEquals(3,ByteUtils.beb2int(b, 12));
    }
    
    public void testMask() throws Exception {
        ClassCNetworks cnc = new ClassCNetworks(16);
        cnc.add(InetAddress.getByName("1.2.3.4"),1);
        cnc.add(InetAddress.getByName("1.2.3.5"),2);
        cnc.add(InetAddress.getByName("1.2.4.5"),1);
        cnc.add(InetAddress.getByName("1.3.5.6"),4);
        List<Map.Entry<Integer,Integer>> top = cnc.getTop();
        // not a stable test criterion, map order might change
        assertEquals(0x01020000, top.get(1).getKey().intValue());
        assertEquals(0x01030000, top.get(0).getKey().intValue());
        assertEquals(4, top.get(0).getValue().intValue());
        assertEquals(4, top.get(1).getValue().intValue());
        assertEquals(2,top.size());
    }
}
