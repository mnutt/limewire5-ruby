package com.limegroup.gnutella.metadata;

import junit.framework.Test;

import com.limegroup.gnutella.metadata.audio.reader.WRMXML;
import com.limegroup.gnutella.metadata.audio.reader.WeedInfo;
import com.limegroup.gnutella.util.LimeTestCase;

public final class WeedInfoTest extends LimeTestCase {
    
	public WeedInfoTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(WeedInfoTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
    //The XML should look something like:
    //<WRMHEADER version="2.0.0.0">
    //    <DATA>
    //        <VersionID>0000000000001370651</VersionID>
    //        <ContentID>214324</ContentID>
    //        <ice9>ice9</ice9>
    //        <License_Date></License_Date>
    //        <License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>
    //        <License_Distributor>Shared Media Licensing, Inc.</License_Distributor>
    //        <Publish_Date>4/14/2005 4:13:50 PM</Publish_Date>
    //        <Content_Distributor_URL>http://www.presidentsrock.com</Content_Distributor_URL>
    //        <Content_Distributor>PUSA Inc.</Content_Distributor>
    //        <Price>0.9900</Price>
    //        <Collection>Love Everybody</Collection>
    //        <Description></Description>
    //        <Copyright>2004 PUSA Inc.</Copyright>
    //        <Artist_URL>http://www.presidentsrock.com</Artist_URL>
    //        <Author>The Presidents of the United States of America</Author>
    //        <Title>Love Everybody</Title>
    //        <SECURITYVERSION>2.2</SECURITYVERSION>
    //        <CID>o9miGn4Z0k2gUeHhN9VxTA==</CID>
    //        <LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>
    //        <KID>ERVOYkZ8qkWZ75OQw9ihnA==</KID>
    //        <CHECKSUM>t1ZpoYJF2w==</CHECKSUM>
    //    </DATA>
    //    <SIGNATURE>
    //        <HASHALGORITHM type="SHA"></HASHALGORITHM>
    //        <SIGNALGORITHM type="MSDRM"></SIGNALGORITHM>
    //        <VALUE>XZkWZWCq919yum!bBGdxvnpiS38npAqAofxT8AkegyJ27zTlb9v4gA==</VALUE>
    //    </SIGNATURE>
    //</WRMHEADER> 
    
    public void testNormalParsing() {
        WRMXML xml = new WRMXML(
            "<WRMHEADER version=\"2.0.0.0\">" +
                "<DATA>" +
                    "<VersionID>myVersionId</VersionID>" +
                    "<ContentID>myContentId</ContentID>" +
                    "<ice9>Cat's Cradle</ice9>" +
                    "<License_Date>myLicense Date</License_Date>" +
                    "<License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>" +
                    "<License_Distributor>Shared Media Licensing, Inc.</License_Distributor>" +
                    "<Publish_Date>myPublishDate</Publish_Date>" +
                    "<Content_Distributor_URL>myContentDistributorURL</Content_Distributor_URL>" +
                    "<Content_Distributor>My Content Distributor</Content_Distributor>" +
                    "<Price>Not Too Expensive</Price>" +
                    "<Collection>A Collection</Collection>" +
                    "<Description>A Description</Description>" +
                    "<Copyright>2004 Me.</Copyright>" +
                    "<Artist_URL>An Artist's URL</Artist_URL>" +
                    "<Author>The Author</Author>" +
                    "<Title>The Title</Title>" +
                    "<SECURITYVERSION>Secure!</SECURITYVERSION>" +
                    "<CID>asfoih</CID>" +
                    "<LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>" +
                    "<KID>hiofsa</KID>" +
                    "<CHECKSUM>ahsifo</CHECKSUM>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"SHA\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"MSDRM\"></SIGNALGORITHM>" +
                    "<VALUE>A Value</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        assertTrue(xml.isValid());
        
        WeedInfo weed = new WeedInfo(xml);
        assertTrue(weed.isValid());
        assertEquals("Secure!", weed.getSecurityVersion());
        assertEquals("asfoih", weed.getCID());
        assertEquals("http://www.shmedlic.com/license/3play.aspx", weed.getLAInfo());
        assertEquals("hiofsa", weed.getKID());
        assertEquals("ahsifo", weed.getChecksum());
        assertEquals("SHA", weed.getHashAlgorithm());
        assertEquals("MSDRM", weed.getSignAlgorithm());
        assertEquals("A Value", weed.getSignatureValue());
        assertEquals("myVersionId", weed.getVersionId());
        assertEquals("myContentId", weed.getContentId());
        assertEquals("Cat's Cradle", weed.getIce9());
        assertEquals("myLicense Date", weed.getLicenseDate());
        assertEquals("http://www.shmedlic.com/", weed.getLicenseDistributorURL());
        assertEquals("Shared Media Licensing, Inc.", weed.getLicenseDistributor());
        assertEquals("myPublishDate", weed.getPublishDate());
        assertEquals("myContentDistributorURL", weed.getContentDistrubutorURL());
        assertEquals("My Content Distributor", weed.getContentDistributor());
        assertEquals("Not Too Expensive", weed.getPrice());
        assertEquals("A Collection", weed.getCollection());
        assertEquals("A Description", weed.getDescription());
        assertEquals("2004 Me.", weed.getCopyright());
        assertEquals("An Artist's URL", weed.getArtistURL());
        assertEquals("The Author", weed.getAuthor());
        assertEquals("The Title", weed.getTitle());        
    }
    
    public void testBareMinimum() {
        WRMXML xml = new WRMXML(
            "<WRMHEADER version=\"2.0.0.0\">" +
                "<DATA>" +
                    "<VersionID>myVersionId</VersionID>" +
                    "<ContentID>myContentId</ContentID>" +
                    "<License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>" +
                    "<License_Distributor>Shared Media Licensing, Inc.</License_Distributor>" +
                    "<LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"SHA\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"MSDRM\"></SIGNALGORITHM>" +
                    "<VALUE>A Value</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        assertTrue(xml.isValid());
        
        WeedInfo weed = new WeedInfo(xml);
        assertTrue(weed.isValid());
        assertEquals(null, weed.getSecurityVersion());
        assertEquals(null, weed.getCID());
        assertEquals("http://www.shmedlic.com/license/3play.aspx", weed.getLAInfo());
        assertEquals(null, weed.getKID());
        assertEquals(null, weed.getChecksum());
        assertEquals("SHA", weed.getHashAlgorithm());
        assertEquals("MSDRM", weed.getSignAlgorithm());
        assertEquals("A Value", weed.getSignatureValue());
        assertEquals("myVersionId", weed.getVersionId());
        assertEquals("myContentId", weed.getContentId());
        assertEquals(null, weed.getIce9());
        assertEquals(null, weed.getLicenseDate());
        assertEquals("http://www.shmedlic.com/", weed.getLicenseDistributorURL());
        assertEquals("Shared Media Licensing, Inc.", weed.getLicenseDistributor());
        assertEquals(null, weed.getPublishDate());
        assertEquals(null, weed.getContentDistrubutorURL());
        assertEquals(null, weed.getContentDistributor());
        assertEquals(null, weed.getPrice());
        assertEquals(null, weed.getCollection());
        assertEquals(null, weed.getDescription());
        assertEquals(null, weed.getCopyright());
        assertEquals(null, weed.getArtistURL());
        assertEquals(null, weed.getAuthor());
        assertEquals(null, weed.getTitle());        
    }
    
    public void testRequirements() {
        // No versionID
        WRMXML xml = new WRMXML(
            "<WRMHEADER version=\"2.0.0.0\">" +
                "<DATA>" +
                    "<ContentID>myContentId</ContentID>" +
                    "<License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>" +
                    "<License_Distributor>Shared Media Licensing, Inc.</License_Distributor>" +
                    "<LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"SHA\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"MSDRM\"></SIGNALGORITHM>" +
                    "<VALUE>A Value</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        assertTrue(xml.isValid());
        
        WeedInfo weed = new WeedInfo(xml);
        assertFalse(weed.isValid());
        
        // No ContentID
        xml = new WRMXML(
            "<WRMHEADER version=\"2.0.0.0\">" +
                "<DATA>" +
                    "<VersionID>myVersionId</VersionID>" +
                    "<License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>" +
                    "<License_Distributor>Shared Media Licensing, Inc.</License_Distributor>" +
                    "<LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"SHA\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"MSDRM\"></SIGNALGORITHM>" +
                    "<VALUE>A Value</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        assertTrue(xml.isValid());
        
        weed = new WeedInfo(xml);
        assertFalse(weed.isValid());
        
        // Wrong URL
        xml = new WRMXML(
            "<WRMHEADER version=\"2.0.0.0\">" +
                "<DATA>" +
                    "<VersionID>myVersionId</VersionID>" +
                    "<ContentID>myContentId</ContentID>" +
                    "<License_Distributor_URL>http://www.somewhereelse.com/</License_Distributor_URL>" +
                    "<License_Distributor>Shared Media Licensing, Inc.</License_Distributor>" +
                    "<LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"SHA\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"MSDRM\"></SIGNALGORITHM>" +
                    "<VALUE>A Value</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        assertTrue(xml.isValid());
        
        weed = new WeedInfo(xml);
        assertFalse(weed.isValid());
        
        // Wrong distributor
        xml = new WRMXML(
            "<WRMHEADER version=\"2.0.0.0\">" +
                "<DATA>" +
                    "<VersionID>myVersionId</VersionID>" +
                    "<ContentID>myContentId</ContentID>" +
                    "<License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>" +
                    "<License_Distributor>Greedy Media Licensing, Inc.</License_Distributor>" +
                    "<LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"SHA\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"MSDRM\"></SIGNALGORITHM>" +
                    "<VALUE>A Value</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        assertTrue(xml.isValid());
        
        weed = new WeedInfo(xml);
        assertFalse(weed.isValid());
        
        // Wrong LAINFO
        xml = new WRMXML(
            "<WRMHEADER version=\"2.0.0.0\">" +
                "<DATA>" +
                    "<VersionID>myVersionId</VersionID>" +
                    "<ContentID>myContentId</ContentID>" +
                    "<License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>" +
                    "<License_Distributor>Shared Media Licensing, Inc.</License_Distributor>" +
                    "<LAINFO>http://www.shmedlic.com/license/4play.aspx</LAINFO>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"SHA\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"MSDRM\"></SIGNALGORITHM>" +
                    "<VALUE>A Value</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        assertTrue(xml.isValid());
        
        weed = new WeedInfo(xml);
        assertFalse(weed.isValid());
    }
}
