package com.limegroup.gnutella.metadata;

import junit.framework.Test;

import com.limegroup.gnutella.metadata.audio.reader.WRMXML;
import com.limegroup.gnutella.util.LimeTestCase;

public final class WRMXMLTest extends LimeTestCase {
    
	public WRMXMLTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(WRMXMLTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
    // The XML should look something like:
    //<WRMHEADER>
    //    <DATA>
    //        <SECURITYVERSION>XXXX</SECURITYVERSION>
    //        <CID>XXXX</CID>
    //        <LAINFO>XXXX</LAINFO>
    //        <KID>XXXX</KID>
    //        <CHECKSUM>XXXX</CHECKSUM>
    //    </DATA>
    //    <SIGNATURE>
    //        <HASHALGORITHM type="XXXX"></HASHALGORITHM>
    //        <SIGNALGORITHM type="XXXX"></SIGNALGORITHM>
    //        <VALUE>XXXX</VALUE>
    //    </SIGNATURE>
    //</WRMHEADER> 
    
    public void testNormalParsing() {
        WRMXML data = new WRMXML(
            "<WRMHEADER>" +
                "<DATA>" + 
                    "<SECURITYVERSION>mySecurityVersion</SECURITYVERSION>" +
                    "<CID>myCID</CID>" +
                    "<LAINFO>myLAINFO</LAINFO>" +
                    "<KID>myKid</KID>" +
                    "<CHECKSUM>myChecksum</CHECKSUM>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"sha1\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"msdrm\"></SIGNALGORITHM>" +
                    "<VALUE>myValue</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        
        assertTrue(data.isValid());
        assertEquals("mySecurityVersion", data.getSecurityVersion());
        assertEquals("myCID", data.getCID());
        assertEquals("myLAINFO", data.getLAInfo());
        assertEquals("myKid", data.getKID());
        assertEquals("myChecksum", data.getChecksum());
        assertEquals("sha1", data.getHashAlgorithm());
        assertEquals("msdrm", data.getSignAlgorithm());
        assertEquals("myValue", data.getSignatureValue());
    }
    
    public void testOtherData() {
        WRMXML data = new WRMXML(
            "<WRMHEADER>" +
                "<DATA>" + 
                    "<SECURITYVERSION ignored=\"data\">mySecurityVersion</SECURITYVERSION>" +
                    "<CID>myCID</CID>" +
                    "<LAINFO>myLAINFO</LAINFO>" +
                    "<KID>myKid</KID>" +
                    "<CHECKSUM>myChecksum</CHECKSUM>" +
                    "<MyExtraElement attr=\"value\"/>" +
                "</DATA>" +
                "<STUFF><DATA>HELLO</DATA></STUFF>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"sha1\" info=\"alive\">Dead</HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"msdrm\"></SIGNALGORITHM>" +
                    "<VALUE>myValue</VALUE>" +
                    "<KEY>myKey</KEY>" + 
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        
        assertTrue(data.isValid());
        assertEquals("mySecurityVersion", data.getSecurityVersion());
        assertEquals("myCID", data.getCID());
        assertEquals("myLAINFO", data.getLAInfo());
        assertEquals("myKid", data.getKID());
        assertEquals("myChecksum", data.getChecksum());
        assertEquals("sha1", data.getHashAlgorithm());
        assertEquals("msdrm", data.getSignAlgorithm());
        assertEquals("myValue", data.getSignatureValue());
    }
    
    public void testMalformedXML() {
        WRMXML data = new WRMXML("asoihtoi4h4taiohb,mndt43lkntohbadofih1o;iht");
        assertFalse(data.isValid());
    }
    
    public void testNotValidData() {
        WRMXML data = new WRMXML(
            "<WRMHEADER>" +
                "<DATA>" + 
                    "<SECURITYVERSION ignored=\"data\">mySecurityVersion</SECURITYVERSION>" +
                    "<CID>myCID</CID>" +
                    "<LAINFO>myLAINFO</LAINFO>" +
                    "<KID>myKid</KID>" +
                    "<CHECKSUM>myChecksum</CHECKSUM>" +
                    "<MyExtraElement attr=\"value\"/>" +
                "</DATA>" +
                "<STUFF><DATA>HELLO</DATA></STUFF>" +
            "</WRMHEADER>"
        );
        
        assertFalse(data.isValid());
    }
    
    public void testBareMinimum() {
        WRMXML data = new WRMXML(
            "<WRMHEADER>" +
                "<DATA>" + 
                    "<LAINFO>myLAINFO</LAINFO>" +
                "</DATA>" +
                "<SIGNATURE>" +
                    "<HASHALGORITHM type=\"sha1\"></HASHALGORITHM>" +
                    "<SIGNALGORITHM type=\"msdrm\"></SIGNALGORITHM>" +
                    "<VALUE>myValue</VALUE>" +
                "</SIGNATURE>" +
            "</WRMHEADER>"
        );
        
        assertTrue(data.isValid());
        assertEquals(null, data.getSecurityVersion());
        assertEquals(null, data.getCID());
        assertEquals("myLAINFO", data.getLAInfo());
        assertEquals(null, data.getKID());
        assertEquals(null, data.getChecksum());
        assertEquals("sha1", data.getHashAlgorithm());
        assertEquals("msdrm", data.getSignAlgorithm());
        assertEquals("myValue", data.getSignatureValue());
    }
       
}
