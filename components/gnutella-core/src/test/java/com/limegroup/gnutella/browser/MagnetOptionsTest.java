package com.limegroup.gnutella.browser;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.EncodingUtils;

public class MagnetOptionsTest extends BaseTestCase {

    MagnetOptions[] validMagnets;
    private String[] guidMagnets;
    private Mockery context;
    
    public void setUp() {
        validMagnets = MagnetOptions.parseMagnet("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
        guidMagnets = new String[] {
                "magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=" + URN.createGUIDUrn(new GUID()),
                "magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=" + URN.createGUIDUrn(new GUID()),
                "magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&as=" + URN.createGUIDUrn(new GUID()),
                "magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=" + URN.createGUIDUrn(new GUID()),
        };
        context = new Mockery();
    }

	public MagnetOptionsTest(String name) {
		super(name);
	}
	
	public void testParseValidMagnet() {
		

		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?dn="
				+ EncodingUtils.encode("compilc.:fileNm?7ßä") + "&kt="
				+ EncodingUtils.encode("keyword topic string")
				+ "&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C");
		assertEquals("Should have parsed one magnet", 1, opts.length);
		opts = MagnetOptions.parseMagnet(opts[0].toString());
		assertEquals("Should have parsed one magnet", 1, opts.length);
		
		opts = MagnetOptions.parseMagnet("magnet:?dn=" + EncodingUtils.encode("compile me") 
		+ "&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO59&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO59");
		assertEquals("Should have parsed one magnet", 1, opts.length);
		opts = MagnetOptions.parseMagnet(opts[0].toString());
		assertEquals("Should have parsed one magnet", 1, opts.length);
				
		opts = MagnetOptions.parseMagnet("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
		assertEquals("Should have parsed 2 magnets", 2, opts.length);
		
		// compound magnets
		opts = MagnetOptions.parseMagnet("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
		assertEquals("Should have parsed 2 magnets", 2, opts.length);
	}
	
	public void testParseInvalidMagnet() {
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?");
		assertEquals("Wrong number of parsed magnets", 0, opts.length);
	}
	
	public void testisDownloadable() {
		
		// invalid magnets
		
		// is invalid because we don't have a url
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		
		// invalid: has empty kt
		opts = MagnetOptions.parseMagnet("magnet:?kt=&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		
		// invalid: has only a display name
		opts = MagnetOptions.parseMagnet("magnet:?dn=me");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		
		// valid magnets
		
		// valid: has a url and a sha1
		opts = MagnetOptions.parseMagnet("magnet:?xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
		// valid: has a url and keyword topic
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
		// valid: has everything
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		
		// downloadable: has kt and hash
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
		// downloadable: has dn and hash
		opts = MagnetOptions.parseMagnet("magnet:?dn=test&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
	}

	public void testIsHashOnly() {
		
		// hash only
		
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		assertTrue("Should be hash only", opts[0].isHashOnly());
		
		// not hash only
		
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		assertFalse("Should not be hash only", opts[0].isHashOnly());
		
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		assertFalse("Should not be hash only", opts[0].isHashOnly());
		assertTrue(opts[0].getGUIDUrns().isEmpty());
	}

	public void testGetFileNameForSaving() throws Exception {

		MagnetOptions magnet = MagnetOptions.createMagnet(null, "file name", null, null);
		assertEquals("file name", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet("keywords", null, null, null);
		assertEquals("keywords", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet("keywords", "", null, null);
		assertEquals("keywords", magnet.getFileNameForSaving());
		
		URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
		
		magnet = MagnetOptions.createMagnet(null, null, urn, null);
		assertEquals(urn.toString(), magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet("", "", urn, null);
		assertEquals(urn.toString(), magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet(null, null, null, new String[] { "http://www.limewire.com:4444/test.file" });
		assertEquals("test.file", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet(null, null, null, new String[] { "http://www.limewire.com:4444/test.file?notpartofthename" });
		assertEquals("test.file", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet("", "", null, new String[] { "http://192.168.0.2/test.file" });
		assertEquals("test.file", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet(null, null, null, new String[] { "http://host/" });
		assertEquals("MAGNET download from host", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet(null, null, null, null);
		assertNotNull(magnet.getFileNameForSaving());
	}
    
    public void testParseValidMagnets() {
        String magnets = createMultiLineMagnetLinks(validMagnets);
        MagnetOptions[] opts = MagnetOptions.parseMagnets(magnets);
        assertEquals("Should have parsed " + validMagnets.length + " magnets", validMagnets.length, opts.length);
        // and parse again
        assertEquals("Should have parsed " + opts.length + " magnets", opts.length, 
                MagnetOptions.parseMagnets(createMultiLineMagnetLinks(opts)).length);
        
        // compound magnets with multiple lines
        opts = MagnetOptions.parseMagnets
            ("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7"
                    + System.getProperty("line.separator")
                    + "magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
        assertEquals("Should have parsed 2 magnets", 4, opts.length);
    }
    
    public void testMagnetOptionsParsesGuidUrns() {
        String prefix = "magnet:?dn=filename&kt=keyword&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C";
        String guidUrn = "urn:guid:" + new GUID().toHexString();
        String magnet = prefix + "&xs=" + guidUrn;
        MagnetOptions opts = MagnetOptions.parseMagnet(magnet)[0];
        assertContains(opts.getXS(), guidUrn);
        
        // same for alternate sources
        magnet = prefix + "&as=" + guidUrn;
        opts = MagnetOptions.parseMagnet(magnet)[0];
        assertContains(opts.getAS(), guidUrn);
    }
    
    public void testGetGUIDUrns() {
        List<String> magnets = new ArrayList<String>(Arrays.asList(guidMagnets));
        // and add them as all upper case too
        for (String magnet : guidMagnets) {
            magnets.add(magnet.toUpperCase(Locale.US));
        }
        for (String magnet : magnets) {
            MagnetOptions options = MagnetOptions.parseMagnet(magnet)[0];
            Collection<URN> urns = options.getGUIDUrns();
            assertFalse(urns.isEmpty());
            for (URN urn : urns) {
                assertTrue(urn.isGUID());
            }
        }
        
        // magnets without GUID urns
        for (MagnetOptions magnet : validMagnets) {
            assertTrue(magnet.getGUIDUrns().isEmpty());
        }
    }
    
    public void testCreateMagnetWithGUIDUrn() throws Exception {
        final GUID guid = new GUID();
        final URN urn = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB");
        final FileDetails fileDetails = context.mock(FileDetails.class);
        context.checking(new Expectations() {{
            allowing(fileDetails).getFileName();
            will(returnValue("filename"));
            allowing(fileDetails).getSHA1Urn();
            will(returnValue(urn));
            // first time a zero file size
            one(fileDetails).getSize();
            will(returnValue(-1L));
            one(fileDetails).getSize();
            will(returnValue(123456789L));
        }});
        MagnetOptions magnet = MagnetOptions.createMagnet(fileDetails, null, guid.bytes());
        assertTrue(magnet.toExternalForm().contains(guid.toHexString()));
        assertEquals(urn, magnet.getSHA1Urn());
        assertEquals(URN.createGUIDUrn(guid), magnet.getGUIDUrns().iterator().next());
        assertFalse(magnet.toExternalForm().contains("xl"));
        assertEquals(-1, magnet.getFileSize());
        
        magnet = MagnetOptions.createMagnet(fileDetails, new InetSocketAddress("127.0.0.1", 5555), guid.bytes());
        assertTrue(magnet.toExternalForm().contains(guid.toHexString()));
        assertEquals(urn, magnet.getSHA1Urn());
        assertEquals(URN.createGUIDUrn(guid), magnet.getGUIDUrns().iterator().next());
        assertEquals(2, magnet.getXS().size());
        assertTrue(magnet.toExternalForm().contains("127.0.0.1:5555"));
        assertEquals(1, magnet.getDefaultURLs().length);
        assertTrue(magnet.toExternalForm().contains("xl=123456789"));
        assertEquals(123456789, magnet.getFileSize());
    }
    
    public void testCreateMagnetOptionsWithGuidUrns() throws Exception {
        GUID guid = new GUID();
        URN guidUrn = URN.createGUIDUrn(guid);
        URN sha1Urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        
        MagnetOptions magnet = MagnetOptions.createMagnet("hello", "fileName", sha1Urn, null, Collections.singleton(guidUrn));
        
        assertEquals(Collections.singleton(guidUrn), magnet.getGUIDUrns());
        assertTrue(magnet.toExternalForm().contains(guidUrn.httpStringValue()));
    }
     
    private String createMultiLineMagnetLinks(MagnetOptions[] opts) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < opts.length; i++) {
            if (i > 0) {
                buffer.append(System.getProperty("line.separator"));
            }
            buffer.append(opts[i].toString());
        }
        return buffer.toString();
    }    
}
