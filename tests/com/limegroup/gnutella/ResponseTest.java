package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.core.settings.MessageSettings;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.ByteUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.filters.LocalIPFilter;
import com.limegroup.gnutella.filters.XMLDocFilterTest;
import com.limegroup.gnutella.filters.IPFilter.IPFilterCallback;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescFactory;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEPKeys;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;

/**
 * This class tests the Response class.
 */
@SuppressWarnings("unchecked")
// TODO should be renamed to response factory test maybe
public final class ResponseTest extends com.limegroup.gnutella.util.LimeTestCase {

	private QueryReplyFactory queryReplyFactory;
    private ResponseFactoryImpl responseFactoryImpl;
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    private AlternateLocationFactory alternateLocationFactory;
    private LimeXMLDocumentHelper limeXMLDocumentHelper;
    private Injector injector;
    private FileDescFactory fileDescFactory;
    private Mockery context;
    private RemoteFileDescFactory remoteFileDescFactory;
    private PushEndpointFactory pushEndpointFactory;
    
    /**
	 * Constructs a new test instance for responses.
	 */
	public ResponseTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ResponseTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	protected void setUp() throws Exception {
	    context = new Mockery();
	    injector = LimeTestUtils.createInjector();
	    queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
	    responseFactoryImpl = (ResponseFactoryImpl) injector.getInstance(ResponseFactory.class);
	    limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
	    limeXMLDocumentHelper = injector.getInstance(LimeXMLDocumentHelper.class);
	    alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
	    fileDescFactory = injector.getInstance(FileDescFactory.class);
	    remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
	    pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
	    
	    final CountDownLatch latch = new CountDownLatch(1);
	    injector.getInstance(LocalIPFilter.class).refreshHosts(new IPFilterCallback() {
            public void ipFiltersLoaded() {
                latch.countDown();
            }
        });
	    assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
	}


	/**
	 * Modified version of the unit test that was formerly embedded in
	 * the Response class.
	 */
	public void testLegacyResponseUnitTest() throws Exception {
        Response r = responseFactoryImpl.createResponse(3, 4096, "A.mp3", UrnHelper.SHA1);
        assertEquals("A.mp3", r.getName());
        assertNull(r.getDocument());

        //
        //Response r2 = new Response("",999,4,"blah.txt");
		//assertEquals("bad meta", null, r2.getMetaBytes());
		//assertEquals("Meta size not right", 0, r2.getMetaBytesSize());
        //Assert.that(r2.getMetaBytes()==null,"bad meta");
        //Assert.that(r2.getMetaBytesSize() == 0,"Meta size not right");
        //String md = "Hello";
        //Response r3 = new Response(md,999,4,"king.txt");
		//assertEquals("bad meta", null, r3.getMetaBytes());
		//assertEquals("Meta size not right", 0, r3.getMetaBytesSize());
        //Assert.that(r3.getMetaBytes()==null,"bad meta");
        //Assert.that(r3.getMetaBytesSize() == 0,"Meta size not right");
        //The three formats we support
		/*
        String[] meta = {"a kbps 44.1 kHz b","akbps 44.1 kHz b", 
                                             "b akbps 44.1kHz" };
        for(int i=0;i<meta.length;i++){
            Response r4 = new Response(meta[i],999+i,4,"abc.txt");
            LimeXMLDocument d=null;
            String xml = r4.getMetadata();
            try{
                d = new LimeXMLDocument(xml);
            }catch (Exception e){
				assertTrue("XML not created well from between nulls", false);
				//Assert.that(false,"XML not created well from between nulls");
            }
            String br = d.getValue("audios__audio__bitrate__");
			assertEquals("a", br);
            //Assert.that(br.equals("a"));
            String len = d.getValue("audios__audio__seconds__");
			assertEquals("b", len);
            //Assert.that(len.equals("b"));
        }
		*/
        //Tests for checking new LimeXMLDocument code added.
        String xml1 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Speech\" bitrate=\"192\"></audio></audios>";
        String xml2 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Speech\" bitrate=\"150\"></audio></audios>";
        
        //create documents.
        LimeXMLDocumentFactory factory = limeXMLDocumentFactory;
        LimeXMLDocument d1 = null;
        LimeXMLDocument d2 = null;
        d1 = factory.createLimeXMLDocument(xml1);
        d2 = factory.createLimeXMLDocument(xml2);
        Response ra = responseFactoryImpl.createResponse(12, 231, "def1.txt", d1, UrnHelper.SHA1);
        Response rb = responseFactoryImpl.createResponse(13, 232, "def2.txt", d2, UrnHelper.SHA1);
		assertEquals("problem with doc constructor", d1, ra.getDocument());
		assertEquals("problem with doc constructor", d2, rb.getDocument());
	}
	
	/**
	 * Tests a simple response can be created from stream (no extensions)
	 */
	public void testSimpleCreateFromStream() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    // write out closing null.
	    baos.write((byte)0);
	    // add on 16 bytes so it thinks a GUID is there.
	    baos.write( new byte[16] );
	    baos.flush();
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    for(int i = 0; i < 16; i++) // read the blank GUID stuff we added
	        assertEquals(0, in.read());
	    assertEquals("leftover input", -1, in.read());
    }
    
    /**
     * Tests multiple responses can be stacked together (no extensions)
     */
    public void testMultipleSimpleResponses() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    // RESPONSE 1.
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    // RESPONSE 2.
	    ByteUtils.int2leb(2181, baos);
	    ByteUtils.int2leb(1981, baos);
	    name = new byte[] { 's', '.', 'a', '.', 'b', 0 };
	    baos.write(name);	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    // add on 16 bytes so it thinks a GUID is there.
	    baos.write( new byte[16] );
	    baos.flush();
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    
	    r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 2181, r.getIndex());
	    assertEquals("wrong size", 1981, r.getSize());
	    assertEquals("wrong name", "s.a.b", r.getName());
	    
	    for(int i = 0; i < 16; i++) // read the blank GUID stuff we added
	        assertEquals(0, in.read());
	    assertEquals("leftover input", -1, in.read());
    }
    
    /**
     * Tests a response can have some data between the nulls (garbage)
     */
    public void testGarbageExtension() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    baos.write("this is a garbage extension".getBytes());
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    assertEquals("wrong extension",
	        "this is a garbage extension", new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());        
	}
	
	/**
	 * Tests multiple extensions (garbage) can be added with the delimiter.
	 */
	public void testMultipleGarbageExtensions() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    baos.write("this is a garbage extension".getBytes());
	    baos.write((byte)0x1c);
	    baos.write("this is another garbage extension".getBytes());
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    assertEquals("wrong extension",
	        "this is a garbage extension\u001cthis is another garbage extension",
	        new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());        	    
    }

    /**
     * Tests that a URN can be read from a response.
     */
    public void testHUGEUrn() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    // add the sha1
	    baos.write(sha1.getBytes());
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    assertEquals("wrong extension", sha1, new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());
	    
	    Set urns = new HashSet();
	    URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
	    urns.add(urn);
	    assertEquals("wrong urns", urns, r.getUrns());
    }
    
    /**
     * Tests that HUGE can work even if seperated with chunk extensions.
     */
    public void testHUGEUrnWithOtherExtensions() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    // add the sha1
	    baos.write("junkie junk".getBytes());
	    baos.write((byte)0x1c);
	    baos.write(sha1.getBytes());
	    baos.write((byte)0x1c);
	    baos.write("jankie jank".getBytes());
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
        assertEquals("wrong extension",
	        "junkie junk\u001c"+sha1+"\u001cjankie jank",
	        new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());
	    
	    Set urns = new HashSet();
	    URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
	    urns.add(urn);
	    assertEquals("wrong urns", urns, r.getUrns());
    }
    
    /**
     * Tests that we understand bearshare1-style metadata
     */
    public void testBearShare1Extensions() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    baos.write("192 kbps 160".getBytes());
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    assertEquals("wrong extension",
	        "192 kbps 160",
	        new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());
	    
	    LimeXMLDocument doc = r.getDocument();
	    assertNotNull("should have document", doc);	    
	    assertEquals("wrong name",
	        "sam", doc.getValue("audios__audio__title__"));
	    assertEquals("wrong bitrate",
	        "192", doc.getValue("audios__audio__bitrate__"));
        assertEquals("wrong length",
            "160", doc.getValue("audios__audio__seconds__"));
    }
    
    /**
     * Tests that we understand bearshare2-style metadata
     */
    public void testBearShare2Extensions() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 'm', 'y', 0 };
	    baos.write(name);
	    
	    baos.write("256kbps something 3528".getBytes());
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sammy", r.getName());
	    assertEquals("wrong extension",
	        "256kbps something 3528",
	        new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());
	    
	    LimeXMLDocument doc = r.getDocument();
	    assertNotNull("should have document", doc);	    
	    assertEquals("wrong name",
	        "sammy", doc.getValue("audios__audio__title__"));
	    assertEquals("wrong bitrate",
	        "256", doc.getValue("audios__audio__bitrate__"));
        assertEquals("wrong length",
            "3528", doc.getValue("audios__audio__seconds__"));
    }
    
    /**
     * Tests that GGEP'd ALT extensions are read correctly.
     */
    public void testGGEPAltExtension() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    GGEP info = new GGEP(true);
	    // locations: 1.2.3.4:1, 4.3.2.1:2
	    byte[] alts = { 1, 2, 3, 4, 1, 0, 4, 3, 2, 1, 2, 0 };
	    info.put(GGEPKeys.GGEP_HEADER_ALTS, alts);
	    info.write(baos);
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    // too annoying to check extension was correct.
	    assertEquals("leftover input", -1, in.read());
	    
	    assertEquals("wrong number of locations", 2, r.getLocations().size());
	    Set endpoints = new IpPortSet();
	    endpoints.add( new IpPortImpl("1.2.3.4", 1) );
	    endpoints.add( new IpPortImpl("4.3.2.1", 2) );
	    assertEquals("wrong alts", endpoints, r.getLocations());
	    assertEquals("should have no time", -1, r.getCreateTime());
    }
    
    /** Tests TLS is read in addition. */
    public void testReadGGEPAltWithTLSExtension() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        ByteUtils.int2leb(257, baos);
        ByteUtils.int2leb(1029, baos);
        byte[] name = new byte[] { 's', 'a', 'm', 0 };
        baos.write(name);
        
        GGEP info = new GGEP(true);
        // locations: 1.2.3.4:1, 4.3.2.1:2, 2.3.4.5:6, 5.4.3.2:7, 2.3.3.2:8
        byte[] alts = { 1, 2, 3, 4, 1, 0, 4, 3, 2, 1, 2, 0, 2, 3, 4, 5, 6, 0, 5, 4, 3, 2, 7, 0, 2, 3, 3, 2, 8, 0 };
        info.put(GGEPKeys.GGEP_HEADER_ALTS, alts);
        byte[] tlsIdx = { (byte)0xC8 }; // 11001
        info.put(GGEPKeys.GGEP_HEADER_ALTS_TLS, tlsIdx );        
        info.write(baos);
        
        // write out closing null.
        baos.write((byte)0);
        
        byte[] output = baos.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(output);
        Response r = responseFactoryImpl.createFromStream(in);
        assertEquals("wrong index", 257, r.getIndex());
        assertEquals("wrong size", 1029, r.getSize());
        assertEquals("wrong name", "sam", r.getName());
        // too annoying to check extension was correct.
        assertEquals("leftover input", -1, in.read());
        
        assertEquals("wrong number of locations", 5, r.getLocations().size());
        Set tls = new IpPortSet();
        Set nonTLS = new IpPortSet();
        for(IpPort ipp : r.getLocations()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                tls.add(ipp);
            else
                nonTLS.add(ipp);
        }
        // TLS should be 3, nonTLS == 2
        assertEquals(3, tls.size());
        assertEquals(2, nonTLS.size());
        Set endpoints = new IpPortSet();
        endpoints.add( new IpPortImpl("1.2.3.4", 1) );
        endpoints.add( new IpPortImpl("4.3.2.1", 2) );
        endpoints.add( new IpPortImpl("2.3.3.2", 8) );
        assertEquals(endpoints, tls);
        
        endpoints.clear();
        endpoints.add( new IpPortImpl("2.3.4.5", 6) );
        endpoints.add( new IpPortImpl("5.4.3.2", 7) );
        assertEquals(endpoints, nonTLS);
        
        assertEquals("should have no time", -1, r.getCreateTime());
    }
    
    /**
     * Tests that the GGEP'd CT extension is read correctly.
     */
    public void testGGEPCreateTimeExtension() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    GGEP info = new GGEP(true);
	    long time = System.currentTimeMillis();
	    info.put(GGEPKeys.GGEP_HEADER_CREATE_TIME, time / 1000);
	    info.write(baos);
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    // too annoying to check extension was correct.
	    assertEquals("leftover input", -1, in.read());
	    
	    time = time / 1000 * 1000; // we lose precision when sending.	    
	    assertEquals("wrong time", time, r.getCreateTime());
	    assertEquals("should have no locs", new HashSet(), r.getLocations());
    }
    
    /**
     * Test GGEP with multiple extensions.
     */
    public void testGGEPMultipleExtensions() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    GGEP info = new GGEP(true);
	    long time = System.currentTimeMillis();
	    info.put(GGEPKeys.GGEP_HEADER_CREATE_TIME, time / 1000);
	    // locations: 1.2.3.4:1, 4.3.2.1:2
	    byte[] alts = { 1, 2, 3, 4, 1, 0, 4, 3, 2, 1, 2, 0 };
	    info.put(GGEPKeys.GGEP_HEADER_ALTS, alts);
	    info.write(baos);
	    
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    // too annoying to check extension was correct.
	    assertEquals("leftover input", -1, in.read());
	    
	    time = time / 1000 * 1000; // we lose precision when sending.	    
	    assertEquals("wrong time", time, r.getCreateTime());
	    
	    assertEquals("wrong number of locations", 2, r.getLocations().size());
	    Set endpoints = new IpPortSet();
	    endpoints.add( new IpPortImpl("1.2.3.4", 1) );
	    endpoints.add( new IpPortImpl("4.3.2.1", 2) );
	    assertEquals("wrong alts", endpoints, r.getLocations());
    }        
        
    /**
     * Tests that GGEP can be both before & after other extensions.
     */
    public void testGGEPWithOtherExtensions() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";        
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteUtils.int2leb(257, baos);
	    ByteUtils.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    baos.write(sha1.getBytes());
	    
	    baos.write((byte)0x1c);
	    
	    GGEP info = new GGEP(true);
	    // locations: 1.2.3.4:1, 4.3.2.1:2
	    byte[] alts = { 1, 2, 3, 4, 1, 0, 4, 3, 2, 1, 2, 0 };
	    info.put(GGEPKeys.GGEP_HEADER_ALTS, alts);
	    info.write(baos);
	    
	    baos.write((byte)0x1c);
	    
	    baos.write("192 kbps 160".getBytes());	    
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = responseFactoryImpl.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    // too annoying to check extension was correct.
	    assertEquals("leftover input", -1, in.read());
	    
	    assertEquals("wrong number of locations", 2, r.getLocations().size());
	    Set endpoints = new IpPortSet();
	    endpoints.add( new IpPortImpl("1.2.3.4", 1) );
	    endpoints.add( new IpPortImpl("4.3.2.1", 2) );
	    assertEquals("wrong alts", endpoints, r.getLocations());
	    
	    Set urns = new HashSet();
	    URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
	    urns.add(urn);
	    assertEquals("wrong urns", urns, r.getUrns());
	    
	    LimeXMLDocument doc = r.getDocument();
	    assertNotNull("should have document", doc);	    
	    assertEquals("wrong name",
	        "sam", doc.getValue("audios__audio__title__"));
	    assertEquals("wrong bitrate",
	        "192", doc.getValue("audios__audio__bitrate__"));
        assertEquals("wrong length",
            "160", doc.getValue("audios__audio__seconds__"));	    
    }
    
    /**
     * Tests the GGEPUtil.addGGEP method to correctly write
     * the correct number of alts out, in the correct format.
     */
    public void testOnly10AltsAreWritten() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
        
        // First create a bunch of alts.
        AlternateLocationCollection alc =
            AlternateLocationCollection.create(urn);
        for(int i = 0; i < 20; i++) {
            AlternateLocation al = alternateLocationFactory.create("1.2.3." + i + ":1", urn);
            alc.add(al);
        }
        
        // Then get them as endpoints (should remove the extra alts).
        Set endpoints = getAsIpPorts(alc);
        assertEquals("didn't filter out extras", 10, endpoints.size());
        
        // Add them to the output stream as a GGEP block.
        ResponseFactoryImpl.GGEPContainer gc = new ResponseFactoryImpl.GGEPContainer(endpoints, -1, 0, null, false, null);
        addGGEP(baos, gc, 0);
        
        // See if we can correctly read the GGEP block.
        baos.flush();
        byte[] output = baos.toByteArray();
        GGEP alt = new GGEP(output, 0, null);
        Set headers = alt.getHeaders();
        assertEquals("wrong size", 1, headers.size());
        assertEquals("wrong header", "ALT", headers.iterator().next());
        byte[] data = alt.getBytes("ALT");
        assertEquals("wrong data length", 60, data.length);
        for(int i = 0; i < data.length; i+=6) {
            assertEquals(1, data[i]);
            assertEquals(2, data[i+1]);
            assertEquals(3, data[i+2]);
            // data[i+3] is the one that changed, and because
            // iterators may return values in any order,
            // it doesn't matter what it is, so long as everything
            // else is correct.
            assertEquals(1, data[i+4]);
            assertEquals(0, data[i+5]);
        }
    }
    
    public void testAltsAndTLSAreWritten() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
        
        // First create a bunch of alts.
        AlternateLocationCollection alc = AlternateLocationCollection.create(urn);
        for(int i = 0; i < 10; i++) {
            AlternateLocation al = alternateLocationFactory.create("1.2.3." + i + ":1", urn, i % 3 == 0);
            alc.add(al);
        }
        
        // Then get them as endpoints (should remove the extra alts).
        Set endpoints = getAsIpPorts(alc);
        assertEquals("didn't filter out extras", 10, endpoints.size());
        
        // Add them to the output stream as a GGEP block.
        ResponseFactoryImpl.GGEPContainer gc = new ResponseFactoryImpl.GGEPContainer(endpoints, -1, 0, null, false, null);
        addGGEP(baos, gc, 0);
        
        // See if we can correctly read the GGEP block.
        baos.flush();
        byte[] output = baos.toByteArray();
        GGEP alt = new GGEP(output, 0, null);
        Set headers = alt.getHeaders();
        assertEquals("wrong size", 2, headers.size());
        assertTrue("no alt!", headers.contains("ALT"));
        assertTrue("no tls!", headers.contains("ALT_TLS"));
        byte[] data = alt.getBytes("ALT");
        assertEquals("wrong data length", 60, data.length);
        for(int i = 0; i < data.length; i+=6) {
            assertEquals(1, data[i]);
            assertEquals(2, data[i+1]);
            assertEquals(3, data[i+2]);
            assertEquals((int)Math.floor(i / 6), data[i+3]);
            assertEquals(1, data[i+4]);
            assertEquals(0, data[i+5]);
        }
        data = alt.getBytes("ALT_TLS");
        assertEquals("wrong tls length", 2, data.length);
        assertEquals(new byte[] { (byte)0x92, (byte)0x40 }, data );
    }
    
    /**
     * Tests the GGEPUtil.getGGEP method.
     */
    public void testGGEPUtilGetGGEP() throws Exception {
        GGEP ggep = new GGEP(true);
        ResponseFactoryImpl.GGEPContainer container;
        long ctime;
        Set locs;
        byte[] data;
        
        data = new byte[20]; // not % 6.
        ggep.put("ALT", data);
        locs = responseFactoryImpl.getGGEP(ggep, 0).locations;
        assertNotNull("should never be null", locs);
        assertEquals("shouldn't have locs", 0, locs.size());
        
        data = new byte[18]; // multiple of 6, but all blank (invalid)
        ggep.put("ALT", data);
        locs = responseFactoryImpl.getGGEP(ggep, 0).locations;
        assertNotNull("should never be null", locs);
        assertEquals("shouldn't have locs", 0, locs.size());

        // multiple of 6, but same        
        byte[] d1 = {1, 2, 3, 4, 1, 0, 1, 2, 3, 4, 1, 0};
        ggep.put("ALT", d1);
        locs = responseFactoryImpl.getGGEP(ggep, 0).locations;
        assertNotNull("should never be null", locs);
        assertEquals("wrong number of locs", 1, locs.size());
        assertEquals("wrong endpoint", 0,
           IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:1"), (IpPort)locs.iterator().next()));

        // multiple of 6, diff            
        byte[] d2 = {1, 2, 3, 4, 1, 0, 1, 2, 3, 4, 2, 0};
        ggep.put("ALT", d2);
        locs = responseFactoryImpl.getGGEP(ggep, 0).locations;
        assertNotNull("should never be null", locs);
        assertEquals("wrong number of locs", 2, locs.size());
        Set eps = new IpPortSet();
        eps.add( new IpPortImpl("1.2.3.4:1") );
        eps.add( new IpPortImpl("1.2.3.4:2") );
        assertEquals("wrong endpoints", eps, locs);
        
        // ctime.
        ggep = new GGEP(true);
        ggep.put("CT", 5341L);
        ctime = responseFactoryImpl.getGGEP(ggep, 0).createTime;
        assertEquals(5341000, ctime);
        
        // alt & ctime.
        ggep.put("CT", 1243L);
        ggep.put("ALT", d2);
        container = responseFactoryImpl.getGGEP(ggep, 0);
        assertNotNull(container);
        assertNotNull(container.locations);
        assertEquals(2, container.locations.size());
        assertEquals(eps, container.locations);
        assertEquals(1243000, container.createTime);
        
        // invalid alt, valid ctime
        ggep.put("ALT", new byte[0]);
        ggep.put("CT", 3214);
        container = responseFactoryImpl.getGGEP(ggep, 0);
        assertEquals(0, container.locations.size());
        assertEquals(3214000, container.createTime);
        
        // invalid ctime, valid alt
        ggep.put("ALT", d2);
        // use 9 bytes (1 byte over the max for a long)
        ggep.put("CT", new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 } );
        container = responseFactoryImpl.getGGEP(ggep, 0);
        assertEquals(2, container.locations.size());
        assertEquals(eps, container.locations);
        assertEquals(-1, container.createTime);
    }
    
    public void testHashCode() {
        Response r1 = responseFactoryImpl.createResponse(0, 0, "name", UrnHelper.SHA1);
        Response r2 = responseFactoryImpl.createResponse(0, 1, "name", UrnHelper.SHA1);
        assertNotEquals(r1.hashCode(), r2.hashCode());
        
        Response r3 = responseFactoryImpl.createResponse(1, 0, "name", UrnHelper.SHA1);
        assertNotEquals(r1.hashCode(), r3.hashCode());
        assertNotEquals(r2.hashCode(), r3.hashCode());
        
        assertEquals(r1.hashCode(), responseFactoryImpl.createResponse(0, 0, "name", UrnHelper.SHA1).hashCode());
        
        // max int values
        r1 = responseFactoryImpl
                .createResponse(Integer.MAX_VALUE, Integer.MAX_VALUE, "name", UrnHelper.SHA1);
        r2 = responseFactoryImpl.createResponse(0, Integer.MAX_VALUE, "name", UrnHelper.SHA1);
        assertNotEquals(r1.hashCode(), r2.hashCode());
    }
    
    public void testIllegalFilenamesInInputStream() throws Exception {
        // illegal filename
        Response resp = responseFactoryImpl.createResponse(1, 2, "a;lksdflkfj../", UrnHelper.SHA1);
        assertResponseParsingFails(resp);
        assertResponseParsingFails(responseFactoryImpl.createResponse(1, 4545, "s;lkdf\n\n\n", UrnHelper.SHA1));
        assertResponseParsingFails(responseFactoryImpl.createResponse(1, 4545, "../../index.html HTTP/1.0\r\n\r\nfoobar.mp3", UrnHelper.SHA1));
        assertResponseParsingFails(responseFactoryImpl.createResponse(4545, 3454, "", UrnHelper.SHA1));
        assertResponseParsingFails(responseFactoryImpl.createResponse(1454, 3245, "dlksdf\r", UrnHelper.SHA1));
    }
    
    public void testLargeFiles() throws Exception {
        Response resp = responseFactoryImpl.createResponse(1, Constants.MAX_FILE_SIZE, "asdf", UrnHelper.SHA1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.writeToStream(baos);
        byte [] data = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Response read = responseFactoryImpl.createFromStream(bais);
        assertEquals(Constants.MAX_FILE_SIZE, read.getSize());
        
        // also check the data manually
        
        // 1. the legacy size field should have all bits set
        for (int i = 4; i < 8; i++)
            assertEquals(-1, data[i]);
        
        // find where the ggep starts
        int firstNull = 8;
        // skip to first 0x0 to find extension bytes
        while(data[firstNull++] != 0x0);
        // inside extension bytes skip over sha1, to ggep
        while(data[firstNull++] != 0x1c);
        GGEP g = new GGEP(data, firstNull);
        assertEquals(Constants.MAX_FILE_SIZE, g.getLong(GGEPKeys.GGEP_HEADER_LARGE_FILE));
        
        // if the file is too large, we do not construct
        try {
            resp = responseFactoryImpl.createResponse(1, Constants.MAX_FILE_SIZE + 1, "asdf", UrnHelper.SHA1);
            fail("constructed too large file");
        } catch (IllegalArgumentException expected){}
        
        // we don't parse responses from network either
        g = new GGEP(true);
        g.put(GGEPKeys.GGEP_HEADER_LARGE_FILE,Constants.MAX_FILE_SIZE + 1);
        baos = new ByteArrayOutputStream();
        baos.write(data,0, firstNull);
        g.write(baos);
        baos.write(0x0);
        bais = new ByteArrayInputStream(baos.toByteArray());
        try {
            responseFactoryImpl.createFromStream(bais);
            fail("read a response with too large file");
        } catch (IOException expected){}
    }
    
    public void testIntervals() throws Exception {
        // response with two urns
        UrnSet set = new UrnSet();
        set.add(UrnHelper.SHA1);
        set.add(UrnHelper.TTROOT);
        
        // a verifying file with some stuff verified
        VerifyingFileFactory vfactory = injector.getInstance(VerifyingFileFactory.class);
        VerifyingFile vf = vfactory.createVerifyingFile(1024*1024);
        Range r = Range.createRange(0,1024 * 101 - 1);
        IntervalSet verified = new IntervalSet();
        verified.add(r);
        PrivilegedAccessor.setValue(vf,"verifiedBlocks",verified);
        
        
        IncompleteFileDesc ifd =
            fileDescFactory.createIncompleteFileDesc(new File("a"),set,11,"a",1024 * 1024, vf);
        Response resp = responseFactoryImpl.createResponse(ifd);
        assertTrue(resp.getUrns().contains(UrnHelper.SHA1));
        assertTrue(resp.getUrns().contains(UrnHelper.TTROOT));
        IntervalSet s = resp.getRanges();
        assertEquals(1,s.getNumberOfIntervals());
        assertEquals(1024 * 101, s.getSize());
        assertTrue(s.contains(r));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.writeToStream(baos);
        byte [] data = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Response read = responseFactoryImpl.createFromStream(bais);
        s = read.getRanges();
        assertEquals(1,s.getNumberOfIntervals());
        assertEquals(1024 * 101, s.getSize());
        assertTrue(s.contains(r));
        assertTrue(read.isVerified());
    }
    
    public void testUnverifiedIntervals() throws Exception {
        // response with two urns
        UrnSet set = new UrnSet();
        set.add(UrnHelper.SHA1);
        set.add(UrnHelper.TTROOT);
        
        // a verifying file with some stuff verified
        VerifyingFileFactory vfactory = injector.getInstance(VerifyingFileFactory.class);
        VerifyingFile vf = vfactory.createVerifyingFile(1024*1024);
        Range r = Range.createRange(0,1024 * 101 - 1);
        IntervalSet partial = new IntervalSet();
        partial.add(r);
        PrivilegedAccessor.setValue(vf,"partialBlocks",partial);
        
        
        IncompleteFileDesc ifd =
            fileDescFactory.createIncompleteFileDesc(new File("a"),set,11,"a",1024 * 1024, vf);
        Response resp = responseFactoryImpl.createResponse(ifd);
        assertTrue(resp.getUrns().contains(UrnHelper.SHA1));
        assertTrue(resp.getUrns().contains(UrnHelper.TTROOT));
        IntervalSet s = resp.getRanges();
        assertEquals(1,s.getNumberOfIntervals());
        assertEquals(1024 * 101, s.getSize());
        assertTrue(s.contains(r));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.writeToStream(baos);
        byte [] data = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Response read = responseFactoryImpl.createFromStream(bais);
        s = read.getRanges();
        assertEquals(1,s.getNumberOfIntervals());
        assertEquals(1024 * 101, s.getSize());
        assertTrue(s.contains(r));
        assertFalse(read.isVerified());
    }
    
    public void testTTROOTInGGEP() throws Exception {
        MessageSettings.TTROOT_IN_GGEP.setValue(true);
        // response with two urns
        UrnSet set = new UrnSet();
        set.add(UrnHelper.SHA1);
        set.add(UrnHelper.TTROOT);
        File f = new File("a") {
            @Override
            public long length() {
                return 1;
            }
        };
        FileDesc fd = fileDescFactory.createFileDesc(f, set, 1);
        Response resp = responseFactoryImpl.createResponse(fd);
        assertTrue(resp.getUrns().contains(UrnHelper.SHA1));
        assertTrue(resp.getUrns().contains(UrnHelper.TTROOT));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.writeToStream(baos);
        byte [] data = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Response read = responseFactoryImpl.createFromStream(bais);
        assertTrue(read.getUrns().contains(UrnHelper.SHA1));
        assertFalse(read.getUrns().contains(UrnHelper.TTROOT));

        // go through the data, make sure its not in HUGE
        String s = StringUtils.getASCIIString(data).toLowerCase();
        assertFalse(s.toLowerCase().contains("ttroot"));
    }

    public void testTTROOTInHUGE() throws Exception {
        MessageSettings.TTROOT_IN_GGEP.setValue(false);
        // response with two urns
        UrnSet set = new UrnSet();
        set.add(UrnHelper.SHA1);
        set.add(UrnHelper.TTROOT);
        File f = new File("a") {
            @Override
            public long length() {
                return 1;
            }
        };
        FileDesc fd = fileDescFactory.createFileDesc(f, set, 1);
        Response resp = responseFactoryImpl.createResponse(fd);
        assertTrue(resp.getUrns().contains(UrnHelper.SHA1));
        assertTrue(resp.getUrns().contains(UrnHelper.TTROOT));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.writeToStream(baos);
        byte [] data = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Response read = responseFactoryImpl.createFromStream(bais);
        assertTrue(read.getUrns().contains(UrnHelper.SHA1));
        assertFalse(read.getUrns().contains(UrnHelper.TTROOT));

        // go through the data, make sure its in HUGE
        String s = StringUtils.getASCIIString(data);
        assertTrue(s.toLowerCase().contains("ttroot"));
    }
    
    /**
     * Ensures the given address is used when creating the rfd.
     */
    public void testUsesGivenAddressForRemoteFileDesc() throws Exception {
        Response response = responseFactoryImpl.createResponse(100, 200, "hello", UrnHelper.SHA1);
        Address address = context.mock(Address.class);
        final QueryReply queryReply = context.mock(QueryReply.class);
        context.checking(new Expectations() {{
            one(queryReply).getClientGUID();
            will(returnValue(GUID.makeGuid()));
            one(queryReply).getSpeed();
            will(returnValue(5));
            one(queryReply).getSupportsChat();
            will(returnValue(true));
            one(queryReply).calculateQualityOfService();
            will(returnValue(0));
            one(queryReply).getSupportsBrowseHost();
            will(returnValue(true));
            one(queryReply).isReplyToMulticastQuery();
            will(returnValue(false));
            one(queryReply).getVendor();
            will(returnValue("vendor"));
        }});
        RemoteFileDesc rfd = response.toRemoteFileDesc(queryReply, address, remoteFileDescFactory, pushEndpointFactory);
        assertSame(address, rfd.getAddress());
        context.assertIsSatisfied();
    }
    
    public void testIsMetaFileWithAllLocales() throws Exception {
        for (Locale locale : Locale.getAvailableLocales()) {
            Locale.setDefault(locale);
            Response response = new ResponseImpl(100, 100, "hello world.torrent", "hello world.torrent".getBytes("UTF-8").length,
                    null, null, null, 5000, null, null, false);
            assertTrue("Failed for locale: " + locale, response.isMetaFile());
        }
    }

    private void assertResponseParsingFails(Response r) throws Exception {
        QueryReply qr = XMLDocFilterTest.createReply(r, 5555, new byte[] { 127, 0, 0, 1 }, queryReplyFactory, limeXMLDocumentHelper);
        try {
            qr.getResultsArray();
            fail("Expected bad packet exception");
        }
        catch (BadPacketException bpe) {
        }
    }
    
    private Set getAsIpPorts(AlternateLocationCollection col)
      throws Exception {
        return (Set)PrivilegedAccessor.invokeMethod(responseFactoryImpl,
            "getAsIpPorts", new Object[] { col } );
    }
    
    private void addGGEP(OutputStream os, ResponseFactoryImpl.GGEPContainer gc, long size) throws Exception {
        PrivilegedAccessor.invokeMethod(responseFactoryImpl, "addGGEP",
            new Object[] { os, gc, size },
            new Class[] { OutputStream.class, ResponseFactoryImpl.GGEPContainer.class, long.class } );
    }
}
