package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;

import junit.framework.Test;

import org.limewire.io.GGEP;
import org.limewire.util.BaseTestCase;

public class GGEPParserTest extends BaseTestCase {
    
    public GGEPParserTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GGEPParserTest.class);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testCreationValues() throws Exception {
        GGEPParser parser = new GGEPParser();
        assertNull(parser.getSecureGGEP());
        assertNull(parser.getNormalGGEP());
        assertEquals(-1, parser.getSecureStartIndex());
        assertEquals(-1, parser.getSecureEndIndex());
    }
    
    public void testSimpleGGEPParse() throws Exception {
        GGEP ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test1", "data");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNotNull(parser.getNormalGGEP());
        assertNull(parser.getSecureGGEP());
        assertEquals(-1, parser.getSecureStartIndex());
        assertEquals(-1, parser.getSecureEndIndex());
        
        GGEP read = parser.getNormalGGEP();
        assertTrue(read.hasKey("test"));
        assertEquals("data", read.getString("test1"));
        assertEquals(2, read.getHeaders().size());
    }
    
    public void testMultipleGGEPsMerged() throws Exception {
        GGEP ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test1", "data");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        
        ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test2", "data2");
        ggep.write(out);
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNotNull(parser.getNormalGGEP());
        assertNull(parser.getSecureGGEP());
        assertEquals(-1, parser.getSecureStartIndex());
        assertEquals(-1, parser.getSecureEndIndex());
        
        GGEP read = parser.getNormalGGEP();
        assertTrue(read.hasKey("test"));
        assertEquals("data", read.getString("test1"));
        assertEquals("data2", read.getString("test2"));
        assertEquals(3, read.getHeaders().size());        
    }
    
    public void testSecureGGEP() throws Exception {
        GGEP ggep = new GGEP(true);
        ggep.put("SB");
        ggep.put("other", "stuff");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNotNull(parser.getSecureGGEP());
        assertEquals(0, parser.getSecureStartIndex());
        assertEquals(out.size(), parser.getSecureEndIndex());
        assertNull(parser.getNormalGGEP());
        
        GGEP read = parser.getSecureGGEP();
        assertTrue(read.hasKey("SB"));
        assertEquals("stuff", read.getString("other"));
        assertEquals(2, read.getHeaders().size());               
    }
    
    public void testSecureGGEPWithNormal() throws Exception {
        GGEP ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test1", "data");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
 
        int sizeOne = out.size();
        
        ggep = new GGEP(true);
        ggep.put("SB");
        ggep.put("other", "stuff");
        ggep.write(out);
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNotNull(parser.getSecureGGEP());
        assertEquals(sizeOne, parser.getSecureStartIndex());
        assertEquals(out.size(), parser.getSecureEndIndex());
        assertNotNull(parser.getNormalGGEP());
        
        GGEP read = parser.getNormalGGEP();
        assertTrue(read.hasKey("test"));
        assertEquals("data", read.getString("test1"));
        assertEquals(2, read.getHeaders().size());
        
        read = parser.getSecureGGEP();
        assertTrue(read.hasKey("SB"));
        assertEquals("stuff", read.getString("other"));
        assertEquals(2, read.getHeaders().size());
    }
    
    public void testMergeAndSecure() throws Exception {
        GGEP ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test1", "data");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        
        ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test2", "data2");
        ggep.write(out);
        
        int sizeOne = out.size();
        
        ggep = new GGEP(true);
        ggep.put("SB");
        ggep.put("other", "stuff");
        ggep.write(out);
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNotNull(parser.getSecureGGEP());
        assertEquals(sizeOne, parser.getSecureStartIndex());
        assertEquals(out.size(), parser.getSecureEndIndex());
        assertNotNull(parser.getNormalGGEP());
        
        GGEP read = parser.getNormalGGEP();
        assertTrue(read.hasKey("test"));
        assertEquals("data", read.getString("test1"));
        assertEquals("data2", read.getString("test2"));
        assertEquals(3, read.getHeaders().size());
        
        read = parser.getSecureGGEP();
        assertTrue(read.hasKey("SB"));
        assertEquals("stuff", read.getString("other"));
        assertEquals(2, read.getHeaders().size());       
    }
    
    public void testDataAfterSecure() throws Exception {
        GGEP ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test1", "data");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        int sizeOne = out.size();
        
        ggep = new GGEP(true);
        ggep.put("SB");
        ggep.put("other", "stuff");
        ggep.write(out);
        int sizeTwo = out.size();
        
        ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test2", "data2");
        ggep.write(out);
        
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNotNull(parser.getSecureGGEP());
        assertEquals(sizeOne, parser.getSecureStartIndex());
        assertEquals(sizeTwo, parser.getSecureEndIndex());
        assertNotNull(parser.getNormalGGEP());
        
        GGEP read = parser.getNormalGGEP();
        assertTrue(read.hasKey("test"));
        assertEquals("data", read.getString("test1"));
        assertEquals(2, read.getHeaders().size());
        
        read = parser.getSecureGGEP();
        assertTrue(read.hasKey("SB"));
        assertEquals("stuff", read.getString("other"));
        assertEquals(2, read.getHeaders().size());           
    }
    
    public void testScansForGGEP() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[100]);
        
        GGEP ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test1", "data");
        ggep.write(out);
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNotNull(parser.getNormalGGEP());
        assertNull(parser.getSecureGGEP());
        assertEquals(-1, parser.getSecureStartIndex());
        assertEquals(-1, parser.getSecureEndIndex());
        
        GGEP read = parser.getNormalGGEP();
        assertTrue(read.hasKey("test"));
        assertEquals("data", read.getString("test1"));
        assertEquals(2, read.getHeaders().size());
    }
    
    public void testFailsGracefully() throws Exception {
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(new byte[100], 0);
        assertNull(parser.getNormalGGEP());
        assertNull(parser.getSecureGGEP());
        assertEquals(-1, parser.getSecureStartIndex());
        assertEquals(-1, parser.getSecureEndIndex());       
    }
    
    public void testMagicByteScrewsParsing() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[100]);
        out.write(GGEP.GGEP_PREFIX_MAGIC_NUMBER);
        
        GGEP ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test1", "data");
        ggep.write(out);
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNull(parser.getNormalGGEP());
        assertNull(parser.getSecureGGEP());
        assertEquals(-1, parser.getSecureStartIndex());
        assertEquals(-1, parser.getSecureEndIndex());
    }
    
    public void testMergingMustBeNextToEachOther() throws Exception {
        GGEP ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test1", "data");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        
        out.write(0);
        
        ggep = new GGEP(true);
        ggep.put("test");
        ggep.put("test2", "data2");
        ggep.write(out);
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(out.toByteArray(), 0);
        assertNotNull(parser.getNormalGGEP());
        assertNull(parser.getSecureGGEP());
        assertEquals(-1, parser.getSecureStartIndex());
        assertEquals(-1, parser.getSecureEndIndex());
        
        GGEP read = parser.getNormalGGEP();
        assertTrue(read.hasKey("test"));
        assertEquals("data", read.getString("test1"));
        assertEquals(2, read.getHeaders().size());                
    }
    
    
}
