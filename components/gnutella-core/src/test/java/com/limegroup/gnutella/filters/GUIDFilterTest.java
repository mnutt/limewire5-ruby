package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Unit tests for GUIDFilterTest
 */
public class GUIDFilterTest extends BaseTestCase {
    SpamFilter filter;
    byte[] guid;
    private Mockery context;
    private QueryRequest query;
    
    
    public GUIDFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GUIDFilterTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
        guid=new byte[16];
        
        filter = new GUIDFilter();
        context = new Mockery();
        
        
        query = context.mock(QueryRequest.class);
        
        context.checking(new Expectations()
        {{ one (query).getGUID();
           will(returnValue(guid));
        }});
        
    }
    
    

    public void testDisallow() {
        guid[0]=(byte)0x41;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5A;

        assertFalse( filter.allow(query));
        context.assertIsSatisfied();
    }

    public void testAllow1() {
        guid[0]=(byte)0x41;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5B;
		
        assertTrue(filter.allow(query));
        context.assertIsSatisfied();
    }

    public void testAllow2() {
        guid[0]=(byte)0x42;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5A;

        assertTrue(filter.allow(query));
        context.assertIsSatisfied();
    }
}
