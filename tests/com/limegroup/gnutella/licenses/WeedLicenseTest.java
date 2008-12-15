package com.limegroup.gnutella.licenses;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;

import junit.framework.Test;

import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.http.httpclient.SimpleLimeHttpClient;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.bootstrap.TestBootstrapServer;

public class WeedLicenseTest extends BaseTestCase {
    
    private LicenseCache licenseCache;
    private LicenseFactory licenseFactory;
    private LimeHttpClient httpClient;

    public WeedLicenseTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(WeedLicenseTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	@Override
	protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		licenseFactory = injector.getInstance(LicenseFactory.class);
	    licenseCache = new LicenseCache();
	    httpClient = new SimpleLimeHttpClient();
	}
	
	public void testBasicParsingXMLWorks() throws Exception {
	    AbstractLicense l = new StubWeedLicense("", "", xml(true, "An Artist", "A Title", "The Price"));
	    assertFalse(l.isVerified());
	    assertFalse(l.isVerifying());
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("http://weedshare.com/company/policies/summary_usage_rights.aspx",
	                 l.getLicenseDeed(null).toExternalForm());
        String desc = "Artist: An Artist\nTitle: A Title\nPrice: The Price";
	    assertEquals(desc, l.getLicenseDescription(null));
    }
    
    public void testCopy() throws Exception {
        License l = new StubWeedLicense("1", "2", "text");
        assertEquals(null, l.getLicense());
        assertEquals("http://www.weedshare.com/license/verify_usage_rights.aspx?versionid=2&contentid=1",
                     l.getLicenseURI().toString());
        
        License l2 = l.copy("text3", new URI("http://uri.com"));
        assertEquals(null, l2.getLicense());
        assertEquals("http://uri.com", l2.getLicenseURI().toString());
    }
    
    public void testSerializeAndDeserialize() throws Exception {
        License l = new StubWeedLicense("3", "4", "page");
        assertEquals(null, l.getLicense());
        assertEquals("http://weedshare.com/company/policies/summary_usage_rights.aspx",
                     l.getLicenseDeed(null).toString());
        assertEquals("http://www.weedshare.com/license/verify_usage_rights.aspx?versionid=4&contentid=3",
                     l.getLicenseURI().toString());
        assertEquals("Details unknown.", l.getLicenseDescription(null));
        assertFalse(l.isVerified());
        assertFalse(l.isValid(null));
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(l);
        
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        l = (License)in.readObject();
        assertEquals(null, l.getLicense()); // CHANGE -- not serialized (but unused anyway)
        assertEquals("http://weedshare.com/company/policies/summary_usage_rights.aspx", l.getLicenseDeed(null).toString());
        assertEquals(null, l.getLicenseURI()); // CHANGE -- not serialized
        assertEquals("Details unknown.", l.getLicenseDescription(null));
        assertTrue(l.isVerified()); // CHANGE -- becomes verified
        assertFalse(l.isValid(null));
        
        // Now try with a full out parsed License.
	    l = new StubWeedLicense("3", "4", xml(true, "Sammy B", "Blues In G", "$4.20"));
	    l.verify(licenseCache, httpClient);
	    assertEquals(null, l.getLicense());
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("http://weedshare.com/company/policies/summary_usage_rights.aspx", l.getLicenseDeed(null).toString());
	    assertEquals("Artist: Sammy B\nTitle: Blues In G\nPrice: $4.20", l.getLicenseDescription(null));
        
        bout = new ByteArrayOutputStream();
        out = new ObjectOutputStream(bout);
        out.writeObject(l);
        
        bin = new ByteArrayInputStream(bout.toByteArray());
        in = new ObjectInputStream(bin);
        l = (License)in.readObject();
        assertEquals(null, l.getLicense()); // CHANGE -- not serialized (but unused)
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("http://weedshare.com/company/policies/summary_usage_rights.aspx", l.getLicenseDeed(null).toString());
	    assertEquals("Artist: Sammy B\nTitle: Blues In G\nPrice: $4.20", l.getLicenseDescription(null));
    }
    
    private String xml(boolean valid,  String artist, String title, String price) {
        return "<WeedVerifyData>" + 
                "<Status>" + (valid ? "Verified" : "Unverified") + "</Status>" +
                "<Artist>" + artist + "</Artist>" +
	            "<Title>" + title + "</Title>" +
	            "<Price>" + price + "</Price>" +
                "</WeedVerifyData>";
    }
    
    public void testAdvancedXMLParsing() throws Exception {
        License l = new StubWeedLicense(
                "<WeedVerifyData>" + 
                "<Status>Verified</Status>" +
                "<Artist>Sam</Artist>" +
	            "<Title>Codes</Title>" +
	            "<Price>Free</Price>" +
	            "<Other>As In Beer</Other>" +
                "</WeedVerifyData>");
        l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("Artist: Sam\nTitle: Codes\nPrice: Free", l.getLicenseDescription(null));
	    
	    l = new StubWeedLicense("<WeedVerifyData/>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));

	    l = new StubWeedLicense(
	            "<NotWeedData>" +
	            "<Status>Verified</Status>" +
                "<Artist>Sam</Artist>" +
	            "<Title>Codes</Title>" +
	            "<Price>Free</Price>" +
	            "<Other>As In Beer</Other>" +
	            "</NotWeedData>");
	    l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
        l = new StubWeedLicense(
                "<WeedVerifyData>" + 
                "<Artist>Sam</Artist>" +
	            "<Title>Codes</Title>" +
	            "<Price>Free</Price>" +
	            "<Other>As In Beer</Other>" +
                "</WeedVerifyData>");
        l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null)); // no Status element
	    assertEquals("Artist: Sam\nTitle: Codes\nPrice: Free", l.getLicenseDescription(null));
	    
        l = new StubWeedLicense(
                "<WeedVerifyData>" + 
                "<Status>Mr.</Status>" +
                "<Artist>Sam</Artist>" +
	            "<Title>Codes</Title>" +
	            "<Price>Free</Price>" +
                "</WeedVerifyData>");
        l.verify(licenseCache, httpClient);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null)); // status isn't verified
	    assertEquals("Artist: Sam\nTitle: Codes\nPrice: Free", l.getLicenseDescription(null));
    }
    
    public void testHTTPRetrieval() throws Exception {
        TestBootstrapServer server = new TestBootstrapServer(20181);
        try {
            server.setResponseData(xml(true, "A", "B", "Free"));
            setLookupPage("http://127.0.0.1:20181/");
            License l = licenseFactory.create("http://www.shmedlic.com/license/3play.aspx cid: 34 vid: 78");
            l.verify(licenseCache, httpClient);
            assertTrue(l.isVerified());
    	    assertTrue(l.isValid(null));
    	    String desc = "Artist: A\nTitle: B\nPrice: Free";
            assertEquals(desc, l.getLicenseDescription(null));
            assertEquals(1, server.getConnectionAttempts());
            assertEquals(1, server.getRequestAttempts());
            assertEquals("GET /?versionid=78&contentid=34&data=1 HTTP/1.1", server.getRequest());
        } finally {
            server.shutdown();
        }
    }
    
    private void setLookupPage(String page) throws Exception {
        PrivilegedAccessor.setValue(WeedLicense.class, "URI", "http://127.0.0.1:20181/");
    }
}