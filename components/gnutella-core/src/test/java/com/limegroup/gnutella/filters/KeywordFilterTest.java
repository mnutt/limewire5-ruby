package com.limegroup.gnutella.filters;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Unit tests for KeywordFilter
 */
public class KeywordFilterTest extends BaseTestCase {

    KeywordFilter filter;
    QueryRequest queryRequestMock = null;
    QueryReply queryReplyMock = null;
    PingRequest pingRequestMock = null;
    Mockery context;
    List<String> banned;

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
        Injector injector = LimeTestUtils.createInjectorNonEagerly();

        context = new Mockery();
        queryRequestMock = context.mock(QueryRequest.class);
        queryReplyMock = context.mock(QueryReply.class);
        pingRequestMock = context.mock(PingRequest.class);

        responseFactory = injector.getInstance(ResponseFactory.class);

        filter = new KeywordFilter(false, false);
        banned = new ArrayList<String>();
    }

    private void keywordContextValue(QueryRequest query, String keyword){
        final QueryRequest localQuery = query;
        final String localKeyword = keyword;

        context.checking(new Expectations() {{
            one(localQuery).getQuery();
            will(returnValue(localKeyword));
        }});
    }

    public void testRequestKeyword(){
        keywordContextValue(queryRequestMock, "pie with rhubarb");
        assertTrue(filter.allow(queryRequestMock));

        banned.add("radishes");
        banned.add("rhuBarb");
        filter = new KeywordFilter(banned);

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

        keywordContextValue(queryRequestMock, "xyzRhubarb***");
        assertFalse(filter.allow(queryRequestMock));

        context.assertIsSatisfied();
    }

    public void testRequestFileExt(){      
        keywordContextValue(queryRequestMock, "test.vbs");
        assertTrue(filter.allow(queryRequestMock));

        banned.add(".vbs");
        filter = new KeywordFilter(banned);

        keywordContextValue(queryRequestMock, "test.vbs");
        assertFalse(filter.allow(queryRequestMock));

        keywordContextValue(queryRequestMock, "test.htm");
        assertTrue(filter.allow(queryRequestMock));

        banned.add(".htm");
        filter = new KeywordFilter(banned);

        keywordContextValue(queryRequestMock, "test.htm");
        assertFalse(filter.allow(queryRequestMock));

        keywordContextValue(queryRequestMock, "test.wmv");
        assertTrue(filter.allow(queryRequestMock));

        keywordContextValue(queryRequestMock, "test.asf");
        assertTrue(filter.allow(queryRequestMock));

        banned.add(".asf");
        banned.add(".asx");
        banned.add(".wmv");
        filter = new KeywordFilter(banned);

        keywordContextValue(queryRequestMock, "test.wmv");
        assertFalse(filter.allow(queryRequestMock));

        keywordContextValue(queryRequestMock, "test.asf");
        assertFalse(filter.allow(queryRequestMock));

        context.assertIsSatisfied();    
    }

    public void testResponseAdult() throws Exception {
        Response response = createResponse("adult");
        assertTrue(filter.allow(queryReplyMock, response));

        response = createResponse("Sex");
        assertTrue(filter.allow(queryReplyMock, response));

        /*
         * turn filter on
         */
        filter = new KeywordFilter(true, false);

        response = createResponse("adult");
        assertFalse(filter.allow(queryReplyMock, response));

        response = createResponse("Sex");
        assertFalse(filter.allow(queryReplyMock, response));

        response = createResponse("innocent");
        assertTrue(filter.allow(queryReplyMock, response));

        context.assertIsSatisfied();   
    }

    public void testOtherMessagesAreIgnored() throws Exception{
        context.checking(new Expectations() {{
            never(pingRequestMock);
        }});

        assertTrue(filter.allow(pingRequestMock));

        context.assertIsSatisfied();
    }

    private Response createResponse (String name) {
        return responseFactory.createResponse(0, 0, name, null, UrnHelper.SHA1);
    }
}
