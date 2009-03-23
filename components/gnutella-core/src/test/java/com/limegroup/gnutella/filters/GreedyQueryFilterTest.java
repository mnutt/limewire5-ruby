package com.limegroup.gnutella.filters;

import junit.framework.Test;
import org.limewire.util.BaseTestCase;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
import org.jmock.Expectations;
import org.jmock.Mockery;

/**
 * Unit tests for GreedyQueryFilter
 */
public class GreedyQueryFilterTest extends BaseTestCase {
    
    private Mockery context;
    private SpamFilter filter;
   
	public GreedyQueryFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(GreedyQueryFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	@Override
    public void setUp()
    {   context = new Mockery();
        filter = new GreedyQueryFilter();
    }
	
	/**
	 * Should only return true on allow if not a QueryRequest
	 */
    public void testPingRequest() throws Exception {

        final PingRequest req = context.mock(PingRequest.class);
        
        context.checking(new Expectations() {
            {
                never(req);
            }
        });
        
        assertTrue(filter.allow(req));
        context.assertIsSatisfied();

    }

    public void testQueryVariousDeny() throws Exception {

        QueryRequest req;
        
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "a",(byte)5);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
        
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*", (byte)5);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();

        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "a.asf", (byte)5);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();

        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "z.mpg", (byte)5);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
    }

    public void testQueryVariousAllow() throws Exception {
        
        QueryRequest req;

        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "z.mp", (byte)5);
        assertTrue(filter.allow(req));
        context.assertIsSatisfied();

        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "z mpg", (byte)5);
        assertTrue(filter.allow(req));
        context.assertIsSatisfied();
        
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "1.mpg", (byte)5);
        mockQueryRequestDefHops(req);
        mockQueryRequestSetTTL(req);
        assertTrue(filter.allow(req));
        context.assertIsSatisfied();
    }
 
    public void testNetworkVariousAllow() throws Exception { 
        
        QueryRequest req;
           
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "--**.*-", (byte)3, (byte)2);
        mockQueryRequestSetTTL(req);
        assertTrue(filter.allow(req));
        context.assertIsSatisfied();

        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "britney*.*", (byte)2, (byte)3);
        assertTrue(filter.allow(req));
        context.assertIsSatisfied();
    
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "new order*", (byte)1, (byte)6);
        assertTrue(filter.allow(req));
        context.assertIsSatisfied();
    }
    
    
    public void testNetworkVariousDeny() throws Exception {
        
        QueryRequest req;
        
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*.mpg", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
               
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*.mp3", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
       
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*.*", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
        
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*.MP3", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
      
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*.mp3", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();

        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "mpg", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
        
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "MP3", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
        
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "MPG", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
        
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "a.b", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
       
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*.*-", (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();
       
        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*****" , (byte)1, (byte)4);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();

        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "*.*.", (byte)2, (byte)3);
        assertFalse(filter.allow(req));
        context.assertIsSatisfied();   
    }
    
    
    private void mockQueryRequest(final QueryRequest req, final String query, final byte ttl) {

        context.checking(new Expectations() {
            {
                atLeast(1).of(req).getQuery();
                will(returnValue(query));
                allowing(req).getTTL();
                will(returnValue(ttl));
                allowing(req).hasQueryUrns();
                will(returnValue(false));
            }
        });
    }
    
    private void mockQueryRequest(final QueryRequest req, final String query, final byte ttl, final byte hops) {

        mockQueryRequest(req, query, ttl);
        
        context.checking(new Expectations() {
            {
                allowing(req).getHops();
                will(returnValue(hops));
            }
        });
    }
    
    public void mockQueryRequestSetTTL(final QueryRequest req) {      

        context.checking(new Expectations() {
            {
                allowing(req).setTTL(with(any(byte.class)));
            }
        });
    }
    
    public void mockQueryRequestDefHops(final QueryRequest msg) {
        context.checking(new Expectations() {
            {
                allowing(msg).getHops();
                will(returnValue((byte) 0));
            }
        });
    }
}
