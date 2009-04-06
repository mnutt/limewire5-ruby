package com.limegroup.gnutella.filters;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Collections;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.util.NameValue;

import com.google.inject.Injector;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLUtils;

public class XMLDocFilterTest extends LimeTestCase {

    protected byte[] address;
    private ResponseFactory responseFactory;
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    private QueryReplyFactory queryReplyFactory;
    private LimeXMLDocumentHelper limeXMLDocumentHelper;
    
    public XMLDocFilterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMLDocFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        address = InetAddress.getLocalHost().getAddress();
        Injector injector = LimeTestUtils.createInjector();
        responseFactory = injector.getInstance(ResponseFactory.class);
        limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        limeXMLDocumentHelper = injector.getInstance(LimeXMLDocumentHelper.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
    }
    
    public void testDisallowAdultXML() throws Exception {
        XMLDocFilter filter = new XMLDocFilter();
        
        
        // test filter switched off
        QueryReply qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "Adult");
        assertTrue(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "R");
        assertTrue(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "NC-17");
        assertTrue(filter.allow(qr));
        
        filter.disallowAdult();
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "R");
        assertFalse(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "NC-17");
        assertFalse(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "Adult");
        assertFalse(filter.allow(qr));

        qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "Adult Film");
        assertFalse(filter.allow(qr));

        qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "Adult Film Clip");
        assertFalse(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "adul");
        assertTrue(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "NR");
        assertTrue(filter.allow(qr));
        
        qr = createReply("Adult Film", responseFactory);
        assertFalse(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "Adults Only");
        assertFalse(filter.allow(qr));
    }
    
    private QueryReply createXMLReply(String field, String values) throws Exception {
        return createXMLReply(field, values, 5555, address);
    }
    
    public QueryReply createXMLReply(String field, String values, int port, byte[] address) throws Exception {
        
        Response resp = createXMLResponse("filename", field, values, responseFactory, limeXMLDocumentFactory);
        
        return createReply(resp, port, address, queryReplyFactory, limeXMLDocumentHelper);
    }
    
    public static QueryReply createReply(Response resp, int port, byte[] address, QueryReplyFactory queryReplyFactory,
            LimeXMLDocumentHelper limeXMLDocumentHelper) throws Exception {
        QueryReply qr = createReply(resp, new GUID(), port, address, queryReplyFactory);

        // write out to network so it is parsed again and xml docs are constructed
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        qr.writePayload(out);
        qr = queryReplyFactory.createFromNetwork(GUID.makeGuid(), (byte) 1, (byte) 1, out.toByteArray());
        
        // hack, query replies have to be equipped with xml again
        ForMeReplyHandler.addXMLToResponses(qr, limeXMLDocumentHelper);
        return qr;
    }
    
    protected QueryReply createReply(String response, ResponseFactory responseFactory) {
        return createReply(responseFactory.createResponse(5, 5, response, UrnHelper.SHA1), new GUID(), 5555, address, queryReplyFactory); 
    }
    
    public static QueryReply createReply(Response resp, GUID guid, int port, byte[] address, QueryReplyFactory queryReplyFactory) {
        String xmlCollectionString = LimeXMLDocumentHelper.getAggregateString(new Response [] { resp } );
        if (xmlCollectionString == null)
            xmlCollectionString = "";

        byte[] xmlBytes = null;
        try {
            xmlBytes = xmlCollectionString.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ueex) {//no support for utf-8?? 
        }
        byte[] xmlCompressed = null;
        if (!xmlCollectionString.equals(""))
            xmlCompressed = LimeXMLUtils.compress(xmlBytes);
        else //there is no XML
            xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY;
        
        return queryReplyFactory.createQueryReply(guid.bytes(), (byte)1,
                port, address, 0, new Response[] { resp }, GUID.makeGuid(), xmlCompressed, false,
                false, true, true, true, false);
    }   
    
    public static Response createXMLResponse(String fileName, String field, String values, ResponseFactory responseFactory, LimeXMLDocumentFactory limeXMLDocumentFactory) throws Exception {
        NameValue<String> nameValue = new NameValue<String>(field, values);
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(Collections.singletonList(nameValue),
                LimeXMLNames.VIDEO_SCHEMA);
        return responseFactory.createResponse(101, 1340, fileName, doc, UrnHelper.SHA1);
    }
}
