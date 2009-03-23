package com.limegroup.gnutella.filters;

import java.util.Collections;
import java.util.concurrent.Callable;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.io.GUID;

import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for DuplicateFilter
 */
// TODO convert to BaseTestCase, get rid of LimeXMLDocument dependencies in last test case
public class DuplicateFilterTest extends LimeTestCase {
    
    DuplicateFilter filter;
    PingRequest pr;
    QueryRequest qr;
    private Mockery context;
    private QueryRequestFactory queryRequestFactory;
    
	public DuplicateFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DuplicateFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	protected void setUp() throws Exception {
	    filter = new DuplicateFilter();
	    filter.setQueryLag(50);
	    filter.setGUIDLag(50);
	    context = new Mockery();
	    qr = context.mock(QueryRequest.class);
	    pr = context.mock(PingRequest.class);
	    
	    queryRequestFactory = LimeTestUtils.createInjector().getInstance(QueryRequestFactory.class);
	}
	
	private void addDefaultReturnValues() {
	    // specify default return values
        context.checking(new Expectations() {{
            allowing(pr).getHops(); will(returnValue((byte)2));
            allowing(qr).getHops(); will(returnValue((byte)2));
            allowing(qr).getQuery(); will(returnValue("blah"));
            allowing(qr).getRichQuery(); will(returnValue(null));
            allowing(qr).getQueryUrns(); will(returnValue(Collections.emptySet()));
            allowing(qr).getMetaMask(); will(returnValue(QueryRequest.AUDIO_MASK));
            allowing(qr).getGUID(); will(new CallableAction(new Callable<byte[]>() {
                public byte[] call() throws Exception {
                    return new GUID().bytes();
                }
            }));
        }});
	}
    
	public void testPingAndQueryWithSameGUIDAreRejected() throws Exception {
	    final GUID guid = new GUID();
	    context.checking(new Expectations() {{
	        exactly(1).of(pr).getGUID();
	        will(returnValue(guid.bytes()));
	        exactly(2).of(qr).getGUID();
	        will(returnValue(guid.bytes()));
	    }});
	    addDefaultReturnValues();
	    
	    assertTrue(filter.allow(pr));
	    assertFalse(filter.allow(qr));

	    waitForGUIDFilterToBePurged();
	    
	    assertTrue(filter.allow(qr));
	    
	    context.assertIsSatisfied();
	}
	
	public void testSameGUIDPingIsNotAllowedBeforeTimeout() throws Exception {
	    final GUID guid = new GUID();
	    context.checking(new Expectations() {{
	        exactly(3).of(pr).getGUID();
	        will(returnValue(guid.bytes()));
	    }});
	    addDefaultReturnValues();
	    
	    assertTrue(filter.allow(pr));
        assertFalse(filter.allow(pr));
        
        waitForGUIDFilterToBePurged();
        
        assertTrue(filter.allow(pr));
        context.assertIsSatisfied();
	}

	public void testSameGUIDDifferentHopCountAllowed() {
	    context.checking(new Expectations() {{
	        GUID guid = new GUID();
	        allowing(pr).getGUID(); will(returnValue(guid.bytes()));
	        one(pr).getHops(); will(returnValue((byte)2));
	        one(pr).getHops(); will(returnValue((byte)3));
	    }});
	    addDefaultReturnValues();
	    
	    assertTrue(filter.allow(pr));
	    assertTrue(filter.allow(pr));
	    
	    context.assertIsSatisfied();
	}
	
	// wait for guid filter to be purged
	private void waitForGUIDFilterToBePurged() throws Exception {
	    synchronized (filter) {
	        try {
	            int lag = filter.getGUIDLag() * 2;
	            assertGreaterThan(0, lag);
	            filter.wait(lag);
	        } catch (InterruptedException e) { }
	    }
    }
	
    private void waitForQueryRequestFilterToBePurged() throws Exception {
        synchronized (filter) {
            try {
                int lag = filter.getQueryLag() * 3;
                assertGreaterThan(0, lag);
                filter.wait(lag);
            } catch (InterruptedException e) { }
        }
    }

	public void testQueryStringDuplicate() throws Exception {
	    context.checking(new Expectations() {{
	        exactly(2).of(qr).getQuery(); will(returnValue("search1"));
	        exactly(3).of(qr).getQuery(); will(returnValue("search2"));
	        exactly(2).of(qr).getQuery(); will(returnValue("search3"));
	        exactly(12).of(qr).getHops(); will(returnValue((byte)2));
	        exactly(2).of(qr).getHops(); will(returnValue((byte)3));
	    }});
	    addDefaultReturnValues();
	    
	    assertTrue("pristine state, should be allowed", filter.allow(qr));
	    assertFalse("same query in there, not allowed", filter.allow(qr));
        assertTrue("different query, should be allowed", filter.allow(qr));

        waitForQueryRequestFilterToBePurged();
        
        assertTrue("cache cleared, same query should be allowed", filter.allow(qr));
        
        assertFalse("same query, not allowed", filter.allow(qr));
        
        assertTrue("different query, allowed", filter.allow(qr));
        
        assertTrue("same query, different hop, allowed", filter.allow(qr));
        
        context.assertIsSatisfied();
    }
        
    public void testURNDuplicate() throws Exception  {
        context.checking(new Expectations() {{
            exactly(2).of(qr).getQueryUrns(); will(returnValue(Collections.singleton(UrnHelper.SHA1)));
            exactly(3).of(qr).getQueryUrns(); will(returnValue(Collections.singleton(UrnHelper.UNIQUE_SHA1)));
        }});
        addDefaultReturnValues();
        
        assertTrue("pristine state, allowed", filter.allow(qr));
        assertFalse("same urn query, not allowed", filter.allow(qr));
        
        assertTrue("different urn query, allowed", filter.allow(qr));
        assertFalse("same urn query, not allowed", filter.allow(qr));

        waitForQueryRequestFilterToBePurged();
        
        assertTrue("cache cleared, same urn query allowed", filter.allow(qr));
        
        context.assertIsSatisfied();
    }

    // TODO, remove dependencies by mocking LimeXMLDocument
    public void testXMLDuplicate() throws Exception {
        
        // use default values, construction takes longer for real query results
        filter.setQueryLag(DuplicateFilter.QUERY_LAG);
        filter.setGUIDLag(DuplicateFilter.GUID_LAG);
        
        // Only allowed once in the timeframe ...
        qr = queryRequestFactory.createQuery("tests");
        assertTrue(filter.allow(qr));
        assertFalse(filter.allow(qr));
        // Invalid XML, considered same as plaintext.
        qr = queryRequestFactory.createQuery("tests", "<?xml");
        assertFalse(filter.allow(qr));
        qr = queryRequestFactory.createQuery("tests",
            "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"sam\" artist=\"sam's band\"></audio></audios>");
        // same plain-text, different XML, allowed ...
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr = queryRequestFactory.createQuery("another test",
            "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"sam\" artist=\"sam's band\"></audio></audios>");
        // same XML, different plaint-text, allowed ...
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr = queryRequestFactory.createQuery("another test",
            "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"sam\" artist=\"sam's choir\"></audio></audios>");        
        // different XML, allowed ...
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));        
        qr = queryRequestFactory.createQuery("another test",
            "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"sam\" artist=\"sam's choir\"></audio></audios>");        
        //same XML and plain-text, not allowed.
        assertTrue(!filter.allow(qr));
    }
    
    private static class CallableAction extends CustomAction {

        private Callable callable;
        
        public CallableAction(Callable callable) {
            super("Calls a callable");
            this.callable = callable;
        }
        
        public Object invoke(Invocation invocation) throws Throwable {
            return callable.call(); 
        }
    }
}    
