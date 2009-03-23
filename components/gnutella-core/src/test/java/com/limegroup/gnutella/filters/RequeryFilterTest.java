package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.QueryRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;

/**
 * Unit tests for RequeryFilter
 */
public class RequeryFilterTest extends BaseTestCase {
        
    private Mockery context;
    private SpamFilter filter;
    
	public RequeryFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(RequeryFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
    public void setUp() {
	    context =  new Mockery();
        filter = new RequeryFilter();
    }
	
	public void testNormalRequery() throws Exception {

        QueryRequest req;

        req = context.mock(QueryRequest.class);
        mockQueryRequest(req, "Requery");
        assertTrue(filter.allow(req));
        assertTrue(filter.allow(req));

    }
	
	public void testLegacy() throws Exception {
	    
	    QueryRequest req;
	    
	    req = context.mock(QueryRequest.class);
	    mockQueryRequest(req, "Hello");
        assertTrue(filter.allow(req));
	    req = context.mock(QueryRequest.class); 
	    mockQueryRequest(req, "Hello");
        assertTrue(filter.allow(req));
        req = context.mock(QueryRequest.class); 
        mockQueryRequest(req, "Hel lo");
        assertTrue(filter.allow(req));
        req = context.mock(QueryRequest.class); 
        mockQueryRequest(req, "asd");
        assertTrue(filter.allow(req));
        context.assertIsSatisfied();
      
    }
	
	public void testGUIDCreate() { 
	    
	    byte[] guid = GUID.makeGuid();
        guid[0] = (byte) 0x02;
        guid[1] = (byte) 0x01;
        guid[2] = (byte) 0x17;
        guid[3] = (byte) 0x05;
        guid[13] = (byte) 0x2E;
        guid[14] = (byte) 0x05;
        
        assertTrue(GUID.isLimeGUID(guid));
        assertTrue(GUID.isLimeRequeryGUID(guid, 1));
        assertFalse(GUID.isLimeRequeryGUID(guid, 0));

        QueryRequest req = context.mock(QueryRequest.class); 
        this.mockQueryRequest(req, guid, "asdf");

       assertFalse(filter.allow(req));
       
       context.assertIsSatisfied();
    }
	
	
    private void mockQueryRequest(final QueryRequest req, final String query) {

        context.checking(new Expectations() {
            {   atLeast(1).of(req).getGUID();
                will(returnValue(GUID.makeGuid()));
            }
        });
    }
    
    private void mockQueryRequest(final QueryRequest req, final byte[] guid, final String query) {
        
        context.checking(new Expectations() {
            {   atLeast(1).of(req).getGUID();
                will(returnValue(guid));
            }
        });
    }
}
