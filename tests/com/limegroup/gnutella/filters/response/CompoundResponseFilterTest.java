package com.limegroup.gnutella.filters.response;

import java.util.Collections;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

public class CompoundResponseFilterTest extends BaseTestCase {

    private Mockery context;
    private ResponseFilter blackListFilter;
    private ResponseFilter whiteListFilter;
    private CompoundResponseFilter compoundFilter;

    public CompoundResponseFilterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(CompoundResponseFilterTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        blackListFilter = context.mock(ResponseFilter.class);
        whiteListFilter = context.mock(ResponseFilter.class);
        
        compoundFilter = new CompoundResponseFilter(Collections.singleton(blackListFilter), Collections.singleton(whiteListFilter));
    }
    
    public void testWhiteListFilterOverridesBlackListFilter() {
        context.checking(new Expectations() {{
            one(blackListFilter).allow(null, null);
            will(returnValue(false));
            one(whiteListFilter).allow(null, null);
            will(returnValue(true));
        }});
        
        assertTrue(compoundFilter.allow(null, null));
        context.assertIsSatisfied();
    }

    public void testWhiteListFilterIsIgnoredIfBlackListFilterAllowsResponse() {
        context.checking(new Expectations() {{
            one(blackListFilter).allow(null, null);
            will(returnValue(true));
            never(whiteListFilter).allow(null, null);
        }});
        
        assertTrue(compoundFilter.allow(null, null));
        context.assertIsSatisfied();
    }
}
