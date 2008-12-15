package org.limewire.io;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Hashtable;

import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;


import junit.framework.Test;

/**
 * Unit tests for GUID.
 */
@SuppressWarnings("unchecked")
public class GUIDTest extends BaseTestCase {
    private byte[] bytes;
    private byte[] b1;
    private byte[] b2;
    private GUID g1;
    private GUID g2;
    private short tag;
    private short s1;
    private short s2;

    public GUIDTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GUIDTest.class);
    }

    @Override
    public void setUp() {
        //Prevents NullPointerException's in testIsX.
        //Otherwise not needed.
        bytes=new byte[16];
    }

    public void testEquals() {
        b1=new byte[16];
        b2=new byte[16];
        for (int i=0; i<16; i++) {
            b1[i]=(byte)i;
            b2[i]=(byte)i;
        }
        g1=new GUID(b1);
        g2=new GUID(b1);
        assertEquals("guids should be equal", g1, g2);
        assertEquals("guids should be equal", g2, g1);
        assertEquals("guid hashcodes should be equal",
            g1.hashCode(), g2.hashCode());
    
        Hashtable t=new Hashtable();
        String out=null;
        t.put(g1,"test");
        assertTrue("Contains 1", t.containsKey(g1));
        out=(String)t.get(g1);
        assertNotNull("1: shouldn't be null", out);
        assertEquals("1: unexpected out value", "test", out);

        assertTrue("Contains 2", t.containsKey(g2));
        out=(String)t.get(g2);
        assertNotNull("2: shouldn't be null", out);
        assertEquals("2: unexpected out value", "test", out);
    }

    public void testHexStrings()  {
        String hexString="FF010A00000000000000000000000001";
        bytes=new byte[16];
        bytes[0]=(byte)255;
        bytes[1]=(byte)1;
        bytes[2]=(byte)10;
        bytes[15]=(byte)1;

        String s=(new GUID(bytes)).toHexString();
        assertEquals("unexpected hex string", hexString, s);
        byte[] bytes2=GUID.fromHexString(s);
        assertTrue(Arrays.equals(bytes2,bytes));

        try {
            GUID.fromHexString("aa01");
            fail("should not have made GUID");
        } catch (IllegalArgumentException e) {
        }

        try {
            GUID.fromHexString("ff010a0000000000000000000000000z");
            fail("should not have made GUID");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testMakeGUID() {
        //Try new GUID generator.
        for (int i=0; i<50; i++) {
            bytes=GUID.makeGuid();     g1=new GUID(bytes);
            assertEquals("unexpected 15th byte", (byte)0x00, bytes[15]);
            assertTrue(g1.isLimeGUID());
            //System.out.println(g1);
        }               
    }
     
    public void testIsLimeGUID() {
        //Try isLimeGUID (again)
        bytes[9]=(byte)0xFF;   g1=new GUID(bytes);
        assertTrue(! g1.isLimeGUID());
        bytes[4]=(byte)0x02;
        bytes[5]=(byte)0x01;
        bytes[6]=(byte)0x00;        
        bytes[7]=(byte)0x05;
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0500+3)=0x0104*0x0503=0x5170C ==> 0x0517
        bytes[9]=(byte)0x17;
        bytes[10]=(byte)0x05;
        g1=new GUID(bytes);
        short s1=ByteUtils.leb2short(bytes, 4);
        short s2=ByteUtils.leb2short(bytes, 6);
        short tag=GUID.tag(s1, s2);
        assertEquals("unexpected s1: " + Integer.toHexString(s1),
            (short)0x0102, s1);
        assertEquals("unexpected s2: " +Integer.toHexString(s2),
            (short)0x0500, s2);
        assertEquals("unexpected tag: " + Integer.toHexString(tag),
            (short)0x0517, tag);
        assertTrue(g1.isLimeGUID());
        //System.out.println(g1);
    }

    public void testRequery0GUID() {
        // Test version 0 requery marking
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0517+3)=0x0104*0x051A=0x52E68 ==> 0x052E
        bytes=new byte[16];
        bytes[0]=(byte)0x02;
        bytes[1]=(byte)0x01;
        bytes[9]=(byte)0x17;
        bytes[10]=(byte)0x05;
        bytes[13]=(byte)0x2E;
        bytes[14]=(byte)0x05;
        s1=ByteUtils.leb2short(bytes, 0);
        s2=ByteUtils.leb2short(bytes, 9);
        tag=GUID.tag(s1,s2);
        assertEquals("unexpected s1: " + Integer.toHexString(s1),
            (short)0x0102, s1);
        assertEquals("unexpected s2: " + Integer.toHexString(s2),
            (short)0x0517, s2);
        assertEquals("unexpected tag: " + Integer.toHexString(tag),
            (short)0x052E, tag);
        g1 = new GUID(bytes);
        assertTrue(g1.isLimeRequeryGUID());
        assertTrue(g1.isLimeRequeryGUID(0));
        assertTrue(! g1.isLimeRequeryGUID(1));
        assertTrue(! g1.isLimeRequeryGUID(2));
    }

    public void testRequery1GUID() {
        // Test version 1 requery marking
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0517+3)=0x0104*0x051A=0x52E68 ==> 0x052E
        bytes=new byte[16];
        bytes[0]=(byte)0x02;
        bytes[1]=(byte)0x01;
        bytes[2]=(byte)0x17;
        bytes[3]=(byte)0x05;
        bytes[13]=(byte)0x2E;
        bytes[14]=(byte)0x05;
        s1=ByteUtils.leb2short(bytes, 0);
        s2=ByteUtils.leb2short(bytes, 2);
        tag=GUID.tag(s1,s2);
        assertEquals("unexpected s1: " + Integer.toHexString(s1),
            (short)0x0102, s1);
        assertEquals("unexpected s2: " + Integer.toHexString(s2),
            (short)0x0517, s2);
        assertEquals("unexpected tag: " + Integer.toHexString(tag),
            (short)0x052E, tag);
        g1 = new GUID(bytes);
        assertTrue(g1.isLimeRequeryGUID());
        assertTrue(g1.isLimeRequeryGUID(1));
        assertTrue(! g1.isLimeRequeryGUID(0));
        assertTrue(! g1.isLimeRequeryGUID(2));
    }

    public void testRequery3GUID() {
        // Test new (version 2) requery marking
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0517+3)=0x0104*0x051A=0x52E68 ==> 0x052E
        bytes=new byte[16];
        bytes[0]=(byte)0x02;
        bytes[1]=(byte)0x01;
        bytes[11]=(byte)0x17;
        bytes[12]=(byte)0x05;
        bytes[13]=(byte)0x2E;
        bytes[14]=(byte)0x05;
        s1=ByteUtils.leb2short(bytes, 0);
        s2=ByteUtils.leb2short(bytes, 11);
        tag=GUID.tag(s1,s2);
        assertEquals("unexpected s1: " + Integer.toHexString(s1),
            (short)0x0102, s1);
        assertEquals("unexpected s2: " + Integer.toHexString(s2),
            (short)0x0517, s2);
        assertEquals("unexpected tag: " + Integer.toHexString(tag),
            (short)0x052E, tag);
        g1 = new GUID(bytes);
        assertTrue(g1.isLimeRequeryGUID());
        assertTrue(! g1.isLimeRequeryGUID(0));
        assertTrue(! g1.isLimeRequeryGUID(1));
        assertTrue(g1.isLimeRequeryGUID(2));

        // Test LimeRequeryGUID construction....
        bytes = GUID.makeGuidRequery();
        GUID gReq = new GUID(bytes);
        //System.out.println(gReq);
        assertTrue(gReq.isLimeGUID());
        assertTrue(gReq.isLimeRequeryGUID());
        assertTrue(gReq.isLimeRequeryGUID(2));
        assertTrue(! gReq.isLimeRequeryGUID(0));
        assertTrue(! gReq.isLimeRequeryGUID(1));
    }

    public void testHashAndCompare() {
        //Test hashcode, compareTo for same
        java.util.Random r=new java.util.Random();       
        b1=new byte[16];
        r.nextBytes(b1);
        b2=new byte[16];
        System.arraycopy(b1,0,b2,0,16);
        g1=new GUID(b1);
        g2=new GUID(b2);
        assertEquals("g1 should compare to same as g2", 0, g1.compareTo(g2));
        assertEquals("g2 should compare to same as g1", 0, g2.compareTo(g1));
        assertEquals("hashcodes should be same", g1.hashCode(), g2.hashCode());
        //System.out.println("Hash: "+Integer.toHexString(g1.hashCode()));

        //make sure we don't rollover, killing the below tests.
        if(b2[7]==255)
            b1[7]--;
        else
            b2[7]+=1;
        g2=new GUID(b2);
        assertLessThan("g1 vs g2",
            0, g1.compareTo(g2));
        assertLessThan("g1 vs g2",
            0, (new GUID.GUIDComparator()).compare(g1, g2));
        assertGreaterThan("g2 vs g1", 
            0, (new GUID.GUIDComparator()).compare(g2, g1));
        assertLessThan("b1 vs b2", 
            0, (new GUID.GUIDByteComparator()).compare(b1, b2));
        assertGreaterThan("b2 vs b1",
            0, (new GUID.GUIDByteComparator()).compare(b2, b1));
        assertGreaterThan("g2 vs g1", 0, g2.compareTo(g1));
        assertNotEquals("hash codes shouldn't be same",
            g1.hashCode(), g2.hashCode());  //not strictly REQUIRED
        //System.out.println("Hash: "+Integer.toHexString(g2.hashCode()));
    }


    public void testAddressEncodedGUID() throws Exception {
        InetAddress nyu = InetAddress.getByName("www.nyu.edu");
        byte[] nyuBytes = nyu.getAddress();
        final int port = 17834;
        byte[] portBytes = new byte[2];
        ByteUtils.short2leb((short) port, portBytes, 0);

        // test construction
        byte[] guidBytes = GUID.makeAddressEncodedGuid(nyuBytes, port);
        for (int i = 0; i < 4; i++)
            assertEquals("bytes should be equal!", guidBytes[i], nyuBytes[i]);
        assertEquals("bytes should be equal!", guidBytes[13], portBytes[0]);
        assertEquals("bytes should be equal!", guidBytes[14], portBytes[1]);

        // construction looks good - lets test accessors....
        GUID guid = new GUID(guidBytes);
        assertEquals("IP Strings not the same!!", guid.getIP(), 
                     nyu.getHostAddress());
        assertEquals("Ports are not the same!!", guid.getPort(), port);

        // test comparator
        assertTrue(guid.addressesMatch(nyuBytes, port));
        
        // test that timestamping doesn't break address encoding
        GUID.timeStampGuid(guidBytes);
        guid = new GUID(guidBytes);
        assertTrue(guid.addressesMatch(nyuBytes, port));
    }
    
    public void testTimeStamp() throws Exception {
        byte [] g = GUID.makeGuid();
        GUID.timeStampGuid(g);
        Thread.sleep(10);
        assertGreaterThan(System.currentTimeMillis() - 2000, GUID.readTimeStamp(g));
    }

}
