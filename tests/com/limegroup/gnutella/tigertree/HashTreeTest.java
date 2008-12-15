package com.limegroup.gnutella.tigertree;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.util.Base32;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dime.DIMEGenerator;
import com.limegroup.gnutella.dime.DIMEParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.util.UUID;

/**
 * Unit tests for HashTree
 */
public class HashTreeTest extends BaseTestCase {
    
    private static final String filename = 
     "com/limegroup/gnutella/metadata/mpg4_golem160x90first120.avi";
    private static final File file = TestUtils.getResourceFile(filename);
    
    // urn & tigertree root from bitcollider
    private static final String sha1 = 
        "urn:sha1:UBJSGDTCVZDSBS4K3ZDQJV5VQ3WTBCOK";
    private static final String root32 =
        "IXVJNDJ7U3NCMZE5ZWBVCXSMWMFY4ZCXG5LUYAY";
        
    private static HashTree hashTree;
    private static HashTree treeFromNetwork;
    private static DIMERecord xmlRecord;
    private static DIMERecord treeRecord;
    private static byte[] written;

    public HashTreeTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HashTreeTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private HashTreeFactoryImpl tigerTreeFactory;
    private HashTreeNodeManager tigerTreeNodeManager;
    private HashTreeWriteHandlerFactory tigerWriteHandlerFactory;
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        tigerTreeFactory = (HashTreeFactoryImpl)injector.getInstance(HashTreeFactory.class);
        tigerTreeNodeManager = injector.getInstance(HashTreeNodeManager.class);
        tigerWriteHandlerFactory = injector.getInstance(HashTreeWriteHandlerFactory.class);
    }
    
    public void testLargeFile()  throws Exception {
    	URN urn = URN.createSHA1Urn(file);
    	try {
    	    tigerTreeFactory.createHashTree(1780149344l, new ByteArrayInputStream(new byte[0]), urn);
    		fail("shouldn't have read whole file");
    	}catch(IOException expected){}
    }
    
    //Due to the long setup time of creating a TigerTree,
    //these tests assign global variables as they go by.
    //These tests must all be run in the exact order they're
    //written so that they work correctly.

    public void testBasicTigerTree() throws Exception {
        assertTrue(file.exists());
        URN urn = URN.createSHA1Urn(file);
        assertEquals(sha1, urn.toString());
                
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            hashTree = tigerTreeFactory.createHashTree(file.length(), in, urn);
        } finally {
            in.close();
        }

        // most important test:
        // if we get the root hash right, the rest will be working, too
        assertEquals(root32, hashTree.getRootHash());
        assertEquals(4, hashTree.getDepth());
        assertTrue(hashTree.isGoodDepth());
        assertEquals("/uri-res/N2X?" + sha1, hashTree.getThexURI());
        assertEquals("/uri-res/N2X?" + sha1 + ";" + root32,
            hashTree.httpStringValue());

        List allNodes = tigerTreeNodeManager.getAllNodes(hashTree);
        assertEquals(5, allNodes.size());
        List one, two, three, four, five;
        one = (List)allNodes.get(0);
        two = (List)allNodes.get(1);
        three = (List)allNodes.get(2);
        four = (List)allNodes.get(3);
        five = (List)allNodes.get(4);
        assertEquals(five, hashTree.getNodes());
        
        // tree looks like:
        //                 u (root)
        //               /         \
        //             t            s
        //          /      \         \
        //        q         r         s
        //     /     \   /    \      /  \
        //    l      m   n     o    p    k
        //   /\     /\  /\    /\   /\     \  
        //  a b    c d e f   g h  i j      k
        
        assertEquals(root32, Base32.encode((byte[])one.get(0)));
        assertEquals(2, two.size());
        assertEquals(3, three.size());
        assertEquals(6, four.size());
        assertEquals(11, five.size());
        assertEquals(1+2+3+6+11, hashTree.getNodeCount());
    }
    
    public void testWriteToStream() throws Exception {
        
        HashTreeWriteHandler tigerWriteHandler = tigerWriteHandlerFactory.createTigerWriteHandler(hashTree);
        
        // Now make sure we can write this record out correctly.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tigerWriteHandler.write(baos);
        
        written = baos.toByteArray();
        assertEquals(written.length, tigerWriteHandler.getOutputLength());
        
        // Should be two DIME Records.
        ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
        DIMEParser parser = new DIMEParser(in);
        xmlRecord = parser.nextRecord();
        treeRecord = parser.nextRecord();
        assertTrue(!parser.hasNext());
        
        UUID uuid = verifyXML(xmlRecord);
        verifyTree(treeRecord, uuid);
    }
    
    public void testReadFromStream() throws Exception {
        HashTreeWriteHandler tigerWriteHandler = tigerWriteHandlerFactory.createTigerWriteHandler(hashTree);        
        
        // Make sure we can read the tree back in.
        treeFromNetwork = tigerTreeFactory.createHashTree(
            new ByteArrayInputStream(written), sha1,
                                     root32, file.length()
        );
        
        assertEquals(hashTree.getDepth(), treeFromNetwork.getDepth());
        assertEquals(hashTree.getRootHash(), treeFromNetwork.getRootHash());
        assertEquals(written.length, tigerWriteHandler.getOutputLength());
        
    }

    public void testVerifyChunk() throws Exception {
        File corrupt = new File("corruptFile");
        FileUtils.copy(file, corrupt);
        assertTrue(corrupt.exists());
        
        // Now corrupt the 4th chunk.
        int chunkSize = hashTree.getNodeSize();
        
        RandomAccessFile raf = new RandomAccessFile(corrupt, "rw");
        raf.seek(3*chunkSize+5);
        raf.write(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
        
        // chunks 1-3 are good
        byte [] chunk = new byte[chunkSize];
        for (int i = 0;i < 3*chunkSize ;i+=chunkSize) {
            raf.seek(i);
            raf.read(chunk);
            assertFalse(hashTree.isCorrupt(Range.createRange(i,i+chunkSize-1),chunk));
        }
        
        // the 4th is corrupt
        raf.seek(3*chunkSize);
        raf.read(chunk);
        assertTrue(hashTree.isCorrupt(Range.createRange(3*chunkSize,4*chunkSize-1),chunk));
        
        // 5th works
        raf.seek(4*chunkSize);
        raf.read(chunk);
        assertFalse(hashTree.isCorrupt(Range.createRange(4*chunkSize,5*chunkSize-1),chunk));

        raf.close();
        
        corrupt.delete();
    }
    
    public void testCorruptedXMLRecord() throws Exception {
        // Easiest way to test is to use existing DIMERecords and then
        // hack them up.
        DIMERecord corruptedXML = null;
        
        // must have valid data.
        String data = new String(xmlRecord.getData());
        corruptedXML = createCorruptRecord(xmlRecord, data.substring(1));
        try {
            createTree(corruptedXML, treeRecord);
            fail("expected exception");
        } catch(IOException expected) {}
        
        // all these must be correct or the stream is bad.
        checkXML("file size=", "02011981", false);
        checkXML("file size=", "abcd", false);
        checkXML("segmentsize=", "42", false);
        checkXML("segmentsize=", "zef", false);
        checkXML("digest algorithm=",
            "http://open-content.net/spec/digest/sha1", false);
        checkXML("outputsize=", "20", false);
        checkXML("outputsize=", "pizza", false);
        checkXML("type=", "http://open-content.net/spec/thex/depthfirst",
            false);

        // depth is not checked heavily.
        checkXML("depth=", "1982", true);
        checkXML("depth=", "large", true);
        
        // test shareaza's wrong system 
        replaceXML("SYSTEM", "system", true);
        // require that the main element is called hashtree
        replaceXML("hashtree>", "random>", false);
        // allow unknown additional elements
        replaceXML("<hashtree>", "<hashtree><element attribute=\"a\"/>", true);
        // allow elements to have random children.
        replaceXML("/></hashtree>",
            ">info</serializedtree></hashtree>", true);
    }
    
    private void checkXML(String search, String replace, boolean good)
      throws Exception {
        String data = new String(xmlRecord.getData());
        StringBuffer sb = new StringBuffer(data);
        int a = data.indexOf("'", data.indexOf(search));
        int b = data.indexOf("'", a+1);
        sb.replace(a+1, b, replace);
        DIMERecord corrupt = createCorruptRecord(xmlRecord, sb.toString());
        try {
            createTree(corrupt, treeRecord);
            if(!good)
                fail("expected exception");
        } catch(IOException expected) {
            if(good)
                throw expected;
        }
    }
    
    private void replaceXML(String search, String replace, boolean good)
      throws Exception {
        String data = new String(xmlRecord.getData());
        StringBuffer sb = new StringBuffer(data);
        int a = -1, b = -1;
        while(true) {
            a = data.indexOf(search, b+1);
            if(a == -1) break;
            b = search.length() + a;
            sb.replace(a, b, replace);
        }
        DIMERecord corrupt = createCorruptRecord(xmlRecord, sb.toString());
        try {
            createTree(corrupt, treeRecord);
            if(!good)
                fail("expected exception");
        } catch(IOException expected) {
            if(good)
                throw expected;
        }
    }    
    
    public void testCorruptedTreeData() throws Exception {
        byte[] data;
        
        // data is too small to fit a root hash.
        data = copyData(treeRecord, 3);
        checkTree(data, false);
        
        // the root hash is off.
        data = copyData(treeRecord, treeRecord.getData().length);
        data[0]++;
        checkTree(data, false);

        // random bytes in the data are off.
        data = copyData(treeRecord, treeRecord.getData().length);
        data[24 + 24*2 + 24*3 + 5]++;
        checkTree(data, false);
        
        // the last generation is off.
        data = copyData(treeRecord, treeRecord.getData().length);
        data[data.length-5]++;
        checkTree(data, false);
        
        // the root hash is correct, but no other data exists.
        // HashTreeHandler.HASH_SIZE==24
        data = copyData(treeRecord, 24);
        checkTree(data, true);
        
        // we have some full correct generations, but not all.
        data = copyData(treeRecord, 24 + 24*2);            
        checkTree(data, true);
        
        // we have correct data that stops in the middle of a generation.
        data = copyData(treeRecord, 24 + 24*2 + 24*1);
        checkTree(data, false);
        
        // the data isn't even a multiple of the hash size.
        data = copyData(treeRecord, 24 + 24*3 + 1);
        checkTree(data, false);
        
        // the data is longer than the ideal depth size would hold.
        data = copyData(treeRecord, treeRecord.getData().length + 24 * 2);
        checkTree(data, true);
    }
    
    private void checkTree(byte[] data, boolean good) throws Exception {
        DIMERecord corrupt = null;
        corrupt = createCorruptRecord(treeRecord, data);
        try {
            createTree(xmlRecord, corrupt);
            if(!good)
                fail("expected exception");
        } catch(IOException expected) {
            if(good)
                throw expected;
        }
    }
    
    private byte[] copyData(DIMERecord a, int length) {
        byte[] ret = new byte[length];
        for(int i = 0; i < ret.length && i < a.getData().length; i++)
            ret[i] = a.getData()[i];
        return ret;
    }
        
    
    private HashTree createTree(DIMERecord a, DIMERecord b) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DIMEGenerator gen = new DIMEGenerator();
        gen.add(a); gen.add(b);
        gen.write(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return tigerTreeFactory.createHashTree(in, sha1, root32, file.length());
    }
    
    private DIMERecord createCorruptRecord(DIMERecord base, byte[] data) {
        return new DIMERecord((byte)base.getTypeId(),
                                 base.getOptions(),
                                 base.getId(),
                                 base.getType(),
                                 data);
    }    
    
    private DIMERecord createCorruptRecord(DIMERecord base, String data) {
        return new DIMERecord((byte)base.getTypeId(),
                                 base.getOptions(),
                                 base.getId(),
                                 base.getType(),
                                 data.getBytes());
    }
    
    private UUID verifyXML(DIMERecord xml) throws Exception {
        // simple tests.
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, xml.getTypeId());
        assertEquals("text/xml", xml.getTypeString());
        assertEquals("", xml.getIdentifier());
        assertEquals(new byte[0], xml.getOptions());
        
        String data = new String(xml.getData(), "UTF-8");
        String current;
        String test = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<!DOCTYPE hashtree SYSTEM "
                    + "\"http://open-content.net/spec/thex/thex.dtd\">"
                    + "<hashtree>";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = "<file size='" + file.length() + "' ";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = "segmentsize='" + HashTreeUtils.BLOCK_SIZE + "'/>";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = "<digest algorithm='http://open-content.net/spec/digest/tiger'";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = " outputsize='24'/>";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = "<serializedtree depth='" + hashTree.getDepth() + "'";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = " type='http://open-content.net/spec/thex/breadthfirst'";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = " uri='uuid:";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        // the uri is the next 36 characters, grab it.
        current = data.substring(0, 36);
        data = data.substring(36);
        UUID uuid = new UUID(current);
        
        test = "'/></hashtree>";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        return uuid;
    }

    private void verifyTree(DIMERecord tree, UUID uuid) throws Exception {
        // simple tests.
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, tree.getTypeId());
        assertEquals("http://open-content.net/spec/thex/breadthfirst",
            tree.getTypeString());
        assertEquals("uuid:" + uuid.toString(), tree.getIdentifier());
        assertEquals(new byte[0], tree.getOptions());
        
        byte[] data = tree.getData();
        int offset = 0;
        List allNodes = tigerTreeNodeManager.getAllNodes(hashTree);
        for(Iterator genIter = allNodes.iterator(); genIter.hasNext(); ) {
            for(Iterator i = ((List)genIter.next()).iterator(); i.hasNext();) {
                byte[] current = (byte[])i.next();
                for(int j = 0; j < current.length; j++)
                    assertEquals(data[offset++], current[j]);
            }
        }
        assertEquals(data.length, offset);
    }
    
}
