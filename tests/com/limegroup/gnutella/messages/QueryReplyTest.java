package com.limegroup.gnutella.messages;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.BitNumbers;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.net.TLSManager;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.library.LibraryUtils;
import com.limegroup.gnutella.library.SharedFilesKeywordIndex;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * This class tests the QueryReply class.
 */
@SuppressWarnings({"unchecked", "null"})
public final class QueryReplyTest extends com.limegroup.gnutella.util.LimeTestCase {

    private static final byte[] IP = new byte[] {1, 1, 1, 1};
    private static final int MAX_LOCATIONS = 10;
    
    private final byte[] guid = { 0x1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte)0xFF };
    private final byte[] ip= {(byte)0xFE, 0, 0, 0x1};
    private final int u4 = Integer.MAX_VALUE;

    private QueryReply qr;
    private Iterator iter;
    private byte[] payload;
    private String vendor;
    private Response[] responses;
    private Response r1;
    private Response r2;
    private ByteArrayOutputStream out;
    private byte[] bytes;
    private int ggepLen;

    private QueryReplyImpl.GGEPUtil _ggepUtil = new QueryReplyImpl.GGEPUtil();
    
    private SecurityToken _token;
    
    private Injector injector;
    private TLSManager tlsManager;

    public QueryReplyTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(QueryReplyTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
    
    public void setUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        	    
	    injector = LimeTestUtils.createInjector();
	    
        byte[] data = new byte[16];
        new Random().nextBytes(data);
        _token = new AddressSecurityToken(data, injector.getInstance(MACCalculatorRepositoryManager.class));
        tlsManager = injector.getInstance(TLSManager.class);
    }
		
	/**
	 * Runs the legacy unit test that was formerly in QueryReply.
	 */
	public void testLegacy() throws Exception {
	    QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
	    ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
		responses=new Response[0];
		qr=queryReplyFactory.createQueryReply(guid, (byte)5, 0xF3F1,
                ip, 1, responses, guid, false);
		assertEquals(1, qr.getSpeed());
		assertEquals(Integer.toHexString(qr.getPort()), 0xF3F1, qr.getPort());

        assertEquals(qr.getResults().hasNext(), false);

		responses=new Response[2];
		responses[0]=responseFactory.createResponse(11, 22, "Sample.txt", UrnHelper.SHA1);
		responses[1]=responseFactory.createResponse(0x2FF2, 0xF11F, "Another file  ", UrnHelper.SHA1);
		qr=queryReplyFactory.createQueryReply(guid, (byte)5, 0xFFFF,
                ip, u4, responses, guid, false);
		assertEquals("254.0.0.1",qr.getIP());
		assertEquals(0xFFFF, qr.getPort());
		assertEquals(u4, qr.getSpeed());
		assertTrue(Arrays.equals(qr.getClientGUID(),guid));

		iter=qr.getResults();
		r1=(Response)iter.next();
		assertEquals(r1, responses[0]);
		r2=(Response)iter.next();
		assertEquals(r2, responses[1]);
		assertFalse(iter.hasNext());
    }
    
    public void testNetworkSimple() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
 		//Normal case: double null-terminated result
		payload=new byte[11+11+16];
		payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
		payload[3]=1;            //non-blank ip
		payload[11+8]=(byte)65;  //The character 'A'

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);


		iter=qr.getResults();
		Response response=(Response)iter.next();
		assertEquals("A", response.getName());
		assertFalse(iter.hasNext());

        try {
			qr.validate(); 
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
        assertEquals("", qr.getVendor());
		try {
			qr.getNeedsPush(); //undefined => exception
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
    }
    
    public void testNetworkNoClientGUID() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        //Bad case: not enough space for client GUID.  We can get
        //the client GUID, but not the results.
        payload=new byte[11+11+15];
        payload[0]=1;                    //Number of results
		payload[1]=1;                    //non-zero port
		payload[3]=1;                    //non-blank ip
        payload[11+8]=(byte)65;          //The character 'A'

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);

        try {
            iter=qr.getResults();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
			qr.validate(); 
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
        assertEquals("", qr.getVendor());
    }
    
    public void testNetworkCheckMetadata() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        //Test case added by Sumeet Thadani to check the metadata part
        //Test case modified by Susheel Daswani to check the metadata part
        payload=new byte[11+11+(4+1+4+5)+16];
        payload[0]=1;                    //Number of results
		payload[1]=1;                    //non-zero port
		payload[3]=1;                    //non-blank ip		
        payload[11+8]=(byte)65;          //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)76;   //The character 'L'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1+2]=(byte)5; //size of xml lsb
        payload[11+11+4+1+3]=(byte)0; // size of xml msb
        payload[11+11+4+1+4]=(byte)'S';   //The character 'L'
        payload[11+11+4+1+4+1]=(byte)'U';   //The character 'L'
        payload[11+11+4+1+4+2]=(byte)'S';   //The character 'M'
        payload[11+11+4+1+4+3]=(byte)'H';   //The character 'E'
        payload[11+11+4+1+4+4]=(byte)0;   //null terminator
		

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);
        iter=qr.getResults();
        Response r = (Response)iter.next();
        assertEquals("Sumeet test1", "A",  r.getName());
        assertNull("bad xml", r.getDocument());
    }
    
    public void testNetworkMetadataNoVendor() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        //Normal case: basic metainfo with no vendor data
        payload=new byte[11+11+(4+1+4+4)+16];
        payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
		payload[3]=1;            //non-blank ip		
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;  //The size of public area
        payload[11+11+4+1]=(byte)0xB1; // set push yes/no flag (and other stuff)
        payload[11+11+4+1+1]=(byte)0x01; // set the push understood flag
        payload[11+11+4+1+2]=(byte)4;  // set xml length
        payload[11+11+4+1+3]=(byte)0;

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);

        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertTrue(qr.getNeedsPush());
    }
    
    public void testNetworkMetadataExtraVendorData() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        //Normal case: basic metainfo with extra vendor data
        payload=new byte[11+11+(4+1+4+20000)+16];
        payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
		payload[3]=1;            //non-blank ip		
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)76;   //The character 'L'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0xF0; //no push flag (and other crap)
        payload[11+11+4+1+1]=(byte)0x01; // push understood flag
        payload[11+11+4+1+2]=(byte)32; //size of xml lsb
        payload[11+11+4+1+3]=(byte)78; // size of xml msb
        for (int i = 0; i < 20000; i++)
            payload[11+11+4+1+4+i] = 'a';

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);

        vendor=qr.getVendor();
        assertEquals(vendor, "LLME", vendor);
        vendor=qr.getVendor();
        assertEquals(vendor, "LLME", vendor);
        assertFalse(qr.getNeedsPush());

        assertFalse(qr.getSupportsChat());
    }
    
    public void testNetworkNoCommonData() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        //Weird case.  No common data.  (Don't allow.)
        payload=new byte[11+11+(4+1+2)+16];
        payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
        payload[3]=1;           //non-blank ip
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+4+1+0]=(byte)1;

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);

        try {
            qr.getNeedsPush();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
			qr.validate(); 
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
        assertEquals("", qr.getVendor());
    }
    
    public void testNetworkInvalidCommonPayloadLength() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        //Bad case.  Common payload length lies.
        payload=new byte[11+11+(4+2+0)+16];
        payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
        payload[3]=1;            //non-blank ip
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)2;

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);

        qr.getResults();
        
        try {
			qr.validate(); 
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
        assertEquals("", qr.getVendor());
    }
    
    public void testNetworkBearShareQHD() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ///////////// BearShare 2.2.0 QHD (busy bits and friends) ///////////
        //Normal case: busy bit undefined and push bits unset.
        //(We don't bother testing undefined and set.  Who cares?)
        payload=new byte[11+11+(4+1+4+1)+16];
        payload[0]=1;                //Number of results
		payload[1]=1;                //non-zero port
        payload[3]=1;                //non-blank ip		
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0x0; //no data known
        payload[11+11+4+1+1]=(byte)0x0; 
        payload[11+11+4+1+2]=(byte)1;         

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);
						  
        vendor=qr.getVendor();
        assertEquals("unexpected vendor", "LIME", vendor);
        
        try {
            qr.getNeedsPush();
            fail("qr should have been invalid");
        } catch(BadPacketException e) {}
        try {
            qr.getIsBusy();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getHadSuccessfulUpload();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getIsMeasuredSpeed();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
    }
    
    public void testNetworkQHDBusyPushBits() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        //Normal case: busy and push bits defined and set
        payload=new byte[11+11+(4+1+4+1+1)+16];
        payload[0]=1;                //Number of results
		payload[1]=1;                //non-zero port
        payload[3]=1;                //non-blank ip				
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)73;   //The character 'I'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4]=(byte)QueryReply.COMMON_PAYLOAD_LEN;    //common payload size
        payload[11+11+4+1]=(byte)0x1d;    // 0001 1101
        payload[11+11+4+1+1]=(byte)0x1d;  // 0001 1101
        payload[11+11+4+1+2]=(byte)1;  // no xml, just a null, so 1
        payload[11+11+4+1+4]=(byte)0x1; //supports chat

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);
							  
        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertTrue(qr.getNeedsPush());
        assertTrue(qr.getIsBusy());
        assertTrue(qr.getIsMeasuredSpeed());
        assertTrue(qr.getHadSuccessfulUpload());
        assertFalse(qr.getSupportsChat());
    }
    
    public void testNetworkQHDBusyPushBitsDefinedAndUnset() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        //Normal case: busy and push bits defined and unset
        payload=new byte[11+11+(4+1+4+1)+16];
        payload[0]=1;                //Number of results
		payload[1]=1;                //non-zero port
        payload[3]=1;                //non-blank ip
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0x1c;    // 0001 1100
        payload[11+11+4+1+1]=(byte)0x01;  // 0000 0001  //push understood
        payload[11+11+4+1+2]=(byte)1;  // no xml, just a null, so 1

		qr=queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);

        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertFalse(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getIsMeasuredSpeed());
        assertFalse(qr.getHadSuccessfulUpload());
        assertFalse("LiME!=LIME when looking at private area", qr.getSupportsChat());
    }
    
    public void testCreateQHDFromScratch() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        //Create extended QHD from scratch
        responses=new Response[2];
        responses[0]=responseFactory.createResponse(11, 22, "Sample.txt", UrnHelper.SHA1);
        responses[1]=responseFactory.createResponse(0x2FF2, 0xF11F, "Another file  ", UrnHelper.SHA1);
        qr=queryReplyFactory.createQueryReply(guid, (byte)5, 0xFFFF,
                ip, u4, responses, guid, false, true, true, false,
                true, false);
        assertEquals("254.0.0.1", qr.getIP());
        assertEquals(0xFFFF, qr.getPort());
        assertEquals(u4, qr.getSpeed());
        assertTrue(Arrays.equals(qr.getClientGUID(),guid));

        iter=qr.getResults();
        r1=(Response)iter.next();
        assertEquals(r1, responses[0]);
        r2=(Response)iter.next();
        assertEquals(r2, responses[1]);
        assertFalse(iter.hasNext());
        assertEquals("LIME", qr.getVendor());
        assertFalse(qr.getNeedsPush());
        assertTrue(qr.getIsBusy());
        assertTrue(qr.getHadSuccessfulUpload());
        assertFalse(qr.getIsMeasuredSpeed());
        assertTrue(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertTrue(!qr.isReplyToMulticastQuery());
    }
    
    public void testCreateQHDFromScratchOtherBits() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        //Create extended QHD from scratch with different bits set
        tlsManager.setIncomingTLSEnabled(false);
        responses=new Response[2];
        responses[0]=responseFactory.createResponse(11, 22, "Sample.txt", UrnHelper.SHA1);
        responses[1]=responseFactory.createResponse(0x2FF2, 0xF11F, "Another file  ", UrnHelper.SHA1);
        qr=queryReplyFactory.createQueryReply(guid, (byte)5, 0xFFFF,
                ip, u4, responses, guid, true, false, false, true,
                false, false);

        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertFalse(qr.isTLSCapable());

        //And check raw bytes....
        out=new ByteArrayOutputStream();
        qr.write(out);

        bytes=out.toByteArray();
        ggepLen = _ggepUtil.getQRGGEP(true, false, false, false, 
                                          new HashSet(), null).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals((23+11+16) // header
                +(8+10+2+41) // first response, 41 for sha1 urn
                +(8+14+2+41) // second response
                +(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen, bytes.length);
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertFalse(qr.isTLSCapable());
    }
    
    public void testCreateQHDWithTLS() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        //Create extended QHD from scratch with different bits set
        tlsManager.setIncomingTLSEnabled(true);
        responses=new Response[2];
        responses[0]=responseFactory.createResponse(11, 22, "Sample.txt", UrnHelper.SHA1);
        responses[1]=responseFactory.createResponse(0x2FF2, 0xF11F, "Another file  ", UrnHelper.SHA1);
        qr=queryReplyFactory.createQueryReply(guid, (byte)5, 0xFFFF,
                ip, u4, responses, guid, true, false, false, true,
                false, false);

        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertTrue(qr.isTLSCapable());

        //And check raw bytes....
        out=new ByteArrayOutputStream();
        qr.write(out);

        bytes=out.toByteArray();
        ggepLen = _ggepUtil.getQRGGEP(true, false, false, true, 
                                          new HashSet(), null).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals((23+11+16) // header
                +(8+10+2 +41) // first response, 41 for sha1
                +(8+14+2 + 41) // second response
                +(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen, bytes.length);
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertTrue(qr.isTLSCapable());
    }
    
    public void testCreateQHDOtherBits2() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        //Create extended QHD from scratch with different bits set
        // Do not set multicast, as that will unset pushing, busy, etc..
        // and generally confuse the test.
        tlsManager.setIncomingTLSEnabled(false);
        responses=new Response[2];
        responses[0]=responseFactory.createResponse(11, 22, "Sample.txt", UrnHelper.SHA1);
        responses[1]=responseFactory.createResponse(0x2FF2, 0xF11F, "Another file  ", UrnHelper.SHA1);
        qr=queryReplyFactory.createQueryReply(guid, (byte)5, 0xFFFF,
                ip, u4, responses, guid, true, false, false, true,
                false, false);

        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertFalse(qr.isTLSCapable());

        //And check raw bytes....
        out=new ByteArrayOutputStream();
        qr.write(out);

        bytes=out.toByteArray();
        ggepLen = _ggepUtil.getQRGGEP(true, false, false,false,
                                      new HashSet(), null).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals(
                (23+11+16) // header
                +(8+10+2 + 41) // first response, 41 for sha1
                +(8+14+2 + 41) // second response
                +(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen,
            bytes.length
        );
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertFalse(qr.isTLSCapable());
    }
    
    public void testCreateQHDOtherBits3() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        //Create extended QHD from scratch with different bits set
        // Do not set multicast, as that will unset pushing, busy, etc..
        // and generally confuse the test.
        responses=new Response[2];
        responses[0]=responseFactory.createResponse(11, 22, "SMDNKD.txt", UrnHelper.SHA1);
        responses[1]=responseFactory.createResponse(0x2FF2, 0xF11F, "OneMore file  ", UrnHelper.SHA1);
        // first take input of proxies
        String[] hosts = {"www.limewire.com", "www.limewire.org",
                          "www.susheeldaswani.com"};
        Set proxies = new TreeSet(IpPort.COMPARATOR);
        for (int i = 0; i < hosts.length; i++)
            proxies.add(new IpPortImpl(hosts[i], 6346));
        tlsManager.setIncomingTLSEnabled(false);
        qr=queryReplyFactory.createQueryReply(guid, (byte)5, 0xFFFF,
                ip, u4, responses, guid, new byte[0], true, false,
                false, true, false, false, true, proxies, null);
        
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertTrue(qr.getSupportsFWTransfer());
        assertFalse(qr.isTLSCapable());

        //And check raw bytes....
        out=new ByteArrayOutputStream();
        qr.write(out);

        bytes=out.toByteArray();
        ggepLen = _ggepUtil.getQRGGEP(true, false, true,false,
                                      proxies, null).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals(
            (23+11+16) // header
            +(8+10+2+41) // first response, 41 for sha1
            +(8+14+2+41) // second response
            +(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen,
            bytes.length
        );
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertTrue(qr.getSupportsFWTransfer());
        assertFalse(qr.isTLSCapable());
    }
    
    public void testCreateQHDNoBits() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        //Create from scratch with no bits set
        responses=new Response[2];
        responses[0]=responseFactory.createResponse(11, 22, "Sample.txt", UrnHelper.SHA1);
        responses[1]=responseFactory.createResponse(0x2FF2, 0xF11F, "Another file  ", UrnHelper.SHA1);
        qr=queryReplyFactory.createQueryReply(guid, (byte)5, 0xFFFF,
                ip, u4, responses, guid, false);
        
        try {
			qr.validate(); 
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
        assertEquals("", qr.getVendor());         
        try {
            qr.getNeedsPush();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        
        try {
            qr.getIsBusy();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        
        try {
            qr.getHadSuccessfulUpload();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        
        try {
            qr.getIsMeasuredSpeed();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
	}

    public void testCalculateQualityOfService() throws UnknownHostException, BadPacketException {        
        
        //Remember that a return value of N corresponds to N+1 stars
        QueryReply reachableNonBusy = getQueryReplyWithCalculateQOSInputs(false, false, true);
        assertEquals(3,  reachableNonBusy.calculateQualityOfService());
        reachableNonBusy = getQueryReplyWithCalculateQOSInputs(false, false, false);
        assertEquals(3,  reachableNonBusy.calculateQualityOfService());
        
        QueryReply reachableBusy = getQueryReplyWithCalculateQOSInputs(false, true, true);
        assertEquals(1,  reachableBusy.calculateQualityOfService());
        reachableBusy = getQueryReplyWithCalculateQOSInputs(false, true, false);
        assertEquals(1,  reachableBusy.calculateQualityOfService());
        
        QueryReply unreachableNonBusy = getQueryReplyWithCalculateQOSInputs(true, false, true);
        assertEquals(2,  unreachableNonBusy.calculateQualityOfService());
        unreachableNonBusy = getQueryReplyWithCalculateQOSInputs(true, false, false);
        assertEquals(-1,  unreachableNonBusy.calculateQualityOfService());
        
        QueryReply unreachableBusy = getQueryReplyWithCalculateQOSInputs(true, true, true);
        assertEquals(0,  unreachableBusy.calculateQualityOfService());
        unreachableBusy = getQueryReplyWithCalculateQOSInputs(true, true, false);
        assertEquals(-1,  unreachableBusy.calculateQualityOfService());        
        
        QueryReply oldStyle=getQueryReplyWithCalculateQOSInputsOldStyle(true);
        assertEquals(0,  oldStyle.calculateQualityOfService());
        oldStyle=getQueryReplyWithCalculateQOSInputsOldStyle(false);
        assertEquals(0,  oldStyle.calculateQualityOfService());               
        
    }
    
    private QueryReply getQueryReplyWithCalculateQOSInputs(boolean needsPush, boolean isBusy, final boolean acceptedIncomingConnection) throws UnknownHostException, BadPacketException {
        final byte[] addr=new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
        Mockery mockery = new Mockery();
        final NetworkManager mockNetworkManager = mockery.mock(NetworkManager.class);
        final NetworkInstanceUtils networkInstanceUtils = mockery.mock(NetworkInstanceUtils.class);
        mockery.checking(new Expectations() {{
            atLeast(0).of(networkInstanceUtils).isPrivateAddress(addr);
            will(returnValue(false));
            atLeast(0).of(mockNetworkManager).getAddress();
            will(returnValue(InetAddress.getLocalHost().getAddress()));            
            atLeast(0).of(mockNetworkManager).acceptedIncomingConnection();
            will(returnValue(acceptedIncomingConnection));
            atLeast(0).of(mockNetworkManager).isIncomingTLSEnabled();
            will(returnValue(true));
        }});
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(mockNetworkManager);
            }
	    });
        
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);

        //query.setMulticastAllowed(true);
        return queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6346,
                addr, 0l, new Response[0], new byte[16], needsPush, isBusy, true, false,
                false, false);
    }
    
    private QueryReply getQueryReplyWithCalculateQOSInputsOldStyle(final boolean acceptedIncomingConnection) throws UnknownHostException, BadPacketException {
        final byte[] addr=new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
        Mockery mockery = new Mockery();
        final NetworkManager mockNetworkManager = mockery.mock(NetworkManager.class);
        final NetworkInstanceUtils networkInstanceUtils = mockery.mock(NetworkInstanceUtils.class);
        mockery.checking(new Expectations() {{
            atLeast(0).of(networkInstanceUtils).isPrivateAddress(addr);
            will(returnValue(false));
            atLeast(0).of(mockNetworkManager).getAddress();
            will(returnValue(InetAddress.getLocalHost().getAddress()));            
            atLeast(0).of(mockNetworkManager).acceptedIncomingConnection();
            will(returnValue(acceptedIncomingConnection));
            atLeast(0).of(mockNetworkManager).isIncomingTLSEnabled();
            will(returnValue(true));
        }});
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(mockNetworkManager);
            }
	    });
        
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
       
        QueryReply oldStyle=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6346,
                addr, 0l, new Response[0], new byte[16], false);
        oldStyle.setMulticastAllowed(true);
        return oldStyle;
    }


    public void testGGEPUtil() throws Exception {
        GGEP testGGEP;

        // test standard null GGEP....
        try {
            // this shouldn't even work....
            new GGEP(_ggepUtil.getQRGGEP(false, false, false,false,
                                         new HashSet(), null), 
                     0, null);
            fail("expected exception");
        }
        catch (BadGGEPBlockException expected) {}

        // test just BH GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(true, false, false,false,
                                                new HashSet(), null), 
                            0, null);
        assertEquals(1, testGGEP.getHeaders().size());
        assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_BROWSE_HOST));
        assertTrue(!testGGEP.hasKey(GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE));

        // test just multicast GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(false, true, false,false,
                                                new HashSet(), null), 
                            0, null);
        assertEquals(1, testGGEP.getHeaders().size());
        assertTrue(!testGGEP.hasKey(GGEPKeys.GGEP_HEADER_BROWSE_HOST));
        assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE));

        // test combo GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(true, true, false,false,
                                                new HashSet(), null),
                            0, null);
        assertEquals(2, testGGEP.getHeaders().size());
        assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_BROWSE_HOST));
        assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE));

    }
    
    public void testGGEPUtilWritesSecurityToken() throws Exception {
        // assert token is written
        GGEP ggep = new GGEP(_ggepUtil.getQRGGEP(false, false, false, false,null, _token), 0, null);
        assertTrue(ggep.hasKey(GGEPKeys.GGEP_HEADER_SECURE_OOB));
        assertEquals(_token.getBytes(), ggep.get(GGEPKeys.GGEP_HEADER_SECURE_OOB));
        
        ggep = new GGEP(_ggepUtil.getQRGGEP(true, true, true, false,null, _token), 0, null);
        assertTrue(ggep.hasKey(GGEPKeys.GGEP_HEADER_SECURE_OOB));
        assertEquals(_token.getBytes(), ggep.get(GGEPKeys.GGEP_HEADER_SECURE_OOB));
        
        Set<IpPort> proxies = new HashSet<IpPort>();
        proxies.add(new Endpoint("127.0.0.1:6464"));
        ggep = new GGEP(_ggepUtil.getQRGGEP(true, true, true, false,proxies, _token), 0, null);
        assertTrue(ggep.hasKey(GGEPKeys.GGEP_HEADER_SECURE_OOB));
        assertEquals(_token.getBytes(), ggep.get(GGEPKeys.GGEP_HEADER_SECURE_OOB));
        
        // assert token is not written
        try {
            new GGEP(_ggepUtil.getQRGGEP(false, false, false,false, null, null), 0, null);
            fail("exception should have been thrown");
        }
        catch (BadGGEPBlockException bgbe) {
        }
                
        ggep = new GGEP(_ggepUtil.getQRGGEP(true, true, true, false,null, null), 0, null);
        assertFalse(ggep.hasKey(GGEPKeys.GGEP_HEADER_SECURE_OOB));
        
        ggep = new GGEP(_ggepUtil.getQRGGEP(true, true, true,false, proxies, null), 0, null);
        assertFalse(ggep.hasKey(GGEPKeys.GGEP_HEADER_SECURE_OOB));
    }


    public void testBasicPushProxyGGEP() throws Exception {
        for(byte i = 0; i < 0x1F; i++) {
            BitNumbers bn = new BitNumbers(new byte[] { i } );
            basicTest(bn.isSet(3), bn.isSet(4), bn.isSet(5), bn.isSet(6), bn.isSet(7));
        }
    }

    private static final Random RND = new Random();
    public void basicTest(final boolean browseHost,
                          final boolean multicast,
                          final boolean fwTransfer,
                          final boolean tls,
                          final boolean proxiesTLS) throws Exception {
        
        int numHeaders = 1;

        // these are in order so that the TreeSet doesn't re-order them -- makes TLS matching easier
        String[] hosts = {"1.2.3.4", "1.2.3.5", "1.2.3.6", "1.2.3.7"};
        Set proxies = new TreeSet(IpPort.COMPARATOR);
        BitNumbers bn = new BitNumbers(4);
        for (int i = 0; i < hosts.length; i++) {
            if(!proxiesTLS || !RND.nextBoolean()) {
                proxies.add(new IpPortImpl(hosts[i], 6346));
            } else {
                proxies.add(new ConnectableImpl(hosts[i], 6346, true));
                bn.set(i);
            }
        }
        GGEP testGGEP = new GGEP(_ggepUtil.getQRGGEP(browseHost, multicast, 
                                                     fwTransfer, tls, proxies, null), 
                                 0, null);
        if (browseHost) {
            numHeaders++;
            assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_BROWSE_HOST));
        }
        if (multicast) {
            numHeaders++;
            assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE));
        }
        if (fwTransfer) {
            numHeaders++;
            assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_FW_TRANS));
        }
        if (tls) {
            numHeaders++;
            assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_TLS_CAPABLE));
        }
        
        if(proxiesTLS && !bn.isEmpty()) {
            numHeaders++;
            assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_PUSH_PROXY_TLS));
            assertEquals(bn.toByteArray(), testGGEP.getBytes(GGEPKeys.GGEP_HEADER_PUSH_PROXY_TLS));
        }
        
        assertTrue(testGGEP.hasKey(GGEPKeys.GGEP_HEADER_PUSH_PROXY));
        assertEquals(numHeaders, testGGEP.getHeaders().size());
        Set retProxies = _ggepUtil.getPushProxies(testGGEP);
        assertEquals(4, retProxies.size());
        assertEquals(retProxies, proxies);
    }


    public void testBadPushProxyInput() throws Exception {
        byte[] badBytes = new byte[6];
        GGEP ggep;
        // test a bad ip
        // 0.0.0.0 is a bad address

        // trying to input
        try {
            NetworkUtils.getBytes(new IpPortImpl("0.0.0.0", 6346), java.nio.ByteOrder.LITTLE_ENDIAN);
            fail("allowed bad PPI");
        } catch (IllegalArgumentException expected) {}

        // from the network
        ggep = new GGEP(true);
        badBytes[0] = (byte) 0;
        badBytes[1] = (byte) 0;
        badBytes[2] = (byte) 0;
        badBytes[3] = (byte) 0;
        badBytes[4] = (byte) 3;
        badBytes[5] = (byte) 4;
        ggep.put(GGEPKeys.GGEP_HEADER_PUSH_PROXY, badBytes);
        assertEquals(0, _ggepUtil.getPushProxies(ggep).size());

        // test a bad port

        // trying to input is the only case
        try {
            NetworkUtils.getBytes(new IpPortImpl("1.2.3.4", 634600), java.nio.ByteOrder.LITTLE_ENDIAN);
            fail("allowed bad PPI");
        } catch (IllegalArgumentException expected) {}

        // this should work fine...
        ggep = new GGEP(true);
        badBytes[0] = (byte) 1;
        badBytes[1] = (byte) 1;
        badBytes[2] = (byte) 2;
        badBytes[3] = (byte) 2;
        badBytes[4] = (byte) 0;
        badBytes[5] = (byte) 0;
        ggep.put(GGEPKeys.GGEP_HEADER_PUSH_PROXY, badBytes);
        assertNotNull(_ggepUtil.getPushProxies(ggep));


        // try to get proxies when the lengths are wrong
        for (int i = 0; i < 100; i++) {
            badBytes = new byte[i];
            // just put some filler in here....
            for (int j = 0; j < badBytes.length; j++) {
                // make sure each one is unique, as the returned
                // Set from getPushProxies will filter duplicates
                if(j%6 == 0) {
                    badBytes[j] = (byte)j;
                }
                else{
                    badBytes[j] = (byte)i;
                }
            }
            ggep = new GGEP(true);
            ggep.put(GGEPKeys.GGEP_HEADER_PUSH_PROXY, badBytes);
            if (i == 0)
                assertEquals(0, _ggepUtil.getPushProxies(ggep).size());
            else if (i < 6) 
                assertEquals(0, _ggepUtil.getPushProxies(ggep).size());
            else  {// length is fine
                   // -1 because the first entry is invalid since it
                   // begins with 0.               
                assertEquals((i / 6) - 1,
                             _ggepUtil.getPushProxies(ggep).size());
                
            }
        }

    }


    public void testManualPushProxyInput() throws Exception {
        Random rand = new Random();
        GGEP ggep;
        for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[6*(i+1)];
            rand.nextBytes(bytes);
            // don't trust zeroes or 255 in IP...
            for (int j = 0; j < bytes.length; j++) {
                if (bytes[j] == (byte) 0) bytes[j] = (byte) 1;
                if (bytes[j] == (byte)255)bytes[j] = (byte)254;
            }

            // from the network
            ggep = new GGEP(true);
            ggep.put(GGEPKeys.GGEP_HEADER_PUSH_PROXY, bytes);

            Set proxies = _ggepUtil.getPushProxies(ggep);
            assertNotNull(proxies);

            Iterator iter = proxies.iterator();
            int j = 0;
            while(iter.hasNext()) {
                final int inIndex = 6*j;
                iter.next(); //IpPort ppi = (IpPort)iter.next();
               // InetAddress addr = ppi.getInetAddress();
                byte[] tempAddr = new byte[4];
                System.arraycopy(bytes, inIndex, tempAddr, 0, 4);

                InetAddress addr2 = InetAddress.getByAddress(tempAddr);
                String address = addr2.getHostAddress();
                
                int curPort = ByteUtils.leb2int(bytes, inIndex+4, 2);
                IpPort ppi2 = 
                    new IpPortImpl(address, curPort);

                assertTrue(proxies.contains(ppi2));
                //assertEquals(addr, addr2);
                //int port = ppi.getPushProxyPort();
                /*
                final int inIndex = 6*j;
                
                byte[] addrBytes = addr.getAddress();
                byte[] portBytes = new byte[2];
                ByteOrder.short2leb((short)port, portBytes, 0);
                
                assertEquals(addrBytes[0], bytes[inIndex]);
                assertEquals(addrBytes[1], bytes[inIndex+1]);
                assertEquals(addrBytes[2], bytes[inIndex+2]);
                assertEquals(addrBytes[3], bytes[inIndex+3]);
                assertEquals(portBytes[0], bytes[inIndex+4]);
                assertEquals(portBytes[1], bytes[inIndex+5]);
                */
                j++;
            }
        }

    }
    
    public void testPushProxyQueryReply() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        
        String[] hosts = {"www.limewire.com", "www.limewire.org",
                          "www.susheeldaswani.com", "www.berkeley.edu"};

        for (int outer = 0; outer < hosts.length; outer++) {
            //PushProxyInterface[] proxies = new PushProxyInterface[outer+1];
            Set proxies = new TreeSet(IpPort.COMPARATOR);
            for (int i = 0; i < hosts.length; i++)
                proxies.add(
                    new IpPortImpl(hosts[i], 6346));
            
            QueryReply qr = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte) 4, 6346,
                    IP, 0, new Response[0], GUID.makeGuid(), new byte[0], false, false,
                    true, true, true, false, proxies);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qr.write(baos);
            ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
            QueryReply readQR = (QueryReply) messageFactory.read(bais, Network.TCP);

            // test read from network            
            Set retProxies = readQR.getPushProxies();
            assertNotNull(retProxies);
            assertTrue(retProxies != proxies);
            assertEquals(retProxies.size(), proxies.size());
            assertEquals(retProxies, proxies);
            assertEquals(proxies, retProxies);

            // test simple accessor
            retProxies = qr.getPushProxies();
            assertNotNull(retProxies);
            assertTrue(retProxies != proxies);
            assertEquals(retProxies.size(), proxies.size());
            assertEquals(retProxies, proxies);
            assertEquals(proxies, retProxies);
        }
    }
    
    public void testPushProxyWithTLS() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        Set proxies = new IpPortSet();
        proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        proxies.add(new ConnectableImpl("1.2.3.5", 5, true));
        proxies.add(new ConnectableImpl("1.2.3.6", 5, true));
        proxies.add(new ConnectableImpl("1.2.3.7", 5, false));
        QueryReply qr = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte) 4, 6346,
                IP, 0, new Response[0], GUID.makeGuid(), new byte[0], false, false,
                true, true, true, false, proxies);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        qr.write(out);
        
        // All we care about is the GGEP in the reply,
        // which should begin at:
        // + 0  // beginning
        // + 23 // header
        // + 11 // responses
        // + 0  // (0 responses)
        // + 10 // GGEP area of QHD
        GGEP ggep = new GGEP(out.toByteArray(), 44);
        assertTrue(ggep.hasKey(GGEPKeys.GGEP_HEADER_PUSH_PROXY));
        assertTrue(ggep.hasKey(GGEPKeys.GGEP_HEADER_PUSH_PROXY_TLS));
        byte[] bytes = ggep.getBytes(GGEPKeys.GGEP_HEADER_PUSH_PROXY);
        assertEquals(new byte[] { 1, 2, 3, 4, 5, 0, 1, 2, 3, 5, 5, 0, 1, 2, 3, 6, 5, 0, 1, 2, 3, 7, 5, 0 }, bytes);
        bytes = ggep.getBytes(GGEPKeys.GGEP_HEADER_PUSH_PROXY_TLS);
        assertEquals(new byte[] { (byte)0xE0 }, bytes);
        
        // Make sure we can deserialize it too.
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        qr = (QueryReply)messageFactory.read(in, Network.TCP);
        assertEquals(4, qr.getPushProxies().size());
        int tls = 0;
        IpPort nonTLS = null;
        for(IpPort ipp : qr.getPushProxies()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                tls++;
            else
                nonTLS = ipp;
        }
        assertEquals(3, tls);
        assertEquals("1.2.3.7", nonTLS.getAddress());
        proxies.retainAll(qr.getPushProxies());
        assertEquals(4, proxies.size()); // make sure it's the right ones!
        
    }
    
    public void testQueryReplyHasAlternates() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        FileManager fileManager = injector.getInstance(FileManager.class);
        SharedFilesKeywordIndex keywordIndex = injector.getInstance(SharedFilesKeywordIndex.class);
        injector.getInstance(ServiceRegistry.class).initialize();
        addFilesToLibrary();
        addAlternateLocationsToFiles();
        
        boolean checked = false;
        for(FileDesc fd : fileManager.getGnutellaFileList()) {
			Response testResponse = responseFactory.createResponse(fd);

            String name = fd.getFileName();
            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
            Arrays.sort(illegalChars);

            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
                    || StringUtils.containsCharacters(name, illegalChars)) {
                continue;
            }
            
			QueryRequest qr = queryRequestFactory.createQuery(fd.getFileName());
			Response[] hits = keywordIndex.query(qr);
			assertNotNull("didn't get a response for query " + qr, hits);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
            if(hits.length == 0)
                fail("no hit for query: " + qr);
			if ( hits.length != 1 )
                continue;
            
            checked = true;
			
			// first check basic stuff on the response.
			assertEquals("responses should be equal", testResponse, hits[0]);
			assertEquals("should have 10 other alts",
			    10, testResponse.getLocations().size());
			assertEquals("should have equal alts",
			    testResponse.getLocations(), hits[0].getLocations());
			    
			// then actually create a QueryReply and read it, to make
			// sure we can write & read stuff correctly.
            QueryReply qReply = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte) 4, 6346,
                    IP, 0, hits, GUID.makeGuid(), new byte[0], false, false,
                    true, true, true, false, null);			    
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qReply.write(baos);
            ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
            QueryReply readQR = (QueryReply) messageFactory.read(bais, Network.TCP);
            
            List readHits = readQR.getResultsAsList();
            assertEquals("wrong # of results", hits.length, readHits.size());
            Response hit = (Response)readHits.get(0);
            assertEquals("wrong # of alts",
                hits[0].getLocations(), hit.getLocations());
		}        
        
        assertTrue("didn't check anything!", checked);
    }
    
    public void testQueryReplyHasCreationTimes() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        FileManager fileManager = injector.getInstance(FileManager.class);
        SharedFilesKeywordIndex keywordIndex = injector.getInstance(SharedFilesKeywordIndex.class);
        injector.getInstance(ServiceRegistry.class).initialize();
        addFilesToLibrary();
        addCreationTimeToFiles();
        boolean checked = false;
        for(FileDesc fd : fileManager.getGnutellaFileList()) {
			long expectTime = (fd.getIndex() + 1) * 10013;
			Response testResponse = responseFactory.createResponse(fd);
			assertEquals(fd.toString(), expectTime, testResponse.getCreateTime());
            
            String name = fd.getFileName();
            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
            Arrays.sort(illegalChars);

            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
                    || StringUtils.containsCharacters(name, illegalChars)) {
                continue;
            }
            
			QueryRequest qr = queryRequestFactory.createQuery(fd.getFileName());
			Response[] hits = keywordIndex.query(qr);
			assertNotNull("didn't get a response for query " + qr, hits);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
            if(hits.length == 0)
                fail("no reply for query: " + qr);
			if ( hits.length != 1 )
                continue;
			checked = true;
			
			// first check basic stuff on the response.
			assertEquals("responses should be equal", testResponse, hits[0]);
            assertEquals("wrong creation time", expectTime,
                                                hits[0].getCreateTime());
			    
			// then actually create a QueryReply and read it, to make
			// sure we can write & read stuff correctly.
            QueryReply qReply = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte) 4, 6346,
                    IP, 0, hits, GUID.makeGuid(), new byte[0], false, false,
                    true, true, true, false, null);			    
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qReply.write(baos);
            ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
            QueryReply readQR = (QueryReply) messageFactory.read(bais, Network.TCP);
            
            List readHits = readQR.getResultsAsList();
            assertEquals("wrong # of results", hits.length, readHits.size());
            Response hit = (Response)readHits.get(0);
            // rounds off to seconds (drops milliseconds)
            assertEquals("wrong creation time", expectTime / 1000 * 1000,
                                                hit.getCreateTime());
		}
        
        assertTrue("didn't check anything!", checked);
    }

    /**
     * Test to make sure that results that have no name are rejected 
     */
    public void testThatEmptyResultsAreRejected() throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
 		// create a payload that says it has one result, but whose
        // result is empty.  This should be rejected!
		byte[] payload=new byte[11+11+16];
		payload[0] = 1;            //Number of results
		payload[1]=1;              //non-zero port
        payload[3]=1;                //non-blank ip
		//payload[11+8]=(byte)65;  //The character 'A'

		QueryReply qr = 
            queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);

        
        try {
            qr.getResultsAsList();
            fail("should have thrown an exception for empty result");
        } catch(BadPacketException e) {
        }
    }
    
    public void testSecureReplyNoSignature() throws Exception {
        int indexes[] = new int[2];
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        
        GGEP ggep = new GGEP(true);
        ggep.put("SB");
        QueryReply reply = newSecureQueryReply(ggep, indexes, payload);
        assertTrue(reply.hasSecureData());
        assertNull(reply.getSecureSignature());
        runSignatureTest(reply, indexes, payload.toByteArray());
    }
    
    public void testSecureReplyWithSignature() throws Exception {
        int indexes[] = new int[2];
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        
        GGEP ggep = new GGEP(true);
        ggep.put("SB");
        byte[] sig = new byte[100];
        new Random().nextBytes(sig);
        ggep.put("SIG", sig);
        QueryReply reply = newSecureQueryReply(ggep, indexes, payload);
        assertTrue(reply.hasSecureData());
        assertEquals(sig, reply.getSecureSignature());
        runSignatureTest(reply, indexes, payload.toByteArray());
    }
    
    public void testNoSecureReply() throws Exception {
        int indexes[] = new int[2];
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        
        GGEP ggep = new GGEP(true);
        ggep.put("SC");
        byte[] sig = new byte[100];
        new Random().nextBytes(sig);
        ggep.put("SIG", sig);        
        QueryReply reply = newSecureQueryReply(ggep, indexes, payload);
        assertFalse(reply.hasSecureData());
        assertNull(reply.getSecureSignature());
        Provider provider = new TestProvider();
        Signature signature = Signature.getInstance("FakeSignature", provider);
        signature.initSign(null);
        reply.updateSignatureWithSecuredBytes(signature);
        assertEquals(-1, FakeSignatureSpi.off1);
        assertEquals(-1, FakeSignatureSpi.off2);
        assertEquals(-1, FakeSignatureSpi.len1);
        assertEquals(-1, FakeSignatureSpi.len2);
        assertNull(FakeSignatureSpi.update1);
        assertNull(FakeSignatureSpi.update2);       
    }
    
    public void testSecureStatus() throws Exception {
        int indexes[] = new int[2];
        ByteArrayOutputStream payload = new ByteArrayOutputStream();        
        GGEP ggep = new GGEP(true);
        QueryReply reply = newSecureQueryReply(ggep, indexes, payload);
        assertEquals(Status.INSECURE, reply.getSecureStatus());
        reply.setSecureStatus(Status.FAILED);
        assertEquals(Status.FAILED, reply.getSecureStatus());
        reply.setSecureStatus(Status.SECURE);
        assertEquals(Status.SECURE, reply.getSecureStatus());
    }
    
    public void testSecurityTokenBytesSetAndParsed() throws IllegalArgumentException, IOException, BadPacketException {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        Response r = responseFactory.createResponse(0, 1, "test", UrnHelper.SHA1);
        QueryReply query = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte)1, 1459,
                InetAddress.getLocalHost().getAddress(), 30945L, new Response[] { r }, GUID.makeGuid(), new byte[0], false, false,
                false, false, false, false, false, IpPort.EMPTY_SET, _token);
                
        assertEquals(_token.getBytes(), query.getSecurityToken());
        
        // test copy constructor preserves security bytes
        query = queryReplyFactory.createQueryReply(GUID.makeGuid(), query);
        assertEquals(_token.getBytes(), query.getSecurityToken());
        
        // test network constructor
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        query.writePayload(out);
        query = queryReplyFactory.createFromNetwork(GUID.makeGuid(), (byte)1,
                (byte)1, out.toByteArray());
        assertEquals(_token.getBytes(), query.getSecurityToken());
    }
    
    private void runSignatureTest(QueryReply reply, int[] indexes, byte[] payload) throws Exception {
        Provider provider = new TestProvider();
        Signature sig = Signature.getInstance("FakeSignature", provider);
        sig.initSign(null);
        reply.updateSignatureWithSecuredBytes(sig);
        assertNotEquals(-1, FakeSignatureSpi.off1);
        assertNotEquals(-1, FakeSignatureSpi.off2);
        assertNotEquals(-1, FakeSignatureSpi.len1);
        assertNotEquals(-1, FakeSignatureSpi.len2);
        assertNotNull(FakeSignatureSpi.update1);
        assertNotNull(FakeSignatureSpi.update2);
        
        assertEquals(0, FakeSignatureSpi.off1);
        assertEquals(indexes[0], FakeSignatureSpi.len1);
        for(int i = 0; i < indexes[0]; i++)
            assertEquals("bad match at offset: " + i, payload[i], FakeSignatureSpi.update1[i]);
        
        assertEquals(indexes[1], FakeSignatureSpi.off2);
        int len2 = payload.length - 16 - indexes[1];
        assertEquals(len2, FakeSignatureSpi.len2);
        for(int i = indexes[1]; i < len2 + indexes[1]; i++)
            assertEquals("bad match at offset: " + i, payload[i], FakeSignatureSpi.update2[i]);
    }
    
    private QueryReply newSecureQueryReply(GGEP secureGGEP, int[] indexes, ByteArrayOutputStream payload) throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        indexes[0] = -1;
        indexes[1] = -1;
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(1); // # of results
        ByteUtils.short2leb((short)6346, out); // port
        out.write(IP); // ip
        ByteUtils.int2leb(1, out);
        Response r = responseFactory.createResponse(0, 1, "test", UrnHelper.SHA1);
        r.writeToStream(out);
        out.write(new byte[] { 'L', 'I', 'M', 'E' });
        out.write(4); // common payload length
        out.write(0x3C); // flags (control no push) 
        out.write(0x21); // control (yes ggep, flag busy)
        ByteUtils.short2leb((short)1, out); // xml size
        out.write(0); // no chat
        
        GGEP ggep = new GGEP(true);
        ggep.put("test", "data");
        ggep.write(out); // normal ggep block.
        
        indexes[0] = out.size();
        secureGGEP.write(out);
        indexes[1] = out.size();
        
        out.write(0); // null after XML
        out.write(new byte[16]); // clientGUID
        
        // copy the payload.
        payload.reset();
        out.writeTo(payload);
        
        return queryReplyFactory.createFromNetwork(new byte[16], (byte)1,
                (byte)1, out.toByteArray());
    }

    private void addFilesToLibrary() throws Exception {
        FileManager fileManager = injector.getInstance(FileManager.class);
        String dirString = "com/limegroup/gnutella";
        File testDir = TestUtils.getResourceFile(dirString);
        testDir = testDir.getCanonicalFile();
        assertTrue("could not find the gnutella directory", testDir.isDirectory());

        File[] testFiles = testDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                // use files with a $ because they'll generally
                // trigger a single-response return, which is
                // easier to check
                return LibraryUtils.isFileManagable(file) && file.getName().indexOf("$") != -1;
            }
        });

        assertNotNull("no files to test against", testFiles);
        assertNotEquals("no files to test against", 0, testFiles.length);

        FileManagerTestUtils.waitForLoad(fileManager, 5000);
        for(File file : testFiles) {
            fileManager.getGnutellaFileList().add(file).get(1, TimeUnit.SECONDS);
        }

        assertEquals("unexpected number of shared files", testFiles.length, fileManager.getGnutellaFileList().size());
    }
    
    private void addAlternateLocationsToFiles() throws Exception {
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        FileManager fileManager = injector.getInstance(FileManager.class);
        for(FileDesc fd : fileManager.getGnutellaFileList()) {
            URN urn = fd.getSHA1Urn();
            for(int j = 0; j < MAX_LOCATIONS + 5; j++) {
                altLocManager.add(alternateLocationFactory.create("1.2.3." + j, urn), null);
            }
        }
    }
    
    private void addCreationTimeToFiles() throws Exception {
        CreationTimeCache creationTimeCache = injector.getInstance(CreationTimeCache.class);
        FileManager fileManager = injector.getInstance(FileManager.class);
        for(FileDesc fd : fileManager.getGnutellaFileList()) {
            long time = (fd.getIndex() + 1) * 10013;
            creationTimeCache.addTime(fd.getSHA1Urn(), time);
            creationTimeCache.commitTime(fd.getSHA1Urn());
        }
    }        
    
    /*
     * a test to see if converting invalid characters from byte array to string and back would work
     */
    public void conversionTest() throws Exception {
        
        byte[] testByte = new byte[]{0x72,0x69,0x63,0x6B,0x20,0x72,0x6F,0x73,0x73,0x20,
        (byte)0x96,0x20,0x61,0x6C,0x6C,0x20,0x77,0x68,0x69,
        0x74,0x65,0x20,0x35,0x32,0x2E,0x77,0x6D,0x61};
        
        String name = new String(testByte, "UTF-8");
        assertNotEquals(testByte.length, name.getBytes("UTF-8").length);
        
    }
    
    public void testParseResults() throws Exception{
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        
        /*
         * a test to see if converting invalid characters from byte array to string and back would work
         */
        conversionTest();
        
        byte[] payload = {1, // number of responses
                          0x12, 0x12, //port
                          0x12, 0x12, 0x12, 0x12, //ip address
                          0x12, 0x12, 0x12, 0x12, //speed
                          0x12, 0x12, 0x12, 0x12, //file index
                          0x12, 0x12, 0x12, 0x12, //file size
                          0x72,0x69,0x63,0x6B,0x20,0x72,0x6F,0x73,0x73,0x20,
                          (byte)0x96,0x20,0x61,0x6C,0x6C,0x20,0x77,0x68,0x69,
                          0x74,0x65,0x20,0x35,0x32,0x2E,0x77,0x6D,0x61, 0x00, //file name - zero terminated
                          0x00,
                          0x72,0x69,0x63,0x6B,0x20,0x72,0x6F,0x73,0x72,0x69,0x63,0x6B,0x20,0x72,0x6F,0x73 // servant GUID
                };
      
        QueryReply newReply = queryReplyFactory.createFromNetwork(new byte[16], (byte)1,
                (byte)1, payload);
               
        assertEquals(1, newReply.getResultCount());
        assertEquals(1, newReply.getUniqueResultCount());
        
        Response response = newReply.getResultsArray()[0];
        assertEquals(28 /* length of invalid unicode */ + 2 /* zero bytes */ + 8 /* index + size */, response.getIncomingLength());
        assertNotEquals(28, response.getName().getBytes("UTF-8").length);
    }
    
    public void testMaxResponses() throws Exception {
        FilterSettings.MAX_RESPONSES_PER_REPLY.setValue(10);
        nResponseTest(FilterSettings.MAX_RESPONSES_PER_REPLY.getValue());
    }
    
    public void testOverMaxResponses() throws Exception {
        FilterSettings.MAX_RESPONSES_PER_REPLY.setValue(10);
        try {
            nResponseTest(FilterSettings.MAX_RESPONSES_PER_REPLY.getValue()+1);
            fail("should not have parsed");
        } catch (BadPacketException expected) {}
    }
     
    private void nResponseTest(int limit) throws Exception {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        // try a reply with max number of responses
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(limit); // # of results
        ByteUtils.short2leb((short)6346, out); // port
        out.write(IP); // ip
        ByteUtils.int2leb(1, out);
        for (int i = 0; i < limit; i++) 
            responseFactory.createResponse(i, 1, "test"+i, UrnHelper.SHA1).writeToStream(out);
        
        out.write(new byte[] { 'L', 'I', 'M', 'E' });
        out.write(4); // common payload length
        out.write(0x3C); // flags (control no push) 
        out.write(0x21); // control (yes ggep, flag busy)
        ByteUtils.short2leb((short)1, out); // xml size
        out.write(0); // no chat
        out.write(0); // null after XML
        out.write(new byte[16]); // clientGUID
        
        QueryReply reply = queryReplyFactory.createFromNetwork(new byte[16], (byte)1,
                (byte)1, out.toByteArray());
        
        assertEquals(limit,reply.getResultsAsList().size());
    }
    
    /*
     * All the below crap is because we can't subclass Signature and instead need to provide an SPI.
     */
    
    /** Provider that references the fake SPI */
    private static final class TestProvider extends Provider {
        TestProvider() {
            super("LIME", 1.0, "LIME test provider");
            put("Signature.FakeSignature", FakeSignatureSpi.class.getName());
        }
    }
    
    /** SPI that stores things statically because there's no other reference to it anywhere. */
    @SuppressWarnings("deprecation")
    public static class FakeSignatureSpi extends SignatureSpi {
        private static byte[] update1;
        private static int off1;
        private static int len1;
        
        private static byte[] update2;
        private static int off2;
        private static int len2;

        protected Object engineGetParameter(String arg0) throws InvalidParameterException {
            return null;
        }

        protected void engineInitSign(PrivateKey arg0) throws InvalidKeyException {
            update1 = null;
            off1 = -1;
            len1 = -1;
            update2 = null;
            off2 = -1;
            len2 = -1;
        }

        protected void engineInitVerify(PublicKey arg0) throws InvalidKeyException {
        }

        protected void engineSetParameter(String arg0, Object arg1) throws InvalidParameterException {
        }

        protected byte[] engineSign() throws SignatureException {
            return null;
        }

        protected void engineUpdate(byte arg0) throws SignatureException {
        }

        protected void engineUpdate(byte[] data, int off, int len) throws SignatureException {
            if(update1 == null) {
                update1 = data;
                off1 = off;
                len1 = len;
            } else if(update2 == null) {
                update2 = data;
                off2 = off;
                len2 = len;
            } else {
                fail("updating signature more than twice!");
            }
        }

        protected boolean engineVerify(byte[] arg0) throws SignatureException {
            return false;
        }
    }
    

    public void testAddGGEP() throws Exception {
           QueryReplyFactoryImpl queryReplyFactory = (QueryReplyFactoryImpl) injector.getInstance(QueryReplyFactory.class);
            //Normal case: basic metainfo with no vendor data
            payload=new byte[11+11+(4+1+1)+16];
            payload[0]=1;            //Number of results
            payload[1]=1;            //non-zero port
            payload[3]=1;            //non-blank ip     
            payload[11+8]=(byte)65;  //The character 'A'
            payload[11+11+0]=(byte)76;   //The character 'L'
            payload[11+11+1]=(byte)105;  //The character 'i'
            payload[11+11+2]=(byte)77;   //The character 'M'
            payload[11+11+3]=(byte)69;   //The character 'E'
            payload[11+11+4+0]=(byte)1;  //The size of public area
            payload[11+11+4+1]=(byte)0x1; // interestingly, we don't parse these...
            
            byte [] guid = GUID.makeGuid();
            System.arraycopy(guid,0,payload,11+11+4+2,16);
            
            QueryReplyImpl qr=(QueryReplyImpl)queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                    (byte)0, payload);
            
            assertTrue(Arrays.equals(guid, qr.getClientGUID()));
            
            iter=qr.getResults();
            Response response=(Response)iter.next();
            assertEquals("A", response.getName());
            assertFalse(iter.hasNext());
            
            // no ggep present
            assertEquals(-1, qr.getGGEPStart());
            assertEquals(-1, qr.getGGEPEnd());
            
            // add some ggep
            GGEP g = new GGEP();
            g.put("badger");
            byte [] ggepB = g.toByteArray();
            QueryReplyImpl patched = (QueryReplyImpl)queryReplyFactory.createWithNewGGEP(qr, ggepB);
            assertNotEquals(-1, patched.getGGEPStart());
            assertEquals(patched.getGGEPStart() + ggepB.length, patched.getGGEPEnd());
            assertTrue(Arrays.equals(guid, patched.getClientGUID()));
            
            iter=patched.getResults();
            assertTrue(patched.getNeedsPush());
            response=(Response)iter.next();
            assertEquals("A", response.getName());
            assertFalse(iter.hasNext());
    }
    
    public void testReplaceGGEP() throws Exception {
        
        // create a query with all kinds of ggep fields in it
        QueryReplyFactoryImpl queryReplyFactory = (QueryReplyFactoryImpl) injector.getInstance(QueryReplyFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);

        String[] hosts = {"www.limewire.com", "www.limewire.org",
                "www.susheeldaswani.com", "www.berkeley.edu"};

        //PushProxyInterface[] proxies = new PushProxyInterface[outer+1];
        Set proxies = new TreeSet(IpPort.COMPARATOR);
        for (int i = 0; i < hosts.length; i++)
            proxies.add(
                    new IpPortImpl(hosts[i], 6346));

        // is multicast, has proxies, supports chat...
        QueryReply qr = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte) 4, 6346,
                IP, 0, new Response[0], GUID.makeGuid(), new byte[0], false, false,
                true, true, true, true, proxies);

        QueryReplyImpl readQR = reparse(qr, messageFactory);
        assertTrue(readQR.getSupportsChat());
        assertTrue(readQR.isFakeMulticast());

        // test read from network            
        Set retProxies = readQR.getPushProxies();
        assertNotNull(retProxies);
        assertTrue(retProxies != proxies);
        assertEquals(retProxies.size(), proxies.size());
        assertEquals(retProxies, proxies);
        assertEquals(proxies, retProxies);

        // replace the new ggep field
        
        GGEP g = new GGEP();
        g.put("badger");
        byte [] ggepB = g.toByteArray();
        QueryReply replaced = queryReplyFactory.createWithNewGGEP(readQR, ggepB);
        
        // we have no proxies anymore as ggep got replaced.
        assertEquals(0,replaced.getPushProxies().size());
        // not multicast either
        assertFalse(replaced.isFakeMulticast());
        // still support chat since that's in the QHD area
        assertTrue(replaced.getSupportsChat());
    }
    
    public void testPatchGGEPWithReturnPath() throws Exception {
           // create a query with all kinds of ggep fields in it
        QueryReplyFactoryImpl queryReplyFactory = (QueryReplyFactoryImpl) injector.getInstance(QueryReplyFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);

        String[] hosts = {"www.limewire.com", "www.limewire.org",
                "www.susheeldaswani.com", "www.berkeley.edu"};

        //PushProxyInterface[] proxies = new PushProxyInterface[outer+1];
        Set proxies = new TreeSet(IpPort.COMPARATOR);
        for (int i = 0; i < hosts.length; i++)
            proxies.add(
                    new IpPortImpl(hosts[i], 6346));

        // is multicast, has proxies, supports chat...
        QueryReply qr = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte) 4, 6346,
                IP, 0, new Response[0], GUID.makeGuid(), new byte[0], false, false,
                true, true, true, true, proxies);

        
        QueryReplyImpl readQR = reparse(qr, messageFactory);
        assertTrue(readQR.getSupportsChat());
        assertTrue(readQR.isFakeMulticast());

        // test read from network            
        Set retProxies = readQR.getPushProxies();
        assertNotNull(retProxies);
        assertTrue(retProxies != proxies);
        assertEquals(retProxies.size(), proxies.size());
        assertEquals(retProxies, proxies);
        assertEquals(proxies, retProxies);
        
        // add some uknown GGEP fields
        GGEP g = new GGEP(readQR.getPayload(),readQR.getGGEPStart());
        g.put("badger","mushroom".getBytes());
     
        // all ggep-carried information is preserved
        QueryReplyImpl unknownGGEP = 
            reparse(queryReplyFactory.createWithNewGGEP(readQR, g.toByteArray()),messageFactory);
        assertTrue(unknownGGEP.getSupportsChat());
        assertTrue(unknownGGEP.isFakeMulticast());
        retProxies = unknownGGEP.getPushProxies();
        assertNotNull(retProxies);
        assertFalse(retProxies.isEmpty());
        
        // now patch in some return path info
        IpPort me = new IpPortImpl("1.1.1.1",1);
        IpPort source = new IpPortImpl("2.2.2.2",2);
        
        QueryReplyImpl returnPath = reparse(queryReplyFactory.createWithReturnPathInfo(unknownGGEP, me, source),messageFactory);
        // all previous ggep info is there
        assertTrue(returnPath.getSupportsChat());
        assertTrue(returnPath.isFakeMulticast());
        retProxies = returnPath.getPushProxies();
        assertNotNull(retProxies);
        assertFalse(retProxies.isEmpty());
        
        // also, we'll find the unknown entry there
        GGEP parsedGGEP = new GGEP(returnPath.getPayload(),returnPath.getGGEPStart());
        byte [] mushroom = parsedGGEP.get("badger");
        assertTrue(Arrays.equals("mushroom".getBytes(),mushroom));
        
        // and we'll find some return-path specific entries as well
        byte [] meParsed = parsedGGEP.get("RPI0");
        assertEquals(6, meParsed.length);
        assertEquals(1,meParsed[0]);
        assertEquals(1,meParsed[1]);
        assertEquals(1,meParsed[2]);
        assertEquals(1,meParsed[3]);
        assertEquals(1,ByteUtils.beb2short(meParsed, 4));
        
        byte[] sourceParsed = parsedGGEP.get("RPS0");
        assertEquals(6, sourceParsed.length);
        assertEquals(2,sourceParsed[0]);
        assertEquals(2,sourceParsed[1]);
        assertEquals(2,sourceParsed[2]);
        assertEquals(2,sourceParsed[3]);
        assertEquals(2,ByteUtils.beb2short(sourceParsed, 4));
        
        // tll
        assertEquals(unknownGGEP.getTTL(),parsedGGEP.getInt("RPT0"));
        // hops
        assertEquals(unknownGGEP.getHops(),parsedGGEP.getInt("RPH0"));
    }
    
    public void testMultipleReturnPaths() throws Exception {
        // create a query with all kinds of ggep fields in it
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);

        String[] hosts = {"www.limewire.com", "www.limewire.org",
                "www.susheeldaswani.com", "www.berkeley.edu"};

        //PushProxyInterface[] proxies = new PushProxyInterface[outer+1];
        Set proxies = new TreeSet(IpPort.COMPARATOR);
        for (int i = 0; i < hosts.length; i++)
            proxies.add(
                    new IpPortImpl(hosts[i], 6346));

        // is multicast, has proxies, supports chat...
        QueryReply qr = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte) 4, 6346,
                IP, 0, new Response[0], GUID.makeGuid(), new byte[0], false, false,
                true, true, true, true, proxies);

        
        QueryReply readQR = reparse(qr, messageFactory);
        
        // add a bunch of return nodes
        IpPort hop0 = new IpPortImpl("0.0.0.0",0);
        IpPort hop1 = new IpPortImpl("1.1.1.1",1);
        IpPort hop2 = new IpPortImpl("2.2.2.2",2);
        IpPort hop3 = new IpPortImpl("3.3.3.3",3);
        
        QueryReplyImpl qr1 = reparse(queryReplyFactory.createWithReturnPathInfo(readQR, hop1, hop0), messageFactory);
        QueryReplyImpl qr2 = reparse(queryReplyFactory.createWithReturnPathInfo(qr1, hop2, hop1), messageFactory);
        QueryReplyImpl qr3 = reparse(queryReplyFactory.createWithReturnPathInfo(qr2, hop3, hop2), messageFactory);
        
        GGEP parsedGGEP1 = new GGEP(qr1.getPayload(), qr1.getGGEPStart());
        GGEP parsedGGEP2 = new GGEP(qr2.getPayload(), qr2.getGGEPStart());
        GGEP parsedGGEP3 = new GGEP(qr3.getPayload(), qr3.getGGEPStart());
        
        // make sure that data is preserved in every message
        byte [] parsedHop1 = parsedGGEP1.get("RPI0");
        assertTrue(Arrays.equals(parsedHop1,parsedGGEP2.get("RPI0")));
        assertTrue(Arrays.equals(parsedHop1,parsedGGEP3.get("RPI0")));
        
        assertNull(parsedGGEP1.get("RPI1"));
        byte [] parsedHop2 = parsedGGEP2.get("RPI1");
        assertTrue(Arrays.equals(parsedHop2,parsedGGEP3.get("RPI1")));
        
        assertNull(parsedGGEP1.get("RPI2"));
        assertNull(parsedGGEP2.get("RPI2"));
        byte [] parsedHop3 = parsedGGEP3.get("RPI2");
        
        // qr1 came from hop0 
        byte [] parsedHop0 = parsedGGEP1.get("RPS0");
        
        // qr2 came from hop1
        assertTrue(Arrays.equals(parsedHop1,parsedGGEP2.get("RPS1")));
        // it also carries the source of qr1 (hop0)
        assertTrue(Arrays.equals(parsedHop0,parsedGGEP2.get("RPS0")));
        
        // qr3 came from hop2
        assertTrue(Arrays.equals(parsedHop2,parsedGGEP3.get("RPS2")));
        // it carries the sources of qr1 (hop0) and qr2 (hop1)
        assertTrue(Arrays.equals(parsedHop0,parsedGGEP2.get("RPS0")));
        assertTrue(Arrays.equals(parsedHop1,parsedGGEP2.get("RPS1")));
        
        // check that the addresses are valid
        assertEquals(6,parsedHop0.length);
        assertEquals(6,parsedHop1.length);
        assertEquals(6,parsedHop2.length);
        assertEquals(6,parsedHop3.length);
        
        for (int i = 0;i < 4; i++) {
            assertEquals((byte )0,parsedHop0[i]);
            assertEquals((byte )1,parsedHop1[i]);
            assertEquals((byte )2,parsedHop2[i]);
            assertEquals((byte )3,parsedHop3[i]);
        }
        
        assertEquals((short)0,ByteUtils.beb2short(parsedHop0,4));
        assertEquals((short)1,ByteUtils.beb2short(parsedHop1,4));
        assertEquals((short)2,ByteUtils.beb2short(parsedHop2,4));
        assertEquals((short)3,ByteUtils.beb2short(parsedHop3,4));
    }
    
    
    private QueryReplyImpl reparse(QueryReply original, MessageFactory messageFactory) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        original.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        return (QueryReplyImpl) messageFactory.read(bais, Network.TCP);
    }
    
    public void testMassageGetVendor() throws UnknownHostException, BadPacketException {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);

        payload=new byte[11+11+16];
		payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
		payload[3]=1;            //non-blank ip
		payload[11+8]=(byte)65;  //The character 'A'

		QueryReply queryReply = queryReplyFactory.createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);
        
        assertEquals("", queryReply.getVendor());

    }
    
    public void testMassageIsChatEnabled() throws UnknownHostException, BadPacketException {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        Response r = responseFactory.createResponse(0, 1, "test", UrnHelper.SHA1);

        QueryReply queryReply = getQueryReplyWithChatEnabledInputs(queryReplyFactory, r, false, false);
        assertEquals(false, queryReply.getSupportsChat());

        queryReply = getQueryReplyWithChatEnabledInputs(queryReplyFactory, r, false, true);
        assertEquals(false, queryReply.getSupportsChat());

        queryReply = getQueryReplyWithChatEnabledInputs(queryReplyFactory, r, true, false);
        assertEquals(true, queryReply.getSupportsChat());

        queryReply = getQueryReplyWithChatEnabledInputs(queryReplyFactory, r, true, true);
        assertEquals(false, queryReply.getSupportsChat());
    }

    private QueryReply getQueryReplyWithChatEnabledInputs(QueryReplyFactory queryReplyFactory, Response r, boolean supportsChat, boolean isFirewalled) throws UnknownHostException, BadPacketException {
        // needsPush is a shorthand for isFirewalled, if isMulticast = false
        return queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte)1, 1459,
                InetAddress.getLocalHost().getAddress(), 30945L, new Response[] { r }, GUID.makeGuid(), new byte[0], isFirewalled, false,
                false, false, supportsChat, false, false, IpPort.EMPTY_SET, _token);
    }

    public void testMassageGetSpeed() throws UnknownHostException, BadPacketException {
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        Response r = responseFactory.createResponse(0, 1, "test", UrnHelper.SHA1);

        // isReplyToMulticast = false
        QueryReply query = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte)1, 1459,
                InetAddress.getLocalHost().getAddress(), 30945L, new Response[] { r }, GUID.makeGuid(), new byte[0], false, false,
                false, false, false, false, false, IpPort.EMPTY_SET, _token);

        assertEquals(30945L, query.getSpeed());

        // isReplyToMulticast = true
        query = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte)1, 1459,
                InetAddress.getLocalHost().getAddress(), 30945L, new Response[] { r }, GUID.makeGuid(), new byte[0], false, false,
                false, false, false, true, false, IpPort.EMPTY_SET, _token);

        query.setMulticastAllowed(true);
        assertEquals(Integer.MAX_VALUE, query.getSpeed());
    }
    
    public void testMassageIsFirewalled() throws UnknownHostException, BadPacketException {        

        QueryReply queryReply = getQueryReplyWithFirewallInputs(false, false, false);
        assertEquals(false, queryReply.isFirewalled());

        queryReply = getQueryReplyWithFirewallInputs(false, false, true);
        assertEquals(false, queryReply.isFirewalled());
        
        queryReply = getQueryReplyWithFirewallInputs(false, true, false);
        assertEquals(true, queryReply.isFirewalled());
        
        queryReply = getQueryReplyWithFirewallInputs(false, true, true);
        assertEquals(false, queryReply.isFirewalled());
        
        queryReply = getQueryReplyWithFirewallInputs(true, false, false);
        assertEquals(true, queryReply.isFirewalled());

        queryReply = getQueryReplyWithFirewallInputs(true, false, true);
        assertEquals(false, queryReply.isFirewalled());
        
        queryReply = getQueryReplyWithFirewallInputs(true, true, false);
        assertEquals(true, queryReply.isFirewalled());
        
        queryReply = getQueryReplyWithFirewallInputs(true, true, true);
        assertEquals(false, queryReply.isFirewalled());
        
        payload=new byte[11+11+16];
		payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
		payload[3]=1;            //non-blank ip
		payload[11+8]=(byte)65;  //The character 'A'

		queryReply = injector.getInstance(QueryReplyFactory.class).createFromNetwork(new byte[16], (byte)5,
                (byte)0, payload);
        try {
			queryReply.getNeedsPush();
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
        
        assertEquals(true, queryReply.isFirewalled());
        
    }

    private QueryReply getQueryReplyWithFirewallInputs(boolean needsPush, final boolean isPrivateAddress, final boolean isReplyToMulticast) throws UnknownHostException, BadPacketException {
        Mockery mockery = new Mockery();
        final NetworkInstanceUtils mockNetworkInstanceUtils = mockery.mock(NetworkInstanceUtils.class);
        mockery.checking(new Expectations() {{
            atLeast(0).of(mockNetworkInstanceUtils).isPrivate();
            will(returnValue(isPrivateAddress));
            atLeast(0).of(mockNetworkInstanceUtils).isPrivateAddress(InetAddress.getLocalHost());
            will(returnValue(isPrivateAddress));
            atLeast(0).of(mockNetworkInstanceUtils).isPrivateAddress(InetAddress.getLocalHost().getAddress());
            will(returnValue(isPrivateAddress));
            atLeast(0).of(mockNetworkInstanceUtils).isPrivateAddress(NetworkUtils.ip2string(InetAddress.getLocalHost().getAddress()));
            will(returnValue(isPrivateAddress));
        }});
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkInstanceUtils.class).toInstance(mockNetworkInstanceUtils);
            }
	    });
        
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        Response r = responseFactory.createResponse(0, 1, "test", UrnHelper.SHA1);
        
        QueryReply query = queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte)1, 1459,
            InetAddress.getLocalHost().getAddress(), 30945L, new Response[] { r }, GUID.makeGuid(), new byte[0], needsPush, false,
            false, false, false, isReplyToMulticast, false, IpPort.EMPTY_SET, _token);
        query.setMulticastAllowed(true);
        return query;
    }
}
