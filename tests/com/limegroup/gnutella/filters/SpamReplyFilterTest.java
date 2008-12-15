package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;


/**
 * Unit tests for SpamReplyFilter
 */
public class SpamReplyFilterTest extends BaseTestCase {
          
    private SpamReplyFilter _filter = new SpamReplyFilter();
    private Mockery context = new Mockery();
    private QueryReply queryReplyMock = context.mock(QueryReply.class);
    private QueryRequest queryRequestMock = context.mock(QueryRequest.class);
   

	public SpamReplyFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(SpamReplyFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
    public void testReplies() throws Exception{
        assertTrue( allow("LIME"));
        assertTrue( allow("BEAR"));
        assertTrue( allow("RAZA"));
        assertTrue(!allow("MUTE"));
        assertTrue( allow("GTKG"));
        assertTrue( allow("GNUC"));
        
        context.assertIsSatisfied();
    }
    
    public void testOtherMessagesAreIgnored() throws Exception{
        context.checking(new Expectations()
        {{ never(queryRequestMock);
        }});
       
        assertTrue( _filter.allow(queryRequestMock));
        
        context.assertIsSatisfied();
    }
    
   private boolean allow(final String vendorCode) throws Exception {
        context.checking(new Expectations() {{
            exactly(1).of(queryReplyMock).getVendor();
            will(returnValue(vendorCode));
        }});
        
        return _filter.allow(queryReplyMock);
    }

}
