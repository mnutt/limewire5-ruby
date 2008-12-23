package org.limewire.filter;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class DefaultFilterSupportTest extends BaseTestCase {

    private DefaultFilterSupport<Object> filterSupport;

    public DefaultFilterSupportTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DefaultFilterSupportTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        filterSupport = new DefaultFilterSupport<Object>();
    }
    
    public void testAddRemoveBlackListFilter() {
        Object o = new Object();
        assertTrue(filterSupport.allow(o));
        Filter<Object> filter = new ObjectFilter(o, false);
        filterSupport.addBlackListFilter(filter);
        assertFalse(filterSupport.allow(o));
        filterSupport.removeBlackListFilter(filter);
        assertTrue(filterSupport.allow(o));
    }

    public void testAddRemoveWhiteListFilter() {
        Object o = new Object();
        assertTrue(filterSupport.allow(o));
        Filter<Object> filter = new ObjectFilter(o, false);
        filterSupport.addBlackListFilter(filter);
        assertFalse(filterSupport.allow(o));
        
        Filter<Object> whiteListFilter = new ObjectFilter(o, true);
        filterSupport.addWhiteListFilter(whiteListFilter);
        assertTrue(filterSupport.allow(o));
        filterSupport.removeWhiteListFilter(whiteListFilter);
        assertFalse(filterSupport.allow(o));
    }

    private class ObjectFilter implements Filter<Object> {
        
        private final Object obj;
        private final boolean allow;

        public ObjectFilter(Object obj, boolean allow) {
            this.obj = obj;
            this.allow = allow;
        }

        @Override
        public boolean allow(Object t) {
            if (obj == t) {
                return allow;
            }
            return true;
        }
    }
}
