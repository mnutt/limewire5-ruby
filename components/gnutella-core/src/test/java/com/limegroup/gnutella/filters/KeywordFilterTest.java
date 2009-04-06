package com.limegroup.gnutella.filters;

import java.util.List;
import java.util.Vector;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * Unit tests for KeywordFilter
 */
public class KeywordFilterTest extends BaseTestCase {
    
    KeywordFilter filter=new KeywordFilter();
    
    QueryRequest queryRequestMock=null;
    QueryReply queryReplyMock = null;
    PingRequest pingRequestMock = null;
    Mockery context;
    
    private ResponseFactory responseFactory;
    
    
	public KeywordFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(KeywordFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
//              bind(ResponseVerifier.class).to(StubVerifier.class);
//              bind(SearchResultHandler.class).to(SearchResultHandlerImpl.class);
            }
        });
        
        context = new Mockery();
                
        queryRequestMock = context.mock(QueryRequest.class);
        queryReplyMock = context.mock(QueryReply.class);
        pingRequestMock = context.mock(PingRequest.class);
        
        responseFactory = injector.getInstance(ResponseFactory.class);
    }
    
    private void keywordContextValue(QueryRequest query, String keyword){
        final QueryRequest localQuery = query;
        final String localKeyword = keyword;
        
        context.checking(new Expectations() {{
            one(localQuery).getQuery();
            will(returnValue(localKeyword));
        }});  
                
    }
    
    public void testAllowKeyword(){
                
        keywordContextValue(queryRequestMock, "pie with rhubarb");
        assertTrue(filter.allow(queryRequestMock));
        filter.disallow("britney spears");
                
        filter.disallow("rhuBarb");
        
        keywordContextValue(queryRequestMock, "rhubar");
        assertTrue(filter.allow(queryRequestMock));
                 
        keywordContextValue(queryRequestMock, "pie with rhubarb");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "rhubarb.txt");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "Rhubarb*");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "Rhubarb#");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "Rhubarb***");
        assertFalse(filter.allow(queryRequestMock));
        
        context.assertIsSatisfied();
    }

    public void testAllowFileExt(){      
        keywordContextValue(queryRequestMock, "test.vbs");
        assertTrue(filter.allow(queryRequestMock));

        filter.disallow(".vbs");
        
        keywordContextValue(queryRequestMock, "test.vbs");
        assertFalse(filter.allow(queryRequestMock));
                
        keywordContextValue(queryRequestMock, "test.htm");
        assertTrue(filter.allow(queryRequestMock));
        
        filter.disallow(".htm");
                 
        keywordContextValue(queryRequestMock, "test.htm");
        assertFalse(filter.allow(queryRequestMock));
                
        keywordContextValue(queryRequestMock, "test.wmv");
        assertTrue(filter.allow(queryRequestMock));
               
        keywordContextValue(queryRequestMock, "test.asf");
        assertTrue(filter.allow(queryRequestMock));
                
        filter.disallow(".asf");
        filter.disallow(".asx");
        filter.disallow(".wmv");

        keywordContextValue(queryRequestMock, "test.wmv");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "test.asf");
        assertFalse(filter.allow(queryRequestMock));
        
        context.assertIsSatisfied();    
    }
    
    public void testDisallowAdult() throws Exception {
        KeywordFilter filter = new KeywordFilter();
        
        createResponseList(queryReplyMock, "adult");
        assertTrue(filter.allow(queryReplyMock));
        
        createResponseList(queryReplyMock, "Sex");
        assertTrue(filter.allow(queryReplyMock));

        /*
         * turn filter on
         */
        filter.disallowAdult();
        
        createResponseList(queryReplyMock, "adult");
        assertFalse(filter.allow(queryReplyMock));
        
        createResponseList(queryReplyMock, "Sex");
        assertFalse(filter.allow(queryReplyMock));
                
        createResponseList(queryReplyMock, "innocent");
        assertTrue(filter.allow(queryReplyMock));
        
        context.assertIsSatisfied();
        
    }
    
    public void queryReply(QueryRequest query, String keyword){
        final QueryRequest localQuery = query;
        final String localKeyword = keyword;
        
        context.checking(new Expectations() {{
            one(localQuery).getQuery();
            will(returnValue(localKeyword));
        }});  
                
    }
    
    public void testOtherMessagesAreIgnored() throws Exception{
        context.checking(new Expectations()
        {{ never(pingRequestMock);
        }});
        
        assertTrue(filter.allow(pingRequestMock));
        
        context.assertIsSatisfied();
    }
    
    protected void createResponseList (QueryReply qr, String response)throws BadPacketException{
                
        final QueryReply localqr = qr;
        final List<Response> responseList = new Vector<Response>();
        
        //create Response
        final Response qrResponse;
        long index = 0;
        long size = 0;
        LimeXMLDocument emptyDoc = null;
        
        qrResponse = responseFactory.createResponse(index, size, response, emptyDoc, UrnHelper.SHA1);
                     
        /*
         * add Response to Response List
         */
         
        responseList.add(qrResponse);
        
        context.checking(new Expectations() {{
            one(localqr).getResultsAsList();
            will(returnValue(responseList));
        }});  
    
    }
}
