package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import junit.framework.Test;

import org.limewire.core.settings.SearchSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.ByteUtils;
import org.limewire.util.MediaType;
import org.limewire.util.OSUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.QueryRequestImpl.QueryRequestPayloadParser;

/**
 * This class tests the QueryRequest class with HUGE v0.94 extensions.
 */
@SuppressWarnings({"unchecked", "null"})
public final class QueryRequestTest extends LimeTestCase {
    
    private final String XML_STRING =
           "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"xml\" artist=\"bloat\"/></audios>";

    private static final String[] QUERY_STRINGS = {
    	"try this",
    	"file",
    	"different file",
    	"file.mp3",
    	"urn",
    	"urnsha1",
    	"good file",
    	"maybe this",
    	"this one too",
    	"how about this one",
    	"file.file",
    	"the best file ever",
    	"big file",
    	"little file",
    	"random file",
    	"first file",
    	"old file",
    	"new file"
    };

    private QueryRequestFactory queryRequestFactory;

    private MessageFactory messageFactory;
    
    private MACCalculatorRepositoryManager macManager;
	
	/**
	 * Constructs a new test instance for query requests.
	 */
	public QueryRequestTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(QueryRequestTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    
    @Override
    public void setUp() throws Exception {
        SearchSettings.DISABLE_OOB_V2.revertToDefault();
        OOBv2Disabled = false;
        Injector injector = LimeTestUtils.createInjector();
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        macManager = injector.getInstance(MACCalculatorRepositoryManager.class);
    }
    
    private boolean OOBv2Disabled;
    
	/**
	 * Tests the constructor that most of the other constructors are built
	 * off of.
	 */
	public void testQueriesFromNetworkWithGGEP() {
		try {
			ByteArrayOutputStream[] baos = 
			    new ByteArrayOutputStream[UrnHelper.URNS.length];
			for(int i=0; i<UrnHelper.URNS.length; i++) {
				baos[i] = new ByteArrayOutputStream();
				baos[i].write(0);
				baos[i].write(0);
				baos[i].write(StringUtils.toUTF8Bytes(QueryRequestTest.QUERY_STRINGS[i]));
				baos[i].write(0);
                // write the GGEP stuff
                byte[] bytes = new byte[4];
                (new Random()).nextBytes(bytes);
                AddressSecurityToken qk = new AddressSecurityToken(bytes, macManager);
                ByteArrayOutputStream qkBytes = new ByteArrayOutputStream();
                qk.write(qkBytes);
                GGEP ggepBlock = new GGEP(true); // do COBS
                ggepBlock.put(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT,
                              qkBytes.toByteArray());
                ggepBlock.write(baos[i]);
                baos[i].write(0x1c);
				baos[i].write(0);
				QueryRequest qr = null;
				try {
					qr = queryRequestFactory.createNetworkQuery(
					    GUID.makeGuid(), (byte)6, (byte)4, 
                        baos[i].toByteArray(), Network.UNKNOWN);
				} catch(BadPacketException e) {
                    e.printStackTrace();
					fail("should have accepted query: "+qr);
				}
				assertEquals("speeds should be equal", 0, qr.getMinSpeed());
				assertEquals("queries should be equal", 
							 QueryRequestTest.QUERY_STRINGS[i], qr.getQuery());
				Set queryUrns = qr.getQueryUrns();
                assertEquals("should not have any URNs", 0, queryUrns.size());

                assertEquals("query keys should be equal",
                             qk, qr.getQueryKey());
			}		   
		} catch(IOException e) {
			fail("unexpected exception: "+e);
		}
	}	

	/**
	 * Tests to make sure that queries with no query string, no xml, and no
	 * URNs are not accepted.
	 */
	public void testEmptyQueryNotAccepted() {
		try {
			queryRequestFactory.createQuery("");
			fail("exception should have been thrown");
		} catch(IllegalArgumentException e) {}		

		try {
			queryRequestFactory.createRequery("");
			fail("exception should have been thrown");
		} catch(IllegalArgumentException e) {}		
		try {
			queryRequestFactory.createQuery((String)null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}		
		try {
			queryRequestFactory.createQuery((URN)null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}		
		try {
			queryRequestFactory.createRequery((String)null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}		

		try {
			queryRequestFactory.createRequery((URN)null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}		
		try {
			queryRequestFactory.createRequery(null, (byte)3);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				


		try {
			queryRequestFactory.createQuery(null, "", "");
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				

		try {
			queryRequestFactory.createQuery(new byte[16], null, "");
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				

		try {
			queryRequestFactory.createQuery(new byte[16], "", null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				

		try {
			queryRequestFactory.createQuery(new byte[16], "", "");
			fail("exception should have been thrown");
		} catch(IllegalArgumentException e) {}	
			
		try {
			queryRequestFactory.createQuery(new byte[15], "test", "");
			fail("exception should have been thrown");
		} catch(IllegalArgumentException e) {}				

		try {
			queryRequestFactory.createQuery("test", null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				

		try {
			queryRequestFactory.createMulticastQuery(new byte[16], null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				


		try {
			//String is double null-terminated.
			byte[] payload = new byte[2+3];
			queryRequestFactory.createNetworkQuery(
			    new byte[16], (byte)0, (byte)0, payload, Network.UNKNOWN );
			fail("exception should have been thrown");
		} catch(BadPacketException e) {
		}
	}

	/**
	 * Tests to make sure that invalid TTL arguments are not accepted,
	 * and that valid ones are.
	 */
	public void testTTLParameters() {
		try {
			queryRequestFactory.createQuery("test", (byte)-1);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}
		try {
			queryRequestFactory.createQuery("test", (byte)8);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}

		try {
			queryRequestFactory.createRequery(UrnHelper.SHA1, (byte)-1);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}
		try {
			queryRequestFactory.createRequery(UrnHelper.SHA1, (byte)8);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}

	    queryRequestFactory.createQuery("test", (byte)1);
		queryRequestFactory.createRequery(UrnHelper.SHA1, (byte)1);
		queryRequestFactory.createQuery("test", (byte)3);
		queryRequestFactory.createRequery(UrnHelper.SHA1, (byte)3);

	}

	/**
	 * Tests to make sure that queries are still created correctly
	 * if some of the potential data is not filled in, such as
	 * XML vs. no XML, URN vs. no URN, etc.
	 */
	public void testStillAcceptedIfOnlyPartsAreEmpty() throws Exception {
		//String is double null-terminated.
		byte[] payload = new byte[2+3];
		payload[2] = (byte)65;
		queryRequestFactory.createNetworkQuery(
		    new byte[16], (byte)0, (byte)0, payload, Network.UNKNOWN);

		
		// now test everything empty but URN
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((short)2);
		baos.write(0); // null query
		baos.write(0); // first null
		
		// no encoding problems in english
		baos.write(StringUtils.toAsciiBytes(UrnHelper.URNS[0].toString())); 		
		baos.write(0); // last null

		queryRequestFactory.createNetworkQuery(
		    new byte[16], (byte)0, (byte)0, payload, Network.UNKNOWN);
	}

	/**
	 * Contains the legacy unit test that was formerly in QueryReqest.
	 */
	public void testLegacyUnitTest() throws Exception {
        QueryRequest qr = null;
       
		qr = queryRequestFactory.createQuery("blah");
		assertEquals("queries should be equal", "blah", qr.getQuery());

        // will normalize to lowercase.
		qr = queryRequestFactory.createQuery("ZZZ");
		assertEquals("queries should be equal", "zzz", qr.getQuery());

        //String is single null-terminated.
		byte[] payload = new byte[2+2];
		payload[2]=(byte)65;
		qr=queryRequestFactory.createNetworkQuery(
		    new byte[16], (byte)0, (byte)0, payload, Network.UNKNOWN);
		
  		assertEquals("queries should be equal", "A", qr.getQuery());
  		assertEquals("query lengths should be equal", 4, qr.getLength());

        //String is double null-terminated.
        payload = new byte[2+3];
        payload[2]=(byte)65;
        qr = queryRequestFactory.createNetworkQuery(
            new byte[16], (byte)0, (byte)0, payload, Network.UNKNOWN);

		assertEquals("queries should be equal", "A", qr.getQuery());

		try {
			//String is empty.
			payload = new byte[2+1];
			qr = queryRequestFactory.createNetworkQuery(
			    new byte[16], (byte)0, (byte)0, payload, Network.UNKNOWN);
			fail("should not have accepted query");
			//assertEquals("queries should be equal", "", qr.getQuery());
		} catch(BadPacketException e) {
			// this is expected for an empty query
		}
	}


    /**
     * Tests to make sure that network queries with URNS are not accepted.
     */
	public void testThatNetworkQueriesWithURNsAreAccepted() throws Exception {
		ByteArrayOutputStream[] URN_BYTES = 
		    new ByteArrayOutputStream[UrnHelper.URNS.length];
		for(int i=0; i<UrnHelper.URNS.length; i++) {
			URN_BYTES[i] = new ByteArrayOutputStream();
			URN_BYTES[i].write(0);
			URN_BYTES[i].write(0);
			URN_BYTES[i].write(StringUtils.toUTF8Bytes(QueryRequestTest.QUERY_STRINGS[i]));
			URN_BYTES[i].write(0);
			Set curUrnSet = new HashSet();
			for(int j=i; j<UrnHelper.URNS.length; j++) {
				URN_BYTES[i].write(StringUtils.toAsciiBytes(UrnHelper.URNS[j].toString()));
				curUrnSet.add(UrnHelper.URNS[j]);
				if((j+1) != UrnHelper.URNS.length) {
					URN_BYTES[i].write(0x1c);
				}
			}
			URN_BYTES[i].write(0);
        }        

        byte ttl = 4;
        byte hops = 2;

        for(int i=0; i<UrnHelper.URNS.length; i++) {
            try {
                queryRequestFactory.createNetworkQuery(GUID.makeGuid(), ttl, hops, 
                                                    URN_BYTES[i].toByteArray(), 
                                                    Network.UNKNOWN);
            } catch(BadPacketException e) {
                fail("should have accepted query!!");
            }
        }
    }


    public void testQueriesWithOnlyURNsAreAccepted() throws Exception {
        QueryRequest qr = queryRequestFactory.createQuery(UrnHelper.SHA1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qr.write(baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        QueryRequest qrRead = (QueryRequest) messageFactory.read(bais, Network.TCP);
        assertNotNull(qrRead);
    }


    /**
     * Test that network queries without URNs are accepted.
     */
	public void testThatWellFormedNetworkQueriesAreAccepted() throws Exception {
		ByteArrayOutputStream[] baos = 
		    new ByteArrayOutputStream[QueryRequestTest.QUERY_STRINGS.length];

        byte ttl = 4;
        byte hops = 2;
		for(int i=0; i<QueryRequestTest.QUERY_STRINGS.length; i++) {
			baos[i] = new ByteArrayOutputStream();
			baos[i].write(0);
			baos[i].write(0);
			baos[i].write(StringUtils.toUTF8Bytes(QueryRequestTest.QUERY_STRINGS[i]));
			baos[i].write(0);
			baos[i].write(0);
			QueryRequest qr = queryRequestFactory.createNetworkQuery(
			    GUID.makeGuid(), ttl, hops, 
			    baos[i].toByteArray(), Network.UNKNOWN);

            assertEquals("incorrect hops", hops, qr.getHops());
            assertEquals("incorrect ttl", ttl, qr.getTTL());
			assertEquals("speeds should be equal", 0, qr.getMinSpeed());
			assertEquals("queries should be equal", 
						 QueryRequestTest.QUERY_STRINGS[i], qr.getQuery());

            // we don't currently verify this -- should we accept queries
            // that don't ask for URNs??
		//	Set queryUrnTypes = qr.getRequestedUrnTypes();
        }
	}


    private static final String[] ILLEGAL_QUERIES = 
        new String[SearchSettings.ILLEGAL_CHARS.get().length];
    
    static {
        for(int i=0; i<ILLEGAL_QUERIES.length; i++) {
            ILLEGAL_QUERIES[i] = "test"+SearchSettings.ILLEGAL_CHARS.get()[i];
        }
    }

    /**
     * Test that network queries without URNs are accepted.
     */
	public void testThatNetworkQueriesWithIllegalCharsAreNotAccepted() throws Exception {
		ByteArrayOutputStream[] baos = 
		    new ByteArrayOutputStream[ILLEGAL_QUERIES.length];

        byte ttl = 4;
        byte hops = 2;
		for(int i=0; i<ILLEGAL_QUERIES.length; i++) {
			baos[i] = new ByteArrayOutputStream();
			baos[i].write(0);
			baos[i].write(0);
			baos[i].write(StringUtils.toUTF8Bytes(ILLEGAL_QUERIES[i]));
			baos[i].write(0);
			baos[i].write(0);
            try {
                queryRequestFactory.createNetworkQuery(GUID.makeGuid(), ttl, hops, 
                                                    baos[i].toByteArray(), 
                                                    Network.UNKNOWN);
                
                fail("should not have accepted illegal query chars");
            } catch(BadPacketException e) {
                // this should be thrown for illegal chars
            }
        }
	}
    


	/**
	 * Tests all constructors that take URNs.
	 */
	public void testUrnQueryConstructors() {
		Set urnSet = new HashSet();
		urnSet.add(UrnHelper.SHA1);
		QueryRequest qr = queryRequestFactory.createRequery(UrnHelper.SHA1);

		runStandardChecks(qr, qr.getQueryUrns());
		runRequeryChecks(qr);


		qr = queryRequestFactory.createRequery(UrnHelper.SHA1, (byte)4);
		runStandardChecks(qr, qr.getQueryUrns());
		runRequeryChecks(qr);
		assertEquals("TTLs should be equal", (byte)4, qr.getTTL());

		qr = queryRequestFactory.createQuery(UrnHelper.SHA1);
		runStandardChecks(qr, qr.getQueryUrns());
		runNonRequeryChecks(qr);
	}

	/**
	 * Tests constructor that only takes a string.
	 */
	public void testStringRequeryConstructor() {
		QueryRequest qr = queryRequestFactory.createRequery("test");
		runStandardChecks(qr, "test");
		runStringRequeryChecks(qr, "test");
	}


	/**
	 * Tests constructor that only takes a string.
	 */
	public void testStringConstructor() {
		QueryRequest qr = queryRequestFactory.createQuery("tests");
		runStandardChecks(qr, "tests");
		runNonRequeryChecks(qr);
	}

	/**
	 * Tests constructor that only takes a string and a TTL.
	 */
	public void testStringTTLConstructor() {
		QueryRequest qr = queryRequestFactory.createQuery("tests", (byte)3);
		runStandardChecks(qr, "tests");
		runNonRequeryChecks(qr);
		assertEquals("ttls should be equal", (byte)3, qr.getTTL());
	}

	/**
	 * Tests constructor that only takes a string and a TTL.
	 */
 	public void testStringXMLConstructor() throws Exception {
 		QueryRequest qr = queryRequestFactory.createQuery("tests", XML_STRING);
 		runStandardChecks(qr, "tests", XML_STRING);
 		runNonRequeryChecks(qr);
 	}

	/**
	 * Tests constructor that only takes a string and a TTL and a GUID.
	 */
	public void testGUIDStringXMLConstructor() throws Exception {
		QueryRequest qr = 
			queryRequestFactory.createQuery(QueryRequestImpl.newQueryGUID(false),
			    "tests", XML_STRING);
		runStandardChecks(qr, "tests", XML_STRING);
		runNonRequeryChecks(qr);
	}

	/**
	 * Tests constructor that only takes a string and a query key.
	 * @throws InvalidSecurityTokenException 
	 */
	public void testStringQueryKeyConstructor() throws InvalidSecurityTokenException {
		AddressSecurityToken key = new AddressSecurityToken(GUID.makeGuid(), macManager);
		QueryRequest qr =
			queryRequestFactory.createQueryKeyQuery("test", key);

		runStandardChecks(qr, false, Collections.EMPTY_SET, key);		
		assertEquals("unexpected query", "test", qr.getQuery());
		assertNull("unexpected xml query", qr.getRichQuery());
	}

	/**
	 * Tests constructor that only takes a QueryRequest for generating
	 * a multicast query.
	 */
	public void testMulticastQuery() {
		QueryRequest testQuery = queryRequestFactory.createQuery("test");
		QueryRequest qr = queryRequestFactory.createMulticastQuery(new byte[16], testQuery);		
		runStandardChecksMulticast(qr, "test");
	}

	/**
	 * Runs standard checks on requeries.
	 *
	 * @param qr the query to check
	 */
	private void runRequeryChecks(QueryRequest qr) {
		assertEquals("unexpected query", "\\", qr.getQuery());
		assertNull("xml should be empty", qr.getRichQuery());
		assertTrue("should have URNs", qr.hasQueryUrns());
		assertTrue("should be a requery", qr.isLimeRequery());
	}

	private void runStringRequeryChecks(QueryRequest qr, String query) {
		assertEquals("unexpected query", query, qr.getQuery());
		assertNull("xml should be null", qr.getRichQuery());
		assertTrue("should not have URNs", !qr.hasQueryUrns());
		assertTrue("should be a requery", qr.isLimeRequery());
	}

	private void runNonRequeryChecks(QueryRequest qr) {
		assertTrue("should not be a requery", !qr.isLimeRequery());		
	}

	/**
	 * Runs standard checks on query that should be valid for almost
	 * all queries.  This should not be 
	 *
	 * @param qr the query to check
	 */
	private void runStandardChecks(QueryRequest qr, 
								   boolean multicast,
								   Set urns, AddressSecurityToken key) {
		if(!multicast) {
			assertTrue("should not be multicast", !qr.isMulticast());
			assertTrue("should be firewalled", qr.isFirewalledSource());
		} else {
			assertTrue("should be multicast", qr.isMulticast());
			assertTrue("should not be firewalled", !qr.isFirewalledSource());
		}
		if(key == null) {
			assertNull("query key should be null", qr.getQueryKey());
		} else {
			assertEquals("unexpected query key", key, qr.getQueryKey());
		}
		assertTrue("should want XML", qr.desiresXMLResponses());
		assertEquals("URNs should be equal", urns, 
					 qr.getQueryUrns());	
        if(qr.getQueryUrns().size() == 0) {
            runIOChecks(qr);
        }
	}


	private void runStandardChecksMulticast(QueryRequest qr, String query) {
		assertEquals("queries should be equal", query, qr.getQuery());
		runStandardChecks(qr, true,
						  Collections.EMPTY_SET, null);
	}

	private void runStandardChecks(QueryRequest qr) {
		runStandardChecks(qr, false,
						  Collections.EMPTY_SET, null);
	}

	private void runStandardChecks(QueryRequest qr, Set urns) {
		runStandardChecks(qr, false, urns, null);
	}
	
	/**
	 * Runs standard checks on query that should be valid for almost
	 * all queries.  This should not be 
	 *
	 * @param qr the query to check
	 * @param query the query string
	 */
	private void runStandardChecks(QueryRequest qr, String query) {
		assertEquals("queries should be equal", query, qr.getQuery());
		runStandardChecks(qr);
	}

	/**
	 * Runs standard checks on query that should be valid for almost
	 * all queries.  This should not be 
	 *
	 * @param qr the query to check
	 * @param query the query string
	 */
	private void runStandardChecks(QueryRequest qr, String query, String xml)
	  throws Exception {
	    if( xml == null || xml.equals("") )
	        assertNull("should not have xml", qr.getRichQuery());
	    else
		    assertEquals("xml queries should be equal", 
		        xml, qr.getRichQuery().getXMLString());
		runStandardChecks(qr);
	}

	/**
	 * Writes the specified query out to a stream and then reads the
	 * stream back in to make sure we end up with the same query we
	 * started with.
	 *
	 * @param qr the query to check
	 */
	private void runIOChecks(QueryRequest qr) {
		QueryRequest storedQuery = qr;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			qr.write(baos);
			baos.flush();
		} catch (IOException e) {
			e.printStackTrace();
			fail("unexpected exception: "+e);
		}

		ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());

		QueryRequest qrTest = null;
		try {
			qrTest = (QueryRequest)messageFactory.read(bais, Network.TCP);
		} catch(Exception e) {
			e.printStackTrace();
			fail("unexpected exception: "+e);
		}

		assertEquals("queries should be equal", storedQuery, qrTest);
	}


    public void testNewMinSpeedUse() throws Exception {
        
        QueryRequest qr = null;

        // BASIC FROM THE NETWORK TESTS
        // --------------------------------------
        //String is empty.
        byte[] payload = new byte[2+1];
        // these two don't matter....give a query string, otherwise the query
		// will be rejected
        payload[2] = (byte) 65;
        payload[1] = (byte) 0;

        // not firewalled, not wanting rich, and can't do FWTrans, just 10000000
        payload[0] = (byte) 0x80;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.canDoFirewalledTransfer());

        // firewalled, not wanting rich, and can't do FWTrans, just 11000000
        payload[0] = (byte) 0xC0;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.canDoFirewalledTransfer());

        // not firewalled, wanting rich, can't do FWTrans, just 10100000
        payload[0] = (byte) 0xA0;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.canDoFirewalledTransfer());

        // firewalled, wanting rich, can't do FWTrans, just 11100000
        payload[0] = (byte) 0xE0;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.canDoFirewalledTransfer());

        // not firewalled, not wanting rich, and can do FWTrans, just 10000010
        payload[0] = (byte) 0x82;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.canDoFirewalledTransfer());

        // firewalled, not wanting rich, and can do FWTrans, just 11000010
        payload[0] = (byte) 0xC2;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.canDoFirewalledTransfer());

        // not firewalled, wanting rich, can do FWTrans, just 10100010
        payload[0] = (byte) 0xA2;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.canDoFirewalledTransfer());

        // firewalled, wanting rich, can do FWTrans, just 11100010
        payload[0] = (byte) 0xE2;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.canDoFirewalledTransfer());

        // now test out-of-band stuff
        // ---------------------------------------------------------
        InetAddress stanford = InetAddress.getByName("www.stanford.edu");
        byte[] stanfordGuid = GUID.makeAddressEncodedGuid(stanford.getAddress(),
                                                          6346);

        // firewalled, wanting rich, desiring out-of-band, can't do firewalled
        // transfer
        payload[0] = (byte) 0xE4;
        qr = queryRequestFactory.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(!qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // firewalled, not wanting rich, desiring out-of-band, can't do
        // firewalled transfer
        payload[0] = (byte) 0xC4;
        qr = queryRequestFactory.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(!qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, wanting rich, desiring out-of-band, can't do
        // firewalled transfer
        payload[0] = (byte) 0xA4;
        qr = queryRequestFactory.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(!qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, not wanting rich, desiring out-of-band, can't do
        // firewalled transfer
        payload[0] = (byte) 0x84;
        qr = queryRequestFactory.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(!qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // firewalled, wanting rich, desiring out-of-band, can do firewalled
        // transfer
        payload[0] = (byte) 0xE6;
        qr = queryRequestFactory.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // firewalled, not wanting rich, desiring out-of-band, can do
        // firewalled transfer
        payload[0] = (byte) 0xC6;
        qr = queryRequestFactory.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, wanting rich, desiring out-of-band, can do
        // firewalled transfer
        payload[0] = (byte) 0xA6;
        qr = queryRequestFactory.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, not wanting rich, desiring out-of-band, can do
        // firewalled transfer
        payload[0] = (byte) 0x86;
        qr = queryRequestFactory.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // ---------------------------------------------------------

        
        // make sure that a multicast source overrides that firewalled bit.
        payload[0] = (byte) 0xE0;
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.MULTICAST);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.isMulticast());
        // --------------------------------------
    }

    public void testNetworkParameter() throws Exception {
        QueryRequest qr;
        
        qr = queryRequestFactory.createQuery("sam");
        assertTrue(!qr.isMulticast());
        assertEquals(Network.UNKNOWN, qr.getNetwork());
        
        qr = queryRequestFactory.createMulticastQuery(new byte[16], qr);
        assertTrue(qr.isMulticast());
        assertEquals(Network.MULTICAST, qr.getNetwork());
        
        //String is empty.
        byte[] payload = new byte[2+1];
        // these two don't matter....give a query string, otherwise the query
		// will be rejected
        payload[2] = (byte) 65;
        payload[1] = (byte) 0;
        
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UNKNOWN);
        assertTrue(qr.isUnknownNetwork());
        assertEquals(Network.UNKNOWN, qr.getNetwork());
        
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.TCP);
        assertTrue(qr.isTCP());
        assertEquals(Network.TCP, qr.getNetwork());
        
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.UDP);
        assertTrue(qr.isUDP());
        assertEquals(Network.UDP, qr.getNetwork());
        
        qr = queryRequestFactory.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Network.MULTICAST);        
        assertTrue(qr.isMulticast());
        assertEquals(Network.MULTICAST, qr.getNetwork());
    }
    

    public void testWhatIsNewQuery() throws Exception {
        QueryRequest outQuery = null, inQuery = null;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;

        {
            outQuery = queryRequestFactory.createWhatIsNewQuery(GUID.makeGuid(), 
                                                         (byte)3);
            baos = new ByteArrayOutputStream();
            outQuery.write(baos);
            bais = new ByteArrayInputStream(baos.toByteArray());
            inQuery = (QueryRequest) messageFactory.read(bais, Network.TCP);
            assertTrue(inQuery.isWhatIsNewRequest());
            assertEquals(inQuery, outQuery);
        }

        {
            outQuery = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), 
                                                         (byte)3);
            baos = new ByteArrayOutputStream();
            outQuery.write(baos);
            bais = new ByteArrayInputStream(baos.toByteArray());
            inQuery = (QueryRequest) messageFactory.read(bais, Network.TCP);
            assertTrue(inQuery.isWhatIsNewRequest());
            assertEquals(inQuery, outQuery);
        }

    }
    
    public void testLimeQuery() throws Exception {
        QueryRequest qr = queryRequestFactory.createQuery("asdf");
        assertFalse(qr.isQueryForLW());
        qr = queryRequestFactory.createQuery("limewire pro");
        assertTrue(qr.isQueryForLW());
        qr = queryRequestFactory.createWhatIsNewQuery(new byte[16], (byte)1);
        assertFalse(qr.isQueryForLW());
        
        String XML_LIME =
            "<?xml version=\"1.0\"?>" +
             "<audios xsi:noNamespaceSchemaLocation=" +
             "\"http://www.limewire.com/schemas/audio.xsd\">" +
             "<audio title=\"xml\" artist=\"limewire\"/></audios>";
        qr = queryRequestFactory.createQuery(new byte[16], "Asdf", XML_LIME);
        assertTrue(qr.isQueryForLW());
        qr = queryRequestFactory.createQuery(new byte[16], "Asdf", XML_STRING);
        assertFalse(qr.isQueryForLW());
    }


    public void testMetaFlagConstructor() throws Exception {
        try {
            queryRequestFactory.createQueryRequest(GUID.makeGuid(),
                    (byte)3, "whatever", "", null, null, true, Network.TCP,
                    false, 0, false, 1);
            assertTrue(false);
        }
        catch (IllegalArgumentException yes) {}

        // when no flag is set
        testMetaFlag(0);

        int[] flags = new int[6];
        flags[0] = QueryRequest.AUDIO_MASK;
        flags[1] = QueryRequest.VIDEO_MASK;
        flags[2] = QueryRequest.DOC_MASK;
        flags[3] = QueryRequest.IMAGE_MASK;
        flags[4] = QueryRequest.WIN_PROG_MASK;
        flags[5] = QueryRequest.LIN_PROG_MASK;
        for (int i = 0; i < flags.length; i++) {
            testMetaFlag(0 | flags[i]);
            for (int j = 0; j < flags.length; j++) {
                if (j == i) continue;
                testMetaFlag(0 | flags[i] | flags[j]);
                for (int k = 0; k < flags.length; k++) {
                    if (k == j) continue;
                    testMetaFlag(0 | flags[i] | flags[j] | flags[k]);
                    for (int l = 0; l < flags.length; l++) {
                        if (l == k) continue;
                        testMetaFlag(0 | flags[i] | flags[j] | flags[k] |
                                     flags[l]);
                        for (int m = 0; m < flags.length; m++) {
                            if (m == l) continue;
                            testMetaFlag(0 | flags[i] | flags[j] | flags[k] |
                                         flags[l] | flags[m]);
                        }
                    }
                }
            }
        }

        try {
            queryRequestFactory.createQueryRequest(GUID.makeGuid(),
                    (byte)3, "whatever", "", null, null, true, Network.TCP,
                    false, 0, false, 0 | flags[0] | flags[1] | flags[2] |  flags[3] | 
                     flags[4] | flags[5]);
            assertTrue(false);
        }
        catch (IllegalArgumentException yes) {}

    }

    private void testMetaFlag(int flag) throws Exception {
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "whatever", "", null, null, true, Network.TCP, false, 0, false,
                flag);
        if (flag == 0) assertTrue(query.desiresAll());
        if ((flag & QueryRequest.AUDIO_MASK) > 0)
            assertTrue(query.desiresAudio());
        if ((flag & QueryRequest.VIDEO_MASK) > 0)
            assertTrue(query.desiresVideo());
        if ((flag & QueryRequest.DOC_MASK) > 0)
            assertTrue(query.desiresDocuments());
        if ((flag & QueryRequest.IMAGE_MASK) > 0)
            assertTrue(query.desiresImages());
        if ((flag & QueryRequest.WIN_PROG_MASK) > 0)
            assertTrue(query.desiresWindowsPrograms());
        if ((flag & QueryRequest.LIN_PROG_MASK) > 0)
            assertTrue(query.desiresLinuxOSXPrograms());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        query.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        QueryRequest qr = (QueryRequest) messageFactory.read(bais, Network.TCP);
        if (flag == 0) assertTrue(query.desiresAll());
        if ((flag & QueryRequest.AUDIO_MASK) > 0)
            assertTrue(qr.desiresAudio());
        if ((flag & QueryRequest.VIDEO_MASK) > 0)
            assertTrue(qr.desiresVideo());
        if ((flag & QueryRequest.DOC_MASK) > 0)
            assertTrue(qr.desiresDocuments());
        if ((flag & QueryRequest.IMAGE_MASK) > 0)
            assertTrue(qr.desiresImages());
        if ((flag & QueryRequest.WIN_PROG_MASK) > 0)
            assertTrue(qr.desiresWindowsPrograms());
        if ((flag & QueryRequest.LIN_PROG_MASK) > 0)
            assertTrue(qr.desiresLinuxOSXPrograms());
    }

    
    public void testMetaFlagConstructor2() throws Exception {
        {
            QueryRequest query = 
                queryRequestFactory.createQuery(GUID.makeGuid(), "whatever", "",
                                         MediaType.getAudioMediaType());
            
            assertTrue(query.desiresAudio());
            assertFalse(query.desiresVideo());
            assertFalse(query.desiresDocuments());
            assertFalse(query.desiresImages());
            assertFalse(query.desiresWindowsPrograms());
            assertFalse(query.desiresLinuxOSXPrograms());
        }
        {
            QueryRequest query = 
                queryRequestFactory.createOutOfBandQuery(GUID.makeGuid(), "whatever", 
                                                  "",
                                                  MediaType.getVideoMediaType());
            
            assertFalse(query.desiresAudio());
            assertTrue(query.desiresVideo());
            assertFalse(query.desiresDocuments());
            assertFalse(query.desiresImages());
            assertFalse(query.desiresWindowsPrograms());
            assertFalse(query.desiresLinuxOSXPrograms());
        }
        {
            QueryRequest query = 
                queryRequestFactory.createQuery(GUID.makeGuid(), "whatever", "",
                                         MediaType.getImageMediaType());
            
            assertFalse(query.desiresAudio());
            assertFalse(query.desiresVideo());
            assertFalse(query.desiresDocuments());
            assertTrue(query.desiresImages());
            assertFalse(query.desiresWindowsPrograms());
            assertFalse(query.desiresLinuxOSXPrograms());
        }
        {
            QueryRequest query = 
                queryRequestFactory.createWhatIsNewQuery(GUID.makeGuid(), (byte) 2,
                                               MediaType.getDocumentMediaType());
            
            assertFalse(query.desiresAudio());
            assertFalse(query.desiresVideo());
            assertTrue(query.desiresDocuments());
            assertFalse(query.desiresImages());
            assertFalse(query.desiresWindowsPrograms());
            assertFalse(query.desiresLinuxOSXPrograms());
        }
        {
            QueryRequest query = 
                queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte) 2,
                                               MediaType.getProgramMediaType());
            
            assertFalse(query.desiresAudio());
            assertFalse(query.desiresVideo());
            assertFalse(query.desiresDocuments());
            assertFalse(query.desiresImages());
            assertEquals(OSUtils.isWindows(), 
                         query.desiresWindowsPrograms());
            assertEquals(OSUtils.isLinux() || OSUtils.isMacOSX(),
                         query.desiresLinuxOSXPrograms());
        }

    }

    public void testProxyQueries() throws Exception {
        QueryRequest query = queryRequestFactory.createQuery("sush");
        assertFalse(query.desiresOutOfBandReplies());
        QueryRequest proxy = queryRequestFactory.createProxyQuery(query,
                                                           query.getGUID());
        assertDesiresOutOfBand(proxy);
        assertFalse(proxy.doNotProxy());
        
        assertEquals(query.getQuery(), proxy.getQuery());
        assertEquals(query.canDoFirewalledTransfer(), proxy.canDoFirewalledTransfer());
        assertEquals(query.getHops(), proxy.getHops());
        assertEquals(query.getTTL(), proxy.getTTL());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        OOBv2Disabled = true;
        proxy = queryRequestFactory.createProxyQuery(query,
                query.getGUID());
        assertDesiresOutOfBand(proxy);
    }
    
    public void testPatchInGGEP() throws Exception {
        GGEP ggep = new GGEP();
        ggep.put(GGEPKeys.GGEP_HEADER_NO_PROXY);
        ggep.put(GGEPKeys.GGEP_HEADER_SECURE_OOB);
        
        // payload without ggep and huge
        byte[] payload = new byte[] { -32, 0, 115, 117, 115, 104, 0, 117, 114, 110, 58, 28, };
        
        QueryRequest query = queryRequestFactory.createNetworkQuery(GUID.makeGuid(), (byte)1, (byte)1, payload, Network.UNKNOWN);
        assertFalse(query.doNotProxy());
        assertFalse(query.desiresOutOfBandReplies());
        
        byte[] newPayload = QueryRequestImpl.patchInGGEP(payload, ggep, macManager);
        
        QueryRequest proxy = queryRequestFactory.createNetworkQuery(query.getGUID(), query.getTTL(), query.getHops(), newPayload, Network.UNKNOWN);
        assertTrue(proxy.doNotProxy());
        assertTrue(proxy.desiresOutOfBandReplies());
        
        // payload with multiple ggeps
        payload = new byte[] { -32, 0, 115, 117, 115, 104, 0, 117, 114, 110, 58, 28, -61, -126, 78, 80, 64, 0x1c, -61, -126, 78, 80, 64, 0 };
        query = queryRequestFactory.createNetworkQuery(GUID.makeGuid(), (byte)1, (byte)1, payload, Network.UNKNOWN);
        assertTrue(query.doNotProxy());
        assertFalse(query.desiresOutOfBandReplies());
        
        newPayload = QueryRequestImpl.patchInGGEP(payload, ggep, macManager);
        
        proxy = queryRequestFactory.createNetworkQuery(query.getGUID(), query.getTTL(), query.getHops(), newPayload, Network.UNKNOWN);
        assertTrue(proxy.doNotProxy());
        assertTrue(proxy.desiresOutOfBandReplies());
        

        // unknown gem
        PositionByteArrayOutputStream out = new PositionByteArrayOutputStream();
        int minspeed = new Random().nextInt();
        minspeed &= QueryRequest.SPECIAL_OUTOFBAND_MASK;
        ByteUtils.short2leb((short)minspeed, out); // write minspeed
        out.write("query".getBytes("UTF-8"));              // write query
        out.write(0);                             // null
        int startGem = out.getPos();
        byte[] gemBytes = "unknowngem".getBytes("UTF-8"); 
        out.write(gemBytes);
        out.write(0x1c);
        
        payload = out.toByteArray();
        query = queryRequestFactory.createNetworkQuery(GUID.makeGuid(), (byte)1, (byte)1, payload, Network.UNKNOWN);
        assertFalse(query.desiresOutOfBandReplies());
        
        newPayload = QueryRequestImpl.patchInGGEP(payload, ggep, macManager);
        
        proxy = queryRequestFactory.createNetworkQuery(query.getGUID(), query.getTTL(), query.getHops(), newPayload, Network.UNKNOWN);
        assertTrue(proxy.doNotProxy());
        assertTrue(proxy.desiresOutOfBandReplies());
        // verfiy unknown gem is still there
        byte[] part = new byte[gemBytes.length];
        System.arraycopy(newPayload, startGem, part, 0, part.length);
        assertEquals(gemBytes, part);
        
        // unknown gem + GGEP with unknown keys
        GGEP unknownKeysGGEP = new GGEP(true);
        unknownKeysGGEP.put("FB", "BF");
        unknownKeysGGEP.put("uk");
        unknownKeysGGEP.write(out);
        
        payload = out.toByteArray();
        
        query = queryRequestFactory.createNetworkQuery(GUID.makeGuid(), (byte)1, (byte)1, payload, Network.UNKNOWN);
        assertFalse(query.desiresOutOfBandReplies());
        
        newPayload = QueryRequestImpl.patchInGGEP(payload, ggep, macManager);
        
        proxy = queryRequestFactory.createNetworkQuery(query.getGUID(), query.getTTL(), query.getHops(), newPayload, Network.UNKNOWN);
        assertTrue(proxy.doNotProxy());
        assertTrue(proxy.desiresOutOfBandReplies());
        System.arraycopy(newPayload, startGem, part, 0, part.length);
        assertEquals(gemBytes, part);
        
        QueryRequestPayloadParser parser = new QueryRequestPayloadParser(newPayload, macManager);
        GGEP parsedGGEP = parser.huge.getGGEP();
        assertEquals("BF", parsedGGEP.getString("FB"));
        assertTrue(parsedGGEP.hasKey("uk"));
        
        byte[] simpleSearchPayload = new byte[] {
                (byte)0xD8, 00, 0x6C, 0x69, 0x6D, 0x65, 0x77, 0x69, 0x72, 0x65, 00
        };
        
        query = queryRequestFactory.createNetworkQuery(GUID.makeGuid(), (byte)1, (byte)1, simpleSearchPayload, Network.UNKNOWN);
        assertEquals("limewire", query.getQuery());
        
        newPayload = QueryRequestImpl.patchInGGEP(query.getPayload(), ggep, macManager);
        
        QueryRequest patched = queryRequestFactory.createNetworkQuery(GUID.makeGuid(), (byte)1, (byte)1, newPayload, Network.UNKNOWN);
        assertEquals(query.getQuery(), patched.getQuery());
        
        patched = queryRequestFactory.createProxyQuery(query, GUID.makeGuid());
        assertEquals(query.getQuery(), patched.getQuery());
        assertTrue (patched.desiresOutOfBandRepliesV3());
    }
    
    /**
     * Tests if the security token key is set for oob query requests and that
     * it's not set otherwise.
     * @throws  
     */
    public void testOOBSecurityTokenSet() throws Exception {
        // oob set
        QueryRequest request = queryRequestFactory.createOutOfBandQuery(GUID.makeGuid(), "query", "");
        assertTrue(request.isSecurityTokenRequired());
        
        QueryRequest fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.isSecurityTokenRequired());
        
        request = queryRequestFactory.createOutOfBandQuery("query", InetAddress.getLocalHost().getAddress(), 4905);
        assertTrue(request.isSecurityTokenRequired());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.isSecurityTokenRequired());
        
        request = queryRequestFactory.createOutOfBandQuery(GUID.makeGuid(), "query", "", MediaType.getAudioMediaType());
        assertTrue(request.isSecurityTokenRequired());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.isSecurityTokenRequired());
        
        request = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte)1);
        assertTrue(request.isSecurityTokenRequired());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.isSecurityTokenRequired());
        
        request = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte)1, MediaType.getDocumentMediaType());
        assertTrue(request.isSecurityTokenRequired());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.isSecurityTokenRequired());
        
        // oob not set
        request = queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)1,
                "query", "", URN.NO_URN_SET, (AddressSecurityToken)null, true, Network.TCP,
                false, 0);
        assertFalse(request.isSecurityTokenRequired());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertFalse(fromNetwork.isSecurityTokenRequired());
        
    }

    /**
     * tests that we && properly the two settings
     */
    public void testDesiresPartialResultsSetting() throws Exception {
        SearchSettings.DESIRES_PARTIAL_RESULTS.setValue(true);
        SearchSettings.DESIRES_PARTIAL_RESULTS_REMOTE.setValue(true);
        assertTrue(SearchSettings.desiresPartialResults());
        
        SearchSettings.DESIRES_PARTIAL_RESULTS.setValue(false);
        SearchSettings.DESIRES_PARTIAL_RESULTS_REMOTE.setValue(true);
        assertFalse(SearchSettings.desiresPartialResults());
        
        SearchSettings.DESIRES_PARTIAL_RESULTS.setValue(true);
        SearchSettings.DESIRES_PARTIAL_RESULTS_REMOTE.setValue(false);
        assertFalse(SearchSettings.desiresPartialResults());
        
        SearchSettings.DESIRES_PARTIAL_RESULTS.setValue(false);
        SearchSettings.DESIRES_PARTIAL_RESULTS_REMOTE.setValue(false);
        assertFalse(SearchSettings.desiresPartialResults());
    }
    
    public void testDesiresPartialResults() throws Exception {
        SearchSettings.DESIRES_PARTIAL_RESULTS.setValue(true);
        SearchSettings.DESIRES_PARTIAL_RESULTS_REMOTE.setValue(true);
        QueryRequest request = queryRequestFactory.createOutOfBandQuery(GUID.makeGuid(), "query", "");
        assertTrue(request.desiresPartialResults());
        
        QueryRequest fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.desiresPartialResults());
        
        request = queryRequestFactory.createOutOfBandQuery("query", InetAddress.getLocalHost().getAddress(), 4905);
        assertTrue(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.desiresPartialResults());
        
        request = queryRequestFactory.createOutOfBandQuery(GUID.makeGuid(), "query", "", MediaType.getAudioMediaType());
        assertTrue(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.desiresPartialResults());
        
        request = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte)1);
        assertTrue(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.desiresPartialResults());
        
        request = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte)1, MediaType.getDocumentMediaType());
        assertTrue(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertTrue(fromNetwork.desiresPartialResults());
        
        
        // disable partial
        SearchSettings.DESIRES_PARTIAL_RESULTS.setValue(false);
        request = queryRequestFactory.createOutOfBandQuery(GUID.makeGuid(), "query", "");
        assertFalse(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertFalse(fromNetwork.desiresPartialResults());
        
        request = queryRequestFactory.createOutOfBandQuery("query", InetAddress.getLocalHost().getAddress(), 4905);
        assertFalse(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertFalse(fromNetwork.desiresPartialResults());
        
        request = queryRequestFactory.createOutOfBandQuery(GUID.makeGuid(), "query", "", MediaType.getAudioMediaType());
        assertFalse(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertFalse(fromNetwork.desiresPartialResults());
        
        request = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte)1);
        assertFalse(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertFalse(fromNetwork.desiresPartialResults());
        
        request = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte)1, MediaType.getDocumentMediaType());
        assertFalse(request.desiresPartialResults());
        
        fromNetwork = queryRequestFactory.createNetworkQuery(request.getGUID(), (byte)1, (byte)1, request.getPayload(), Network.UDP);
        assertFalse(fromNetwork.desiresPartialResults());
        
    }
    
    public void testUnmarkOOBQuery() throws Exception {
        QueryRequest query = queryRequestFactory.createOutOfBandQuery("query", InetAddress.getLocalHost().getAddress(), 5555);
        assertDesiresOutOfBand(query);
        
        QueryRequest copy = queryRequestFactory.unmarkOOBQuery(query);
        assertNotDesiresOutOfBand(copy);
        assertEquals(query.getQuery(), copy.getQuery());
        
        query = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte)1);
        assertDesiresOutOfBand(query);
        
        copy = queryRequestFactory.unmarkOOBQuery(query);
        assertNotDesiresOutOfBand(copy);
        assertEquals(query.getQuery(), copy.getQuery());
        assertEquals(query.isWhatIsNewRequest(), copy.isWhatIsNewRequest());
        assertEquals(query.getGUID(), copy.getGUID());
        
        query = queryRequestFactory.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte)1, MediaType.getDocumentMediaType());
        assertDesiresOutOfBand(query);
        
        copy = queryRequestFactory.unmarkOOBQuery(query);
        assertNotDesiresOutOfBand(copy);
        assertEquals(query.getQuery(), copy.getQuery());
        assertEquals(query.isWhatIsNewRequest(), copy.isWhatIsNewRequest());
        assertEquals(query.getGUID(), copy.getGUID());
        assertEquals(query.getMetaMask(), copy.getMetaMask());
        
        query = queryRequestFactory.createOutOfBandQuery(GUID.makeGuid(), "query", "");
        assertDesiresOutOfBand(query);
        copy = queryRequestFactory.unmarkOOBQuery(query);
        assertNotDesiresOutOfBand(copy);
        assertEquals(query.getQuery(), copy.getQuery());
        assertEquals(query.getGUID(), copy.getGUID());
        
     
        query = queryRequestFactory.createQuery("query");
        assertNotDesiresOutOfBand(query);
        
        copy = queryRequestFactory.unmarkOOBQuery(query);
        assertNotDesiresOutOfBand(copy);
        assertEquals(query.getPayload(), copy.getPayload());
    }
    
    public void testNotUnmarkOOBQuery() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        OOBv2Disabled = true;
        testUnmarkOOBQuery();
    }
    
    public void testCreateDoNotProxyQuery() throws UnknownHostException {
        QueryRequest query = queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)1, 5,
                "query", "", URN.NO_URN_SET, new AddressSecurityToken(InetAddress.getLocalHost(), 1094, macManager), false, Network.MULTICAST,
                false, 0, false, 0, false);

        try {
            queryRequestFactory.createDoNotProxyQuery(query);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException iae) {
        }
        
        query.originate();
        
        // precondition
        assertFalse(query.doNotProxy());
        
        QueryRequest proxy = queryRequestFactory.createDoNotProxyQuery(query);
        
        // post condition
        assertTrue(proxy.doNotProxy());
        
        assertEquals(query.getGUID(), proxy.getGUID());
        assertEquals(query.getHops(), proxy.getHops());
        assertEquals(query.getMinSpeed(), proxy.getMinSpeed());
        assertEquals(query.getRichQueryString(), proxy.getRichQueryString());
        assertEquals(query.getQueryUrns(), proxy.getQueryUrns());
        assertEquals(query.getQueryKey(), proxy.getQueryKey());
        assertEquals(query.isFirewalledSource(), proxy.isFirewalledSource());
        assertEquals(query.getFeatureSelector(), proxy.getFeatureSelector());
        assertEquals(query.getMetaMask(), proxy.getMetaMask());
        assertEquals(query.desiresOutOfBandReplies(), proxy.desiresOutOfBandReplies());
        assertEquals(query.getNetwork(), proxy.getNetwork());
        
        // idempotence
        proxy.originate();
        query = queryRequestFactory.createDoNotProxyQuery(proxy);
        
        assertEquals(query.getGUID(), proxy.getGUID());
        assertEquals(query.getHops(), proxy.getHops());
        assertEquals(query.getMinSpeed(), proxy.getMinSpeed());
        assertEquals(query.getRichQueryString(), proxy.getRichQueryString());
        assertEquals(query.getQueryUrns(), proxy.getQueryUrns());
        assertEquals(query.getQueryKey(), proxy.getQueryKey());
        assertEquals(query.isFirewalledSource(), proxy.isFirewalledSource());
        assertEquals(query.getFeatureSelector(), proxy.getFeatureSelector());
        assertEquals(query.getMetaMask(), proxy.getMetaMask());
        assertEquals(query.desiresOutOfBandReplies(), proxy.desiresOutOfBandReplies());
        assertEquals(query.getNetwork(), proxy.getNetwork());
    }
    
    public void testIsQueryForLWAllLocales() {
        for (Locale locale : Locale.getAvailableLocales()) {
            Locale.setDefault(locale);
            QueryRequestImpl queryRequest = new QueryRequestImpl(GUID.makeGuid(), (byte)1, 1, "LIMEWIRE", null, null, null, false, Network.TCP, true, 0, true, 0, false, true, null);
            assertTrue("Failed for locale: " + locale, queryRequest.isQueryForLW());
            queryRequest = new QueryRequestImpl(GUID.makeGuid(), (byte)1, 1, "limewire", null, null, null, false, Network.TCP, true, 0, true, 0, false, true, null);
            assertTrue("Failed for locale: " + locale, queryRequest.isQueryForLW());
        }
    }

    public void testCreateQueryWithExcessivelyLongQueryString() {
        String queryTooLong = "test test test test test test test test " +
                "test test test test test test test test test test test test test test test " +
                "test test test test test test test test test test test test test test test " +
                "test test test test test test test test test test test test test test";

        try {
            queryRequestFactory.createQuery(queryTooLong);
            fail("Did not receive expected exception regarding excessively long query!");
        } catch (IllegalArgumentException e) {
            String errMsg = e.getMessage();

            // received expected exception
            assertTrue(errMsg.startsWith("query too big:"));
        }
    }

    /**
     * testExtendedQueryString*Tests added for LWC-1744 - Since LW will no longer be sending out rich queries, the
     * projected length of the query string will likely exceed the limit set in older LW clients.
     * However, older LW clients will still need to handle these newer queries.
     * What this means is that the query string for the new queries will be split up,
     * and the query string field will contain all words that fit into the limit (30 chars).
     * A new GGEP will be introduced (ignored by older LW clients) that will contain the entire
     * query string.
     *
     */

    /**
     * Test creating a QueryRequest with a query string where the last word makes the
     * query string exceed the old LW limit (30 chars). Given the following Query string:
     * <p/>
     * "test testa teste testi testo testu rest" (length = 40 chars, exceeds old LW limit)
     * <p/>
     * The QueryRequest's query string field should be "test testa teste testi testo"
     */
    public void testExtendedQueryStringFieldLastWordPushesPastOldLwLimit() throws Exception {
        extendedQueryStringTest("test testa teste testi testo testu rest",
                                "test testa teste testi testo");
    }

    /**
     * Test creating a QueryRequest with a query string where the first word itself
     * exceeds the old LW limit (30 chars). Given the following Query string:
     * <p/>
     * "testonetestatestetestitestotestu test rest" (length 43, 1st word exceeds old LW limit)
     * <p/>
     * The QueryRequest's query string field should be "test rest"
     */
    public void testExtendedQueryStringFirstWordExceedsOldLwLimit() throws Exception {
        extendedQueryStringTest("testonetestatestetestitestotestu test rest",
                                "test rest");
    }

    /**
     * Test creating a QueryRequest with a query string where a word in the middle
     * exceeds the old LW limit (30 chars). Given the following Query string:
     * <p/>
     * "testa testy testonetestatestetestitestotestu test rest" (word in the middle exceeds old LW limit)
     * <p/>
     * The QueryRequest's query string field should be "testa testy test rest"
     */
    public void testExtendedQueryStringMiddleWordExceedsOldLwLimit() throws Exception {
        extendedQueryStringTest("testa testy testonetestatestetestitestotestu test rest",
                                "testa testy test rest");
    }

    /**
     * Test creating a QueryRequest with a query string where multiple words in the middle
     * exceed the old LW limit (30 chars). Given the following Query string:
     * <p/>
     * "testa testy testonetestatestetestitestotestu tentonetentatestetestitestotestu test rest"
     * (Multiple words in the middle exceed old LW limit)
     * <p/>
     * The QueryRequest's query string field should be "testa testy test rest"
     */
    public void testExtendedQueryStringMultipleMiddleWordsExceedOldLwLimit() throws Exception {
        extendedQueryStringTest(
            "testa testy testonetestatestetestitestotestu tentonetentatestetestitestotestu test rest", 
            "testa testy test rest");
    }

    /**
     * Test creating a QueryRequest with a query string where the entire string is 1 long word
     * from which keywords cannot be extracted. Given the following Query string:
     * <p/>
     * "testonetestatestetestitestotestu" (exceeds old LW limit)
     * <p/>
     * The QueryRequest's query string field should be "testonetestatestetestitestotes"
     * (truncation of the original query string to the length of the old LW limit)
     */
    public void testExtendedQueryStringExceedsOldLwLimitOnlyOneWord() throws Exception {
        extendedQueryStringTest("testonetestatestetestitestotestu",
                                "testonetestatestetestitestotes");
    }

    /**
     * Test creating a QueryRequest with a query string where the entire string consists of long words
     * all of which exceed the old LW limit (30 chars). Given the following Query string:
     * <p/>
     * "testonetestatestetestitestotestu bestonetestatestetestitestotestu xestonetestatestetestitestotestu"
     * (each keyword exceeds old LW limit)
     * <p/>
     * The QueryRequest's query string field should be "testonetestatestetestitestotes"
     * (truncation of the 1st keyword to the length of the old LW limit)
     */
    public void testExtendedQueryStringAllKeywordsExceedOldLwLimit() throws Exception {
        extendedQueryStringTest("testonetestatestetestitestotestu " +
                                "bestonetestatestetestitestotestu " +
                                "xestonetestatestetestitestotestu",
                                "testonetestatestetestitestotes");
    }

    /**
     * Test creating a QueryRequest with a query string that has 1 keyword that is exactly at the old LW limit.
     * Given the following Query string:
     * <p/>
     * "abcdefghijklmnopqrstuvwxyz1234"
     * (The entire query string is at the old LW limit)
     * <p/>
     * The QueryRequest's query string field should be "abcdefghijklmnopqrstuvwxyz1234"
     * (no change whatsoever)
     */
    public void testExtendedQueryStringExactlyAtOldLwLimit() throws Exception {
        extendedQueryStringTest("abcdefghijklmnopqrstuvwxyz1234",
                                "abcdefghijklmnopqrstuvwxyz1234");
    }

    /**
     * Test creating a QueryRequest with a query string where the old LW limit is not exceeded
     * Given the following Query string:
     * <p/>
     * "testa testy test rest" (Within old LW limit)
     * <p/>
     * The QueryRequest's query string field should be the same ("testa testy test rest")
     */
    public void testQueryStringOldLwLimitNotExceeded() throws Exception {
        extendedQueryStringTest("testa testy test rest",
                                "testa testy test rest");
    }

    /**
     * This method creates a {@link QueryRequest} message from the queryString parameter.
     * <p/>
     * 1. Tests that the QueryRequest message's query field matches the passed in expected value.
     * 2. Tests that calling {@link QueryRequest#getQuery()} returns the original query string.  Write message out
     *    to bytes, and read it back in to another {@link QueryRequest} message.
     *    Test that {@link QueryRequest#getQuery()} returns the same original query string.
     *
     * @param queryString query string used for creating QueryRequest message
     * @param expectedQueryFieldValue Expected value of the query field in the created QueryRequest
     */
    private void extendedQueryStringTest(String queryString, String expectedQueryFieldValue) throws Exception {

        final int queryFieldOffset = 2;

        QueryRequest query = queryRequestFactory.createQuery(queryString);
        byte[] queryField =
                Arrays.copyOfRange(query.getPayload(), queryFieldOffset,
                        queryFieldOffset + expectedQueryFieldValue.getBytes("UTF-8").length);
        String actualQueryFieldValue = new String(queryField, "UTF-8");

        assertEquals(expectedQueryFieldValue, actualQueryFieldValue);

        // test that this is the end of the query string field of the message
        assertEquals(0x00, query.getPayload()[queryField.length + queryFieldOffset]);

        extendedQueryStringTestReadingFromNetwork(query, queryString);
    }

    private void extendedQueryStringTestReadingFromNetwork(QueryRequest query, String expectedQueryString) throws Exception {

        assertEquals(expectedQueryString, query.getQuery());

        // create the QueryRequest from bytes, as if from network
        // Test that getQuery returns the same result.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        query.write(baos);
        byte[] messageAsBytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(messageAsBytes);

        QueryRequest readInRequestFromBytes = (QueryRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(query.getQuery(), readInRequestFromBytes.getQuery());
    }
    
    private void assertDesiresOutOfBand(QueryRequest query) {
        assertTrue(query.desiresOutOfBandReplies());
        assertNotEquals(OOBv2Disabled,query.desiresOutOfBandRepliesV2());
        assertTrue(query.desiresOutOfBandRepliesV3());
    }
    
    private void assertNotDesiresOutOfBand(QueryRequest query) {
        assertFalse(query.desiresOutOfBandReplies());
        assertFalse(query.desiresOutOfBandRepliesV2());
        assertFalse(query.desiresOutOfBandRepliesV3());
    }
    
    static class PositionByteArrayOutputStream extends ByteArrayOutputStream {

        public int getPos() {
            return count;
        }
        
    }
}
