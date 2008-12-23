package com.limegroup.gnutella.search;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.NameValue;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.ResponseVerifier;
import com.limegroup.gnutella.ResponseVerifierImpl;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;

public class SearchResultHandlerTest extends LimeTestCase {
    
    private LimeXMLDocumentFactory factory;
    private MyActivityCallback callback;
    private LimeXMLDocumentHelper limeXMLDocumentHelper;
    private SearchResultHandler searchResultHandler;
    private ResponseFactory responseFactory;
    private QueryReplyFactory queryReplyFactory;

    public SearchResultHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SearchResultHandlerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(MyActivityCallback.class, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ResponseVerifier.class).to(StubVerifier.class);
            }
        });

        callback = (MyActivityCallback) injector.getInstance(ActivityCallback.class);
        
        limeXMLDocumentHelper = injector.getInstance(LimeXMLDocumentHelper.class);
        
        factory = injector.getInstance(LimeXMLDocumentFactory.class);
        
        searchResultHandler = injector.getInstance(SearchResultHandler.class);
        
        responseFactory = injector.getInstance(ResponseFactory.class);
        
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
    }
    
    public void testSecureActionSent() throws Exception {
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        Response actionResponse = responseFactory.createResponse(0, 1, "test", actionDoc, UrnHelper.SHA1);
        QueryReply reply = newQueryReply(new Response[] { actionResponse } );
        reply.setSecureStatus(Status.SECURE);
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(1, callback.results.size());
        RemoteFileDesc rfd = callback.getRFD();
        assertNotNull(rfd.getXMLDocument());
        assertEquals("http://somewhere.com", rfd.getXMLDocument().getAction());
        assertEquals(Status.SECURE, rfd.getSecureStatus());
    }
    
    public void testInsecureActionNotSent() throws Exception {
        Response actionResponse = responseFactory.createResponse(0, 1, "test", UrnHelper.SHA1);
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        QueryReply reply = newQueryReply(new Response[] { actionResponse } );
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(0, callback.results.size());        
    }
    
    public void testInsecureResponseWithoutActionSent() throws Exception {
        Response actionResponse = responseFactory.createResponse(0, 1, "test", UrnHelper.SHA1);
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        
        Response noDoc = responseFactory.createResponse(1, 2, "other", UrnHelper.SHA1);
        QueryReply reply = newQueryReply(new Response[] { actionResponse, noDoc } );
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(1, callback.results.size());
        RemoteFileDesc rfd = callback.getRFD();
        assertNull(rfd.getXMLDocument());
        assertEquals("other", rfd.getFileName());
    }
    
    public void testFailedReplyNotForwarded() throws Exception {
        Response actionResponse = responseFactory.createResponse(0, 1, "test", UrnHelper.SHA1);
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        
        Response noDoc = responseFactory.createResponse(1, 2, "other", UrnHelper.SHA1);
        QueryReply reply = newQueryReply(new Response[] { actionResponse, noDoc } );
        reply.setSecureStatus(Status.FAILED);
        
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(0, callback.results.size());        
    }
    
    private QueryReply newQueryReply(Response[] responses) throws Exception {
        QueryReply reply = 
               queryReplyFactory.createQueryReply(new byte[16], (byte)1, 6346,
                new byte[] { (byte)1, (byte)1, (byte)1, (byte)1 }, 1, responses, new byte[16], LimeXMLDocumentHelper.getAggregateString(responses).getBytes(), false, false,
                true, true, false, false, false, IpPort.EMPTY_SET, null);
        ForMeReplyHandler.addXMLToResponses(reply, limeXMLDocumentHelper);
        return reply;
    }
    
    @Singleton
    private static class MyActivityCallback extends ActivityCallbackStub {
        
        List<RemoteFileDesc> results = new LinkedList<RemoteFileDesc>();

        @Override
        public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply, Set alts) {
            results.add(rfd);
        }
        
        public RemoteFileDesc getRFD() {
            return results.remove(0);
        }
    }
    
    @Singleton
    private static class StubVerifier extends ResponseVerifierImpl {

        @Override
        public synchronized boolean matchesQuery(byte[] guid, Response response) {
            return true;
        }

        @Override
        public boolean isMandragoreWorm(byte[] guid, Response response) {
            return false;
        }

        @Override
        public boolean matchesType(byte[] guid, Response response) {
            return true;
        } 
        
    }
}
