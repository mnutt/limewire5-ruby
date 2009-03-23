package com.limegroup.gnutella.routing;


import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.zip.Inflater;

import junit.framework.Test;

import org.limewire.io.IOUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryRequestFactory;

public class QueryRouteTableTest extends com.limegroup.gnutella.util.LimeTestCase {
    private QueryRequestFactory queryRequestFactory;
    
    public QueryRouteTableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(QueryRouteTableTest.class);
    }

    @Override
    protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }
    
    /** May return null!
     */ 
    private QRTTableStorage getBitTable(QueryRouteTable source) throws Exception {
        QRTTableStorage retSet = null;
            retSet = (QRTTableStorage) PrivilegedAccessor.getValue(source,
                                                          "storage");
        return retSet;
    }

    /** If it can't get anything, will return 0.
     */ 
    private int getBitTableLength(QueryRouteTable source) throws Exception {
        int retLength = 0;
            Integer intObj = 
               (Integer)PrivilegedAccessor.getValue(source,
                                                    "bitTableLength");
            retLength = intObj.intValue();
        return retLength;
    }


    /** If it can't get anything, will return 0.
     */ 
    private void setUncompressor(QueryRouteTable source, Inflater inflater) throws Exception {
            PrivilegedAccessor.setValue(source, "uncompressor", inflater);
    }

    private byte[] invokeUncompress(QueryRouteTable source, byte[] chunk) throws Exception {
        byte[] retBytes = new byte[0];
            retBytes = 
            (byte[]) PrivilegedAccessor.invokeMethod(source, "uncompress", chunk);
        return retBytes;
    }
    
    private int entries(QueryRouteTable tbl) throws Exception{
        return getBitTable(tbl).cardinality();
    }

    public void testCompressionAndUncompress() throws Exception {
        //0. compress/uncompress.  First we make a huge array with lots of
        //random bytes but also long strings of zeroes.  This means that
        //compression will work, but not too well.  Then we take the compressed
        //value and dice it up randomly.  It's critical to make sure that
        //decompress works incrementally without blocking.
        QueryRouteTable dummy=new QueryRouteTable();
        setUncompressor(dummy, new Inflater());
        byte[] data=new byte[10000];
        Random rand=new Random();
        rand.nextBytes(data);
        for (int i=100; i<7000; i++) {
            data[i]=(byte)0;
        }
        byte[] dataCompressed= IOUtils.deflate(data);
        assertLessThan(data.length, dataCompressed.length);
    
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        for (int i=0; i<dataCompressed.length; ) {
            int length=Math.min(rand.nextInt(100), dataCompressed.length-i);
            byte[] chunk=new byte[length];
            System.arraycopy(dataCompressed, i, chunk, 0, length);
            byte[] chunkRead=invokeUncompress(dummy, chunk);
            baos.write(chunkRead);
            i+=length;
        }
        baos.flush();
        assertEquals("Compress/uncompress loop failed", data, baos.toByteArray());

        setUncompressor(dummy, null);
    }
    
    public void testHalveAndUnhalve() throws Exception {
        //0.1. halve/unhalve
        assertEquals(0x03, QueryRouteTable.extendNibble((byte)0x03));
        assertEquals((byte)0xF9, QueryRouteTable.extendNibble((byte)0x09));
        byte[] big={(byte)1, (byte)7, (byte)-1, (byte)-8};
        byte[] small={(byte)0x17, (byte)0xF8};
        assertTrue(Arrays.equals(QueryRouteTable.halve(big), small));
        assertTrue(Arrays.equals(QueryRouteTable.unhalve(small), big));
    }
    
    public void testEntries() throws Exception {
        QueryRouteTable qrt=new QueryRouteTable(1000);
        qrt.add("good book");
        assertEquals(2,entries(qrt));
        qrt.add("bad");   //{good, book, bad}
        assertEquals(3,entries(qrt));
        qrt.add("bad");   //{good, book, bad}
        assertEquals(3,entries(qrt));
        //{good, book, bad, SHA1}
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        assertEquals(4,entries(qrt));


        //1. Simple keyword tests (add, contains)
        //we have moved to 1-bit entry per hash, so either absent or present....
        assertTrue(! qrt.contains(
                queryRequestFactory.createQuery("garbage",(byte)4)));
        assertTrue(qrt.contains(queryRequestFactory.createQuery("bad", (byte)2)));
        assertTrue(qrt.contains(queryRequestFactory.createQuery("bad", (byte)3)));
        assertTrue(qrt.contains(queryRequestFactory.createQuery("bad", (byte)4)));
        assertTrue(qrt.contains(
                queryRequestFactory.createQuery("good bad", (byte)2)));
        assertTrue(! qrt.contains(
                queryRequestFactory.createQuery("good bd", (byte)3)));
        assertTrue(qrt.contains(queryRequestFactory.createQuery(
                                                  "good bad book", (byte)3)));
        assertTrue(! qrt.contains(queryRequestFactory.createQuery(
                                                    "good bad bok", (byte)3)));
        assertTrue(qrt.contains(
                queryRequestFactory.createQuery(UrnHelper.UNIQUE_SHA1)));
    }
    
    public void testAddAll() throws Exception {
        // set up initial qrt.
        QueryRouteTable qrt = new QueryRouteTable(1000);
        qrt.add("good book");
        qrt.add("bad");
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        
        //2. addAll tests
        QueryRouteTable qrt2=new QueryRouteTable(1000);
        assertEquals(0,entries(qrt2));
        qrt2.add("new");
        qrt2.add("book");
        qrt2.addAll(qrt);     //{book, good, new, bad, SHA1}
        assertEquals(5, entries(qrt2));

        QueryRouteTable qrt3=new QueryRouteTable(1000);
        qrt3.add("book");
        qrt3.add("good");
        qrt3.add("new");
        qrt3.add("bad");
        qrt3.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        assertEquals(qrt2,qrt3);
        assertEquals(qrt3,qrt2);
    }
    
    public void testEncodeAndDecode() throws Exception {
        // set up initial qrt.
        QueryRouteTable qrt = new QueryRouteTable(1000);
        qrt.add("good book");
        qrt.add("bad");
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());

        //3. encode-decode test--with compression
        //qrt={good, book, bad}
        QueryRouteTable qrt2=new QueryRouteTable(1000);
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            if(m instanceof PatchTableMessage) {
                qrt2.patch((PatchTableMessage)m); 
            } else {
                qrt2.reset((ResetTableMessage)m);
            }
            if (m instanceof PatchTableMessage)
                assertTrue(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_DEFLATE);
        }
        assertEquals(qrt2, qrt);

        qrt.add("bad");
        qrt.add("other"); //qrt={good, book, bad, other}
        assertNotEquals(qrt2,(qrt));
        for (Iterator iter=qrt.encode(qrt2).iterator(); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            if(m instanceof PatchTableMessage) {
                qrt2.patch((PatchTableMessage)m); 
            } else {
                qrt2.reset((ResetTableMessage)m);
            }
            if (m instanceof PatchTableMessage)
                assertEquals(PatchTableMessage.COMPRESSOR_DEFLATE,
                             ((PatchTableMessage)m).getCompressor());
        }
        assertEquals(qrt2,qrt);
        assertEquals(entries(qrt),entries(qrt2));

        Iterator iter=qrt2.encode(qrt).iterator();
        assertTrue(! iter.hasNext());                     //test optimization

        iter=(new QueryRouteTable(1000).encode(null).iterator()); //blank table
        assertInstanceof(ResetTableMessage.class, iter.next());
        assertTrue(! iter.hasNext());
    }
    
    public void testEncodeAndDecodeNoCompression() throws Exception {
        //4. encode-decode test--without compression.  (We know compression
        //won't work because the table is very small and filled with random 
        //bytes.)
        QueryRouteTable qrt=new QueryRouteTable(10);
        Random rand=new Random();
        for (int i=0; i<getBitTableLength(qrt); i++) 
            if (rand.nextBoolean())
                getBitTable(qrt).set(i);
        getBitTable(qrt).set(0);
        QueryRouteTable qrt2=new QueryRouteTable(10);
        assertNotEquals(qrt2,qrt);

        for (Iterator iter=qrt.encode(qrt2).iterator(); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            if(m instanceof PatchTableMessage) {
                try { 
                    qrt2.patch((PatchTableMessage)m); 
                } catch (BadPacketException e) {
                }
            } else {
                qrt2.reset((ResetTableMessage)m);
            }
            if (m instanceof PatchTableMessage)
                assertEquals(PatchTableMessage.COMPRESSOR_NONE,
                ((PatchTableMessage)m).getCompressor());
        }
        assertEquals(qrt2,(qrt));
    }
    
    public void testEncodeAndDecodeMultiplePatches() throws Exception {
        //4b. Encode/decode tests with multiple patched messages.
        QueryRouteTable qrt=new QueryRouteTable(5000);
        Random rand=new Random();
        for (int i=0; i<getBitTableLength(qrt); i++)
            if (rand.nextBoolean())
                getBitTable(qrt).set(i);
        QueryRouteTable qrt2=new QueryRouteTable(5000);
        assertNotEquals(qrt2,(qrt));

        for (Iterator iter=qrt.encode(qrt2).iterator(); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            if(m instanceof PatchTableMessage) {
                try { 
                    qrt2.patch((PatchTableMessage)m); 
                } catch (BadPacketException e) {
                }
            } else {
                qrt2.reset((ResetTableMessage)m);
            }
        }
        assertEquals(qrt2,(qrt));
        
        //4c. Encode/decode tests with multiple patched messages, 
        //    and a different infinity.
        qrt = new QueryRouteTable(5000, (byte)15);
        rand=new Random();
        for (int i=0; i<getBitTableLength(qrt); i++)
            if (rand.nextBoolean())
                getBitTable(qrt).set(i);
        qrt2 = new QueryRouteTable(5000);
        assertNotEquals(qrt2,(qrt));

        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            if(m instanceof PatchTableMessage) {
                try { 
                    qrt2.patch((PatchTableMessage)m); 
                } catch (BadPacketException e) {
                }
            } else {
                qrt2.reset((ResetTableMessage)m);
            }
        }
        assertEquals(qrt2,(qrt));        
    }
    
    public void testDifferentInfinities() throws Exception {
        //Simple test -- make sure different infinities don't change
        //equality.
        QueryRouteTable qrt = new QueryRouteTable(5000, (byte)7);
        qrt.add("good");
        qrt.add("bad");
        qrt.add("book");
        
        QueryRouteTable qrt2 = new QueryRouteTable(5000, (byte)15);
        qrt2.add("good");
        qrt2.add("bad");
        qrt2.add("book");
        
        assertEquals(qrt, qrt2);
    }
    
    public void testInterpolationAndExtrapolation() throws Exception {
        //5. Interpolation/extrapolation glass-box tests.  Remember that +1 is
        //added to everything!
        QueryRouteTable qrt=new QueryRouteTable(4);  // 1 4 5 X ==> 2 6
        QueryRouteTable qrt2=new QueryRouteTable(2);
        getBitTable(qrt).set(0);
        getBitTable(qrt).set(1);
        getBitTable(qrt).set(2);
        getBitTable(qrt).clear(3);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(0));
        assertTrue(getBitTable(qrt2).get(1));

        //This also tests tables with different TTL problem.  (The 6 is qrt
        //is interepreted as infinity in qrt2, not a 7.)
        qrt=new QueryRouteTable(2);  // 1 X ==> 2 2 X X
        qrt2=new QueryRouteTable(4);
        getBitTable(qrt).set(0);
        getBitTable(qrt).clear(1);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(0));
        assertTrue(getBitTable(qrt2).get(1));
        assertTrue(!getBitTable(qrt2).get(2));
        assertTrue(!getBitTable(qrt2).get(3));

        qrt=new QueryRouteTable(4);  // 1 2 4 X ==> 2 3 5
        qrt2=new QueryRouteTable(3);
        getBitTable(qrt).set(0);
        getBitTable(qrt).set(1);
        getBitTable(qrt).set(2);
        getBitTable(qrt).clear(3);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(0));
        assertTrue(getBitTable(qrt2).get(1));
        assertTrue(getBitTable(qrt2).get(2));
        assertEquals(3, entries(qrt2));

        qrt=new QueryRouteTable(3);  // 1 4 X ==> 2 2 5 X
        qrt2=new QueryRouteTable(4);
        getBitTable(qrt).set(0);
        getBitTable(qrt).set(1);
        getBitTable(qrt).clear(2);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(0));
        assertTrue(getBitTable(qrt2).get(1));
        assertTrue(getBitTable(qrt2).get(2));
        assertTrue(!getBitTable(qrt2).get(3));
        assertEquals(3, entries(qrt2));
        
        qrt=new QueryRouteTable(100);
        qrt2=new QueryRouteTable(10);
        getBitTable(qrt).set(11);
        getBitTable(qrt).set(20);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(1));
        assertTrue(getBitTable(qrt2).get(2));
        assertEquals(2, entries(qrt2));        
    }
    
    public void testAddAllBlackBox() throws Exception {
        //5b. Black-box test for addAll.
        QueryRouteTable qrt=new QueryRouteTable(128);
        qrt.add("good book");
        qrt.add("bad");   //{good/1, book/1, bad/3}
        QueryRouteTable qrt2=new QueryRouteTable(512);
        qrt2.addAll(qrt);
        assertTrue(qrt2.contains(queryRequestFactory.createQuery("bad", (byte)4)));
        assertTrue(qrt2.contains(queryRequestFactory.createQuery("good", (byte)4)));
        qrt2=new QueryRouteTable(32);
        qrt2.addAll(qrt);
        assertTrue(qrt2.contains(queryRequestFactory.createQuery("bad", (byte)4)));
        assertTrue(qrt2.contains(queryRequestFactory.createQuery("good", (byte)4)));
    }
    
    public void testBadPackets() throws Exception {
        //6. Test sequence numbers.
        QueryRouteTable qrt=new QueryRouteTable();   //a. wrong sequence after reset
        ResetTableMessage reset=null;
        PatchTableMessage patch=new PatchTableMessage((short)2, (short)2,
            PatchTableMessage.COMPRESSOR_DEFLATE, (byte)8, new byte[10], 0, 10);
        try {
            qrt.patch(patch);
            fail("bpe should have been thrown - expecting sequence number 1");
        } catch (BadPacketException e) {
        }

        qrt=new QueryRouteTable();  //b. sequence sizes don't match
        reset=new ResetTableMessage(1024, (byte)2);
        qrt.reset(reset);
        patch=new PatchTableMessage((short)1, (short)3,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
        qrt.patch(patch);
        patch=new PatchTableMessage((short)2, (short)4,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
        try {
            qrt.patch(patch);
            fail("bpe should have been thrown - seq size changed btw patches");
        } catch (BadPacketException e) { 
        }

        qrt=new QueryRouteTable();  //c. missing sequence number 2
        patch=new PatchTableMessage((short)1, (short)3,
           PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
        qrt.patch(patch);
        patch=new PatchTableMessage((short)3, (short)3,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);        
        try {
            qrt.patch(patch);
            fail("bpe should have been thrown - missing sequence 2");
        } catch (BadPacketException e) {
        }        

        qrt=new QueryRouteTable();  //d. sequence interrupted by reset (is ok)
        patch=new PatchTableMessage((short)1, (short)3,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
        qrt.patch(patch);
        reset=new ResetTableMessage(1024, (byte)2);
        qrt.reset(reset);
        patch=new PatchTableMessage((short)1, (short)6,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
        qrt.patch(patch);

        qrt=new QueryRouteTable();  //e. More sequences than seq size sent
        patch=new PatchTableMessage((short)1, (short)2,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
        qrt.patch(patch);
        patch=new PatchTableMessage((short)2, (short)2,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
        qrt.patch(patch);
        patch=new PatchTableMessage((short)3, (short)2,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
        try {
            qrt.patch(patch);
            fail("bpe should have been thrown - seq size: 2, but 3 are sent");
        } catch (BadPacketException e) {
        }
        
        qrt=new QueryRouteTable(); //f. Unknown compress value
        patch=new PatchTableMessage((short)1, (short)2,
            (byte)0x2, (byte)8, new byte[10], 0, 10);
        try {
            qrt.patch(patch);
            fail("bpe should have been thrown -- unknown compressor");
        } catch(BadPacketException e) {
        }
        
        qrt=new QueryRouteTable(); //g. Unable to uncompress
        patch=new PatchTableMessage((short)1, (short)2,
            PatchTableMessage.COMPRESSOR_DEFLATE, (byte)8, new byte[10], 0, 10);
        try {
            qrt.patch(patch);
            fail("bpe should have been thrown -- invalid compressed data");
        } catch(BadPacketException e) {
        }
        
        qrt=new QueryRouteTable(); //h. Sending more data than table can hold
        reset=new ResetTableMessage(1024, (byte)2);
        qrt.reset(reset);
        patch=new PatchTableMessage((short)1, (short)6,
            PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[1025], 0, 1025);
        try {
            qrt.patch(patch);
            fail("bpe should have been thrown - patched more than table size");
        } catch(BadPacketException e) {
        }
        
        qrt=new QueryRouteTable(); //i. Unknown entryBits value.
        patch=new PatchTableMessage((short)1, (short)2,
            PatchTableMessage.COMPRESSOR_NONE, (byte)1, new byte[10], 0, 10);
        try {
            qrt.patch(patch);
            fail("bpe should have been thrown - invalid entry bits");
        } catch(BadPacketException e) {
        }
    }
}
