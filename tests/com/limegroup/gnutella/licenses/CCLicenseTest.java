package com.limegroup.gnutella.licenses;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;

import junit.framework.Test;

import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.http.httpclient.SimpleLimeHttpClient;
import org.limewire.inject.Providers;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.bootstrap.TestBootstrapServer;

public final class CCLicenseTest extends BaseTestCase {
    
    private static final String RDF_GOOD = 
"<rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
"   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
"   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
"  <Work rdf:about=\"urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD\">" +
"     <dc:date>2003</dc:date>" +
"     <dc:format>audio/mpeg</dc:format>" +
"     <dc:identifier>http://example.com/mp3s/test.mp3</dc:identifier>" +
"     <dc:rights><Agent><dc:title>ExampleBand</dc:title></Agent></dc:rights>" +
"     <dc:title>Example Song</dc:title>" +
"     <dc:type rdf:resource=\"http://purl.org/dc/dcmitype/Sound\" />" +
"     <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />" +
"  </Work>" +
"  <License rdf:about=\"http://creativecommons.org/licenses/by/2.0/\">" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
"     <prohibits rdf:resource=\"http://web.resource.org/cc/Fun\"/>" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Notice\" />" +
"  </License>" +
"</rdf:RDF>";
    
    private LicenseFactoryImpl licenseFactory;

    private LicenseCache licenseCache;
    
    private LimeHttpClient httpClient;

	public CCLicenseTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(CCLicenseTest.class);
	}

	/**
     * Runs this test individually.
     */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	@Override
	protected void setUp() throws Exception {
	    licenseCache = new LicenseCache();
	    licenseFactory = new LicenseFactoryImpl(Providers.of(licenseCache));
	    httpClient = new SimpleLimeHttpClient();
	}
	
	@Override
	protected void tearDown() throws Exception {
	}
	
	public void testBasicParsingRDF() throws Exception {
	    URN good = URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
	    URN bad = URN.createSHA1Urn("urn:sha1:SAMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
	    
	    License l = new StubCCLicense(RDF_GOOD);
	    assertFalse(l.isVerified());
	    assertFalse(l.isVerifying());
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(good));
	    assertFalse(l.isValid(bad));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toExternalForm());
        assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(good).toExternalForm());
        assertEquals(null, l.getLicenseDeed(bad));
	    // more stringent than necessary - asserting order too.
        String desc = "Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                  "Prohibited: Fun\n" +
	                  "Required: Attribution, Notice";
	    assertEquals(desc, l.getLicenseDescription(null));
	    assertEquals(desc, l.getLicenseDescription(good));
	    assertEquals("Permissions unknown.", l.getLicenseDescription(bad));
    }
    
    public void testGuessLicenseDeed() throws Exception {
        License l = new StubCCLicense("", "");
        assertFalse(l.isVerified());
        assertNull(l.getLicenseDeed(null));
        
        l = new StubCCLicense("a license, http://creativecommons.org/licenses/mylicese is cool", "");
        assertFalse(l.isVerified());
        assertEquals("http://creativecommons.org/licenses/mylicese", l.getLicenseDeed(null).toExternalForm());
        
        l = new StubCCLicense("a license, http:// creativecommons.org/licenses/mylicese", "");
        assertFalse(l.isVerified());
        assertNull(l.getLicenseDeed(null));
        
        l = new StubCCLicense("http://creativecommons.org/licenses/me", "");
        assertEquals("http://creativecommons.org/licenses/me", l.getLicenseDeed(null).toExternalForm());
        
        l = new StubCCLicense("alazamhttp://creativecommons.org/licenses/me2", "");
        assertNull(l.getLicenseDeed(null));
        
        l = new StubCCLicense("http://limewire.org", "");
        assertNull(l.getLicenseDeed(null));
        
        l = new StubCCLicense("creativecommons.org/licenses", "");
        assertNull(l.getLicenseDeed(null));
    }
    
    public void testCopy() throws Exception {
        License l = new StubCCLicense("text1", "");
        assertEquals("text1", l.getLicense());
        assertEquals("http://1.2.3.4/page", l.getLicenseURI().toString());
        
        License l2 = l.copy("text3", new URI("http://uri.com"));
        assertEquals("text3", l2.getLicense());
        assertEquals("http://uri.com", l2.getLicenseURI().toString());
    }
    
    public void testSerializeAndDeserialize() throws Exception {
        License l = new StubCCLicense("license text", "");
        assertEquals("license text", l.getLicense());
        assertNull(l.getLicenseDeed(null));
        assertEquals("http://1.2.3.4/page", l.getLicenseURI().toString());
        assertEquals("Permissions unknown.", l.getLicenseDescription(null));
        assertFalse(l.isVerified());
        assertFalse(l.isValid(null));
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(l);
        
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        l = (License)in.readObject();
        assertEquals(null, l.getLicense()); // CHANGE -- not serialized
        assertNull(l.getLicenseDeed(null));
        assertEquals(null, l.getLicenseURI()); // CHANGE -- not serialized
        assertEquals("Permissions unknown.", l.getLicenseDescription(null));
        assertTrue(l.isVerified()); // CHANGE -- becomes verified
        assertFalse(l.isValid(null));
        
        // Now try with a full out parsed License.
	    l = new StubCCLicense("good license text", RDF_GOOD);
	    l.verify(licenseCache, httpClient);
	    assertEquals("good license text", l.getLicense());
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toString());
	    // more stringent than necessary - asserting order too.
	    assertEquals("Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                 "Prohibited: Fun\n" +
	                 "Required: Attribution, Notice", l.getLicenseDescription(null));
        
        bout = new ByteArrayOutputStream();
        out = new ObjectOutputStream(bout);
        out.writeObject(l);
        
        bin = new ByteArrayInputStream(bout.toByteArray());
        in = new ObjectInputStream(bin);
        l = (License)in.readObject();
        assertEquals(null, l.getLicense()); // CHANGE -- not serialized
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toString());
	    // more stringent than necessary - asserting order too.
	    assertEquals("Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                 "Prohibited: Fun\n" +
	                 "Required: Attribution, Notice", l.getLicenseDescription(null));
    }
    
    public void testAdvancedRDFParsing() throws Exception {
        URN goodUrn = URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
        URN goodUrn2 = URN.createSHA1Urn("urn:sha1:GOODC5VEUDLTC26UT5W7GZBAKZHCY2MD");
	    URN badUrn = URN.createSHA1Urn("urn:sha1:BADBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
        
        // GOOD: within HTML comments.
        AbstractLicense l = new StubCCLicense("<html><--" +
"<rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
"   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
"   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
"  <Work>" +
"    <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />" +
"  </Work>" +
"  <License />" +
"</rdf:RDF>" +
"--></html>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toString());
	    assertEquals("Permissions unknown.", l.getLicenseDescription(null));
	    
	    // BAD: No data.
	    l = new StubCCLicense("<rdf:RDF/>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));

        // BAD: RDF not bound.
	    l = new StubCCLicense("<rdf:RDF><Work/></rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // BAD: Work, no license for it.
	    l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><Work/></rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // BAD: Work, Valid for specific URN, but no license for it.
	    l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD\"/></rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    assertFalse(l.isValid(goodUrn));
	    
	    // GOOD: Work, license.
	    l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work>" +
	    "<license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />"  +
	    "</Work></rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertTrue(l.isValid(badUrn));
	    
	    // GOOD: Work, license & URN.
	    l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"" + goodUrn + "\">" +
	    "<license rdf:resource=\"http://mydeed.com\" />" +
	    "</Work></rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertEquals("http://mydeed.com", l.getLicenseDeed(goodUrn).toExternalForm());
	    assertEquals("http://mydeed.com", l.getLicenseDeed(null).toExternalForm());
	    assertFalse(l.isValid(badUrn));
	    
	    // GOOD: Multiple works, licenses & URNs.
	    l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"" + goodUrn + "\">" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "</Work>" +
	    "<Work rdf:about=\"" + goodUrn2 + "\">" +
	    "<license rdf:resource=\"http://deed2.com\" />" +
	    "</Work>" +
	    "</rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertTrue(l.isValid(goodUrn2));
	    assertEquals("http://deed1.com", l.getLicenseDeed(goodUrn).toExternalForm());
	    assertEquals("http://deed2.com", l.getLicenseDeed(goodUrn2).toExternalForm());
	    assertFalse(l.isValid(badUrn));
	    
	    // No Work item.
        l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
        "<License/></rdf:RDF>");
        l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // Valid, duplicate distribution permission ignored.
	    l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work>" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "</Work>" +
	    "<License rdf:about=\"http://deed1.com\">" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
"  </License></rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("Permitted: Distribution, DerivativeWorks\n" +
	                 "Required: Attribution", l.getLicenseDescription(null));
	    
        // Valid, unknown License element.
	    l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work>" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "</Work>" +
	    "<License rdf:about=\"http://deed1.com\">" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <unknown rdf:resource=\"http://web.resource.org/cc/Unknown\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
"  </License></rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("Permitted: Distribution, DerivativeWorks\n" +
	                 "Required: Attribution", l.getLicenseDescription(null));
	                 
        // Matches up the licenses among the multiple works correctly.
	    // GOOD: Multiple works, licenses & URNs.
	    l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"" + goodUrn + "\">" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "</Work>" +
	    "<Work rdf:about=\"" + goodUrn2 + "\">" +
	    "<license rdf:resource=\"http://deed2.com\" />" +
	    "</Work>" +
        "<License rdf:about=\"http://deed1.com\">" +
        "     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
        "     <prohibits rdf:resource=\"http://web.resource.org/cc/Evil\" />" +
        "  </License>" +
        "<License rdf:about=\"http://deed2.com\">" +
        "     <requires rdf:resource=\"http://web.resource.org/cc/Peace\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Love\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Happiness\" />" +
        "  </License>" +     
	    "</rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertTrue(l.isValid(goodUrn2));
	    assertEquals("http://deed1.com", l.getLicenseDeed(goodUrn).toExternalForm());
	    assertEquals("http://deed2.com", l.getLicenseDeed(goodUrn2).toExternalForm());
	    assertFalse(l.isValid(badUrn));
	    assertEquals("Permitted: Distribution\n" +
	                 "Prohibited: Evil\n" +
	                 "Required: Attribution", l.getLicenseDescription(goodUrn));
        assertEquals("Permitted: Love, Happiness\n" +
                     "Required: Peace", l.getLicenseDescription(goodUrn2));
	                 
        // Valid, unknown body element.
        l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
        "<Work><license rdf:resource=\"http://deed1.com\" /></Work>" +
        "<Unknown/></rdf:RDF>");
        l.verify(licenseCache, httpClient);
        assertTrue(l.isVerified());
        assertTrue(l.isValid(null));
        
        // Invalid -- Work is inside an unknown element.
        l = new StubCCLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
        "<Unknown><Work><license rdf:resource=\"http://deed1.com\" /></Work></Unknown></rdf:RDF>");
        l.verify(licenseCache, httpClient);
        assertTrue(l.isVerified());
        assertFalse(l.isValid(null));
    }
    

    
    public void testMultipleLicenseElementsInWork() throws Exception {
        URN goodUrn = URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");        
        
	    AbstractLicense l = new StubCCLicense("http://creativecommons.org/licenses/mylicense verify at http://nowhere.com", 
	    "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"" + goodUrn + "\">" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "<license rdf:resource=\"http://creativecommons.org/licenses/mylicense\" />" +
	    "<license rdf:resource=\"http://deed2.com\" />" +
	    "</Work>" +
        "<License rdf:about=\"http://deed1.com\">" +
        "     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
        "     <prohibits rdf:resource=\"http://web.resource.org/cc/Evil\" />" +
        "  </License>" +
        "<License rdf:about=\"http://creativecommons.org/licenses/mylicense\">" +
        "     <requires rdf:resource=\"http://web.resource.org/cc/Peace\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Love\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Happiness\" />" +
        "  </License>" +     
	    "</rdf:RDF>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertEquals("http://creativecommons.org/licenses/mylicense", l.getLicenseDeed(goodUrn).toExternalForm());
        assertEquals("Permitted: Love, Happiness\n" +
                     "Required: Peace", l.getLicenseDescription(goodUrn));
    }        
    
    public void testHTTPRetrieval() throws Exception {
        URN good = URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
        URN bad = URN.createSHA1Urn("urn:sha1:SAMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");

        TestBootstrapServer server = new TestBootstrapServer(20181);
        try {
            server.setResponseData("<html><head>Hi</head><body><--\n"+
                                   RDF_GOOD + "\n--></body></html>");
            
            License l = licenseFactory.create("verify at http://127.0.0.1:20181/");
            l.verify(licenseCache, httpClient);
            assertTrue(l.isVerified());
    	    assertTrue(l.isValid(null));
    	    assertTrue(l.isValid(good));
    	    assertFalse(l.isValid(bad));
    	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(good).toExternalForm());
    	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toExternalForm());
    	    assertEquals(null, l.getLicenseDeed(bad));
    	    // more stringent than necessary - asserting order too.
    	    String desc = "Permitted: Reproduction, Distribution, DerivativeWorks\n" +
    	                  "Prohibited: Fun\n" +
    	                  "Required: Attribution, Notice";
    	    assertEquals(desc, l.getLicenseDescription(good));
            assertEquals(desc, l.getLicenseDescription(null));
            assertEquals("Permissions unknown.", l.getLicenseDescription(bad));
            assertEquals(1, server.getConnectionAttempts());
            assertEquals(1, server.getRequestAttempts());
        } finally {
            server.shutdown();
        }
    }
    
    public void testSeparateLicenseRetrieval() throws Exception {
        URN goodUrn1 = URN.createSHA1Urn("urn:sha1:GOOD15VEUDLTC26UT5W7GZBAKZHCY2MD");
        URN goodUrn2 = URN.createSHA1Urn("urn:sha1:GOOD25VEUDLTC26UT5W7GZBAKZHCY2MD");
        URN goodUrn3 = URN.createSHA1Urn("urn:sha1:GOOD35VEUDLTC26UT5W7GZBAKZHCY2MD");
        URN goodUrn4 = URN.createSHA1Urn("urn:sha1:GOOD45VEUDLTC26UT5W7GZBAKZHCY2MD");
        URN badUrn   = URN.createSHA1Urn("urn:sha1:BADAC5VEUDLTC26UT5W7GZBAKZHCY2MD");        

        TestBootstrapServer  deed1  = new TestBootstrapServer(11111);
        deed1.setAllowConnectionReuse(true);
        try {
            TestBootstrapServer  deed2  = new TestBootstrapServer(22222);
            deed2.setAllowConnectionReuse(true);
            try {
                TestBootstrapServer  deed3  = new TestBootstrapServer(33333);
                deed3.setAllowConnectionReuse(true);
                try {
                    TestBootstrapServer server1 = new TestBootstrapServer(44444);
                    try {
                        TestBootstrapServer server2 = new TestBootstrapServer(55555);
                        try {
                            String rdf =
"<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
    "<Work rdf:about=\"" + goodUrn1 + "\">" +
        "<license rdf:resource=\"http://127.0.0.1:11111\" />" +
    "</Work>" +
    "<Work rdf:about=\"" + goodUrn2 + "\">" +
        "<license rdf:resource=\"http://127.0.0.1:22222\" />" +
    "</Work>" +
    "<Work rdf:about=\"" + goodUrn3 + "\">" +
        "<license rdf:resource=\"http://127.0.0.1:33333\" />" +
    "</Work>" +    
    "<Work rdf:about=\"" + goodUrn4 + "\">" +
        "<license rdf:resource=\"http://deed4.com\" />" +
    "</Work>" +        
    "<License rdf:about=\"http://deed4.com\">" +
        "<requires rdf:resource=\"http://web.resource.org/cc/Peace\" />" +
        "<permits rdf:resource=\"http://web.resource.org/cc/Love\" />" +
        "<permits rdf:resource=\"http://web.resource.org/cc/Happiness\" />" +
    "</License>" +
"</rdf:RDF>";
                            server1.setResponseData(rdf);
                            server2.setResponseData(rdf);

                            String data =
    "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
    "<License rdf:about=\"http://127.0.0.1:11111\">" +
        "<requires rdf:resource=\"http://web.resource.org/cc/War\" />" +
        "<permits rdf:resource=\"http://web.resource.org/cc/Hate\" />" +
        "<permits rdf:resource=\"http://web.resource.org/cc/Evil\" />" +
    "</License></rdf:RDF>";
                            deed1.setResponseData(data);
                            deed1.setResponse("HTTP/1.1 200 OK\r\nContent-Length: " + data.length());
    
                            // throw in an extra work -- make sure it never is added.
                            data =
    "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
    "<Work rdf:about=\"" + badUrn + "\">" +
        "<license rdf:resource=\"http://deeddead.com\" />" +
    "</Work>" +        
    "<License rdf:about=\"http://127.0.0.1:22222\">" +
        "<requires rdf:resource=\"http://web.resource.org/cc/Joy\" />" +
        "<permits rdf:resource=\"http://web.resource.org/cc/Fun\" />" +
        "<prohibits rdf:resource=\"http://web.resource.org/cc/Sadness\" />" +
    "</License></rdf:RDF>";
                            deed2.setResponseData(data);
                            deed2.setResponse("HTTP/1.1 200 OK\r\nContent-Length: " + data.length());
        
                            data =
    "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
    "<License rdf:about=\"http://127.0.0.1:8080\">" + // wrong about!!
        "<requires rdf:resource=\"http://web.resource.org/cc/Beauty\" />" +
        "<permits rdf:resource=\"http://web.resource.org/cc/Models\" />" +
        "<prohibits rdf:resource=\"http://web.resource.org/cc/UglyPeople\" />" +
    "</License></rdf:RDF>";
                            deed3.setResponseData(data);
                            deed3.setResponse("HTTP/1.1 200 OK\r\nContent-Length: " + data.length());
    
                            License l = licenseFactory.create("verify at http://127.0.0.1:44444/");
                            l.verify(licenseCache, httpClient);
                            assertTrue(l.isVerified());
                            assertTrue(l.isValid(null));
                            assertTrue(l.isValid(goodUrn1));
                            assertTrue(l.isValid(goodUrn2));
                            assertTrue(l.isValid(goodUrn3));
                            assertTrue(l.isValid(goodUrn4));
                            assertFalse(l.isValid(badUrn));

                            assertEquals("http://127.0.0.1:11111", l.getLicenseDeed(goodUrn1).toExternalForm());
                            assertEquals("http://127.0.0.1:22222", l.getLicenseDeed(goodUrn2).toExternalForm());
                            assertEquals("http://127.0.0.1:33333", l.getLicenseDeed(goodUrn3).toExternalForm());
                            assertEquals("http://deed4.com", l.getLicenseDeed(goodUrn4).toExternalForm());
                            assertEquals(null, l.getLicenseDeed(badUrn));

                            // Alright -- these are the real tests.
                            // Make sure that the license was picked up correctly!
                            // (Order is Permit, Prohibit, Require)

                            assertEquals("Permitted: Hate, Evil\n" +
                                    "Required: War", l.getLicenseDescription(goodUrn1));

                            assertEquals("Permitted: Fun\n" +
                                    "Prohibited: Sadness\n" +
                                    "Required: Joy", l.getLicenseDescription(goodUrn2));

                            assertEquals("Permissions unknown.", l.getLicenseDescription(goodUrn3));

                            assertEquals("Permitted: Love, Happiness\n" +
                                    "Required: Peace", l.getLicenseDescription(goodUrn4));

                            assertEquals("Permissions unknown.", l.getLicenseDescription(badUrn));

                            assertEquals(1, server1.getConnectionAttempts());
                            assertEquals(1, server1.getRequestAttempts());
                            assertEquals(0, server2.getConnectionAttempts());
                            assertEquals(0, server2.getRequestAttempts());            
                            assertEquals(1, deed1.getConnectionAttempts());
                            assertEquals(1, deed1.getRequestAttempts());
                            assertEquals(1, deed2.getConnectionAttempts());
                            assertEquals(1, deed2.getRequestAttempts());
                            assertEquals(1, deed3.getConnectionAttempts());
                            assertEquals(1, deed3.getRequestAttempts());

                            // Okay -- now contact again for a new license.
                            // The details should already have been cached for 11111 & 22222,
                            // and even 33333 (even though it failed 'cause it had the wrong
                            // addr).

                            l = licenseFactory.create("verify at http://127.0.0.1:55555/");
                            l.verify(licenseCache, httpClient);
                            assertTrue(l.isVerified());
                            assertTrue(l.isValid(null));
                            assertTrue(l.isValid(goodUrn1));
                            assertTrue(l.isValid(goodUrn2));
                            assertTrue(l.isValid(goodUrn3));
                            assertTrue(l.isValid(goodUrn4));
                            assertFalse(l.isValid(badUrn));

                            assertEquals("http://127.0.0.1:11111", l.getLicenseDeed(goodUrn1).toExternalForm());
                            assertEquals("http://127.0.0.1:22222", l.getLicenseDeed(goodUrn2).toExternalForm());
                            assertEquals("http://127.0.0.1:33333", l.getLicenseDeed(goodUrn3).toExternalForm());
                            assertEquals("http://deed4.com", l.getLicenseDeed(goodUrn4).toExternalForm());
                            assertEquals(null, l.getLicenseDeed(badUrn));

                            // Alright -- these are the real tests.
                            // Make sure that the license was picked up correctly!
                            // (Order is Permit, Prohibit, Require)

                            assertEquals("Permitted: Hate, Evil\n" +
                                    "Required: War", l.getLicenseDescription(goodUrn1));

                            assertEquals("Permitted: Fun\n" +
                                    "Prohibited: Sadness\n" +
                                    "Required: Joy", l.getLicenseDescription(goodUrn2));

                            assertEquals("Permissions unknown.", l.getLicenseDescription(goodUrn3));

                            assertEquals("Permitted: Love, Happiness\n" +
                                    "Required: Peace", l.getLicenseDescription(goodUrn4));

                            assertEquals("Permissions unknown.", l.getLicenseDescription(badUrn));

                            assertEquals(1, server1.getConnectionAttempts());
                            assertEquals(1, server1.getRequestAttempts());
                            assertEquals(1, server2.getConnectionAttempts());
                            assertEquals(1, server2.getRequestAttempts());
                            assertEquals(1, deed1.getConnectionAttempts());
                            assertEquals(1, deed1.getRequestAttempts());
                            assertEquals(1, deed2.getConnectionAttempts());
                            assertEquals(1, deed2.getRequestAttempts());
                            assertEquals(1, deed3.getConnectionAttempts());
                            assertEquals(1, deed3.getRequestAttempts());

                        } finally {
                            server2.shutdown();
                        } 
                    } finally {
                        server1.shutdown();
                    } 
                } finally {
                    deed3.shutdown();
                }
            } finally {
                deed2.shutdown();
            }
        } finally {
            deed1.shutdown();
        }
    }
    
}