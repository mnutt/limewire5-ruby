package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.util.TestUtils;


/**
 * This class handles testing all methods of the urn class.  This test
 * needs to be run from either the core directory or the directory above
 * the core directory.
 */
public final class UrnTest extends com.limegroup.gnutella.util.LimeTestCase {
	
	private static final String [] VALID_URNS = {
		"urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB",
		"Urn:sha1:OLSTHIPQGSSZTS5FJUPAKTZWUGZQYPFB",
		"uRn:sHa1:JLRTHIPQGSSZTS5RJUPAKRZWUGYQYPFB",
		"urn:sha1:RLPTHIPQGSSZTS5FRUPAKEZWUGYQYPFB",
		"urn:Sha1:MLSTHIPQGSSZTS5FJRPAKWZWUGYQYPFB",
		"UrN:sha1:WLSTHIPQGSSZTS5FJURAKQZWUGYQYPFB",
		"urn:sHa1:ALSTIIPQGSSZTS5FJUPRKAZWUGYQYPFB",
		"urn:sha1:ZLSTXIPQGSSZTS5FJUPARCZWUGYQYPFB",
		"urn:sha1:PLSTTIPQGSSZTS5FJUPAKXZWUGYQYPFB",
		" urn:sHa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	};
	
	private static final String [] INVALID_URNS = {
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFBC",
		"urn:sh1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"ur:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"rn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urnsha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn::sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn: sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1 :PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1 :PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1: PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWU GYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWU GYQYPFB "
	};

	private static final String [] VALID_SHA1_URNS = {
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"Urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"uRn:sHa1:PLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:Sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"UrN:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sHa1:PLSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	};

	private static URN[] urns;
	private static URN[] sha1Urns;

	private final String [] VALID_URN_HTTP_STRINGS = {
		"/uri-res/N2R?urn:sha1:BLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"/uri-res/N2R?urn:sha1:BLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"/URI-RES/N2R?urn:sha1:WLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"/uri-res/n2R?urn:sha1:RLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"/uri-res/N2r?urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"/uri-res/n2r?urn:sha1:GLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		"/uri-res/N2R?UrN:sha1:LLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"/uri-res/N2R?urn:sHa1:VLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"/uri-res/N2R?urn:sha1:OLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		"/uri-res/N2R?urn:sha1:ULSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HtTP/1.0"
	};

	private final String [] VALID_URN_URI_RES_STRINGS = {
		"/uri-res/N2R?urn:sha1:BLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/uri-res/N2R?urn:sha1:BLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/URI-RES/N2R?urn:sha1:WLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/uri-res/n2R?urn:sha1:RLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/uri-res/N2r?urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/uri-res/n2r?urn:sha1:GLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/uri-res/N2R?UrN:sha1:LLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/uri-res/N2R?urn:sHa1:VLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/uri-res/N2R?urn:sha1:OLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"/uri-res/N2R?urn:sha1:ULSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
	};

	private final URN[] VALID_URNS_HTTP = new URN[VALID_URN_HTTP_STRINGS.length];

	private final URN[] VALID_URNS_URI_RES = new URN[VALID_URN_URI_RES_STRINGS.length];
	
	private final String [] invalidURNStrings = {
		"GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.2",
		"GET /urires/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		"/uri-es/N2R?urn:sha1:PLSTHIPQGSSZTS5FJcdirnZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.2",
		"GET /uri-res/N2Rurn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urn:sh1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		"GET/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urn:bitprint::PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		"GET /uri-res/N2R?urn:sha1::PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		"GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPF HTTP/1.0",
		"GET /uri-res/N2R?ur:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R? urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?  urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?                                                    "+
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urn:sha1: PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/ N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2Rurn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urnsha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urn:sa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0 ",
		" GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0 ",
		" ",
		"GET",
		"GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFBC HTTP/1.0",
	};

	private File _testDir;

	public UrnTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UrnTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() throws Exception {
		urns = new URN[VALID_URNS.length];
		for(int i=0; i<urns.length; i++) {
			urns[i] = URN.createSHA1Urn(VALID_URNS[i]);
			assertNotNull("urn should not be null",urns[i]);
			assertTrue("should be SHA1: '"+urns[i]+"'\r\n"+
					   urns[i].getUrnType(), urns[i].isSHA1());
			assertNotEquals("urn should not have the empty string", 
					   "", urns[i].toString());

		}
		sha1Urns = new URN[VALID_SHA1_URNS.length];
		for(int i=0; i<sha1Urns.length; i++) {
			sha1Urns[i] = URN.createSHA1Urn(VALID_SHA1_URNS[i]);
			assertNotNull("urn should not be null",sha1Urns[i]);
			assertNotEquals("urn should not have the empty string", 
					   "", sha1Urns[i].toString());
		}
		for(int i=0; i<VALID_URN_HTTP_STRINGS.length; i++) {
			try {
				VALID_URNS_HTTP[i] = URN.createSHA1UrnFromHttpRequest(VALID_URN_HTTP_STRINGS[i]);
			} catch(IOException e) {
			    fail( "could not create urns for URNTest setup", e);
			}
		}

		for(int i=0; i<VALID_URN_URI_RES_STRINGS.length; i++) {
			try {
				VALID_URNS_URI_RES[i] = URN.createSHA1UrnFromUriRes(VALID_URN_URI_RES_STRINGS[i]);
			} catch(IOException e) {
				fail("could not create urns for URNTest setup", e);
			}
		}

		_testDir = TestUtils.getResourceFile("com/limegroup/gnutella");
		assertTrue("should have been able to initialize gnutella dir", 
				   _testDir.isDirectory());
		File[] files = _testDir.listFiles();
		assertNotNull("test directory should contain files", files);
		assertGreaterThan("should have more than 10 files: "+_testDir, 10, files.length);
	}


	/**
	 * Tests that valid urn strings successfully construct new URN instances.
	 */
	public void testValidUrns() throws Exception {
		for(int i=0; i<VALID_URNS.length; i++) {
            URN.createSHA1Urn(VALID_URNS[i]);
		}
	}


	/**
	 * Tests the urn contructor that takes a string to make sure that invalid
	 * string inputs fail properly.
	 */
	public void testInvalidUrns() {
		for(int i=0; i<INVALID_URNS.length; i++) {
			try {
				URN.createSHA1Urn(INVALID_URNS[i]);
				fail("should have thrown an exception on: " + INVALID_URNS[i]);
			} catch(IOException e) {
			}
		}
	}

	/**
	 * Tests the URN constructor that takes a File instance.
	 */
	public void testUrnConstructionFromFiles() throws Exception {
		// TESTS FOR URN CONSTRUCTION FROM FILES, WITH SHA1 CALCULATION
		File[] testFiles = _testDir.listFiles();
		File curFile = null;
		for(int i=0; i<10; i++) {
			curFile = testFiles[i];
			if(!curFile.isFile()) {
				continue;
			}
			URN urn = URN.createSHA1Urn(curFile);
			assertTrue("should be a valid SHA1", urn.isSHA1());
			assertTrue("should be considered a urn", URN.isUrn(urn.toString()));
			assertEquals("should be == UrnTypes", urn.getUrnType(), URN.Type.SHA1);
            URN newURN = URN.createSHA1Urn(urn.toString());
            assertEquals("urns should be equal", urn, newURN);
		}
	}


	/**
	 * Test the constructor that constructs a URN from a URN HTTP request.
	 */
	public void testUrnHttpConstructor() {
		for(int i=0; i<VALID_URN_HTTP_STRINGS.length; i++) {
			try {
				URN.createSHA1UrnFromHttpRequest(VALID_URN_HTTP_STRINGS[i]);
			} catch(IOException e) {
				fail("construction of an URN from a valid get request failed",
						   e);
			}
		}
		for(int i=0; i<invalidURNStrings.length; i++) {
			try {
				URN.createSHA1UrnFromHttpRequest(invalidURNStrings[i]);
				fail("construction of a URN from an invalid get request succeeded: "+
						   invalidURNStrings[i]);
			} catch(IOException e) {
				continue;
			}			
		}
	}

	/**
	 * Test the URN constructor that takes only a file parameter.
	 */
	public void testCreateSHA1Urn() throws Exception {
		File[] files = _testDir.listFiles();
		for(int i=0; i<files.length; i++) {
			if(!files[i].isFile()) continue;
			try {
				URN.createSHA1Urn(files[i]);
			} catch(IOException e) {
				fail("could not create a SHA1 URN from a valid file", e);
			}
		}
	}

	/**
	 * Tests the isUrnType method.
	 */
	public void testIsUrnTypeMethod() {
		// TEST FOR isURNType method
		String[] validURNTypes = {
			"urn:",
			"urn:sha1:",
			"Urn:",
			"urn:Sha1:"
		};

		String[] invalidURNTypes = {
			"urn: sha1:",
			"urn::",
			"urn:sha2:",
			" urn:sha1",
			"rn:sha1",
			" "
		};
		
		for(int i=0; i<validURNTypes.length; i++) {			
			assertTrue("should be supported URNType", URN.Type.isSupportedUrnType(validURNTypes[i]));
		}

		for(int i=0; i<invalidURNTypes.length; i++) {
			assertTrue("should not be supported URNType", !URN.Type.isSupportedUrnType(invalidURNTypes[i]));
		}
	}


	/**
	 * Tests the hashCode method.
	 */
	public void testHashCode() throws Exception {
		int[] hashCodes = new int[VALID_URNS.length];
		for(int i=0; i<VALID_URNS.length; i++) {
            hashCodes[i] = URN.createSHA1Urn(VALID_URNS[i]).hashCode();
		}

		for(int i=0; i<hashCodes.length; i++) {
			int curCode = hashCodes[i];
			for(int j=0; j<hashCodes.length; j++) {
				if(i == j) continue;
				assertNotEquals("hashes of two different URNs should not be equal", curCode, hashCodes[j]);
			}
		}
	}

	/**
	 * Tests the equals method.
	 */
	public void testEquals()  throws Exception {
		URN curUrn;
		for(int i=0; i<urns.length; i++) {
			curUrn = urns[i];
			assertNotNull("current urn is unexpectedly null", curUrn);
			for(int j=0; j<urns.length; j++) {
				if(i == j) {
					URN tempUrn = URN.createSHA1Urn(urns[j].toString());
					assertEquals("urns should be equal", curUrn, tempUrn);
					continue;
				}
				else {
					assertNotEquals("urns are unexpectedly equal" +
							   "i: "+i+" j: "+j, 
							   curUrn, urns[j]);
				}
				assertNotNull("urn is unexpectedly null", urns[j]);
			}
		}
	}

	/**
	 * Tests the isSHA1 method.
	 */
	public void testIsSHA1Method() {
		for(int i=0; i<sha1Urns.length; i++) {
			assertTrue(sha1Urns[i].isSHA1());
		}
	}
	
	public void testCreateGUIDUrnFromString() throws IOException {
	    // test valid urns, we can reuse sha1 urns and just replace sha1 with guid
	    for (String sha1URN : VALID_URNS) {
	        String guidURN = sha1URN.toLowerCase(Locale.US).replace("sha1", "guid");
	        URN urn = URN.createGUIDUrn(guidURN);
	        assertNotNull(urn);
	        assertTrue(urn.isGUID());
	    }
	    
	    // invalid sha1 urns should also be invalid guid urns
	    for (String sha1URN : INVALID_URNS) {
	        String guidURN = sha1URN.toLowerCase(Locale.US).replace("sha1", "guid");
	        try {
	            URN urn = URN.createGUIDUrn(guidURN);
	            fail("urn should not have been created for invalid input: " + urn + " original: " + sha1URN);
	        } catch (IOException ie) {
	        }
	    }
	    
	    // valid sha1 urns should not be guid urns
	    for (String sha1URN : VALID_SHA1_URNS) {
            try {
                URN urn = URN.createGUIDUrn(sha1URN);
                fail("urn should not have been created for invalid input: " + urn + " original: " + sha1URN);
            } catch (IOException ie) {
            }
        }
	}
	
	public void testCreateGUIDUrnFromGUID() {
	    GUID guid = new GUID();
	    URN urn = URN.createGUIDUrn(guid);
	    assertTrue(urn.isGUID());
	    assertEquals(guid.toHexString(), urn.getNamespaceSpecificString());
	    assertEquals("urn:guid:" + guid.toHexString(), urn.toString());
	}
}
