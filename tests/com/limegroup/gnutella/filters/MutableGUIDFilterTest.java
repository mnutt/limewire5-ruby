package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

public class MutableGUIDFilterTest extends BaseTestCase {

    Mockery context;
        
    MutableGUIDFilter filter;
    final KeywordFilterStub filterKeyword = new KeywordFilterStub();
        
    QueryReply queryReplyMock;  
    QueryRequest queryRequestMock;
    
    public MutableGUIDFilterTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        filter = new MutableGUIDFilter(filterKeyword);
        
        queryReplyMock = context.mock(QueryReply.class);
        queryRequestMock = context.mock(QueryRequest.class);
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
        Message msg = null;
        
        /*
         * check if filter allows messages
         */
        assertTrue(filter.allow(msg));
        
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
        
        assertFalse(filter.allow(queryReplyMock));
        assertTrue(filter.allow(queryReplyMock));
        
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
        
        assertTrue(filter.allow(queryReplyMock));
        assertTrue(filter.allow(queryReplyMock));
        
        context.assertIsSatisfied();
    }
    
    public void testOtherMessagesAreIgnored() throws Exception{
        context.checking(new Expectations()
        {{ never(queryRequestMock);
        }});
        
        assertTrue(filter.allow(queryRequestMock));
        
        context.assertIsSatisfied();
    }
    
    private class KeywordFilterStub extends KeywordFilter {
                               
        @Override
        boolean allow(QueryReply qr){
            return false;
        }
    }
}
