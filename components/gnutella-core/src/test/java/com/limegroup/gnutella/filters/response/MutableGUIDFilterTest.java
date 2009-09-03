package com.limegroup.gnutella.filters.response;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.KeywordFilter;
import com.limegroup.gnutella.messages.QueryReply;

public class MutableGUIDFilterTest extends BaseTestCase {

    Mockery context;
        
    MutableGUIDFilter filter;
    final KeywordFilterStub filterKeyword = new KeywordFilterStub();
        
    QueryReply queryReplyMock;
    Response responseMock;
    
    public MutableGUIDFilterTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        filter = new MutableGUIDFilter(filterKeyword);
        
        queryReplyMock = context.mock(QueryReply.class);
        responseMock = context.mock(Response.class);
    }
    
    public static Test suite() {
        return buildTestSuite(MutableGUIDFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /*
     * add and remove GUIDS:
     * add GUIDs and test accordingly
     * remove GUIDS and test accordingly
     */
    public void testAddRemoveGUID(){
        
        final GUID guid = new GUID();
        final GUID guid2 = new GUID();
        
        /*
         * add guid to filter
         */
        filter.addGUID(guid.bytes());
               
        context.checking(new Expectations() {{
            exactly(1).of(queryReplyMock).getGUID();
            will(returnValue(guid.bytes()));
            exactly(1).of(queryReplyMock).getGUID();
            will(returnValue(guid2.bytes()));
        }});
        
        assertFalse(filter.allow(queryReplyMock, responseMock));
        assertTrue(filter.allow(queryReplyMock, responseMock));
        
        /*
         * remove guid from filter
         */
        filter.removeGUID(guid.bytes());
        
        context.checking(new Expectations() {{
            exactly(1).of(queryReplyMock).getGUID();
            will(returnValue(guid.bytes()));
            exactly(1).of(queryReplyMock).getGUID();
            will(returnValue(guid2.bytes()));
        }});
        
        assertTrue(filter.allow(queryReplyMock, responseMock));
        assertTrue(filter.allow(queryReplyMock, responseMock));
        
        context.assertIsSatisfied();
    }
    
    private class KeywordFilterStub extends KeywordFilter {
        
        KeywordFilterStub() {
            super(false, false);
        }
        
        @Override
        public boolean allow(QueryReply qr, Response response){
            return false;
        }
    }
}
